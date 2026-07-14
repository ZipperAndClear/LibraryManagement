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
 * 罚款记录业务实现类。
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li>罚款记录创建：由归还操作（逾期）或标记丢失操作触发</li>
 *   <li>罚款缴纳：用户主动缴纳罚款，管理员不可代缴（需校验用户身份）</li>
 *   <li>罚款豁免：管理员可免除指定罚款</li>
 *   <li>罚款列表查询：分页查询，支持按用户和状态筛选</li>
 *   <li>罚款统计：查询用户是否有未缴罚款、计算未缴总额</li>
 * </ul>
 *
 * <h3>关键依赖</h3>
 * <ul>
 *   <li>{@link SysUserMapper}：VO 转换时补全用户名</li>
 *   <li>{@link BookMapper}：VO 转换时通过借阅记录反查书名</li>
 *   <li>{@link BorrowRecordMapper}：VO 转换时关联借阅记录以获取书名</li>
 * </ul>
 *
 * <h3>事务边界</h3>
 * <p>所有涉及数据库写操作的方法（创建罚款、缴纳、豁免）均使用
 * {@code @Transactional} 注解。查询方法不加事务。</p>
 *
 * <h3>罚款状态流转</h3>
 * <pre>
 *   UNPAID(0) ──payFine──→ PAID(1)
 *   UNPAID(0) ──exemptFine──→ EXEMPTED(2)
 * </pre>
 *
 * @see FineRecordService
 * @see FineRecord
 * @see FineRecord.FineStatus
 */
@Service
public class FineRecordServiceImpl extends ServiceImpl<FineRecordMapper, FineRecord> implements FineRecordService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private BookMapper bookMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    /**
     * 创建罚款记录。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>构造 {@link FineRecord} 实体，填入借阅记录 ID、用户 ID、金额、原因</li>
     *   <li>状态初始化为"未缴"（{@link FineRecord.FineStatus#UNPAID}）</li>
     *   <li>持久化到数据库</li>
     * </ol>
     *
     * <h4>调用场景</h4>
     * <ul>
     *   <li>还书逾期：{@link BorrowRecordServiceImpl#returnBook} 调用</li>
     *   <li>图书丢失赔偿：{@link BorrowRecordServiceImpl#markBookLost} 调用</li>
     * </ul>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}，通常被外层事务方法调用，
     * 传播行为默认加入已有事务。</p>
     *
     * @param borrowRecordId 关联的借阅记录 ID
     * @param userId         欠款用户 ID
     * @param fineAmount     罚款金额
     * @param reason         罚款原因描述（如"逾期归还（逾期3天）"、"图书丢失赔偿"）
     */
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

    /**
     * 缴纳罚款（管理员可为任意用户缴纳，学生只能缴纳自己的罚款）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>查询罚款记录，不存在则抛出异常</li>
     *   <li><b>状态校验</b>：已缴纳（PAID）或已豁免（EXEMPTED）的记录不可重复缴纳</li>
     *   <li>状态更新为"已缴纳"（{@link FineRecord.FineStatus#PAID}），记录缴纳时间</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param fineRecordId 罚款记录 ID
     * @param userId       缴纳操作发起人 ID（用于审计，不作为所有权校验）
     * @throws BusinessException 若记录不存在、已缴纳或已豁免
     */
    @Override
    @Transactional
    public void payFine(Long fineRecordId, Long userId) {
        FineRecord fine = getById(fineRecordId);
        if (fine == null) {
            throw new BusinessException("罚款记录不存在");
        }
        if (fine.isPaid()) {
            throw new BusinessException("该罚款已缴纳");
        }
        if (fine.isExempted()) {
            throw new BusinessException("该罚款已被免除");
        }
        fine.setStatus(FineRecord.FineStatus.PAID.getCode());
        fine.setPayTime(LocalDateTime.now());
        updateById(fine);
    }

    /**
     * 豁免罚款（管理员操作）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>查询罚款记录，不存在则抛出异常</li>
     *   <li>状态更新为"已豁免"（{@link FineRecord.FineStatus#EXEMPTED}）</li>
     * </ol>
     *
     * <p><b>注意：</b>当前实现未校验操作者是否为管理员角色，
     * 该校验应在 Controller 层通过权限注解完成。</p>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param fineRecordId 罚款记录 ID
     * @throws BusinessException 若记录不存在
     */
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

    /**
     * 分页查询罚款记录列表。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>构建分页查询条件：按用户 ID 和状态筛选</li>
     *   <li>按创建时间倒序排序</li>
     *   <li>查询结果集，逐条转换为 {@link FineRecordVO}，补全：
     *     <ul>
     *       <li>用户名（通过 user ID 关联查询）</li>
     *       <li>书名（通过 borrowRecordId → bookId 两级关联查询）</li>
     *     </ul>
     *   </li>
     * </ol>
     *
     * @param page   页码（从 1 开始）
     * @param size   每页条数
     * @param userId 用户 ID 筛选，可为 {@code null}
     * @param status 状态筛选（参见 {@link FineRecord.FineStatus}），可为 {@code null}
     * @return 分页结果，包含补全了用户名和书名的记录
     */
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
            SysUser u = sysUserMapper.selectById(f.getUserId());
            if (u != null) {
                vo.setUserRealName(u.getRealName());
            }
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

    /**
     * 判断指定用户是否有未缴罚款。
     *
     * <p>查询条件：用户 ID 匹配 且 状态为"未缴"（{@code UNPAID}）。</p>
     *
     * <p>该方法在借书流程（{@link BorrowRecordServiceImpl#borrowBook}）中作为前置校验被调用。</p>
     *
     * @param userId 用户 ID
     * @return {@code true} 有未缴罚款，{@code false} 无
     */
    @Override
    public boolean hasUnpaidFines(Long userId) {
        return lambdaQuery()
                .eq(FineRecord::getUserId, userId)
                .eq(FineRecord::getStatus, FineRecord.FineStatus.UNPAID.getCode())
                .count() > 0;
    }

    /**
     * 计算指定用户未缴罚款的总额。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>查询该用户所有状态为"未缴"的罚款记录</li>
     *   <li>对所有罚款金额求和（使用 {@link BigDecimal#add}）</li>
     *   <li>若无未缴记录则返回 {@link BigDecimal#ZERO}</li>
     * </ol>
     *
     * @param userId 用户 ID
     * @return 未缴罚款总金额（不会为 {@code null}）
     */
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
