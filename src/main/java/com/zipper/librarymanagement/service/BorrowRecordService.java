package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.entity.BorrowRecord;
import com.zipper.librarymanagement.vo.BorrowRecordVO;

import java.util.List;

/**
 * 借阅记录业务接口（核心业务模块）
 * <p>管理图书借阅的完整生命周期：借书 → 续借 → 还书（含逾期判定和罚款生成）。
 * 借书和还书涉及多张表的联写操作，全部在事务中完成。</p>
 */
public interface BorrowRecordService extends IService<BorrowRecord> {

    /**
     * 借阅图书（核心多表事务方法）
     * <p>完整事务流程：</p>
     * <ol>
     *   <li>校验用户状态（是否禁用、有无未缴罚款、是否已达借阅上限、有无逾期未还）</li>
     *   <li>校验图书状态（是否可借、用户是否已借此书未还）</li>
     *   <li>调用 BookService.deductStock 原子扣减库存</li>
     *   <li>读取 sys.borrow.days 配置计算应还时间</li>
     *   <li>生成借阅记录并保存</li>
     * </ol>
     */
    void borrowBook(Long userId, Long bookId);

    /**
     * 归还图书（核心多表事务方法）
     * <p>完整事务流程：</p>
     * <ol>
     *   <li>校验借阅记录存在且状态允许归还（BORROWING 或 OVERDUE_UNRETURNED）</li>
     *   <li>记录 actualReturnTime = now</li>
     *   <li>调用 BookService.restoreStock 恢复库存</li>
     *   <li>逾期判定：now > expectReturnTime 则计算罚金并调用 FineRecordService 生成罚款单</li>
     * </ol>
     */
    void returnBook(Long borrowRecordId, Long userId);

    /**
     * 续借图书
     * <p>校验条件：借阅状态为 BORROWING（逾期不可续借）、续借次数未达上限、
     * 续借成功后 expectReturnTime 顺延一个借阅周期</p>
     */
    void renewBook(Long borrowRecordId, Long userId);

    /**
     * 管理员标记图书丢失
     * <p>将借阅记录标记为 BOOK_LOST，图书标记为 LOST，
     * 按图书价格生成赔偿罚款单</p>
     */
    void markBookLost(Long borrowRecordId);

    /**
     * 分页查询借阅记录
     * @param userId 用户 ID 筛选（普通用户只能查自己的，管理员可查全部）
     * @param status 借阅状态筛选（null=全部）
     * @param keyword 图书名称关键词搜索
     * @return 分页结果，每项包含用户名、书名、罚款金额等关联信息
     */
    IPage<BorrowRecordVO> listRecords(Integer page, Integer size,
                                      Long userId, Integer status, String keyword);

    /**
     * 统计用户当前借阅中（BORROWING + OVERDUE_UNRETURNED）的数量
     */
    int countCurrentBorrowing(Long userId);

    /**
     * 检查用户是否有逾期未还记录
     * <p>查询条件：status = BORROWING 且 expectReturnTime < now</p>
     */
    boolean hasOverdueRecords(Long userId);

    /**
     * 获取全部逾期未还列表（管理员催还用）
     * @return 逾期借阅列表，包含用户名和书名
     */
    List<BorrowRecordVO> getOverdueList();
}
