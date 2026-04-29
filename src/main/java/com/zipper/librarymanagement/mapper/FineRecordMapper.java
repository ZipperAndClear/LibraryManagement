package com.zipper.librarymanagement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zipper.librarymanagement.entity.FineRecord;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository

@Mapper
public interface FineRecordMapper extends BaseMapper<FineRecord>{
}