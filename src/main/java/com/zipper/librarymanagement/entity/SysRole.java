package com.zipper.librarymanagement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
// 核心：对应数据库的 sys_role 表
@TableName("sys_role")
public class SysRole {
    /**
     * 角色ID - 主键（自增）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 角色名称（如：超级管理员、学生）
     */
    @TableField("role_name")
    private String roleName;

    /**
     * 角色权限字符串（如：admin、student，唯一索引）
     */
    @TableField("role_code")
    private String roleCode;

    /**
     * 角色状态：1-正常 0-停用
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
     * 创建者ID
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
     * 更新者ID
     */
    @TableField("update_by")
    private Long updateBy;

    /**
     * 更新时间（格式化输出）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private LocalDateTime updateTime;

    // ---------------- 以下是灵活扩展的轻量级业务方法 ----------------
    /**
     * 判断角色是否正常
     */
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }

    /**
     * 判断角色是否停用
     */
    public boolean isDisabled() {
        return !isEnabled();
    }

    // ---------------- 状态枚举（替代魔法数字，提升可读性） ----------------
    public enum SysRoleStatus {
        NORMAL(1, "正常"),
        DISABLED(0, "停用");

        private final Integer code;
        private final String desc;

        SysRoleStatus(Integer code, String desc) {
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