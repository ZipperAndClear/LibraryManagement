package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.entity.SysRole;

import java.util.List;

/**
 * 系统角色业务接口
 * <p>基于 RBAC 模型的角色管理，角色编码（roleCode）全局唯一，
 * 删除角色前需校验是否有用户关联该角色。</p>
 */
public interface SysRoleService extends IService<SysRole> {

    /**
     * 获取全部正常角色列表（不分页，供下拉选择器使用）
     * @return 状态为正常的角色列表
     */
    List<SysRole> listAllRoles();

    /**
     * 分页查询角色列表（管理员端角色管理页面）
     * @param page    页码
     * @param size    每页条数
     * @param keyword 关键词（模糊匹配角色名称或角色编码）
     * @return 分页结果
     */
    IPage<SysRole> listRoles(Integer page, Integer size, String keyword);

    /**
     * 新增角色
     * <p>校验 roleCode 唯一性，已存在则抛 BusinessException</p>
     */
    void addRole(SysRole role);

    /**
     * 编辑角色
     */
    void updateRole(SysRole role);

    /**
     * 删除角色
     * <p>前置校验：查询 sys_user_role 中是否有此 roleId 的关联记录，有则抛异常</p>
     */
    void deleteRole(Long roleId);

    /**
     * 更新角色状态（启用/禁用）
     * @param roleId 角色 ID
     * @param status 1-正常 0-停用
     */
    void updateRoleStatus(Long roleId, Integer status);
}
