package com.zipper.librarymanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zipper.librarymanagement.entity.SysCategory;
import com.zipper.librarymanagement.mapper.SysCategoryMapper;
import com.zipper.librarymanagement.service.SysCategoryService;
import org.springframework.stereotype.Service;

@Service
public class SysCategoryServiceImpl extends ServiceImpl<SysCategoryMapper, SysCategory> implements SysCategoryService {
}