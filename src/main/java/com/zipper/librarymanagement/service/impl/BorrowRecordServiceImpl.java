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
 * 借阅记录业务实现类——系统最核心的业务模块。
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li><b>借书</b>（{@link #borrowBook}）：多维度校验 → 原子扣库存 → 生成借阅记录</li>
 *   <li><b>还书</b>（{@link #returnBook}）：校验记录 → 恢复库存 → 逾期判定 → 计算罚金 → 生成罚款单</li>
 *   <li><b>续借</b>（{@link #renewBook}）：校验续借条件 → 顺延应还日期</li>
 *   <li><b>标记丢失</b>（{@link #markBookLost}）：更新记录状态 → 更新图书状态 → 生成赔偿罚款单</li>
 *   <li>借阅记录分页列表查询、逾期记录查询</li>
 * </ul>
 *
 * <h3>关键依赖</h3>
 * <ul>
 *   <li>{@link BookService}：库存原子操作（扣减/恢复）及可借性校验</li>
 *   <li>{@link FineRecordService}：生成逾期罚款、丢失赔偿、查询未缴罚款</li>
 *   <li>{@link SysConfigService}：读取借阅天数、续借上限、每日罚款金额等系统配置</li>
 *   <li>{@link SysUserMapper}：用户状态查询</li>
 *   <li>{@link BookMapper}：图书信息查询</li>
 * </ul>
 *
 * <h3>事务边界</h3>
 * <p><b>借书和还书</b>两个核心方法涉及多张表的联写操作（借阅记录表、图书库存表、罚款记录表），
 * 全部使用 {@code @Transactional} 保证事务一致性。任一环节失败则整体回滚。
 * 查询类方法（列表、统计）不加事务。</p>
 *
 * @see BorrowRecordService
 * @see BorrowRecord
 * @see BorrowRecord.BorrowStatus
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

    /**
     * 借阅图书——系统最核心的业务方法之一。
     *
     * <h4>详细步骤</h4>
     * <ol>
     *   <li><b>校验用户状态</b>：查询用户是否存在且未被禁用，检查是否有未缴罚款</li>
     *   <li><b>校验借阅限额</b>：读取系统配置最大借阅数（{@code sys.borrow.max}），
     *       统计用户当前在借数量，超出限额则拒绝</li>
     *   <li><b>校验逾期记录</b>：检查用户是否有逾期未还的图书（状态为借阅中且应还日期已过）</li>
     *   <li><b>校验图书可借性</b>：调用 {@link BookService#isAvailableForBorrow} 检查图书库存和状态；
     *       同时检查用户是否已借此书且未归还（防止重复借阅同一本书）</li>
     *   <li><b>扣减库存</b>：调用 {@link BookService#deductStock} 原子扣减，
     *       扣减失败则抛出"库存不足"</li>
     *   <li><b>生成借阅记录</b>：读取借阅天数配置（{@code sys.borrow.days}），
     *       设置借阅时间、应还时间、续借次数为 0、状态为"借阅中"，持久化到数据库</li>
     * </ol>
     *
     * <h4>前置条件</h4>
     * <ul>
     *   <li>用户状态正常（未被禁用）</li>
     *   <li>无未缴罚款</li>
     *   <li>未超出最大借阅数量</li>
     *   <li>无逾期未还记录</li>
     *   <li>目标图书库存 > 0 且状态为"在库"</li>
     *   <li>未重复借阅同一本书</li>
     * </ul>
     *
     * <h4>副作用</h4>
     * <ul>
     *   <li>图书库存减 1（若库存归零则状态自动变为"全部借出"）</li>
     *   <li>新增一条借阅记录</li>
     * </ul>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}，库存扣减与借阅记录生成在同一事务中，
     * 任一失败则全部回滚。</p>
     *
     * @param userId 借阅用户 ID
     * @param bookId 目标图书 ID
     * @throws BusinessException 若任一校验环节不通过
     */
    @Override
    @Transactional
    public void borrowBook(Long userId, Long bookId) {
        // ========== 第一步：校验用户状态 ==========
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null || user.isDisabled()) {
            throw new BusinessException("用户不存在或已被禁用");
        }
        boolean hasUnpaid = fineRecordService.hasUnpaidFines(userId);
        if (hasUnpaid) {
            throw new BusinessException("您有未缴纳的罚款，请先缴纳");
        }
        // ========== 第二步：校验借阅限额 ==========
        int maxBorrow = sysConfigService.getIntByKey("sys.borrow.max_num");
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
        int borrowDays = sysConfigService.getIntByKey("sys.borrow.max_days");
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

    /**
     * 归还图书——系统最核心的业务方法之一。
     *
     * <h4>详细步骤</h4>
     * <ol>
     *   <li><b>校验借阅记录</b>：查询记录是否存在；
     *       检查记录状态是否为"借阅中"（{@code BORROWING}）或"逾期未还"（{@code OVERDUE_UNRETURNED}），
     *       其他状态（已归还、已丢失等）不允许再次归还</li>
     *   <li><b>执行还书</b>：设置实际归还时间为当前时间；
     *       调用 {@link BookService#restoreStock} 恢复图书库存（stock + 1，状态恢复为"在库"）</li>
     *   <li><b>逾期判定</b>：
     *     <ul>
     *       <li>若当前时间晚于应还时间 → 判定为逾期归还：
     *         <ul>
     *           <li>状态设为"逾期已归还"（{@code OVERDUE_RETURNED}）</li>
     *           <li>计算逾期天数 = 当前时间 - 应还时间（按整天计）</li>
     *           <li>从系统配置读取每日罚款金额（{@code sys.fine.per_day}）</li>
     *           <li>调用 {@link FineRecordService#createFine} 生成罚款记录</li>
     *         </ul>
     *       </li>
     *       <li>否则 → 正常归还，状态设为"正常归还"（{@code RETURNED_NORMALLY}）</li>
     *     </ul>
     *   </li>
     *   <li>持久化更新借阅记录</li>
     * </ol>
     *
     * <h4>前置条件</h4>
     * <ul>
     *   <li>借阅记录存在</li>
     *   <li>记录状态为"借阅中"或"逾期未还"</li>
     *   <li>操作用户与借阅用户一致</li>
     * </ul>
     *
     * <h4>副作用</h4>
     * <ul>
     *   <li>图书库存 +1，状态恢复为"在库"</li>
     *   <li>若逾期：生成一条罚款记录（状态为"未缴"）</li>
     *   <li>借阅记录状态更新</li>
     * </ul>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}，库存恢复、罚款生成、状态更新在同一事务中。</p>
     *
     * @param borrowRecordId 借阅记录 ID
     * @param userId         执行归还操作的用户 ID
     * @throws BusinessException 若记录不存在 或 状态不允许归还
     */
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
            record.setStatus(BorrowRecord.BorrowStatus.OVERDUE_RETURNED.getCode());
            long overdueDays = ChronoUnit.DAYS.between(record.getExpectReturnTime(), now);
            BigDecimal finePerDay = sysConfigService.getDecimalByKey("sys.borrow.daily_fine");
            BigDecimal fineAmount = finePerDay.multiply(BigDecimal.valueOf(overdueDays));
            fineRecordService.createFine(borrowRecordId, userId, fineAmount,
                    "逾期归还（逾期" + overdueDays + "天）");
        } else {
            record.setStatus(BorrowRecord.BorrowStatus.RETURNED_NORMALLY.getCode());
        }
        updateById(record);
    }

    /**
     * 续借图书（顺延应还日期）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>查询借阅记录，不存在则抛出异常</li>
     *   <li>校验：仅"借阅中"（{@code BORROWING}）状态的记录可续借</li>
     *   <li>校验：已逾期（应还时间 < 当前时间）的记录不可续借</li>
     *   <li>校验续借次数上限（配置项 {@code sys.borrow.renew.max}）</li>
     *   <li>应还时间顺延一个借阅周期（配置项 {@code sys.borrow.days} 天）</li>
     *   <li>续借次数 +1</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param borrowRecordId 借阅记录 ID
     * @param userId         操作者用户 ID
     * @throws BusinessException 若记录不存在、状态不符、已逾期 或 超出续借次数
     */
    @Override
    @Transactional
    public void renewBook(Long borrowRecordId, Long userId) {
        BorrowRecord record = getById(borrowRecordId);
        if (record == null) {
            throw new BusinessException("借阅记录不存在");
        }
        if (!record.isBorrowing()) {
            throw new BusinessException("只有借阅中的图书可以续借");
        }
        if (record.getExpectReturnTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("图书已逾期，无法续借");
        }
        int maxRenew = sysConfigService.getIntByKey("sys.borrow.renew_limit");
        if (record.getRenewCount() >= maxRenew) {
            throw new BusinessException("已达到最大续借次数(" + maxRenew + "次)");
        }
        int borrowDays = sysConfigService.getIntByKey("sys.borrow.max_days");
        record.setExpectReturnTime(record.getExpectReturnTime().plusDays(borrowDays));
        record.setRenewCount(record.getRenewCount() + 1);
        updateById(record);
    }

    /**
     * 标记图书为丢失。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>查询借阅记录，不存在则抛出异常</li>
     *   <li>校验记录状态：仅"借阅中"或"逾期未还"可标记丢失</li>
     *   <li>更新借阅记录状态为"图书丢失"（{@code BOOK_LOST}）</li>
     *   <li>更新关联图书的状态为"遗失"（{@link Book.BookStatus#LOST}）</li>
     *   <li>按图书价格生成赔偿罚款单</li>
     * </ol>
     *
     * <h4>副作用</h4>
     * <ul>
     *   <li>借阅记录状态变为"图书丢失"</li>
     *   <li>图书状态变为"遗失"</li>
     *   <li>生成一条赔偿罚款记录（金额 = 图书原价）</li>
     * </ul>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}，状态更新与罚款生成在同一事务中。</p>
     *
     * @param borrowRecordId 借阅记录 ID
     * @throws BusinessException 若记录不存在 或 状态不允许标记丢失
     */
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
        record.setStatus(BorrowRecord.BorrowStatus.BOOK_LOST.getCode());
        updateById(record);
        Book book = bookMapper.selectById(record.getBookId());
        if (book != null && book.getPrice() != null) {
            fineRecordService.createFine(borrowRecordId, record.getUserId(),
                    book.getPrice(), "图书丢失赔偿");
        }
    }

    /**
     * 分页查询借阅记录列表。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>构建分页查询条件：按用户 ID、状态、关键词（通过书名反查 bookId）筛选</li>
     *   <li>按创建时间倒序排序</li>
     *   <li>查询结果集，逐条转换为 {@link BorrowRecordVO}，补全：
     *     <ul>
     *       <li>用户名（通过 user ID 关联查询）</li>
     *       <li>书名和 ISBN（通过 book ID 关联查询）</li>
     *       <li>关联罚款金额（通过 borrowRecordId 关联查询第一条罚款）</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param userId  用户 ID 筛选，可为 {@code null}
     * @param status  状态筛选（参见 {@link BorrowRecord.BorrowStatus}），可为 {@code null}
     * @param keyword 书名关键词搜索，可为 {@code null}
     * @return 分页结果，包含补全了用户名和书名的记录
     */
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
            SysUser u = sysUserMapper.selectById(r.getUserId());
            if (u != null) {
                vo.setUserRealName(u.getRealName());
            }
            Book b = bookMapper.selectById(r.getBookId());
            if (b != null) {
                vo.setBookName(b.getName());
                vo.setBookIsbn(b.getIsbn());
            }
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

    /**
     * 统计指定用户当前在借的图书数量。
     *
     * <p>查询条件：用户 ID 匹配 且 状态为"借阅中"（0）或"逾期未还"（2）。</p>
     *
     * @param userId 用户 ID
     * @return 当前在借数量
     */
    @Override
    public int countCurrentBorrowing(Long userId) {
        return lambdaQuery()
                .eq(BorrowRecord::getUserId, userId)
                .in(BorrowRecord::getStatus, 0, 2)
                .count().intValue();
    }

    /**
     * 判断指定用户是否有逾期未还的借阅记录。
     *
     * <p>逾期判定条件：状态为"借阅中"（{@code BORROWING}）且应还时间早于当前时间。</p>
     *
     * @param userId 用户 ID
     * @return {@code true} 有逾期记录，{@code false} 无
     */
    @Override
    public boolean hasOverdueRecords(Long userId) {
        return lambdaQuery()
                .eq(BorrowRecord::getUserId, userId)
                .eq(BorrowRecord::getStatus, BorrowRecord.BorrowStatus.BORROWING.getCode())
                .lt(BorrowRecord::getExpectReturnTime, LocalDateTime.now())
                .count() > 0;
    }

    /**
     * 获取全局逾期未还借阅记录列表。
     *
     * <p>查询条件：状态为"借阅中"且应还时间早于当前时间的全部记录，
     * 转换为 VO 时补全用户名和书名。</p>
     *
     * @return 逾期记录 VO 列表
     */
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
