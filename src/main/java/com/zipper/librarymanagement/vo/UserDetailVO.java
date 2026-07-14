package com.zipper.librarymanagement.vo;

import lombok.Data;

import java.util.List;

/**
 * 当前登录用户详情值对象
 * <p>用于获取当前登录用户完整信息的接口返回，包含用户基本信息及角色权限</p>
 */
@Data
public class UserDetailVO {

    /** 用户 ID */
    private Long id;

    /** 登录账号（学号/工号） */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 头像 URL */
    private String avatar;

    /** 电子邮箱 */
    private String email;

    /** 手机号 */
    private String phone;

    /** 账号状态：1-正常 0-禁用 */
    private Integer status;

    /** 角色名称列表（如 ["超级管理员", "图书管理员"]） */
    private List<String> roles;

    /** 角色编码列表（如 ["admin", "librarian"]） */
    private List<String> roleCodes;
}
