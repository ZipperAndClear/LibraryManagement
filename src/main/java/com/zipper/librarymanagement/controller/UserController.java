package com.zipper.librarymanagement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zipper.librarymanagement.common.Result;
import com.zipper.librarymanagement.common.RoleUtil;
import com.zipper.librarymanagement.dto.AddUserDTO;
import com.zipper.librarymanagement.dto.UpdateUserDTO;
import com.zipper.librarymanagement.service.SysUserService;
import com.zipper.librarymanagement.vo.UserListVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final SysUserService sysUserService;
    private final HttpServletRequest request;

    @Operation(summary = "分页查询用户列表", description = "支持按关键词（用户名/真实姓名）和账号状态筛选，返回结果中每个用户包含其角色名称列表。")
    @Parameters({
            @Parameter(name = "page", description = "页码，从1开始"),
            @Parameter(name = "size", description = "每页条数"),
            @Parameter(name = "keyword", description = "关键词（模糊匹配用户名或真实姓名）"),
            @Parameter(name = "status", description = "账号状态筛选：1-正常 0-禁用（不传则查全部）")
    })
    @GetMapping("/list")
    public Result<IPage<UserListVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer status) {
        RoleUtil.requireSuperAdmin(request);
        IPage<UserListVO> result = sysUserService.listUsers(page, size, keyword, status);
        return Result.success(result);
    }

    @Operation(summary = "获取用户详情", description = "根据用户ID获取单个用户的基本信息和角色权限，供编辑用户时回填表单使用。")
    @GetMapping("/detail/{id}")
    public Result<UserListVO> detail(@Parameter(description = "用户ID") @PathVariable Long id) {
        RoleUtil.requireSuperAdmin(request);
        UserListVO user = sysUserService.getUserDetail(id);
        return Result.success(user);
    }

    @Operation(summary = "新增用户", description = "管理员后台创建用户。校验用户名唯一性，保存用户信息后按传入的角色ID列表分配角色。")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody AddUserDTO dto) {
        RoleUtil.requireSuperAdmin(request);
        sysUserService.addUser(dto);
        return Result.success();
    }

    @Operation(summary = "编辑用户", description = "编辑用户基本信息（真实姓名、邮箱、手机号）和角色分配。传入roleIds为null表示不修改角色。")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody UpdateUserDTO dto) {
        RoleUtil.requireSuperAdmin(request);
        sysUserService.updateUser(dto);
        return Result.success();
    }

    @Operation(summary = "重置用户密码", description = "管理员直接重置指定用户的密码，无需旧密码校验。")
    @Parameters({
            @Parameter(name = "userId", description = "目标用户ID"),
            @Parameter(name = "newPassword", description = "新密码（明文）")
    })
    @PutMapping("/reset-password")
    public Result<Void> resetPassword(@RequestParam Long userId,
                                      @RequestParam String newPassword) {
        RoleUtil.requireSuperAdmin(request);
        sysUserService.resetPassword(userId, newPassword);
        return Result.success();
    }

    @Operation(summary = "当前用户修改密码", description = "用户自行修改密码，需验证原密码正确性。")
    @Parameters({
            @Parameter(name = "userId", description = "当前登录用户ID"),
            @Parameter(name = "oldPassword", description = "原密码"),
            @Parameter(name = "newPassword", description = "新密码")
    })
    @PutMapping("/change-password")
    public Result<Void> changePassword(@RequestParam Long userId,
                                       @RequestParam String oldPassword,
                                       @RequestParam String newPassword) {
        RoleUtil.requireSuperAdmin(request);
        sysUserService.changeMyPassword(userId, oldPassword, newPassword);
        return Result.success();
    }

    @Operation(summary = "启用/禁用用户", description = "修改用户账号状态：1-正常 0-禁用。被禁用的用户无法登录系统。")
    @Parameters({
            @Parameter(name = "userId", description = "目标用户ID"),
            @Parameter(name = "status", description = "状态值：1-正常 0-禁用")
    })
    @PutMapping("/status")
    public Result<Void> updateStatus(@RequestParam Long userId,
                                     @RequestParam Integer status) {
        RoleUtil.requireSuperAdmin(request);
        sysUserService.updateUserStatus(userId, status);
        return Result.success();
    }

    @Operation(summary = "删除用户", description = "逻辑删除用户，同时清理该用户的角色关联记录。删除后用户无法登录。")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@Parameter(description = "用户ID") @PathVariable Long id) {
        RoleUtil.requireSuperAdmin(request);
        sysUserService.deleteUser(id);
        return Result.success();
    }
}
