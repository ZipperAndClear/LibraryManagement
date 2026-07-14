package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.dto.AddUserDTO;
import com.zipper.librarymanagement.dto.RegisterDTO;
import com.zipper.librarymanagement.dto.UpdateUserDTO;
import com.zipper.librarymanagement.entity.SysUser;
import com.zipper.librarymanagement.vo.CaptchaVO;
import com.zipper.librarymanagement.vo.LoginResultVO;
import com.zipper.librarymanagement.vo.UserDetailVO;
import com.zipper.librarymanagement.vo.UserListVO;

/**
 * 系统用户业务接口（核心模块）
 * <p>处理用户认证（登录/注册/退出）、用户管理（CRUD）、密码修改、
 * 图形验证码生成等核心功能。用户状态变更和密码安全是本模块的关键关注点。</p>
 *
 * <p>用户状态定义：
 * <ul>
 *   <li>{@code 0} — 禁用，无法登录系统</li>
 *   <li>{@code 1} — 正常，可正常使用系统</li>
 * </ul>
 * </p>
 *
 * <p>认证流程：
 * <ol>
 *   <li>登录前先获取图形验证码（{@link #generateCaptcha()}）</li>
 *   <li>调用 {@link #login(String, String, String, String)} 完成认证</li>
 *   <li>退出时调用 {@link #logout(String)} 将 token 加入黑名单</li>
 * </ol>
 * </p>
 *
 * @author zipper
 * @since 1.0
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户登录
     * <p>完整的登录认证流程：</p>
     * <ol>
     *   <li>校验图形验证码是否正确（对比 {@code captcha} 与缓存中 {@code captchaKey} 对应的值）</li>
     *   <li>根据 {@code username} 查询数据库，用户不存在则抛出异常</li>
     *   <li>使用 BCrypt 或等效算法校验密码匹配</li>
     *   <li>检查账号状态，若为禁用（{@code status=0}）则抛出异常</li>
     *   <li>查询用户角色列表，生成 JWT token 并封装返回</li>
     * </ol>
     * <p>验证码校验通过后立即从缓存中移除，防止重复使用。</p>
     *
     * @param username   登录账号（用户名）
     * @param password   明文密码（服务端会进行加密比对）
     * @param captcha    图形验证码的用户输入值
     * @param captchaKey 验证码缓存键，由 {@link #generateCaptcha()} 返回
     * @return 登录结果 VO，包含 JWT token、用户基本信息、角色列表和权限列表
     * @throws com.zipper.librarymanagement.exception.BusinessException 若验证码错误、用户名不存在、密码错误或账号已被禁用
     */
    LoginResultVO login(String username, String password, String captcha, String captchaKey);

    /**
     * 用户退出登录
     * <p>将当前请求携带的 JWT token 加入 Redis 黑名单（以 token 过期时间作为 TTL），
     * 后续请求拦截器（如 {@code JwtInterceptor}）会校验黑名单，拒绝已注销的 token 访问。</p>
     *
     * @param token 当前用户的 JWT token（从请求头 {@code Authorization} 中提取）
     */
    void logout(String token);

    /**
     * 获取当前登录用户的详细信息
     * <p>根据用户 ID 查询用户完整信息，包括基本信息、角色列表和权限列表。
     * 通常在登录成功后或前端刷新个人信息时调用。</p>
     *
     * @param userId 用户 ID（从已认证的 JWT token 中解析，确保不会被篡改）
     * @return 用户详情 VO，包含用户基本信息、所属角色列表、权限编码列表
     * @throws com.zipper.librarymanagement.exception.BusinessException 若用户不存在
     */
    UserDetailVO getCurrentUser(Long userId);

    /**
     * 学生自助注册
     * <p>注册流程：</p>
     * <ol>
     *   <li>校验图形验证码是否正确</li>
     *   <li>校验两次输入的密码是否一致</li>
     *   <li>校验用户名是否已被占用（唯一性检查）</li>
     *   <li>对密码进行加密后保存用户记录</li>
     *   <li>自动为新用户分配默认的“学生”角色</li>
     * </ol>
     * <p>注册成功后账号状态默认为正常（{@code status=1}）。</p>
     *
     * @param registerDTO 注册数据传输对象，包含用户名、密码、确认密码、验证码等信息
     * @throws com.zipper.librarymanagement.exception.BusinessException 若验证码错误、两次密码不一致或用户名已存在
     */
    void register(RegisterDTO registerDTO);

    /**
     * 分页查询用户列表（管理员端用户管理页面）
     * <p>支持按用户名或真实姓名进行模糊搜索，支持按账号状态筛选。
     * 返回结果中每条记录包含该用户已分配的角色名称列表。</p>
     *
     * @param page    页码，从 1 开始
     * @param size    每页记录条数
     * @param keyword 关键词（模糊匹配用户名 {@code username} 或真实姓名 {@code realName}，
     *                {@code null} 或空字符串表示不筛选）
     * @param status  账号状态筛选（{@code null} 表示全部，{@code 0} 表示禁用，{@code 1} 表示正常）
     * @return 包含 {@link UserListVO} 的分页对象，每项包含用户基本信息和角色名称
     */
    IPage<UserListVO> listUsers(Integer page, Integer size, String keyword, Integer status);

    /**
     * 根据用户 ID 获取用户详细信息（包含角色 ID 列表用于编辑回填）。
     *
     * @param userId 用户 ID
     * @return 包含用户名、姓名、邮箱、手机、角色 ID 列表的用户详情
     * @throws BusinessException 若用户不存在
     */
    UserListVO getUserDetail(Long userId);

    /**
     * 管理员新增用户
     * <p>校验用户名唯一性，对密码进行加密后保存用户记录，
     * 并根据传入的角色 ID 列表为新用户分配角色。</p>
     *
     * @param dto 新增用户数据传输对象，包含用户名、密码、角色 ID 列表等信息
     * @throws com.zipper.librarymanagement.exception.BusinessException 若用户名已存在或角色 ID 无效
     */
    void addUser(AddUserDTO dto);

    /**
     * 管理员编辑用户信息
     * <p>可修改用户的基本信息（真实姓名、手机号、邮箱等）。
     * 若传入的 {@code roleIds} 不为 {@code null}，则同步更新该用户的角色分配：
     * 先删除旧的角色关联，再插入新的角色关联列表。
     * 若传入 {@code roleIds} 为 {@code null}，则保持原有角色分配不变。</p>
     *
     * @param dto 更新用户数据传输对象，包含用户 ID 及待更新的字段
     * @throws com.zipper.librarymanagement.exception.BusinessException 若用户不存在或角色 ID 无效
     */
    void updateUser(UpdateUserDTO dto);

    /**
     * 管理员重置用户密码
     * <p>无需提供旧密码，直接将指定用户的密码设置为新值。
     * 新密码在保存前会进行加密处理。常用于用户忘记密码时由管理员协助重置。</p>
     *
     * @param userId      目标用户 ID
     * @param newPassword 新密码（明文，服务端会加密存储）
     * @throws com.zipper.librarymanagement.exception.BusinessException 若用户不存在或新密码不符合复杂度要求
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 当前用户自行修改密码
     * <p>需要用户提供旧密码进行身份验证，验证通过后才能设置新密码。
     * 新密码不能与旧密码相同，且需满足密码复杂度要求。</p>
     *
     * @param userId      当前登录用户 ID
     * @param oldPassword 旧密码（明文，用于校验身份）
     * @param newPassword 新密码（明文，校验通过后加密存储）
     * @throws com.zipper.librarymanagement.exception.BusinessException 若旧密码错误、新旧密码相同或新密码不符合复杂度要求
     */
    void changeMyPassword(Long userId, String oldPassword, String newPassword);

    /**
     * 启用/禁用用户账号
     * <p>禁用用户后，该用户无法登录系统，且其 JWT token 将被视为无效。
     * 已登录的禁用用户将在下一次请求时被拦截器拒绝访问。
     * 启用用户后，该用户恢复正常登录权限。</p>
     *
     * @param userId 目标用户 ID
     * @param status 目标状态（{@code 0} — 禁用，{@code 1} — 正常）
     * @throws com.zipper.librarymanagement.exception.BusinessException 若用户不存在或状态值非法
     */
    void updateUserStatus(Long userId, Integer status);

    /**
     * 删除用户（物理删除）
     * <p>从数据库中彻底移除该用户记录，不可恢复。删除用户的同时
     * 会清理该用户在 {@code sys_user_role} 表中的所有角色关联记录，
     * 确保不会遗留孤立数据。</p>
     *
     * @param userId 待删除的用户 ID
     * @throws com.zipper.librarymanagement.exception.BusinessException 若用户不存在或为系统内置管理员账号
     */
    void deleteUser(Long userId);

    /**
     * 生成图形验证码
     * <p>生成一个随机数学表达式（如“3+5=?”）的验证码，
     * 将计算结果存入 Redis 缓存并返回 Base64 编码的 SVG 图片字符串。
     * Redis 缓存键为随机生成的 UUID，默认有效期为 5 分钟。</p>
     *
     * @return 验证码 VO，包含 {@code captchaKey}（缓存键，登录时需回传）和
     *         {@code captchaImage}（Base64 编码的 SVG 图片，可直接嵌入 {@code <img>} 标签）
     */
    CaptchaVO generateCaptcha();
}
