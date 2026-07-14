package com.zipper.librarymanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zipper.librarymanagement.common.BusinessException;
import com.zipper.librarymanagement.common.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.zipper.librarymanagement.dto.AddUserDTO;
import com.zipper.librarymanagement.dto.RegisterDTO;
import com.zipper.librarymanagement.dto.UpdateUserDTO;
import com.zipper.librarymanagement.entity.BorrowRecord;
import com.zipper.librarymanagement.entity.FineRecord;
import com.zipper.librarymanagement.entity.SysRole;
import com.zipper.librarymanagement.entity.SysUser;
import com.zipper.librarymanagement.entity.SysUserRole;
import com.zipper.librarymanagement.mapper.BorrowRecordMapper;
import com.zipper.librarymanagement.mapper.FineRecordMapper;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 系统用户业务实现类——用户管理的总入口。
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li><b>用户认证</b>（{@link #login}）：验证码校验 → 账号密码校验 → 状态检查 → 生成 JWT</li>
 *   <li><b>用户注册</b>（{@link #register}）：密码一致性校验 → 用户名唯一性校验 → 验证码校验 → 创建用户并分配默认角色</li>
 *   <li><b>用户管理</b>：CRUD（分页查询、新增、更新、删除）、状态管理、密码管理</li>
 *   <li><b>验证码</b>（{@link #generateCaptcha}）：生成 4 位字符验证码并返回 Base64 格式 SVG 图片</li>
 *   <li><b>登出</b>（{@link #logout}）：将 token 加入本地黑名单</li>
 * </ul>
 *
 * <h3>关键依赖</h3>
 * <ul>
 *   <li>{@link SysUserMapper}：用户数据持久化（含物理删除方法）</li>
 *   <li>{@link SysUserRoleService}：用户角色关联管理（分配/查询）</li>
 *   <li>{@link SysRoleMapper}：角色数据查询</li>
 *   <li>{@link SysUserRoleMapper}：用户-角色关联表操作</li>
 *   <li>{@link BorrowRecordMapper} / {@link FineRecordMapper}：删除用户前的关联记录检查</li>
 *   <li>{@link BCryptPasswordEncoder}：密码加密（兼容 BCrypt 密文和旧明文）</li>
 *   <li>{@link JwtUtil}：JWT 令牌生成</li>
 * </ul>
 *
 * <h3>本地缓存</h3>
 * <ul>
 *   <li>{@code captchaCache}：验证码缓存（key=captchaKey, value=验证码答案），一次使用后即消费（remove）</li>
 *   <li>{@code tokenBlacklist}：Token 黑名单缓存，用于登出后使 token 失效</li>
 * </ul>
 * <p><b>注意：</b>以上缓存均为本地内存缓存，多实例部署或重启后失效。
 * 生产环境建议替换为 Redis。</p>
 *
 * <h3>密码兼容策略</h3>
 * <p>密码验证同时支持 BCrypt 密文（以 {@code $2a$} 开头）和旧明文密码。
 * 新密码统一使用 {@link BCryptPasswordEncoder} 加密存储。</p>
 *
 * <h3>事务边界</h3>
 * <p>所有涉及多表写操作的方法（新增、更新、删除、注册、重置密码）均使用
 * {@code @Transactional} 注解。仅读操作和纯计算的方法（登录、查询、验证码生成、登出）不加事务。</p>
 *
 * @see SysUserService
 * @see SysUser
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private SysUserRoleService sysUserRoleService;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private BorrowRecordMapper borrowRecordMapper;

    @Autowired
    private FineRecordMapper fineRecordMapper;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Value("${jwt.secretKey}")
    private String jwtSecretKey;

    @Value("${jwt.ttlMillis}")
    private long jwtTtlMillis;

    /** 验证码缓存（key=captchaKey, value=验证码答案） */
    private final ConcurrentHashMap<String, String> captchaCache = new ConcurrentHashMap<>();

    /** Token 黑名单缓存（用于登出后使 token 失效） */
    private final ConcurrentHashMap<String, Boolean> tokenBlacklist = new ConcurrentHashMap<>();

    /**
     * 用户登录——认证流程的核心方法。
     *
     * <h4>详细步骤</h4>
     * <ol>
     *   <li><b>验证图形验证码</b>（若传入 captcha 和 captchaKey）：
     *       从 {@link #captchaCache} 中取出验证码答案并消费（remove），
     *       比对用户输入的验证码（大小写不敏感），不匹配则抛出异常</li>
     *   <li><b>查询用户</b>：通过 username 查数据库，不存在则抛出"账号不存在"</li>
     *   <li><b>校验密码</b>（兼容 BCrypt 密文和旧明文）：
     *     <ul>
     *       <li>若密码以 {@code "$2a$"} 开头 → 使用 {@link BCryptPasswordEncoder#matches} 比对</li>
     *       <li>否则 → 明文比对（兼容历史数据）</li>
     *     </ul>
     *     不匹配则抛出"密码错误"</li>
     *   <li><b>检查账号状态</b>：若用户被禁用则抛出"账号已被禁用"</li>
     *   <li><b>查询角色并生成 JWT</b>：获取用户角色编码列表，构造 claims（userId, username），
     *       调用 {@link JwtUtil#createJWT} 生成 token，组装 {@link LoginResultVO} 并返回</li>
     * </ol>
     *
     * <h4>前置条件</h4>
     * <ul>
     *   <li>若传入验证码，必须先调用 {@link #generateCaptcha()} 获取 captchaKey</li>
     * </ul>
     *
     * <h4>副作用</h4>
     * <ul>
     *   <li>验证码在验证后立即从缓存中移除（一次性使用）</li>
     * </ul>
     *
     * <h4>异常条件</h4>
     * <ul>
     *   <li>验证码错误或已过期</li>
     *   <li>账号不存在</li>
     *   <li>密码错误</li>
     *   <li>账号已被禁用</li>
     * </ul>
     *
     * @param username   用户名
     * @param password   密码
     * @param captcha    用户输入的验证码，可为 {@code null}
     * @param captchaKey 验证码缓存键（由 {@link #generateCaptcha} 返回），可为 {@code null}
     * @return 登录结果（含 JWT token、用户基本信息、角色列表）
     * @throws BusinessException 若任一校验环节不通过
     */
    @Override
    public LoginResultVO login(String username, String password, String captcha, String captchaKey) {
        if (captcha != null && captchaKey != null) {
            String cached = captchaCache.remove(captchaKey);
            if (cached == null || !cached.equalsIgnoreCase(captcha)) {
                throw new BusinessException("验证码错误或已过期");
            }
        }
        SysUser user = lambdaQuery().eq(SysUser::getUsername, username).one();
        if (user == null) {
            throw new BusinessException("账号不存在");
        }
        boolean passwordMatched = user.getPassword() != null
                && (user.getPassword().startsWith("$2a$")
                    ? passwordEncoder.matches(password, user.getPassword())
                    : password.equals(user.getPassword()));
        if (!passwordMatched) {
            throw new BusinessException("密码错误");
        }
        if (user.isDisabled()) {
            throw new BusinessException("账号已被禁用");
        }
        List<String> roleCodes = sysUserRoleService.getRoleCodesByUserId(user.getId());
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("roles", roleCodes);
        String token = JwtUtil.createJWT(jwtSecretKey, jwtTtlMillis, claims);
        LoginResultVO vo = new LoginResultVO();
        vo.setToken(token);
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setRoles(roleCodes);
        return vo;
    }

    /**
     * 用户登出。
     *
     * <p>将当前 token 加入 {@link #tokenBlacklist} 本地黑名单中。
     * 后续请求可通过拦截器检查黑名单以禁止该 token 继续使用。</p>
     *
     * <h4>副作用</h4>
     * <ul>
     *   <li>token 加入黑名单，但未设置过期时间（不会自动清理）</li>
     * </ul>
     *
     * <p><b>注意：</b>本地黑名单在服务重启后丢失，生产环境应使用 Redis。</p>
     *
     * @param token 需要失效的 JWT token
     */
    @Override
    public void logout(String token) {
        tokenBlacklist.put(token, true);
    }

    /**
     * 获取当前登录用户的详细信息（含角色）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>根据用户 ID 查询用户实体，不存在则抛出异常</li>
     *   <li>查询该用户的所有角色</li>
     *   <li>组装 {@link UserDetailVO}（含用户名、真实姓名、头像、邮箱、手机、
     *       状态、角色名称列表、角色编码列表）</li>
     * </ol>
     *
     * @param userId 用户 ID
     * @return 用户详细信息 VO
     * @throws BusinessException 若用户不存在
     */
    @Override
    public UserDetailVO getCurrentUser(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
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

    /**
     * 用户注册。
     *
     * <h4>详细步骤</h4>
     * <ol>
     *   <li><b>校验密码一致性</b>：两次输入的密码必须相同</li>
     *   <li><b>校验用户名唯一性</b>：查询数据库确认无重复</li>
     *   <li><b>校验验证码</b>（若传入）：从缓存中取出并比对，不匹配则抛出异常</li>
     *   <li><b>创建用户</b>：密码使用 BCrypt 加密，默认状态设为启用（status=1）</li>
     *   <li><b>分配默认角色</b>：自动查询 roleCode = "student" 的角色，
     *       若存在则建立用户-角色关联</li>
     * </ol>
     *
     * <h4>前置条件</h4>
     * <ul>
     *   <li>两次密码输入一致</li>
     *   <li>用户名未被占用</li>
     *   <li>若需验证码，必须先调用 {@link #generateCaptcha()}</li>
     *   <li>系统中存在 roleCode = "student" 的角色（否则注册后无角色）</li>
     * </ul>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}，用户创建与角色分配在同一事务中。</p>
     *
     * @param registerDTO 注册数据传输对象
     * @throws BusinessException 若密码不一致、用户名已存在 或 验证码错误
     */
    @Override
    @Transactional
    public void register(RegisterDTO registerDTO) {
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            throw new BusinessException("两次密码不一致");
        }
        Long count = lambdaQuery().eq(SysUser::getUsername, registerDTO.getUsername()).count();
        if (count > 0) {
            throw new BusinessException("账号已存在");
        }
        if (registerDTO.getCaptcha() != null && registerDTO.getCaptchaKey() != null) {
            String cached = captchaCache.remove(registerDTO.getCaptchaKey());
            if (cached == null || !cached.equalsIgnoreCase(registerDTO.getCaptcha())) {
                throw new BusinessException("验证码错误或已过期");
            }
        }
        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRealName(registerDTO.getRealName());
        user.setStatus(1);
        save(user);
        SysRole studentRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, "student"));
        if (studentRole != null) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(user.getId());
            userRole.setRoleId(studentRole.getId());
            sysUserRoleMapper.insert(userRole);
        }
    }

    /**
     * 分页查询用户列表（管理端）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>构建动态查询条件：关键词模糊匹配用户名或真实姓名（OR 关系）、按状态筛选</li>
     *   <li>按创建时间倒序排序</li>
     *   <li>执行分页查询</li>
     *   <li>逐条转换为 {@link UserListVO}，补全用户的角色名称列表</li>
     * </ol>
     *
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param keyword 搜索关键词（模糊匹配用户名和真实姓名），可为 {@code null}
     * @param status  用户状态筛选（1=启用 0=禁用），可为 {@code null}
     * @return 分页结果，包含补全了角色名称的用户列表
     */
    @Override
    public IPage<UserListVO> listUsers(Integer page, Integer size, String keyword, Integer status) {
        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysUser::getUsername, keyword).or().like(SysUser::getRealName, keyword);
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> userPage = page(pageParam, wrapper);
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
            List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(user.getId());
            if (!roleIds.isEmpty()) {
                List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
                vo.setRoleNames(roles.stream().map(SysRole::getRoleName).collect(Collectors.toList()));
                vo.setRoleIds(roleIds);
            }
            return vo;
        }).collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public UserListVO getUserDetail(Long userId) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        UserListVO vo = new UserListVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        List<Long> roleIds = sysUserRoleService.getRoleIdsByUserId(user.getId());
        if (!roleIds.isEmpty()) {
            List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
            vo.setRoleNames(roles.stream().map(SysRole::getRoleName).collect(Collectors.toList()));
            vo.setRoleIds(roleIds);
        }
        return vo;
    }

    /**
     * 管理端新增用户。
     *
     * <h4>详细步骤</h4>
     * <ol>
     *   <li>校验用户名唯一性</li>
     *   <li>创建用户实体：密码 BCrypt 加密，默认状态启用（status=1）</li>
     *   <li>持久化到数据库</li>
     *   <li>若 DTO 中指定了角色 ID 列表，则分配角色</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}，用户创建与角色分配在同一事务中。</p>
     *
     * @param dto 新增用户数据传输对象
     * @throws BusinessException 若用户名已存在
     */
    @Override
    @Transactional
    public void addUser(AddUserDTO dto) {
        Long count = lambdaQuery().eq(SysUser::getUsername, dto.getUsername()).count();
        if (count > 0) {
            throw new BusinessException("账号已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setStatus(1);
        save(user);
        if (dto.getRoleIds() != null && !dto.getRoleIds().isEmpty()) {
            sysUserRoleService.assignRoles(user.getId(), dto.getRoleIds());
        }
    }

    /**
     * 更新用户信息。
     *
     * <h4>详细步骤</h4>
     * <ol>
     *   <li>根据 ID 查询用户，不存在则抛出异常</li>
     *   <li>若修改了用户名：校验新用户名是否被其他用户占用（排除自身），
     *       若冲突则抛出异常</li>
     *   <li>逐字段更新：真实姓名、邮箱、手机、头像（若传入）</li>
     *   <li>持久化用户信息</li>
     *   <li>若 DTO 中 roleIds 不为 {@code null}（包括空列表），
     *       则更新角色分配（空列表表示清空所有角色）</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}，用户更新与角色变更在同一事务中。</p>
     *
     * @param dto 更新用户数据传输对象（必须包含 id）
     * @throws BusinessException 若用户不存在 或 新用户名已被占用
     */
    @Override
    @Transactional
    public void updateUser(UpdateUserDTO dto) {
        SysUser user = getById(dto.getId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (dto.getUsername() != null && !dto.getUsername().isEmpty()
                && !dto.getUsername().equals(user.getUsername())) {
            Long count = lambdaQuery().eq(SysUser::getUsername, dto.getUsername())
                    .ne(SysUser::getId, dto.getId()).count();
            if (count > 0) {
                throw new BusinessException("用户名已被其他用户使用");
            }
            user.setUsername(dto.getUsername());
        }
        user.setRealName(dto.getRealName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        updateById(user);
        if (dto.getRoleIds() != null) {
            sysUserRoleService.assignRoles(dto.getId(), dto.getRoleIds());
        }
    }

    /**
     * 重置用户密码（管理员操作，无需旧密码）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>根据 ID 查询用户，不存在则抛出异常</li>
     *   <li>将新密码 BCrypt 加密后更新</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param userId      目标用户 ID
     * @param newPassword 新密码（明文）
     * @throws BusinessException 若用户不存在
     */
    @Override
    @Transactional
    public void resetPassword(Long userId, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }

    /**
     * 用户自主修改密码（需校验旧密码）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>根据 ID 查询用户，不存在则抛出异常</li>
     *   <li><b>校验旧密码</b>（兼容 BCrypt 密文和旧明文）：
     *     <ul>
     *       <li>若密码以 {@code "$2a$"} 开头 → 使用 BCrypt 比对</li>
     *       <li>否则 → 明文比对</li>
     *     </ul>
     *   </li>
     *   <li>旧密码错误则抛出异常</li>
     *   <li>新密码 BCrypt 加密后更新</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param userId      当前用户 ID
     * @param oldPassword 旧密码（用于身份验证）
     * @param newPassword 新密码（明文）
     * @throws BusinessException 若用户不存在 或 旧密码错误
     */
    @Override
    @Transactional
    public void changeMyPassword(Long userId, String oldPassword, String newPassword) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        boolean oldPasswordMatched = user.getPassword() != null
                && (user.getPassword().startsWith("$2a$")
                    ? passwordEncoder.matches(oldPassword, user.getPassword())
                    : oldPassword.equals(user.getPassword()));
        if (!oldPasswordMatched) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        updateById(user);
    }

    /**
     * 更新用户状态（启用/禁用）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>根据 ID 查询用户，不存在则抛出异常</li>
     *   <li>更新 status 字段并持久化</li>
     * </ol>
     *
     * <p>状态值：1 = 启用，0 = 禁用。</p>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param userId 用户 ID
     * @param status 目标状态（1 启用 / 0 禁用）
     * @throws BusinessException 若用户不存在
     */
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

    /**
     * 物理删除用户及其关联数据。
     *
     * <h4>详细步骤</h4>
     * <ol>
     *   <li><b>校验未归还记录</b>：查询用户是否有状态为"借阅中"（0）或"逾期未还"（2）的借阅记录</li>
     *   <li>若有未归还记录则拒绝删除</li>
     *   <li><b>校验未缴罚款</b>：查询用户是否有状态为"未缴"（0）的罚款记录</li>
     *   <li>若有未缴罚款则拒绝删除</li>
     *   <li><b>清理关联数据</b>：
     *     <ul>
     *       <li>删除该用户的所有借阅记录</li>
     *       <li>删除该用户的所有罚款记录</li>
     *       <li>删除该用户的所有角色关联</li>
     *     </ul>
     *   </li>
     *   <li><b>物理删除用户</b>：从数据库彻底移除用户记录</li>
     * </ol>
     *
     * <h4>前置条件</h4>
     * <ul>
     *   <li>用户无未归还的借阅记录</li>
     *   <li>用户无未缴纳的罚款</li>
     * </ul>
     *
     * <h4>副作用</h4>
     * <ul>
     *   <li>用户的所有借阅记录被物理删除</li>
     *   <li>用户的所有罚款记录被物理删除</li>
     *   <li>用户的所有角色关联被删除</li>
     *   <li>用户本身被物理删除</li>
     * </ul>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}，所有删除操作在同一事务中，
     * 任一失败则全部回滚。</p>
     *
     * @param userId 用户 ID
     * @throws BusinessException 若存在未归还记录 或 存在未缴罚款
     */
    @Override
    @Transactional
    public void deleteUser(Long userId) {
        long activeBorrows = borrowRecordMapper.selectCount(
                new LambdaQueryWrapper<BorrowRecord>()
                        .eq(BorrowRecord::getUserId, userId)
                        .in(BorrowRecord::getStatus, 0, 2));
        if (activeBorrows > 0) {
            throw new BusinessException("该用户仍有未归还的图书，无法删除");
        }
        long unpaidFines = fineRecordMapper.selectCount(
                new LambdaQueryWrapper<FineRecord>()
                        .eq(FineRecord::getUserId, userId)
                        .eq(FineRecord::getStatus, 0));
        if (unpaidFines > 0) {
            throw new BusinessException("该用户仍有未缴纳的罚款，无法删除");
        }
        borrowRecordMapper.delete(new LambdaQueryWrapper<BorrowRecord>().eq(BorrowRecord::getUserId, userId));
        fineRecordMapper.delete(new LambdaQueryWrapper<FineRecord>().eq(FineRecord::getUserId, userId));
        sysUserRoleMapper.deleteByUserId(userId);
        baseMapper.physicalDeleteById(userId);
    }

    /**
     * 生成图形验证码。
     *
     * <h4>详细步骤</h4>
     * <ol>
     *   <li>从字符集（排除易混淆字符 O/0/I/1）中随机选取 4 个字符组成验证码</li>
     *   <li>生成 UUID 作为缓存键（captchaKey）</li>
     *   <li>将验证码答案存入 {@link #captchaCache}（key=UUID, value=验证码）</li>
     *   <li>调用 {@link #generateSimpleCaptchaImage} 生成 SVG 格式的 Base64 图片</li>
     *   <li>返回 {@link CaptchaVO}（含 captchaKey 和 Base64 图片 data URL）</li>
     * </ol>
     *
     * <h4>返回值</h4>
     * <p>客户端应将 captchaKey 与用户输入一同提交到登录/注册接口。
     * captchaKey 对应的验证码在验证后即从缓存中移除（一次性使用）。</p>
     *
     * @return 验证码 VO（含缓存键和 Base64 图片）
     */
    @Override
    public CaptchaVO generateCaptcha() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 4; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        String key = UUID.randomUUID().toString();
        captchaCache.put(key, code.toString());
        String imageBase64 = generateSimpleCaptchaImage(code.toString());
        return new CaptchaVO(key, imageBase64);
    }

    /**
     * 生成简易 SVG 格式验证码图片的 Base64 编码。
     *
     * <p>生成一个 120x40 的 SVG 图片，浅灰色背景 + 深灰色加粗文字，
     * 最终编码为 {@code data:image/svg+xml;base64,...} 格式的 data URL。</p>
     *
     * @param code 验证码文字（4 位）
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
