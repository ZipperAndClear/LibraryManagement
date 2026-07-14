package com.zipper.librarymanagement.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
// 核心：对应数据库的 sys_user_role 表
@TableName("sys_user_role")
public class SysUserRole {
    /**
     * 用户ID（联合主键之一）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 角色ID（联合主键之一）
     */
    @TableField("role_id")
    private Long roleId;
}