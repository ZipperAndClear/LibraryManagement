package com.zipper.librarymanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zipper.librarymanagement.entity.SysCategory;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 系统分类数据访问层映射接口
 * <p>映射数据库表 {@code sys_category}，继承 MyBatis-Plus 的 {@link BaseMapper}，
 * 自动获得通用 CRUD 方法（insert、update、delete、select 等）。
 * 用于管理图书分类信息（如文学、科技、历史等）。</p>
 *
 * @author zipper
 * @see BaseMapper
 * @see SysCategory
 */
@Repository

@Mapper
public interface SysCategoryMapper extends BaseMapper<SysCategory>{

    /**
     * 根据主键 ID 物理删除一条分类记录
     * <p>执行 {@code DELETE FROM sys_category WHERE id = #{id}}，
     * 直接从数据库移除数据，不经过逻辑删除字段标记。</p>
     *
     * @param id 分类主键 ID
     * @return 受影响的行数（删除成功返回 1，不存在返回 0）
     */
    @Delete("DELETE FROM sys_category WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
