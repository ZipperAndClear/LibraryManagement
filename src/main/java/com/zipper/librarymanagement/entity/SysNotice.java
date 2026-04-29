package com.zipper.librarymanagement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
// 核心：对应数据库的 sys_notice 表
@TableName("sys_notice")
public class SysNotice {
    /**
     * 公告ID - 主键（自增）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 公告标题
     */
    @TableField("title")
    private String title;

    /**
     * 公告正文（支持富文本）
     */
    @TableField("content")
    private String content;

    /**
     * 是否置顶：0-否 1-是
     */
    @TableField("is_top")
    private Integer isTop;

    /**
     * 公告状态：1-发布 0-草稿
     */
    @TableField("status")
    private Integer status;

    /**
     * 发布人ID
     */
    @TableField("create_by")
    private Long createBy;

    /**
     * 发布时间（格式化输出）
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
     * 判断是否置顶
     */
    public boolean isTop() {
        return this.isTop != null && this.isTop == 1;
    }

    /**
     * 判断是否已发布
     */
    public boolean isPublished() {
        return this.status != null && this.status == 1;
    }

    /**
     * 判断是否为草稿
     */
    public boolean isDraft() {
        return this.status != null && this.status == 0;
    }

    // ---------------- 状态枚举（替代魔法数字，提升可读性） ----------------
    public enum NoticeStatus {
        DRAFT(0, "草稿"),
        PUBLISHED(1, "发布");

        private final Integer code;
        private final String desc;

        NoticeStatus(Integer code, String desc) {
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