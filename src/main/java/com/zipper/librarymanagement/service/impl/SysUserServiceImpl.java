package com.zipper.librarymanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zipper.librarymanagement.entity.SysUser;
import com.zipper.librarymanagement.mapper.SysUserMapper;
import com.zipper.librarymanagement.service.SysUserService;
import org.springframework.stereotype.Service;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
}