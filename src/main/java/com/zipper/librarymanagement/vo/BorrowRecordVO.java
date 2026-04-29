package com.zipper.librarymanagement.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 借阅记录值对象
 * <p>前端展示借阅列表示意的完整信息，包含关联的用户名、书名、罚款金额等跨表数据</p>
 */
@Data
public class BorrowRecordVO {

    /** 借阅流水 ID */
    private Long id;

    /** 借阅人（用户）ID */
    private Long userId;

    /** 借阅人真实姓名（关联 sys_user 表查询） */
    private String userRealName;

    /** 借阅图书 ID */
    private Long bookId;

    /** 图书名称（关联 book 表查询） */
    private String bookName;

    /** 图书 ISBN */
    private String bookIsbn;

    /** 借出时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime borrowTime;

    /** 应还时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expectReturnTime;

    /** 实际归还时间（未还时为 null） */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime actualReturnTime;

    /** 已续借次数 */
    private Integer renewCount;

    /** 借阅状态：0-借阅中 1-正常归还 2-逾期未还 3-逾期已还 4-图书遗失 */
    private Integer status;

    /** 该借阅记录的罚款金额（关联 fine_record 表查询，无罚款时为 null） */
    private BigDecimal fineAmount;
}
