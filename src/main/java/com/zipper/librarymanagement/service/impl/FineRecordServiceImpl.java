package com.zipper.librarymanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zipper.librarymanagement.entity.FineRecord;
import com.zipper.librarymanagement.mapper.FineRecordMapper;
import com.zipper.librarymanagement.service.FineRecordService;
import org.springframework.stereotype.Service;

@Service
public class FineRecordServiceImpl extends ServiceImpl<FineRecordMapper, FineRecord> implements FineRecordService {
}