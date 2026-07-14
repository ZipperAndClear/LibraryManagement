package com.zipper.librarymanagement.common;

import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 角色权限校验工具类
 * <p>从 HttpServletRequest 中提取 JWT 解析出的角色列表，判断当前用户是否具有管理员权限。</p>
 */
@SuppressWarnings("unchecked")
public class RoleUtil {

    private RoleUtil() {}

    /**
     * 从请求属性中获取角色列表
     *
     * @param request HTTP 请求
     * @return 角色编码列表（如 ["admin", "librarian"]）
     */
    public static List<String> getRoles(HttpServletRequest request) {
        Object rolesObj = request.getAttribute("roles");
        if (rolesObj instanceof List) {
            return (List<String>) rolesObj;
        }
        return List.of();
    }

    /**
     * 判断当前用户是否为管理员（admin 或 librarian）。
     *
     * @param request HTTP 请求
     * @return true=管理员
     */
    public static boolean isAdmin(HttpServletRequest request) {
        List<String> roles = getRoles(request);
        return roles.contains("admin") || roles.contains("librarian");
    }

    /**
     * 检查当前用户必须是管理员，否则抛出无权限异常。
     *
     * @param request HTTP 请求
     * @throws BusinessException 若非管理员角色
     */
    public static void requireAdmin(HttpServletRequest request) {
        if (!isAdmin(request)) {
            throw new BusinessException("无权操作，仅管理员可执行");
        }
    }

    /**
     * 判断当前用户是否为超级管理员（仅 admin 角色，不包含 librarian）。
     *
     * @param request HTTP 请求
     * @return true=超级管理员
     */
    public static boolean isSuperAdmin(HttpServletRequest request) {
        return getRoles(request).contains("admin");
    }

    /**
     * 检查当前用户必须是超级管理员，否则抛出无权限异常。
     *
     * @param request HTTP 请求
     * @throws BusinessException 若非超级管理员
     */
    public static void requireSuperAdmin(HttpServletRequest request) {
        if (!isSuperAdmin(request)) {
            throw new BusinessException("无权操作，仅超级管理员可执行");
        }
    }
}
