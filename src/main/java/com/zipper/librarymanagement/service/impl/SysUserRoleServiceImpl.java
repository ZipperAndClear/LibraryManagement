package com.zipper.librarymanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zipper.librarymanagement.entity.SysRole;
import com.zipper.librarymanagement.entity.SysUserRole;
import com.zipper.librarymanagement.mapper.SysRoleMapper;
import com.zipper.librarymanagement.mapper.SysUserRoleMapper;
import com.zipper.librarymanagement.service.SysUserRoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户角色关联业务实现类
 * <p>用户与角色是多对多关系。分配角色时采用"先删后插"策略，
 * 在一个事务中完成，保证数据一致性。</p>
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    @Transactional
    public void assignRoles(Long userId, List<Long> roleIds) {
        // 第一步：删除该用户已有的全部角色关联
        remove(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        // 第二步：批量插入新的角色关联
        for (Long roleId : roleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            save(userRole);
        }
    }

    @Override
    public List<Long> getRoleIdsByUserId(Long userId) {
        return lambdaQuery().eq(SysUserRole::getUserId, userId)
                .list().stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
    }

    @Override
    public List<String> getRoleCodesByUserId(Long userId) {
        // 先查角色 ID 列表，再批量查询角色编码
        List<Long> roleIds = getRoleIdsByUserId(userId);
        if (roleIds.isEmpty()) {
            return List.of();
        }
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        return roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList());
    }
}
