package com.zipper.librarymanagement.controller;

import com.zipper.librarymanagement.common.BusinessException;
import com.zipper.librarymanagement.common.Result;
import com.zipper.librarymanagement.dto.RegisterDTO;
import com.zipper.librarymanagement.service.SysUserService;
import com.zipper.librarymanagement.vo.CaptchaVO;
import com.zipper.librarymanagement.vo.LoginResultVO;
import com.zipper.librarymanagement.vo.UserDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Tag(name = "认证管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserService sysUserService;
    private final HttpServletRequest request;

    @Operation(summary = "用户登录", description = "通过账号密码登录，校验验证码后返回JWT token。登录成功后将token存入localStorage，后续请求需在Authorization header中携带。")
    @Parameters({
            @Parameter(name = "username", description = "登录账号（学号/工号）"),
            @Parameter(name = "password", description = "登录密码（明文）"),
            @Parameter(name = "captcha", description = "图形验证码文本（4位字母数字）"),
            @Parameter(name = "captchaKey", description = "验证码缓存键（从获取验证码接口返回）")
    })
    @PostMapping("/login")
    public Result<LoginResultVO> login(@RequestParam String username,
                                       @RequestParam String password,
                                       @RequestParam(required = false) String captcha,
                                       @RequestParam(required = false) String captchaKey) {
        LoginResultVO result = sysUserService.login(username, password, captcha, captchaKey);
        return Result.success(result);
    }

    @Operation(summary = "用户注册", description = "学生自助注册，需提供学号、密码、真实姓名。注册成功后默认分配学生角色。")
    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterDTO registerDTO) {
        sysUserService.register(registerDTO);
        return Result.success();
    }

    @Operation(summary = "获取验证码", description = "生成4位字母数字组合的SVG验证码图片，返回captchaKey和Base64编码的图片数据。登录和注册时需要携带captchaKey和用户输入值。")
    @GetMapping("/captcha")
    public Result<CaptchaVO> captcha() {
        CaptchaVO captcha = sysUserService.generateCaptcha();
        return Result.success(captcha);
    }

    @Operation(summary = "退出登录", description = "将当前JWT token加入黑名单，后续请求携带此token将被拒绝。前端需清除localStorage中的token。")
    @PostMapping("/logout")
    public Result<Void> logout(@Parameter(description = "Authorization header，格式为 Bearer {token}") @RequestHeader("Authorization") String authHeader) {
        String token = authHeader.startsWith("Bearer ")
                ? authHeader.substring(7).trim()
                : authHeader;
        sysUserService.logout(token);
        return Result.success();
    }

    @Operation(summary = "获取当前登录用户信息", description = "根据JWT中解析出的userId查询用户详情，返回用户基本信息及角色权限列表。")
    @GetMapping("/me")
    public Result<UserDetailVO> me() {
        Object idObj = request.getAttribute("userId");
        Long userId = idObj instanceof Number ? ((Number) idObj).longValue() : null;
        if (userId == null) {
            throw new BusinessException("未登录或登录已过期");
        }
        UserDetailVO user = sysUserService.getCurrentUser(userId);
        return Result.success(user);
    }
}
