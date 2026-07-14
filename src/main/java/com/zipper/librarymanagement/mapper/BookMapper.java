package com.zipper.librarymanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zipper.librarymanagement.entity.Book;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * 图书数据访问层映射接口
 * <p>映射数据库表 {@code book}，继承 MyBatis-Plus 的 {@link BaseMapper}，
 * 自动获得通用 CRUD 方法（insert、update、delete、select 等）。
 * 额外提供物理删除方法，绕过 MyBatis-Plus 的逻辑删除机制。</p>
 *
 * @author zipper
 * @see BaseMapper
 * @see Book
 */
@Repository
@Mapper
public interface BookMapper extends BaseMapper<Book> {

    /**
     * 根据主键 ID 物理删除一条图书记录
     * <p>执行 {@code DELETE FROM book WHERE id = #{id}}，
     * 直接从数据库移除数据，不经过逻辑删除字段标记。</p>
     *
     * @param id 图书主键 ID
     * @return 受影响的行数（删除成功返回 1，不存在返回 0）
     */
    @Delete("DELETE FROM book WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
