package com.zipper.librarymanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zipper.librarymanagement.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 系统配置数据访问层映射接口
 * <p>映射数据库表 {@code sys_config}，继承 MyBatis-Plus 的 {@link BaseMapper}，
 * 自动获得通用 CRUD 方法（insert、update、delete、select 等），
 * 用于管理系统的全局配置参数（如借阅天数上限、罚款金额等）。</p>
 *
 * @author zipper
 * @see BaseMapper
 * @see SysConfig
 */
@Repository
@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig>{
}
