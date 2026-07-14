package com.zipper.librarymanagement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zipper.librarymanagement.common.BusinessException;
import com.zipper.librarymanagement.entity.Book;
import com.zipper.librarymanagement.entity.SysCategory;
import com.zipper.librarymanagement.mapper.BookMapper;
import com.zipper.librarymanagement.mapper.SysCategoryMapper;
import com.zipper.librarymanagement.service.SysCategoryService;
import com.zipper.librarymanagement.vo.CategoryTreeVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 图书分类业务实现类。
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li>分类树构建：通过 {@code parentId} 字段实现无限层级，递归构建树形结构</li>
 *   <li>分类 CRUD：新增、更新、删除分类</li>
 *   <li>删除前完整性校验：确保无子分类且无图书关联</li>
 * </ul>
 *
 * <h3>关键依赖</h3>
 * <ul>
 *   <li>{@link SysCategoryMapper}：分类数据持久化（含物理删除方法）</li>
 *   <li>{@link BookMapper}：删除前校验是否有图书属于此分类</li>
 * </ul>
 *
 * <h3>事务边界</h3>
 * <p>所有写操作（新增、更新、删除）均使用 {@code @Transactional} 注解。
 * 读操作（查询分类树）不加事务。</p>
 *
 * @see SysCategoryService
 * @see SysCategory
 * @see CategoryTreeVO
 */
@Service
public class SysCategoryServiceImpl extends ServiceImpl<SysCategoryMapper, SysCategory> implements SysCategoryService {

    @Autowired
    private BookMapper bookMapper;

    /**
     * 获取完整的分类树结构（无限层级）。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>查出全部未删除的分类，按 {@code sort} 升序排列</li>
     *   <li>按 {@code parentId} 将所有分类分组（{@code parentId} 为 {@code null} 的视为根节点，归入 parentId=0 组）</li>
     *   <li>找出所有根节点（parentId=0），逐一递归构建子树</li>
     *   <li>递归过程：对每个节点，从分组 Map 中取出其子节点列表，递归构建子节点的子树</li>
     * </ol>
     *
     * <p><b>注意：</b>当前实现未处理逻辑删除（deleted 标记），仅查询了物理存在的记录。</p>
     *
     * @return 分类树根节点列表（平铺的一级分类，每项内含嵌套的 children）
     */
    @Override
    public List<CategoryTreeVO> getCategoryTree() {
        List<SysCategory> all = lambdaQuery().orderByAsc(SysCategory::getSort).list();
        Map<Long, List<SysCategory>> parentMap = all.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));
        List<CategoryTreeVO> tree = new ArrayList<>();
        List<SysCategory> roots = parentMap.getOrDefault(0L, new ArrayList<>());
        for (SysCategory root : roots) {
            tree.add(buildTree(root, parentMap));
        }
        return tree;
    }

    /**
     * 递归构建分类树节点。
     *
     * <p>将 {@link SysCategory} 实体转换为 {@link CategoryTreeVO}，
     * 递归加载其所有子节点。</p>
     *
     * @param category  当前分类实体
     * @param parentMap 按 parentId 分组后的分类 Map（key=parentId, value=子分类列表）
     * @return 构建好的分类树节点（含嵌套子节点）
     */
    private CategoryTreeVO buildTree(SysCategory category, Map<Long, List<SysCategory>> parentMap) {
        CategoryTreeVO node = new CategoryTreeVO();
        node.setId(category.getId());
        node.setParentId(category.getParentId());
        node.setName(category.getName());
        node.setSort(category.getSort());
        List<SysCategory> children = parentMap.getOrDefault(category.getId(), new ArrayList<>());
        for (SysCategory child : children) {
            node.getChildren().add(buildTree(child, parentMap));
        }
        return node;
    }

    /**
     * 新增图书分类。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>若未指定父分类 ID，默认设为 0（顶级节点）</li>
     *   <li>持久化到数据库</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param category 分类实体（需至少包含 name，可选 parentId、sort）
     */
    @Override
    @Transactional
    public void addCategory(SysCategory category) {
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        save(category);
    }

    /**
     * 更新图书分类信息。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>直接按 ID 执行更新（全量覆盖）</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param category 分类实体（必须包含 id，其余字段按需传入）
     */
    @Override
    @Transactional
    public void updateCategory(SysCategory category) {
        updateById(category);
    }

    /**
     * 物理删除图书分类。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li><b>校验子分类</b>：查询是否有子分类（parentId = 当前分类 ID），若有则拒绝删除</li>
     *   <li><b>校验关联图书</b>：查询是否有图书属于此分类，若有则拒绝删除</li>
     *   <li>两项校验均通过后，执行物理删除</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param categoryId 分类 ID
     * @throws BusinessException 若存在子分类 或 存在关联图书
     */
    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        long childCount = lambdaQuery().eq(SysCategory::getParentId, categoryId).count();
        if (childCount > 0) {
            throw new BusinessException("该分类下有子分类，无法删除");
        }
        long bookCount = bookMapper.selectCount(
                new LambdaQueryWrapper<Book>().eq(Book::getCategoryId, categoryId));
        if (bookCount > 0) {
            throw new BusinessException("该分类下有图书，无法删除");
        }
        baseMapper.physicalDeleteById(categoryId);
    }
}
