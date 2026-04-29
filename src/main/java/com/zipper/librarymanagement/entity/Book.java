package com.zipper.librarymanagement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("book")
public class Book {
    /**
     * 图书ID - 主键（自增）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 国际标准书号 (ISBN)
     */
    @TableField("isbn")
    private String isbn;

    /**
     * 图书名称
     */
    @TableField("name")
    private String name;

    /**
     * 作者
     */
    @TableField("author")
    private String author;

    /**
     * 出版社
     */
    @TableField("publisher")
    private String publisher;

    /**
     * 图书价格
     */
    @TableField("price")
    private BigDecimal price;

    /**
     * 当前库存数量
     */
    @TableField("stock")
    private Integer stock;

    /**
     * 所属分类ID
     */
    @TableField("category_id")
    private Long categoryId;

    /**
     * 图书封面图片URL
     */
    @TableField("cover_url")
    private String coverUrl;

    /**
     * 图书内容简介
     */
    @TableField("introduction")
    private String introduction;

    /**
     * 状态：1-在库 2-全部借出 3-下架 4-遗失
     */
    @TableField("status")
    private Integer status;

    /**
     * 逻辑删除标记：0-未删 1-已删
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 录入人ID
     */
    @TableField("create_by")
    private Long createBy;

    /**
     * 创建时间（格式化输出）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 修改人ID
     */
    @TableField("update_by")
    private Long updateBy;

    /**
     * 修改时间（格式化输出）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private LocalDateTime updateTime;

    // ---------------- 以下是灵活扩展的轻量级业务方法 ----------------
    /**
     * 判断图书是否在库
     */
    public boolean isInStock() {
        return this.status != null && this.status == 1;
    }

    /**
     * 判断图书是否可借阅（在库且库存>0）
     */
    public boolean isAvailableForBorrow() {
        return isInStock() && this.stock != null && this.stock > 0;
    }

    /**
     * 判断图书是否已下架
     */
    public boolean isOffShelf() {
        return this.status != null && this.status == 3;
    }

    /**
     * 判断图书是否遗失
     */
    public boolean isLost() {
        return this.status != null && this.status == 4;
    }

    // ---------------- 状态枚举（替代魔法数字，提升可读性） ----------------
    public enum BookStatus {
        IN_STOCK(1, "在库"),
        ALL_BORROWED(2, "全部借出"),
        OFF_SHELF(3, "下架"),
        LOST(4, "遗失");

        private final Integer code;
        private final String desc;

        BookStatus(Integer code, String desc) {
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