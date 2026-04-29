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
 * 系统角色业务实现类
 * <p>角色编码（roleCode）全局唯一索引，删除角色时需确认无用户关联。</p>
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Override
    public List<SysRole> listAllRoles() {
        // 只返回正常状态的角色，供前端下拉选择器使用
        return lambdaQuery().eq(SysRole::getStatus, 1).list();
    }

    @Override
    public IPage<SysRole> listRoles(Integer page, Integer size, String keyword) {
        Page<SysRole> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        // 关键词模糊匹配角色名称或角色编码
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysRole::getRoleName, keyword).or().like(SysRole::getRoleCode, keyword);
        }
        wrapper.orderByAsc(SysRole::getCreateTime);
        return page(pageParam, wrapper);
    }

    @Override
    @Transactional
    public void addRole(SysRole role) {
        // 校验角色编码唯一性
        Long count = lambdaQuery().eq(SysRole::getRoleCode, role.getRoleCode()).count();
        if (count > 0) {
            throw new BusinessException("角色编码已存在");
        }
        save(role);
    }

    @Override
    @Transactional
    public void updateRole(SysRole role) {
        updateById(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long roleId) {
        // 检查是否有用户关联此角色
        Long userCount = sysUserRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getRoleId, roleId));
        if (userCount > 0) {
            throw new BusinessException("该角色下有用户关联，无法删除");
        }
        removeById(roleId);
    }

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
