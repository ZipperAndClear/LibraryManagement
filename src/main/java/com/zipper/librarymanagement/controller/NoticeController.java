package com.zipper.librarymanagement.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zipper.librarymanagement.common.Result;
import com.zipper.librarymanagement.common.RoleUtil;
import com.zipper.librarymanagement.entity.SysNotice;
import com.zipper.librarymanagement.service.SysNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "公告管理")
@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final SysNoticeService noticeService;
    private final HttpServletRequest request;

    @Operation(summary = "分页查询公告列表", description = "支持按状态筛选和标题关键词搜索。管理员可查看全部状态，学生只能看到已发布公告。排序规则：置顶优先、按时间倒序。")
    @Parameters({
            @Parameter(name = "page", description = "页码"),
            @Parameter(name = "size", description = "每页条数"),
            @Parameter(name = "status", description = "公告状态筛选：0-草稿 1-已发布（不传则查全部）"),
            @Parameter(name = "keyword", description = "标题关键词模糊搜索")
    })
    @GetMapping("/list")
    public Result<IPage<SysNotice>> list(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam(required = false) Integer status,
                                         @RequestParam(required = false) String keyword) {
        RoleUtil.requireAdmin(request);
        IPage<SysNotice> result = noticeService.listNotices(page, size, status, keyword);
        return Result.success(result);
    }

    @Operation(summary = "已发布公告列表", description = "获取所有已发布的公告列表（学生端首页展示用），置顶公告排在最前面。")
    @GetMapping("/published")
    public Result<List<SysNotice>> published() {
        List<SysNotice> notices = noticeService.getPublishedNotices();
        return Result.success(notices);
    }

    @Operation(summary = "新增公告", description = "新增公告，默认为草稿状态。可指定标题、正文内容、是否置顶。")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody SysNotice notice) {
        RoleUtil.requireAdmin(request);
        noticeService.addNotice(notice);
        return Result.success();
    }

    @Operation(summary = "编辑公告", description = "编辑已有公告的标题、正文、置顶状态等信息。")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody SysNotice notice) {
        RoleUtil.requireAdmin(request);
        noticeService.updateNotice(notice);
        return Result.success();
    }

    @Operation(summary = "发布公告", description = "将草稿状态的公告发布，发布后学生端可看到。状态流转：草稿 → 已发布。")
    @PutMapping("/publish/{id}")
    public Result<Void> publish(@Parameter(description = "公告ID") @PathVariable Long id) {
        RoleUtil.requireAdmin(request);
        noticeService.publishNotice(id);
        return Result.success();
    }

    @Operation(summary = "撤回公告", description = "将已发布的公告撤回为草稿状态，撤回后学生端不再显示。状态流转：已发布 → 草稿。")
    @PutMapping("/unpublish/{id}")
    public Result<Void> unpublish(@Parameter(description = "公告ID") @PathVariable Long id) {
        RoleUtil.requireAdmin(request);
        noticeService.unpublishNotice(id);
        return Result.success();
    }

    @Operation(summary = "删除公告", description = "直接删除公告。")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@Parameter(description = "公告ID") @PathVariable Long id) {
        RoleUtil.requireSuperAdmin(request);
        noticeService.deleteNotice(id);
        return Result.success();
    }
}
