package com.zipper.librarymanagement.controller;

import com.zipper.librarymanagement.common.Result;
import com.zipper.librarymanagement.common.RoleUtil;
import com.zipper.librarymanagement.entity.SysConfig;
import com.zipper.librarymanagement.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "系统配置")
@RestController
@RequestMapping("/api/config")
@RequiredArgsConstructor
public class ConfigController {

    private final SysConfigService configService;
    private final HttpServletRequest request;

    @Operation(summary = "获取所有配置", description = "返回系统全部可配参数列表（如最大借阅数、借阅天数、每日罚金等），供管理员在后台查看和修改。")
    @GetMapping("/list")
    public Result<List<SysConfig>> list() {
        RoleUtil.requireSuperAdmin(request);
        List<SysConfig> configs = configService.listAllConfigs();
        return Result.success(configs);
    }

    @Operation(summary = "更新单个配置", description = "根据配置键更新对应的配置值。")
    @Parameters({
            @Parameter(name = "configKey", description = "配置键名（如 sys.borrow.max、sys.borrow.days、sys.fine.per_day）"),
            @Parameter(name = "configValue", description = "新的配置值（字符串格式，程序会自动转换类型）")
    })
    @PutMapping("/update")
    public Result<Void> update(@RequestParam String configKey,
                               @RequestParam String configValue) {
        RoleUtil.requireSuperAdmin(request);
        configService.updateConfig(configKey, configValue);
        return Result.success();
    }

    @Operation(summary = "批量更新配置", description = "批量更新多项配置值。传入JSON对象，key为配置键名，value为配置值。")
    @PutMapping("/batch-update")
    public Result<Void> batchUpdate(@RequestBody Map<String, String> configMap) {
        RoleUtil.requireSuperAdmin(request);
        configService.batchUpdateConfigs(configMap);
        return Result.success();
    }
}
