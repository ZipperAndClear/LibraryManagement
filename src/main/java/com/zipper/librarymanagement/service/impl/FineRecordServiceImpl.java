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
import com.zipper.librarymanagement.mapper.FineRecordMapper;
import com.zipper.librarymanagement.mapper.BorrowRecordMapper;
import com.zipper.librarymanagement.mapper.SysUserMapper;
import com.zipper.librarymanagement.service.FineRecordService;
import com.zipper.librarymanagement.vo.FineRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 罚款记录业务实现类
 * <p>罚款记录由归还操作（逾期）或标记丢失操作触发创建。
 * 用户可直接在系统中缴纳罚款，管理员可豁免罚款。
 * 用户借书前会检查是否有未缴罚款。</p>
 */
@Service
public class FineRecordServiceImpl extends ServiceImpl<FineRecordMapper, FineRecord> implements FineRecordService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Override
    @Transactional
    public void createFine(Long borrowRecordId, Long userId, BigDecimal fineAmount, String reason) {
        FineRecord fine = new FineRecord();
        fine.setBorrowRecordId(borrowRecordId);
        fine.setUserId(userId);
        fine.setFineAmount(fineAmount);
        fine.setReason(reason);
        fine.setStatus(FineRecord.FineStatus.UNPAID.getCode());
        save(fine);
    }

    @Override
    @Transactional
    public void payFine(Long fineRecordId, Long userId) {
        FineRecord fine = getById(fineRecordId);
        if (fine == null) {
            throw new BusinessException("罚款记录不存在");
        }
        // 校验：只能缴纳自己的罚款
        if (!fine.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该罚款");
        }
        if (fine.isPaid()) {
            throw new BusinessException("该罚款已缴纳");
        }
        if (fine.isExempted()) {
            throw new BusinessException("该罚款已被免除");
        }
        // 标记为已缴纳
        fine.setStatus(FineRecord.FineStatus.PAID.getCode());
        fine.setPayTime(LocalDateTime.now());
        updateById(fine);
    }

    @Override
    @Transactional
    public void exemptFine(Long fineRecordId) {
        FineRecord fine = getById(fineRecordId);
        if (fine == null) {
            throw new BusinessException("罚款记录不存在");
        }
        fine.setStatus(FineRecord.FineStatus.EXEMPTED.getCode());
        updateById(fine);
    }

    @Override
    public IPage<FineRecordVO> listFines(Integer page, Integer size,
                                         Long userId, Integer status) {
        Page<FineRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<FineRecord> wrapper = new LambdaQueryWrapper<>();
        if (userId != null) {
            wrapper.eq(FineRecord::getUserId, userId);
        }
        if (status != null) {
            wrapper.eq(FineRecord::getStatus, status);
        }
        wrapper.orderByDesc(FineRecord::getCreateTime);
        IPage<FineRecord> finePage = page(pageParam, wrapper);
        // 转换为 VO（补全用户名和关联的图书名）
        IPage<FineRecordVO> voPage = new Page<>(finePage.getCurrent(), finePage.getSize(), finePage.getTotal());
        List<FineRecordVO> voList = finePage.getRecords().stream().map(f -> {
            FineRecordVO vo = new FineRecordVO();
            vo.setId(f.getId());
            vo.setBorrowRecordId(f.getBorrowRecordId());
            vo.setUserId(f.getUserId());
            vo.setFineAmount(f.getFineAmount());
            vo.setReason(f.getReason());
            vo.setStatus(f.getStatus());
            vo.setPayTime(f.getPayTime());
            vo.setCreateTime(f.getCreateTime());
            // 查询用户名
            SysUser u = sysUserMapper.selectById(f.getUserId());
            if (u != null) {
                vo.setUserRealName(u.getRealName());
            }
            // 通过借阅记录关联查询书名
            if (f.getBorrowRecordId() != null) {
                BorrowRecord record = borrowRecordMapper.selectById(f.getBorrowRecordId());
                if (record != null) {
                    Book b = bookMapper.selectById(record.getBookId());
                    if (b != null) {
                        vo.setBookName(b.getName());
                    }
                }
            }
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public boolean hasUnpaidFines(Long userId) {
        return lambdaQuery()
                .eq(FineRecord::getUserId, userId)
                .eq(FineRecord::getStatus, FineRecord.FineStatus.UNPAID.getCode())
                .count() > 0;
    }

    @Override
    public BigDecimal getUnpaidTotal(Long userId) {
        List<FineRecord> list = lambdaQuery()
                .eq(FineRecord::getUserId, userId)
                .eq(FineRecord::getStatus, FineRecord.FineStatus.UNPAID.getCode())
                .list();
        return list.stream()
                .map(FineRecord::getFineAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
