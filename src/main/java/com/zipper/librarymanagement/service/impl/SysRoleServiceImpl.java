package com.zipper.librarymanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zipper.librarymanagement.common.BusinessException;
import com.zipper.librarymanagement.entity.SysRole;
import com.zipper.librarymanagement.entity.SysUserRole;
import com.zipper.librarymanagement.mapper.SysRoleMapper;
import com.zipper.librarymanagement.mapper.SysUserRoleMapper;
import com.zipper.librarymanagement.service.SysRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 系统角色业务实现类。
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li>角色列表查询：全量查询（供下拉选择器）和分页查询（管理端列表）</li>
 *   <li>角色 CRUD：新增、更新、物理删除</li>
 *   <li>角色状态管理：启用/禁用角色</li>
 *   <li>删除前完整性校验：确保无用户关联此角色</li>
 * </ul>
 *
 * <h3>关键约束</h3>
 * <ul>
 *   <li>{@code roleCode} 角色编码全局唯一，新增时校验重复</li>
 *   <li>删除角色前必须确认无用户关联（通过 {@link SysUserRoleMapper} 检查）</li>
 *   <li>状态为 0（禁用）的角色不会出现在 {@link #listAllRoles()} 的结果中</li>
 * </ul>
 *
 * <h3>关键依赖</h3>
 * <ul>
 *   <li>{@link SysRoleMapper}：角色数据持久化（含物理删除方法）</li>
 *   <li>{@link SysUserRoleMapper}：用户-角色关联表查询，用于删除前的关联校验</li>
 * </ul>
 *
 * <h3>事务边界</h3>
 * <p>所有写操作（新增、更新、删除、状态变更）均使用 {@code @Transactional} 注解。
 * 读操作不加事务。</p>
 *
 * @see SysRoleService
 * @see SysRole
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    /**
     * 获取所有启用状态的角色列表。
     *
     * <p>筛选条件：status = 1（启用）。
     * 该方法供前端下拉选择器使用，不包含已禁用的角色。</p>
     *
     * @return 启用角色列表
     */
    @Override
    public List<SysRole> listAllRoles() {
        return lambdaQuery().eq(SysRole::getStatus, 1).list();
    }

    /**
     * 分页查询角色列表。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>构建分页查询条件：关键词模糊匹配角色名称或角色编码（OR 关系）</li>
     *   <li>按创建时间升序排序</li>
     *   <li>执行分页查询并返回</li>
     * </ol>
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param keyword 搜索关键词（模糊匹配角色名称和编码），可为 {@code null}
     * @return 分页结果
     */
    @Override
    public IPage<SysRole> listRoles(Integer page, Integer size, String keyword) {
        Page<SysRole> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysRole::getRoleName, keyword).or().like(SysRole::getRoleCode, keyword);
        }
        wrapper.orderByAsc(SysRole::getCreateTime);
        return page(pageParam, wrapper);
    }

    /**
     * 新增角色。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>校验角色编码唯一性：查询是否有相同 roleCode 的记录</li>
     *   <li>若重复则抛出异常</li>
     *   <li>持久化角色到数据库</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param role 角色实体（需至少包含 roleName 和 roleCode）
     * @throws BusinessException 若角色编码已存在
     */
    @Override
    @Transactional
    public void addRole(SysRole role) {
        Long count = lambdaQuery().eq(SysRole::getRoleCode, role.getRoleCode()).count();
        if (count > 0) {
            throw new BusinessException("角色编码已存在");
        }
        save(role);
    }

    /**
     * 更新角色信息。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>按 ID 执行全量更新</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * <p><b>注意：</b>当前实现未校验 roleCode 唯一性变更，
     * 若修改了 roleCode，可能产生重复编码。</p>
     *
     * @param role 角色实体（必须包含 id）
     */
    @Override
    @Transactional
    public void updateRole(SysRole role) {
        updateById(role);
    }

    /**
     * 物理删除角色。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>查询用户-角色关联表中是否有记录关联到此角色</li>
     *   <li>若存在关联用户则拒绝删除</li>
     *   <li>无关联则执行物理删除</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param roleId 角色 ID
     * @throws BusinessException 若该角色下存在用户关联
     */
    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        Long userCount = sysUserRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
        if (userCount > 0) {
            throw new BusinessException("该角色下有用户关联，无法删除");
        }
        baseMapper.physicalDeleteById(roleId);
    }

    /**
     * 更新角色状态（启用/禁用）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>根据 ID 查询角色，不存在则抛出异常</li>
     *   <li>更新 status 字段并持久化</li>
     * </ol>
     *
     * <p>状态值：1 = 启用，0 = 禁用。</p>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param roleId 角色 ID
     * @param status 目标状态（1 启用 / 0 禁用）
     * @throws BusinessException 若角色不存在
     */
    @Override
    @Transactional
    public void updateRoleStatus(Long roleId, Integer status) {
        SysRole role = getById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        role.setStatus(status);
        updateById(role);
    }
}
