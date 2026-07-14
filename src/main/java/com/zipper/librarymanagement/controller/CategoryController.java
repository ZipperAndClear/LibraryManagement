package com.zipper.librarymanagement.controller;

import com.zipper.librarymanagement.common.Result;
import com.zipper.librarymanagement.common.RoleUtil;
import com.zipper.librarymanagement.entity.SysCategory;
import com.zipper.librarymanagement.service.SysCategoryService;
import com.zipper.librarymanagement.vo.CategoryTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "分类管理")
@RestController
@RequestMapping("/api/category")
@RequiredArgsConstructor
public class CategoryController {

    private final SysCategoryService categoryService;
    private final HttpServletRequest request;

    @Operation(summary = "获取分类树", description = "以树形结构返回全部分类（从顶级节点开始递归包含子分类），用于前端分类选择器展示。")
    @GetMapping("/tree")
    public Result<List<CategoryTreeVO>> tree() {
        List<CategoryTreeVO> tree = categoryService.getCategoryTree();
        return Result.success(tree);
    }

    @Operation(summary = "新增分类", description = "新增图书分类。parentId为空时作为顶级节点，不为空时作为指定父分类的子节点。")
    @PostMapping("/add")
    public Result<Void> add(@RequestBody SysCategory category) {
        RoleUtil.requireAdmin(request);
        categoryService.addCategory(category);
        return Result.success();
    }

    @Operation(summary = "编辑分类", description = "修改分类名称、排序等基本信息。")
    @PutMapping("/update")
    public Result<Void> update(@RequestBody SysCategory category) {
        RoleUtil.requireAdmin(request);
        categoryService.updateCategory(category);
        return Result.success();
    }

    @Operation(summary = "删除分类", description = "删除分类。前置校验：如有子分类或有关联图书则禁止删除。")
    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@Parameter(description = "分类ID") @PathVariable Long id) {
        RoleUtil.requireAdmin(request);
        categoryService.deleteCategory(id);
        return Result.success();
    }
}
