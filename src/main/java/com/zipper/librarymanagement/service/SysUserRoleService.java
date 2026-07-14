package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.entity.SysUserRole;

import java.util.List;

/**
 * 用户角色关联业务接口
 * <p>管理用户（{@code sys_user}）与角色（{@code sys_role}）之间的多对多关系映射。
 * 用户和角色通过中间表 {@code sys_user_role} 关联，一个用户可以拥有多个角色，
 * 一个角色也可以被分配给多个用户。</p>
 *
 * <p>由于 MyBatis-Plus 的 {@code IService} 不直接支持复合主键的 CRUD，
 * 本接口提供自定义的关联操作方法，实现上使用原生 SQL 或自定义 Mapper 操作关联表。</p>
 *
 * <p>所有写操作均为事务性的：先删除旧关联再插入新关联，
 * 确保不会产生脏数据或部分失败的数据状态。</p>
 *
 * @author zipper
 * @since 1.0
 */
public interface SysUserRoleService extends IService<SysUserRole> {

    /**
     * 为用户分配角色
     * <p>采用“先删后增”策略，操作步骤：</p>
     * <ol>
     *   <li>删除该用户在 {@code sys_user_role} 表中的所有已有关联记录</li>
     *   <li>遍历 {@code roleIds}，逐一插入新的关联记录</li>
     * </ol>
     * <p>整个操作在事务中执行，任意步骤失败则全部回滚。</p>
     *
     * @param userId  用户 ID
     * @param roleIds 角色 ID 列表（传入 {@code null} 或空列表表示清空该用户的全部角色）
     * @throws com.zipper.librarymanagement.exception.BusinessException 若用户不存在
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 获取用户已分配的角色 ID 列表
     * <p>从 {@code sys_user_role} 表中查询指定用户的所有关联记录，
     * 提取其中的角色 ID 并返回。仅返回角色 ID，不包含角色详细信息。</p>
     *
     * @param userId 用户 ID
     * @return 角色 ID 列表，若用户未分配任何角色则返回空列表
     */
    List<Long> getRoleIdsByUserId(Long userId);

    /**
     * 获取用户已分配的角色编码列表
     * <p>通过联表查询 {@code sys_user_role} 和 {@code sys_role}，
     * 获取指定用户所拥有的全部角色的 {@code roleCode}。
     * 角色编码用于权限注解判断（如 {@code @RequiresRoles("admin")}）。</p>
     *
     * <p>返回值示例：{@code ["admin", "student"]}</p>
     *
     * @param userId 用户 ID
     * @return 角色编码字符串列表，若用户未分配任何角色则返回空列表
     */
    List<String> getRoleCodesByUserId(Long userId);
}
