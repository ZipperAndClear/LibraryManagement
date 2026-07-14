package com.zipper.librarymanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zipper.librarymanagement.entity.SysNotice;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 系统公告数据访问层映射接口
 * <p>映射数据库表 {@code sys_notice}，继承 MyBatis-Plus 的 {@link BaseMapper}，
 * 自动获得通用 CRUD 方法（insert、update、delete、select 等），
 * 用于管理系统发布的公告通知信息。</p>
 *
 * @author zipper
 * @see BaseMapper
 * @see SysNotice
 */
@Repository

@Mapper
public interface SysNoticeMapper extends BaseMapper<SysNotice>{
}
