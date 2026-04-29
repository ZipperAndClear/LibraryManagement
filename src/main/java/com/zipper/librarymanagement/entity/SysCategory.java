package com.zipper.librarymanagement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
// 核心：对应数据库的 sys_category 表
@TableName("sys_category")
public class SysCategory {
    /**
     * 分类ID - 主键（自增）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 父分类ID（0为顶级节点）
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 分类名称
     */
    @TableField("name")
    private String name;

    /**
     * 显示顺序
     */
    @TableField("sort")
    private Integer sort;

    /**
     * 逻辑删除标记：0-未删 1-已删
     */
    @TableLogic
    @TableField("is_deleted")
    private Integer isDeleted;

    /**
     * 创建时间（格式化输出）
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
     * 判断是否为顶级分类
     */
    public boolean isTopCategory() {
        return this.parentId == null || this.parentId == 0;
    }

    /**
     * 判断是否有父分类（非顶级）
     */
    public boolean hasParent() {
        return !isTopCategory();
    }
}