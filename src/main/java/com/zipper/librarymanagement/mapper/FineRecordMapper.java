package com.zipper.librarymanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zipper.librarymanagement.entity.FineRecord;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

/**
 * 罚款记录数据访问层映射接口
 * <p>映射数据库表 {@code fine_record}，继承 MyBatis-Plus 的 {@link BaseMapper}，
 * 自动获得通用 CRUD 方法（insert、update、delete、select 等），
 * 用于管理读者逾期归还图书产生的罚款记录。</p>
 *
 * @author zipper
 * @see BaseMapper
 * @see FineRecord
 */
@Repository

@Mapper
public interface FineRecordMapper extends BaseMapper<FineRecord>{
}
