package com.zipper.librarymanagement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("borrow_record")
public class BorrowRecord {
    /**
     * 借阅流水ID - 主键（自增）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 借阅人(用户)ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 借阅图书ID
     */
    @TableField("book_id")
    private Long bookId;

    /**
     * 借出时间（格式化输出）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("borrow_time")
    private LocalDateTime borrowTime;

    /**
     * 应还时间（格式化输出）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("expect_return_time")
    private LocalDateTime expectReturnTime;

    /**
     * 实际归还时间（格式化输出）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("actual_return_time")
    private LocalDateTime actualReturnTime;

    /**
     * 已续借次数
     */
    @TableField("renew_count")
    private Integer renewCount;

    /**
     * 借阅状态：0-借阅中 1-正常归还 2-逾期未还 3-逾期已还 4-图书遗失
     */
    @TableField("status")
    private Integer status;

    /**
     * 创建时间（即记录生成时间，格式化输出）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 状态更新时间（格式化输出）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private LocalDateTime updateTime;

    // ---------------- 以下是灵活扩展的轻量级业务方法 ----------------
    /**
     * 判断是否正在借阅中
     */
    public boolean isBorrowing() {
        return this.status != null && this.status == 0;
    }

    /**
     * 判断是否已正常归还
     */
    public boolean isReturnedNormally() {
        return this.status != null && this.status == 1;
    }

    /**
     * 判断是否逾期未还
     */
    public boolean isOverdueUnreturned() {
        return this.status != null && this.status == 2;
    }

    /**
     * 判断是否逾期已还
     */
    public boolean isOverdueReturned() {
        return this.status != null && this.status == 3;
    }

    /**
     * 判断图书是否遗失
     */
    public boolean isBookLost() {
        return this.status != null && this.status == 4;
    }

    /**
     * 判断是否已逾期（结合应还时间和当前时间判断，不依赖status字段）
     */
    public boolean isActuallyOverdue() {
        if (this.expectReturnTime == null) {
            return false;
        }
        // 如果当前时间晚于应还时间，且未归还/未遗失，则视为逾期
        return LocalDateTime.now().isAfter(this.expectReturnTime)
                && (isBorrowing() || isOverdueUnreturned());
    }

    // ---------------- 状态枚举（替代魔法数字，提升可读性） ----------------
    public enum BorrowStatus {
        BORROWING(0, "借阅中"),
        RETURNED_NORMALLY(1, "正常归还"),
        OVERDUE_UNRETURNED(2, "逾期未还"),
        OVERDUE_RETURNED(3, "逾期已还"),
        BOOK_LOST(4, "图书遗失");

        private final Integer code;
        private final String desc;

        BorrowStatus(Integer code, String desc) {
            this.code = code;
            this.desc = desc;
        }

        public Integer getCode() {
            return code;
        }

        public String getDesc() {
            return desc;
        }
    }
}