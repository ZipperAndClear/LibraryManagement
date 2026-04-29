package com.zipper.librarymanagement.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 罚款记录值对象
 * <p>前端展示罚款列表的完整信息，包含关联的用户名和书名等跨表数据</p>
 */
@Data
public class FineRecordVO {

    /** 罚单 ID */
    private Long id;

    /** 关联的借阅记录 ID */
    private Long borrowRecordId;

    /** 受罚用户 ID */
    private Long userId;

    /** 受罚用户真实姓名（关联 sys_user 表查询） */
    private String userRealName;

    /** 关联的图书名称（通过 borrow_record → book 关联查询） */
    private String bookName;

    /** 罚款金额 */
    private BigDecimal fineAmount;

    /** 罚款原因（如"逾期归还（逾期5天）"、"图书丢失赔偿"） */
    private String reason;

    /** 缴费状态：0-未缴费 1-已缴费 2-已免除 */
    private Integer status;

    /** 实际缴费时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime payTime;

    /** 罚单生成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
