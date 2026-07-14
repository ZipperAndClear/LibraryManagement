package com.zipper.librarymanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zipper.librarymanagement.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 借阅记录数据访问层映射接口
 * <p>映射数据库表 {@code borrow_record}，继承 MyBatis-Plus 的 {@link BaseMapper}，
 * 自动获得通用 CRUD 方法（insert、update、delete、select 等），
 * 用于管理读者的图书借阅与归还记录。</p>
 *
 * @author zipper
 * @see BaseMapper
 * @see BorrowRecord
 */
@Repository
@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord>{
}
