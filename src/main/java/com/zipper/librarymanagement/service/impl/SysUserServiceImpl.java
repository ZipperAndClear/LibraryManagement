package com.zipper.librarymanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zipper.librarymanagement.common.BusinessException;
import com.zipper.librarymanagement.dto.AddUserDTO;
import com.zipper.librarymanagement.dto.RegisterDTO;
import com.zipper.librarymanagement.dto.UpdateUserDTO;
import com.zipper.librarymanagement.entity.SysRole;
import com.zipper.librarymanagement.entity.SysUser;
import com.zipper.librarymanagement.entity.SysUserRole;
import com.zipper.librarymanagement.mapper.SysRoleMapper;
import com.zipper.librarymanagement.mapper.SysUserMapper;
import com.zipper.librarymanagement.mapper.SysUserRoleMapper;
import com.zipper.librarymanagement.service.SysUserRoleService;
import com.zipper.librarymanagement.service.SysUserService;
import com.zipper.librarymanagement.vo.CaptchaVO;
import com.zipper.librarymanagement.vo.LoginResultVO;
import com.zipper.librarymanagement.vo.UserDetailVO;
import com.zipper.librarymanagement.vo.UserListVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 系统用户业务实现类
 * <p>覆盖用户认证（登录/注册）、用户管理（CRUD/状态管理/密码管理）等功能。
 * 验证码使用本地 ConcurrentHashMap 缓存，token 黑名单使用本地缓存（生产环境建议替换为 Redis）。</p>
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    /** 验证码缓存（key=captchaKey, value=验证码答案） */
    private final ConcurrentHashMap<String, String> captchaCache = new ConcurrentHashMap<>();

    /** Token 黑名单缓存（用于登出后使 token 失效） */
    private final ConcurrentHashMap<String, Boolean> tokenBlacklist = new ConcurrentHashMap<>();

    @Override
    public LoginResultVO login(String username, String password, String captcha, String captchaKey) {
        // 第一步：校验图形验证码（若传入 captcha 和 captchaKey）
        if (captcha != null && captchaKey != null) {
            String cached = captchaCache.remove(captchaKey);
            if (cached == null || !cached.equalsIgnoreCase(captcha)) {
                throw new BusinessException("验证码错误或已过期");
            }
        }
        // 第二步：查库确认用户存在
        SysUser user = lambdaQuery().eq(SysUser::getUsername, username).one();
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        // 第三步：校验密码
        if (!password.equals(user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        // 第四步：检查账号是否被禁用
        if (user.isDisabled()) {
            throw new BusinessException("账号已被禁用");
        }
        // 第五步：查询角色并生成 token
        List<String> roleCodes = sysUserRoleService.getRoleCodesByUserId(user.getId());
        String token = UUID.randomUUID().toString().replace("-", "");
        LoginResultVO vo = new LoginResultVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(roleCodes);
        return vo;
    }

    @Override
    public void logout(String token) {
        tokenBlacklist.put(token, true);
    }

    @Override
    public UserDetailVO getCurrentUser(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 查询用户角色信息
        List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(userId);
        List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setRoles(roles.stream().map(SysRole::getRoleName).collect(Collectors.toList()));
        vo.setRoleCodes(roles.stream().map(SysRole::getRoleCode).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional
    public void register(RegisterDTO registerDTO) {
        // 校验两次密码一致性
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException("两次密码不一致");
        }
        // 校验用户名唯一性
        Long count = lambdaQuery().eq(SysUser::getUsername, registerDTO.getUsername()).count();
        if (count > 0) {
            throw new BusinessException("账号已存在");
        }
        // 校验验证码
        if (registerDTO.getCaptcha() != null && registerDTO.getCaptchaKey() != null) {
            String cached = captchaCache.remove(registerDTO.getCaptchaKey());
            if (cached == null || !cached.equalsIgnoreCase(registerDTO.getCaptcha())) {
                throw new BusinessException("验证码错误或已过期");
            }
        }
        // 创建用户并默认分配学生角色
        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(registerDTO.getPassword());
        user.setRealName(registerDTO.getRealName());
        user.setStatus(1);
        save(user);
        // 自动分配默认角色（role_code = 'student'）
        SysRole studentRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "student"));
        if (studentRole != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(studentRole.getId());
            sysUserRoleMapper.insert(userRole);
        }
    }

    @Override
    public IPage<UserListVO> listUsers(Integer page, Integer size, String keyword, Integer status) {
        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        // 动态拼接查询条件
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        // 查数据库
        IPage<SysUser> userPage = page(pageParam, wrapper);
        // 转换为 VO（增加角色名称列表）
        IPage<UserListVO> voPage = new Page<>(userPage.getCurrent(), userPage.getSize(), userPage.getTotal());
        List<UserListVO> voList = userPage.getRecords().stream().map(user -> {
            UserListVO vo = new UserListVO();
            vo.setId(user.getId());
            vo.setUsername(user.getUsername());
            vo.setRealName(user.getRealName());
            vo.setAvatar(user.getAvatar());
            vo.setEmail(user.getEmail());
            vo.setPhone(user.getPhone());
            vo.setStatus(user.getStatus());
            vo.setCreateTime(user.getCreateTime());
            // 查询用户角色名称
            List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(user.getId());
            if (!roleIds.isEmpty()) {
                List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
                vo.setRoleNames(roles.stream().map(SysRole::getRoleName).collect(Collectors.toList()));
            }
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    @Transactional
    public void addUser(AddUserDTO dto) {
        // 校验用户名唯一性
        Long count = lambdaQuery().eq(SysUser::getUsername, dto.getUsername()).count();
        if (count > 0) {
            throw new BusinessException("账号已存在");
        }
        // 新增用户
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        save(user);
        // 分配角色
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            sysUserRoleService.assignRoles(user.getId(), dto.getRoleIds());
        }
    }

    @Override
    @Transactional
    public void updateUser(UpdateUserDTO dto) {
        SysUser user = getById(dto.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 只更新可修改字段（不允许改 username）
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        updateById(user);
        // roleIds 不为 null 则更新角色分配（为空列表表示清空角色）
        if (dto.getRoleIds() != null) {
            sysUserRoleService.assignRoles(dto.getId(), dto.getRoleIds());
        }
    }

    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(newPassword);
        updateById(user);
    }

    @Override
    @Transactional
    public void changeMyPassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        // 校验原密码
        if (!user.getPassword().equals(oldPassword)) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(newPassword);
        updateById(user);
    }

    @Override
    @Transactional
    public void updateUserStatus(Long userId, Integer status) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(status);
        updateById(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        // 先清理角色关联，再删除用户
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        removeById(userId);
    }

    @Override
    public CaptchaVO generateCaptcha() {
        // 生成4位大写字母+数字验证码（排除易混淆字符 O 0 I 1）
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 4; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        // 存入缓存
        String key = UUID.randomUUID().toString();
        captchaCache.put(key, code.toString());
        // 生成 SVG 格式验证码图片并 Base64 编码
        String imageBase64 = generateSimpleCaptchaImage(code.toString());
        return new CaptchaVO(key, imageBase64);
    }

    /**
     * 生成简易 SVG 验证码图片的 Base64 编码
     * @param code 验证码文字
     * @return 完整的 Base64 图片 data URL
     */
    private String generateSimpleCaptchaImage(String code) {
        StringBuilder sb = new StringBuilder();
        sb.append("data:image/svg+xml;base64,");
        String svg = "<svg xmlns='http://www.w3.org/2000/svg' width='120' height='40'>"
                + "<rect width='120' height='40' fill='#f0f0f0' rx='4'/>"
                + "<text x='60' y='28' text-anchor='middle' font-size='22' "
                + "font-family='Arial' fill='#333' font-weight='bold'>"
                + code + "</text></svg>";
        sb.append(java.util.Base64.getEncoder().encodeToString(svg.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return sb.toString();
    }
}
