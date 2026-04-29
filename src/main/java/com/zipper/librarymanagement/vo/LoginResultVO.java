package com.zipper.librarymanagement.vo;

import lombok.Data;

import java.util.List;

/**
 * 登录成功响应值对象
 * <p>用户登录成功后返回的完整信息，包含身份凭证和基础用户信息</p>
 */
@Data
public class LoginResultVO {

    /** JWT 身份令牌（后续请求需在 Header 中携带此 token） */
    private String token;

    /** 用户 ID */
    private Long userId;

    /** 登录账号（学号/工号） */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 头像 URL */
    private String avatar;

    /** 角色编码列表（如 ["admin", "student"]） */
    private List<String> roles;
}
