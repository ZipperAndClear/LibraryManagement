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
 * 系统用户业务接口
 * <p>处理用户认证（登录/注册/退出）、用户管理（CRUD）、密码修改等核心功能。
 * 用户状态变更和密码安全是本模块的关键关注点。</p>
 */
public interface SysUserService extends IService<SysUser> {

    /**
     * 用户登录
     * <ol>
     *   <li>校验图形验证码</li>
     *   <li>按 username 查库，不存在则抛异常</li>
     *   <li>校验密码</li>
     *   <li>检查账号状态（禁用则抛异常）</li>
     *   <li>查询用户角色，生成 token 并返回</li>
     * </ol>
     * @param username   登录账号
     * @param password   明文密码
     * @param captcha    图形验证码用户输入
     * @param captchaKey 验证码缓存键
     * @return 登录结果（含 token、用户信息、角色列表）
     */
    LoginResultVO login(String username, String password, String captcha, String captchaKey);

    /**
     * 用户退出登录
     * <p>将当前 token 加入黑名单（后续请求拦截器会校验黑名单）</p>
     */
    void logout(String token);

    /**
     * 获取当前登录用户的详细信息
     * @param userId 用户 ID（从已认证的 JWT token 中解析）
     */
    UserDetailVO getCurrentUser(Long userId);

    /**
     * 学生自助注册
     * <p>校验验证码、校验用户名唯一性、校验两次密码一致，
     * 保存用户后自动分配默认的学生角色。</p>
     */
    void register(RegisterDTO registerDTO);

    /**
     * 分页查询用户列表（管理员端）
     * @param page    页码
     * @param size    每页条数
     * @param keyword 关键词（模糊匹配用户名或真实姓名）
     * @param status  账号状态筛选（null=全部）
     * @return 分页结果，其中每项包含该用户的角色名称
     */
    IPage<UserListVO> listUsers(Integer page, Integer size, String keyword, Integer status);

    /**
     * 管理员新增用户
     * <p>校验用户名唯一性，保存用户并分配传入的角色列表</p>
     */
    void addUser(AddUserDTO dto);

    /**
     * 管理员编辑用户信息
     * <p>可修改基本信息及角色分配（传入 roleIds 不为 null 则表示更新角色）</p>
     */
    void updateUser(UpdateUserDTO dto);

    /**
     * 管理员重置用户密码（不需要旧密码）
     */
    void resetPassword(Long userId, String newPassword);

    /**
     * 当前用户自行修改密码（需要旧密码校验）
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     */
    void changeMyPassword(Long userId, String oldPassword, String newPassword);

    /**
     * 启用/禁用用户账号
     * @param status 1-正常 0-禁用
     */
    void updateUserStatus(Long userId, Integer status);

    /**
     * 删除用户（逻辑删除）
     * <p>同时清理该用户在 sys_user_role 中的关联记录</p>
     */
    void deleteUser(Long userId);

    /**
     * 生成图形验证码
     * @return 包含 captchaKey（缓存键）和 captchaImage（Base64 SVG 图片）的信息
     */
    CaptchaVO generateCaptcha();
}
