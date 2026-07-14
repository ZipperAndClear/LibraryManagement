package com.zipper.librarymanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zipper.librarymanagement.entity.SysUserRole;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 用户角色关联数据访问层映射接口
 * <p>映射数据库表 {@code sys_user_role}，继承 MyBatis-Plus 的 {@link BaseMapper}，
 * 自动获得通用 CRUD 方法（insert、update、delete、select 等）。
 * 用于维护系统用户与角色之间的多对多关联关系。</p>
 *
 * @author zipper
 * @see BaseMapper
 * @see SysUserRole
 */
@Repository
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 根据用户 ID 删除该用户的所有角色关联
     * <p>执行 {@code DELETE FROM sys_user_role WHERE user_id = #{userId}}，
     * 移除指定用户与所有角色的绑定关系。通常在重新分配角色前调用。</p>
     *
     * @param userId 用户主键 ID
     * @return 受影响的行数（即该用户之前绑定的角色数量）
     */
    @Delete("DELETE FROM sys_user_role WHERE user_id = #{userId}")
    int deleteByUserId(@Param("userId") Long userId);
}
