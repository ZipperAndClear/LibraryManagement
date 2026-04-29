package com.zipper.librarymanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zipper.librarymanagement.common.BusinessException;
import com.zipper.librarymanagement.entity.Book;
import com.zipper.librarymanagement.entity.FineRecord;
import com.zipper.librarymanagement.entity.BorrowRecord;
import com.zipper.librarymanagement.entity.SysUser;
import com.zipper.librarymanagement.mapper.BookMapper;
import com.zipper.librarymanagement.mapper.BorrowRecordMapper;
import com.zipper.librarymanagement.mapper.SysUserMapper;
import com.zipper.librarymanagement.service.BookService;
import com.zipper.librarymanagement.service.FineRecordService;
import com.zipper.librarymanagement.service.SysConfigService;
import com.zipper.librarymanagement.service.BorrowRecordService;
import com.zipper.librarymanagement.vo.BorrowRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 借阅记录业务实现类（系统最核心的业务模块）
 * <p>借书（borrowBook）和还书（returnBook）两个方法涉及多张表的联写操作，
 * 全部使用 @Transactional 保证事务一致性。具体流程如下：</p>
 * <ul>
 *   <li>借书：校验用户（状态/限额/逾期/罚款）→ 校验图书 → 原子扣库存 → 生成借阅记录</li>
 *   <li>还书：校验记录 → 恢复库存 → 逾期判定 → 计算罚金 → 生成罚款单（如果逾期）</li>
 * </ul>
 */
@Service
public class BorrowRecordServiceImpl extends ServiceImpl<BorrowRecordMapper, BorrowRecord> implements BorrowRecordService {

    @Autowired
    private BookService bookService;

    @Autowired
    private FineRecordService fineRecordService;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BookMapper bookMapper;

    @Override
    @Transactional
    public void borrowBook(Long userId, Long bookId) {
        // ========== 第一步：校验用户状态 ==========
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.isDisabled()) {
            throw new BusinessException("用户不存在或已被禁用");
        }
        // 校验是否有未缴罚款
        boolean hasUnpaid = fineRecordService.hasUnpaidFines(userId);
        if (hasUnpaid) {
            throw new BusinessException("您有未缴纳的罚款，请先缴纳");
        }
        // ========== 第二步：校验借阅限额 ==========
        int maxBorrow = sysConfigService.getIntByKey("sys.borrow.max");
        int currentBorrowing = countCurrentBorrowing(userId);
        if (currentBorrowing >= maxBorrow) {
            throw new BusinessException("已达到最大借阅数量(" + maxBorrow + "本)，无法继续借阅");
        }
        // ========== 第三步：校验逾期记录 ==========
        if (hasOverdueRecords(userId)) {
            throw new BusinessException("您有逾期未还的图书，请先归还");
        }
        // ========== 第四步：校验图书可借性 ==========
        if (!bookService.isAvailableForBorrow(bookId)) {
            throw new BusinessException("该书目前不可借阅");
        }
        // 校验用户是否已借此书且未还（防止重复借阅同一本书）
        Long alreadyBorrowed = lambdaQuery()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getBookId, bookId)
                .in(BorrowRecord::getStatus, 0, 2)
                .count();
        if (alreadyBorrowed > 0) {
            throw new BusinessException("您已借阅过该书且未归还");
        }
        // ========== 第五步：扣减库存 ==========
        boolean deducted = bookService.deductStock(bookId);
        if (!deducted) {
            throw new BusinessException("库存不足");
        }
        // ========== 第六步：生成借阅记录 ==========
        int borrowDays = sysConfigService.getIntByKey("sys.borrow.days");
        LocalDateTime now = LocalDateTime.now();
        BorrowRecord record = new BorrowRecord();
        record.setUserId(userId);
        record.setBookId(bookId);
        record.setBorrowTime(now);
        record.setExpectReturnTime(now.plusDays(borrowDays));
        record.setRenewCount(0);
        record.setStatus(BorrowRecord.BorrowStatus.BORROWING.getCode());
        save(record);
    }

    @Override
    @Transactional
    public void returnBook(Long borrowRecordId, Long userId) {
        // ========== 第一步：校验借阅记录 ==========
        BorrowRecord record = getById(borrowRecordId);
        if (record == null) {
            throw new BusinessException("借阅记录不存在");
        }
        if (!record.isBorrowing() && !record.isOverdueUnreturned()) {
            throw new BusinessException("该记录状态不允许归还");
        }
        // ========== 第二步：执行还书操作 ==========
        LocalDateTime now = LocalDateTime.now();
        record.setActualReturnTime(now);
        bookService.restoreStock(record.getBookId());
        // ========== 第三步：逾期判定 ==========
        if (now.isAfter(record.getExpectReturnTime())) {
            // 逾期归还
            record.setStatus(BorrowRecord.BorrowStatus.OVERDUE_RETURNED.getCode());
            long overdueDays = ChronoUnit.DAYS.between(record.getExpectReturnTime(), now);
            BigDecimal finePerDay = sysConfigService.getDecimalByKey("sys.fine.per_day");
            BigDecimal fineAmount = finePerDay.multiply(BigDecimal.valueOf(overdueDays));
            fineRecordService.createFine(borrowRecordId, userId, fineAmount,
                    "逾期归还（逾期" + overdueDays + "天）");
        } else {
            // 正常归还
            record.setStatus(BorrowRecord.BorrowStatus.RETURNED_NORMALLY.getCode());
        }
        updateById(record);
    }

    @Override
    @Transactional
    public void renewBook(Long borrowRecordId, Long userId) {
        BorrowRecord record = getById(borrowRecordId);
        if (record == null) {
            throw new BusinessException("借阅记录不存在");
        }
        // 只有借阅中（BORROWING）的图书可以续借
        if (!record.isBorrowing()) {
            throw new BusinessException("只有借阅中的图书可以续借");
        }
        // 逾期的图书不可续借
        if (record.getExpectReturnTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("图书已逾期，无法续借");
        }
        // 校验续借次数上限
        int maxRenew = sysConfigService.getIntByKey("sys.borrow.renew.max");
        if (record.getRenewCount() >= maxRenew) {
            throw new BusinessException("已达到最大续借次数(" + maxRenew + "次)");
        }
        // 顺延应还时间
        int borrowDays = sysConfigService.getIntByKey("sys.borrow.days");
        record.setExpectReturnTime(record.getExpectReturnTime().plusDays(borrowDays));
        record.setRenewCount(record.getRenewCount() + 1);
        updateById(record);
    }

    @Override
    @Transactional
    public void markBookLost(Long borrowRecordId) {
        BorrowRecord record = getById(borrowRecordId);
        if (record == null) {
            throw new BusinessException("借阅记录不存在");
        }
        if (!record.isBorrowing() && !record.isOverdueUnreturned()) {
            throw new BusinessException("该记录状态不允许标记丢失");
        }
        // 更新借阅记录状态
        record.setStatus(BorrowRecord.BorrowStatus.BOOK_LOST.getCode());
        updateById(record);
        // 更新图书状态为遗失
        Book book = bookMapper.selectById(record.getBookId());
        if (book != null) {
            book.setStatus(Book.BookStatus.LOST.getCode());
            bookMapper.updateById(book);
        }
        // 按图书价格生成赔偿罚款单
        if (book != null && book.getPrice() != null) {
            fineRecordService.createFine(borrowRecordId, record.getUserId(),
                    book.getPrice(), "图书丢失赔偿");
        }
    }

    @Override
    public IPage<BorrowRecordVO> listRecords(Integer page, Integer size,
                                             Long userId, Integer status, String keyword) {
        Page<BorrowRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BorrowRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(BorrowRecord::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(BorrowRecord::getStatus, status);
        }
        // 关键词搜索：通过书名模糊匹配找到对应的 bookId
        if (keyword != null && !keyword.isEmpty()) {
            List<Long> bookIds = bookMapper.selectList(
                    new LambdaQueryWrapper<Book>()
                            .like(Book::getName, keyword)
                            .select(Book::getId))
                    .stream().map(Book::getId).collect(Collectors.toList());
            if (!bookIds.isEmpty()) {
                wrapper.in(BorrowRecord::getBookId, bookIds);
            }
        }
        wrapper.orderByDesc(BorrowRecord::getCreateTime);
        IPage<BorrowRecord> recordPage = page(pageParam, wrapper);
        // 转换为 VO（补全用户名、书名、罚款金额）
        IPage<BorrowRecordVO> voPage = new Page<>(recordPage.getCurrent(), recordPage.getSize(), recordPage.getTotal());
        List<BorrowRecordVO> voList = recordPage.getRecords().stream().map(r -> {
            BorrowRecordVO vo = new BorrowRecordVO();
            vo.setId(r.getId());
            vo.setUserId(r.getUserId());
            vo.setBookId(r.getBookId());
            vo.setBorrowTime(r.getBorrowTime());
            vo.setExpectReturnTime(r.getExpectReturnTime());
            vo.setActualReturnTime(r.getActualReturnTime());
            vo.setRenewCount(r.getRenewCount());
            vo.setStatus(r.getStatus());
            // 关联查询用户名
            SysUser u = sysUserMapper.selectById(r.getUserId());
            if (u != null) {
                vo.setUserRealName(u.getRealName());
            }
            // 关联查询书名
            Book b = bookMapper.selectById(r.getBookId());
            if (b != null) {
                vo.setBookName(b.getName());
                vo.setBookIsbn(b.getIsbn());
            }
            // 关联查询罚款金额
            List<FineRecord> fines = fineRecordService.lambdaQuery()
                    .eq(FineRecord::getBorrowRecordId, r.getId())
                    .list();
            if (!fines.isEmpty()) {
                vo.setFineAmount(fines.get(0).getFineAmount());
            }
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public int countCurrentBorrowing(Long userId) {
        return lambdaQuery()
                .eq(BorrowRecord::getUserId, userId)
                .in(BorrowRecord::getStatus, 0, 2)
                .count().intValue();
    }

    @Override
    public boolean hasOverdueRecords(Long userId) {
        // 查询条件：状态为 BORROWING 且应还时间早于当前时间
        return lambdaQuery()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getStatus, BorrowRecord.BorrowStatus.BORROWING.getCode())
                .lt(BorrowRecord::getExpectReturnTime, LocalDateTime.now())
                .count() > 0;
    }

    @Override
    public List<BorrowRecordVO> getOverdueList() {
        List<BorrowRecord> records = lambdaQuery()
                .eq(BorrowRecord::getStatus, BorrowRecord.BorrowStatus.BORROWING.getCode())
                .lt(BorrowRecord::getExpectReturnTime, LocalDateTime.now())
                .list();
        return records.stream().map(r -> {
            BorrowRecordVO vo = new BorrowRecordVO();
            vo.setId(r.getId());
            vo.setUserId(r.getUserId());
            vo.setBookId(r.getBookId());
            vo.setBorrowTime(r.getBorrowTime());
            vo.setExpectReturnTime(r.getExpectReturnTime());
            vo.setRenewCount(r.getRenewCount());
            vo.setStatus(r.getStatus());
            SysUser u = sysUserMapper.selectById(r.getUserId());
            if (u != null) {
                vo.setUserRealName(u.getRealName());
            }
            Book b = bookMapper.selectById(r.getBookId());
            if (b != null) {
                vo.setBookName(b.getName());
            }
            return vo;
        }).collect(Collectors.toList());
    }
}
