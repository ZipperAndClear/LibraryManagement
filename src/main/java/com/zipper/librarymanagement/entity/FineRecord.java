package com.zipper.librarymanagement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("fine_record")
public class FineRecord {
    /**
     * 罚单ID - 主键（自增）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 关联的借阅记录ID
     */
    @TableField("borrow_record_id")
    private Long borrowRecordId;

    /**
     * 受罚用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 罚款金额
     */
    @TableField("fine_amount")
    private BigDecimal fineAmount;

    /**
     * 罚款原因
     */
    @TableField("reason")
    private String reason;

    /**
     * 缴费状态：0-未缴费 1-已缴费 2-已免除
     */
    @TableField("status")
    private Integer status;

    /**
     * 实际缴费时间（格式化输出）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("pay_time")
    private LocalDateTime payTime;

    /**
     * 罚单生成时间（格式化输出）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间（格式化输出）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private LocalDateTime updateTime;

    // ---------------- 以下是灵活扩展的轻量级业务方法 ----------------
    /**
     * 判断是否未缴费
     */
    public boolean isUnpaid() {
        return this.status != null && this.status == 0;
    }

    /**
     * 判断是否已缴费
     */
    public boolean isPaid() {
        return this.status != null && this.status == 1;
    }

    /**
     * 判断是否已免除
     */
    public boolean isExempted() {
        return this.status != null && this.status == 2;
    }

    // ---------------- 状态枚举（替代魔法数字，提升可读性） ----------------
    public enum FineStatus {
        UNPAID(0, "未缴费"),
        PAID(1, "已缴费"),
        EXEMPTED(2, "已免除");

        private final Integer code;
        private final String desc;

        FineStatus(Integer code, String desc) {
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