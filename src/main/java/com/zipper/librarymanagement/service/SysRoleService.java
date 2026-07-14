package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.entity.SysRole;

import java.util.List;

/**
 * 系统角色业务接口
 * <p>基于 RBAC（Role-Based Access Control）模型的角色管理模块。
 * 角色编码（{@code roleCode}）全局唯一，用于权限判断。
 * 删除角色前需校验是否有用户关联该角色，防止产生孤立权限记录。</p>
 *
 * <p>角色状态定义：
 * <ul>
 *   <li>{@code 0} — 停用，关联该角色的用户其对应权限将被禁用</li>
 *   <li>{@code 1} — 正常，关联该角色的用户拥有对应权限</li>
 * </ul>
 * </p>
 *
 * @author zipper
 * @since 1.0
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 获取全部正常角色列表（不分页）
     * <p>仅返回状态为正常（{@code status=1}）的角色，
     * 供下拉选择器、角色分配弹窗等组件使用。</p>
     *
     * @return 状态为正常的角色列表，无数据时返回空列表
     */
    List<SysRole> listAllRoles();

    /**
     * 分页查询角色列表（管理员端角色管理页面）
     * <p>支持按角色名称或角色编码进行模糊搜索，返回分页结果。</p>
     *
     * @param page    页码，从 1 开始
     * @param size    每页记录条数
     * @param keyword 关键词（模糊匹配角色名称 {@code roleName} 或角色编码 {@code roleCode}，
     *                {@code null} 或空字符串表示不筛选）
     * @return 包含角色实体列表的分页对象，按创建时间倒序排列
     */
    IPage<SysRole> listRoles(Integer page, Integer size, String keyword);

    /**
     * 新增角色
     * <p>创建前会校验 {@code roleCode} 是否已存在，
     * 若已存在则抛出业务异常。新创建的角色默认为正常状态。</p>
     *
     * @param role 角色实体，需包含角色名称 {@code roleName} 和角色编码 {@code roleCode}
     * @throws com.zipper.librarymanagement.exception.BusinessException 若 {@code roleCode} 已存在
     */
    void addRole(SysRole role);

    /**
     * 编辑已有角色
     * <p>支持修改角色名称、角色编码、角色描述等信息。
     * 若修改了 {@code roleCode}，同样需要校验唯一性。</p>
     *
     * @param role 角色实体，必须包含有效的角色 ID
     * @throws com.zipper.librarymanagement.exception.BusinessException 若角色不存在或新的 {@code roleCode} 已被占用
     */
    void updateRole(SysRole role);

    /**
     * 删除角色
     * <p>删除前会查询 {@code sys_user_role} 表中是否有该角色 ID 的关联记录。
     * 若存在关联用户，则不允许删除并抛出业务异常，以防止用户失去角色后无法正常使用系统。</p>
     *
     * @param roleId 待删除的角色 ID
     * @throws com.zipper.librarymanagement.exception.BusinessException 若角色不存在或仍有用户关联该角色
     */
    void deleteRole(Long roleId);

    /**
     * 更新角色状态（启用/禁用）
     * <p>禁用角色后，已关联该角色的用户将暂时失去对应权限；
     * 重新启用后权限自动恢复。此操作不影响用户与角色的关联关系。</p>
     *
     * @param roleId 角色 ID
     * @param status 目标状态（{@code 0} — 停用，{@code 1} — 正常）
     * @throws com.zipper.librarymanagement.exception.BusinessException 若角色不存在或状态值非法
     */
    void updateRoleStatus(Long roleId, Integer status);
}
