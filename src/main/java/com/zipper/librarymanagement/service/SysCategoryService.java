package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.entity.SysCategory;
import com.zipper.librarymanagement.vo.CategoryTreeVO;

import java.util.List;

/**
 * 图书分类业务接口
 * <p>
 * 管理图书分类体系的完整 CRUD 操作，支持无限级分类层级。
 * 分类以 {@code parentId} 字段实现父子关联（{@code parentId = 0} 表示顶级节点），
 * 提供树形结构组装功能供前端分类选择器使用。
 * </p>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>分类树查询</b> — 查询全部未删除分类并按父子关系递归组装为树形结构</li>
 *   <li><b>新增分类</b> — 添加新的分类节点，支持指定父级分类</li>
 *   <li><b>编辑分类</b> — 修改已有分类的名称、排序等属性</li>
 *   <li><b>删除分类</b> — 删除前校验无子分类和无图书关联，保证数据完整性</li>
 * </ul>
 *
 * @author zipper
 */
public interface SysCategoryService extends IService<SysCategory> {

    /**
     * 获取完整的分类树
     * <p>
     * 查询数据库中所有未被逻辑删除的分类记录，按 {@code parentId} 分组后
     * 从顶级节点（{@code parentId = 0}）开始递归组装树形结构。
     * 每个节点包含自身信息及其子分类列表 {@code children}。
     * </p>
     * <p>
     * 树形结构按 {@code sortOrder} 升序排列，便于前端直接渲染分类选择器或导航菜单。
     * </p>
     *
     * @return 分类树形结构列表，每项为 {@link CategoryTreeVO}，可能包含嵌套的 {@code children} 子树；
     *         无任何分类时返回空列表
     */
    List<CategoryTreeVO> getCategoryTree();

    /**
     * 新增图书分类
     * <p>
     * 在分类体系中添加一个新的分类节点。若传入的 {@code parentId} 为 {@code null}，
     * 系统自动将其设置为 {@code 0}，表示该分类为顶级节点。
     * </p>
     * <p>
     * 新增前会校验同级下分类名称的唯一性，防止重名。
     * </p>
     *
     * @param category 待新增的分类实体对象，需包含分类名称、父级 ID 等信息
     */
    void addCategory(SysCategory category);

    /**
     * 编辑已有图书分类
     * <p>
     * 更新指定分类的属性信息（如名称、排序等）。传入的分类实体必须包含有效的 {@code id}，
     * 系统根据该 ID 定位并更新对应记录。
     * </p>
     * <p>
     * 编辑前会校验同名冲突：若修改后的名称与同级其他分类重名，则拒绝更新。
     * </p>
     *
     * @param category 待更新的分类实体对象，必须包含有效的分类 ID 及要更新的字段
     */
    void updateCategory(SysCategory category);

    /**
     * 删除指定图书分类
     * <p>
     * 执行删除前进行两项前置校验，任一条件不满足则抛出 {@code BusinessException}：
     * </p>
     * <ol>
     *   <li><b>无子分类</b> — 该分类下不得存在以本分类为父级的子分类，
     *       需先删除或移走所有子分类</li>
     *   <li><b>无图书关联</b> — 不得存在以本分类作为归属分类的图书记录，
     *       需先将关联图书修改到其他分类</li>
     * </ol>
     * <p>
     * 校验通过后执行逻辑删除（{@code is_deleted = 1}），而非物理删除。
     * </p>
     *
     * @param categoryId 要删除的分类唯一标识
     */
    void deleteCategory(Long categoryId);
}
