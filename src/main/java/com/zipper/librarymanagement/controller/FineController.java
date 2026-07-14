package com.zipper.librarymanagement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zipper.librarymanagement.common.Result;
import com.zipper.librarymanagement.common.RoleUtil;
import com.zipper.librarymanagement.service.FineRecordService;
import com.zipper.librarymanagement.vo.FineRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "罚款管理")
@RestController
@RequestMapping("/api/fine")
@RequiredArgsConstructor
public class FineController {

    private final FineRecordService fineRecordService;
    private final HttpServletRequest request;

    @Operation(summary = "分页查询罚款记录")
    @Parameters({
            @Parameter(name = "page", description = "页码"),
            @Parameter(name = "size", description = "每页条数"),
            @Parameter(name = "userId", description = "用户ID筛选"),
            @Parameter(name = "status", description = "缴费状态：0-未缴 1-已缴 2-已免除")
    })
    @GetMapping("/list")
    public Result<IPage<FineRecordVO>> list(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) Long userId,
                                            @RequestParam(required = false) Integer status) {
        IPage<FineRecordVO> result = fineRecordService.listFines(page, size, userId, status);
        return Result.success(result);
    }

    @Operation(summary = "缴纳罚款")
    @PostMapping("/pay")
    public Result<Void> pay(@Parameter(description = "罚款记录ID") @RequestParam Long fineRecordId) {
        Object idObj = request.getAttribute("userId");
        Long userId = idObj instanceof Number ? ((Number) idObj).longValue() : null;
        fineRecordService.payFine(fineRecordId, userId);
        return Result.success();
    }

    @Operation(summary = "豁免罚款（管理员）")
    @PostMapping("/exempt")
    public Result<Void> exempt(@Parameter(description = "罚款记录ID") @RequestParam Long fineRecordId) {
        RoleUtil.requireAdmin(request);
        fineRecordService.exemptFine(fineRecordId);
        return Result.success();
    }
}
