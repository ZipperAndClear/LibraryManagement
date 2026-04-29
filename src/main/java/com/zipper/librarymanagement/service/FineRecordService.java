package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.entity.FineRecord;
import com.zipper.librarymanagement.vo.FineRecordVO;

import java.math.BigDecimal;

/**
 * 罚款记录业务接口
 * <p>处理逾期罚款和图书丢失赔偿的全生命周期管理。
 * 罚款单由 BorrowRecordService 在还书时自动触发创建，
 * 支持用户缴纳和管理员豁免两种完结方式。</p>
 */
public interface FineRecordService extends IService<FineRecord> {

    /**
     * 创建罚款单
     * <p>由 BorrowRecordService 在还书逾期或标记丢失时调用</p>
     * @param borrowRecordId 关联的借阅记录 ID
     * @param userId         受罚用户 ID
     * @param fineAmount     罚款金额
     * @param reason         罚款原因描述
     */
    void createFine(Long borrowRecordId, Long userId, BigDecimal fineAmount, String reason);

    /**
     * 用户缴纳罚款
     * <p>校验：罚款记录必须属于当前用户、状态必须为 UNPAID</p>
     */
    void payFine(Long fineRecordId, Long userId);

    /**
     * 管理员豁免罚款
     */
    void exemptFine(Long fineRecordId);

    /**
     * 分页查询罚款记录
     * @param userId 用户 ID 筛选
     * @param status 缴费状态筛选（null=全部）
     * @return 分页结果，每项包含用户名、书名等关联信息
     */
    IPage<FineRecordVO> listFines(Integer page, Integer size,
                                  Long userId, Integer status);

    /**
     * 检查用户是否有未缴纳罚款
     */
    boolean hasUnpaidFines(Long userId);

    /**
     * 获取用户未缴纳罚款总额
     */
    BigDecimal getUnpaidTotal(Long userId);
}
