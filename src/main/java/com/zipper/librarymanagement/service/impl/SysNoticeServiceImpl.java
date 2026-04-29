package com.zipper.librarymanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zipper.librarymanagement.entity.SysNotice;
import com.zipper.librarymanagement.mapper.SysNoticeMapper;
import com.zipper.librarymanagement.service.SysNoticeService;
import org.springframework.stereotype.Service;

@Service
public class SysNoticeServiceImpl extends ServiceImpl<SysNoticeMapper, SysNotice> implements SysNoticeService {
}