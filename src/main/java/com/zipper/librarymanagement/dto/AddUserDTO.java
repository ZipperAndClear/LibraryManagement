package com.zipper.librarymanagement.dto;

import lombok.Data;

import java.util.List;

/**
 * 管理员新增用户数据传输对象
 * <p>管理员在后台创建用户时提交的数据，包含角色分配信息</p>
 */
@Data
public class AddUserDTO {

    /** 登录账号（学号/工号，必填，需唯一） */
    private String username;

    /** 密码（明文） */
    private String password;

    /** 真实姓名 */
    private String realName;

    /** 电子邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 要分配的角色 ID 列表（如 [1, 2] 表示同时赋予管理员和学生角色） */
    private List<Long> roleIds;
}
