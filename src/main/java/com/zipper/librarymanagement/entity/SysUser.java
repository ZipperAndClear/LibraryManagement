package com.zipper.librarymanagement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
// 核心：对应你的数据库表名 sys_user
@TableName("sys_user")
public class SysUser {
    /**
     * 用户ID - 主键（自增）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 登录账号（学号/工号）
     */
    @TableField("username")
    private String username;

    /**
     * 密码（JSON序列化时忽略，绝对不返回给前端）
     */
    @JsonIgnore
    @TableField("password")
    private String password;

    /**
     * 真实姓名
     */
    @TableField("real_name")
    private String realName;

    /**
     * 用户头像URL
     */
    @TableField("avatar")
    private String avatar;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 手机号
     */
    @TableField("phone")
    private String phone;

    /**
     * 账号状态：1-正常 0-禁用
     */
    @TableField("status")
    private Integer status;

    /**
     * 逻辑删除：0-未删 1-已删
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

    // ---------------- 灵活扩展的轻量级业务方法 ----------------
    /**
     * 判断账号是否正常
     */
    public boolean isEnabled() {
        return this.status != null && this.status == 1;
    }

    /**
     * 判断账号是否被禁用
     */
    public boolean isDisabled() {
        return !isEnabled();
    }
}