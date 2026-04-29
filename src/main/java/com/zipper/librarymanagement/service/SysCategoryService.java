package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.entity.SysCategory;
import com.zipper.librarymanagement.vo.CategoryTreeVO;

import java.util.List;

/**
 * 图书分类业务接口
 * <p>支持无限级分类管理，以 parentId 实现层级关系。提供树形结构组装、
 * 新增/编辑/删除功能。删除分类时需校验是否有子分类或图书关联。</p>
 */
public interface SysCategoryService extends IService<SysCategory> {

    /**
     * 获取分类树
     * <p>查询全部分类（未被逻辑删除的），按 parentId 分组后递归组装为树形结构</p>
     * @return 树形结构列表，每一项可能包含 children 子树
     */
    List<CategoryTreeVO> getCategoryTree();

    /**
     * 新增分类
     * @param category 分类实体（parentId 为 null 时自动设为 0，即顶级节点）
     */
    void addCategory(SysCategory category);

    /**
     * 编辑分类
     * @param category 分类实体（必须包含 id）
     */
    void updateCategory(SysCategory category);

    /**
     * 删除分类
     * <p>前置校验：① 无子分类 ② 无图书关联到此分类，任一不满足则抛 BusinessException</p>
     * @param categoryId 分类 ID
     */
    void deleteCategory(Long categoryId);
}
