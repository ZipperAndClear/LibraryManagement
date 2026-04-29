package com.zipper.librarymanagement.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户列表项值对象
 * <p>管理员端用户管理页面的列表展示项，包含基础信息及该用户分配的角色名称</p>
 */
@Data
public class UserListVO {

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

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /** 该用户拥有的角色名称列表（如 ["超级管理员", "图书管理员"]） */
    private List<String> roleNames;
}
