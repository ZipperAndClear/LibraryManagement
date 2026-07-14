package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.entity.BorrowRecord;
import com.zipper.librarymanagement.vo.BorrowRecordVO;

import java.util.List;

/**
 * 借阅记录业务接口（核心业务模块）
 * <p>
 * 管理图书借阅的完整生命周期，涵盖借书、续借、还书、丢失标记以及逾期跟踪等核心操作。
 * 借书和还书涉及多张数据库表的联写操作（用户状态校验、库存扣减/恢复、罚款生成），
 * 全部在事务中完成以保证数据一致性。
 * </p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>借阅图书</b> — 校验用户状态与图书库存后生成借阅记录，并根据系统配置自动计算应还时间</li>
 *   <li><b>归还图书</b> — 记录实际归还时间、恢复库存，逾期时自动计算罚款并生成罚款单</li>
 *   <li><b>续借图书</b> — 在允许的续借次数范围内延长应还时间</li>
 *   <li><b>丢失标记</b> — 管理员将借阅记录标记为丢失，按图书价格生成赔偿罚款单</li>
 *   <li><b>记录查询</b> — 支持多条件分页检索借阅记录</li>
 *   <li><b>逾期管理</b> — 统计当前借阅数量、检测逾期记录、导出逾期列表</li>
 * </ul>
 *
 * @author zipper
 */
public interface BorrowRecordService extends IService<BorrowRecord> {

    /**
     * 借阅图书（核心多表事务方法）
     * <p>
     * 在单个事务中执行以下完整流程：
     * </p>
     * <ol>
     *   <li>校验用户状态：是否被禁用、有无未缴罚款、是否已达借阅上限（由
     *       {@code sys.borrow.max} 配置控制）、有无逾期未还记录</li>
     *   <li>校验图书状态：是否可借（未被删除、未被借出或丢失），用户是否已借此书且未归还</li>
     *   <li>调用 {@link BookService#deductStock(Long)} 原子性地扣减图书库存</li>
     *   <li>读取 {@code sys.borrow.days} 系统配置，计算应还时间
     *       （{@code expectReturnTime = now + borrowDays}）</li>
     *   <li>生成借阅记录实体并持久化到数据库</li>
     * </ol>
     *
     * @param userId 借阅用户的唯一标识，用于状态校验和记录关联
     * @param bookId 目标图书的唯一标识，用于库存扣减和记录关联
     */
    void borrowBook(Long userId, Long bookId);

    /**
     * 归还图书（核心多表事务方法）
     * <p>
     * 在单个事务中执行以下完整流程：
     * </p>
     * <ol>
     *   <li>校验借阅记录是否存在且状态为 {@code BORROWING} 或 {@code OVERDUE_UNRETURNED}，
     *       不允许重复归还或归还已丢失的记录</li>
     *   <li>设置 {@code actualReturnTime} 为当前时间，标记归还完成</li>
     *   <li>调用 {@link BookService#restoreStock(Long)} 恢复图书库存</li>
     *   <li>逾期判定：若 {@code actualReturnTime > expectReturnTime}，则根据
     *       {@code sys.fine.per.day} 配置计算逾期天数（向上取整），
     *       生成罚金金额并调用 {@link FineRecordService#createFine(Long, Long, java.math.BigDecimal, String)}
     *       创建罚款单</li>
     * </ol>
     *
     * @param borrowRecordId 要归还的借阅记录唯一标识
     * @param userId         执行归还操作的用户唯一标识，用于权限校验（普通用户只能归还自己的记录）
     */
    void returnBook(Long borrowRecordId, Long userId);

    /**
     * 续借图书
     * <p>
     * 在满足以下条件时将应还时间顺延一个借阅周期（由 {@code sys.borrow.days} 配置决定）：
     * </p>
     * <ul>
     *   <li>借阅状态必须为 {@code BORROWING}（已逾期的记录不可续借）</li>
     *   <li>当前续借次数未达到系统配置的上限（{@code sys.renew.max}）</li>
     *   <li>续借成功后，续借次数加一，{@code expectReturnTime} 从当前时间起顺延对应天数</li>
     * </ul>
     *
     * @param borrowRecordId 要续借的借阅记录唯一标识
     * @param userId         执行续借操作的用户唯一标识，用于权限校验
     */
    void renewBook(Long borrowRecordId, Long userId);

    /**
     * 管理员标记图书丢失
     * <p>
     * 当读者确认图书遗失且无法归还时，由管理员执行此操作：
     * </p>
     * <ol>
     *   <li>将借阅记录状态更新为 {@code BOOK_LOST}</li>
     *   <li>将关联图书的状态更新为 {@code LOST}</li>
     *   <li>读取图书的定价信息，按图书价格自动生成一笔赔偿罚款单，
     *       调用 {@link FineRecordService#createFine(Long, Long, java.math.BigDecimal, String)}</li>
     * </ol>
     *
     * @param borrowRecordId 要标记为丢失的借阅记录唯一标识
     */
    void markBookLost(Long borrowRecordId);

    /**
     * 分页查询借阅记录
     * <p>
     * 支持按用户、状态、关键词进行多条件组合筛选。查询结果每项记录会关联查询用户名、
     * 图书名、罚款金额等附加信息，便于前端展示完整的借阅详情。
     * </p>
     *
     * @param page    分页页码，从 1 开始
     * @param size    每页记录数
     * @param userId  用户唯一标识筛选（普通用户只能查询自己的记录，管理员可传 {@code null} 查询全部）
     * @param status  借阅状态筛选（{@code null} 表示查询全部状态）
     * @param keyword 图书名称关键词，支持模糊匹配（{@code null} 或空字符串表示不按关键词筛选）
     * @return 分页结果对象，每项为 {@link BorrowRecordVO}，包含用户名、书名、罚款金额等关联信息
     */
    IPage<BorrowRecordVO> listRecords(Integer page, Integer size,
                                      Long userId, Integer status, String keyword);

    /**
     * 统计用户当前借阅中的图书数量
     * <p>
     * 统计状态为 {@code BORROWING} 或 {@code OVERDUE_UNRETURNED} 的借阅记录总数。
     * 该数值用于借阅上限校验（与 {@code sys.borrow.max} 配置对比）。
     * </p>
     *
     * @param userId 目标用户的唯一标识
     * @return 用户当前借阅中的图书数量
     */
    int countCurrentBorrowing(Long userId);

    /**
     * 检查用户是否有逾期未还的记录
     * <p>
     * 查询条件：状态为 {@code BORROWING} 且 {@code expectReturnTime < now}。
     * 用户存在逾期记录时将被限制继续借阅。
     * </p>
     *
     * @param userId 目标用户的唯一标识
     * @return {@code true} 表示存在逾期未还记录，{@code false} 表示全部按期借阅中
     */
    boolean hasOverdueRecords(Long userId);

    /**
     * 获取全部逾期未还的借阅列表
     * <p>
     * 查询所有状态为 {@code BORROWING} 且超过应还时间的记录，用于管理员催还操作。
     * 返回数据包含关联的用户名和图书名称，方便管理员识别和联系。
     * </p>
     *
     * @return 逾期借阅记录列表，每项为 {@link BorrowRecordVO}，包含用户名和书名等关键信息；
     *         无逾期记录时返回空列表
     */
    List<BorrowRecordVO> getOverdueList();
}
