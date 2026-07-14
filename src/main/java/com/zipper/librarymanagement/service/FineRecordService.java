package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.entity.FineRecord;
import com.zipper.librarymanagement.vo.FineRecordVO;

import java.math.BigDecimal;

/**
 * 罚款记录业务接口
 * <p>
 * 管理图书馆运营中产生的各类罚款的全生命周期，包括逾期罚款和图书丢失赔偿。
 * 罚款单通常由 {@link BorrowRecordService} 在还书或标记丢失时自动触发创建，
 * 支持用户主动缴纳和管理员豁免两种完结方式。
 * </p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>罚款创建</b> — 由借阅模块自动调用，根据逾期天数或图书价格生成罚款记录</li>
 *   <li><b>缴纳罚款</b> — 用户在线缴纳罚款，校验归属和状态后标记为已缴</li>
 *   <li><b>豁免罚款</b> — 管理员权限直接免除指定罚款</li>
 *   <li><b>罚款查询</b> — 支持多条件分页检索罚款记录</li>
 *   <li><b>状态检查</b> — 检测用户是否有未缴罚款及其总额，用于借阅资格校验</li>
 * </ul>
 *
 * @author zipper
 */
public interface FineRecordService extends IService<FineRecord> {

    /**
     * 创建罚款单
     * <p>
     * 由 {@link BorrowRecordService} 在还书逾期或管理员标记图书丢失时自动调用。
     * 创建的罚款记录初始状态为 {@code UNPAID}，关联到对应的借阅记录和受罚用户。
     * </p>
     *
     * @param borrowRecordId 触发罚款的借阅记录唯一标识，用于追溯罚款来源
     * @param userId         被罚款用户的唯一标识
     * @param fineAmount     罚款金额，逾期罚款由 {@code sys.fine.per.day} 配置 × 逾期天数计算；
     *                       丢失赔偿为图书定价
     * @param reason         罚款原因描述，如"逾期3天"或"丢失《Java编程思想》"
     */
    void createFine(Long borrowRecordId, Long userId, BigDecimal fineAmount, String reason);

    /**
     * 用户缴纳罚款
     * <p>
     * 用户在线支付罚款，系统进行以下校验：
     * </p>
     * <ul>
     *   <li>罚款记录必须存在且属于当前操作用户（防止越权操作）</li>
     *   <li>罚款状态必须为 {@code UNPAID}（已缴或已豁免的罚款不可重复缴纳）</li>
     * </ul>
     * <p>
     * 校验通过后将罚款状态更新为 {@code PAID}，并记录缴费时间。
     * </p>
     *
     * @param fineRecordId 要缴纳的罚款记录唯一标识
     * @param userId       执行缴纳操作的用户唯一标识，用于归属校验
     */
    void payFine(Long fineRecordId, Long userId);

    /**
     * 管理员豁免罚款
     * <p>
     * 由管理员直接免除指定的罚款记录，无需用户支付。
     * 豁免操作需校验操作者具有管理员权限（在 Controller 层或 AOP 层完成），
     * 豁免后将罚款状态更新为 {@code EXEMPTED} 并记录操作信息。
     * </p>
     *
     * @param fineRecordId 要豁免的罚款记录唯一标识
     */
    void exemptFine(Long fineRecordId);

    /**
     * 分页查询罚款记录
     * <p>
     * 支持按用户和缴费状态进行多条件组合筛选。查询结果每项记录会关联查询用户名、
     * 书名等附加信息，便于前端展示完整的罚款详情。
     * </p>
     *
     * @param page   分页页码，从 1 开始
     * @param size   每页记录数
     * @param userId 用户唯一标识筛选（普通用户只能查询自己的罚款，管理员可传 {@code null} 查询全部）
     * @param status 缴费状态筛选（{@code null} 表示查询全部状态）
     * @return 分页结果对象，每项为 {@link FineRecordVO}，包含用户名、书名等关联信息
     */
    IPage<FineRecordVO> listFines(Integer page, Integer size,
                                  Long userId, Integer status);

    /**
     * 检查用户是否有未缴纳罚款
     * <p>
     * 查询目标用户下状态为 {@code UNPAID} 的罚款记录。
     * 该方法用于借阅资格预检 — 存在未缴罚款的用户将被限制继续借阅。
     * </p>
     *
     * @param userId 目标用户的唯一标识
     * @return {@code true} 表示存在未缴纳的罚款记录，{@code false} 表示全部已缴或已豁免
     */
    boolean hasUnpaidFines(Long userId);

    /**
     * 获取用户未缴纳罚款的总额
     * <p>
     * 汇总目标用户所有状态为 {@code UNPAID} 的罚款记录的金额总和。
     * 该方法用于前端展示用户待缴金额，以及借阅资格校验。
     * </p>
     *
     * @param userId 目标用户的唯一标识
     * @return 用户未缴纳罚款的金额总和；如无未缴罚款则返回 {@code BigDecimal.ZERO}
     */
    BigDecimal getUnpaidTotal(Long userId);
}
