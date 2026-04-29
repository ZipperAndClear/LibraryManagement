package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.entity.SysUserRole;

import java.util.List;

/**
 * 用户角色关联业务接口
 * <p>管理用户与角色之间的多对多关系映射。
 * 由于 MyBatis-Plus 不支持多主键的 IService，此 Service 提供自定义的关联操作方法。</p>
 */
public interface SysUserRoleService extends IService<SysUserRole> {

    /**
     * 为用户分配角色
     * <p>先删除该用户已有的全部角色关联，再批量插入新角色列表。
     * 此操作为事务性，避免出现脏数据。</p>
     * @param userId  用户 ID
     * @param roleIds 角色 ID 列表（传入空列表表示清空该用户的角色）
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 获取用户已分配的角色 ID 列表
     */
    List<Long> getRoleIdsByUserId(Long userId);

    /**
     * 获取用户已分配的角色编码列表
     * <p>用于权限判断，如 ["admin", "student"]</p>
     */
    List<String> getRoleCodesByUserId(Long userId);
}
