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
 * 图书分类业务实现类
 * <p>分类以 parentId 实现无限层级。getCategoryTree 将所有分类按 parentId 分组后
 * 从根节点（parentId=0）开始递归构建树结构。删除时校验子分类和图书关联。</p>
 */
@Service
public class SysCategoryServiceImpl extends ServiceImpl<SysCategoryMapper, SysCategory> implements SysCategoryService {

    @Autowired
    private BookMapper bookMapper;

    @Override
    public List<CategoryTreeVO> getCategoryTree() {
        // 查出全部未删除的分类，按 sort 升序排列
        List<SysCategory> all = lambdaQuery().orderByAsc(SysCategory::getSort).list();
        // 按 parentId 分组，方便递归
        Map<Long, List<SysCategory>> parentMap = all.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));
        List<CategoryTreeVO> tree = new ArrayList<>();
        // 从根节点开始递归构建
        List<SysCategory> roots = parentMap.getOrDefault(0L, new ArrayList<>());
        for (SysCategory root : roots) {
            tree.add(buildTree(root, parentMap));
        }
        return tree;
    }

    /**
     * 递归构建分类树节点
     * @param category  当前分类
     * @param parentMap 按 parentId 分组后的分类 Map
     */
    private CategoryTreeVO buildTree(SysCategory category, Map<Long, List<SysCategory>> parentMap) {
        CategoryTreeVO node = new CategoryTreeVO();
        node.setId(category.getId());
        node.setParentId(category.getParentId());
        node.setName(category.getName());
        node.setSort(category.getSort());
        // 递归构建子节点
        List<SysCategory> children = parentMap.getOrDefault(category.getId(), new ArrayList<>());
        for (SysCategory child : children) {
            node.getChildren().add(buildTree(child, parentMap));
        }
        return node;
    }

    @Override
    @Transactional
    public void addCategory(SysCategory category) {
        // 未指定父分类时默认作为顶级节点
        if (category.getParentId() == null) {
            category.setParentId(0L);
        }
        save(category);
    }

    @Override
    @Transactional
    public void updateCategory(SysCategory category) {
        updateById(category);
    }

    @Override
    @Transactional
    public void deleteCategory(Long categoryId) {
        // 校验：是否有子分类
        long childCount = lambdaQuery().eq(SysCategory::getParentId, categoryId).count();
        if (childCount > 0) {
            throw new BusinessException("该分类下有子分类，无法删除");
        }
        // 校验：是否有图书属于此分类
        long bookCount = bookMapper.selectCount(
                new LambdaQueryWrapper<Book>().eq(Book::getCategoryId, categoryId));
        if (bookCount > 0) {
            throw new BusinessException("该分类下有图书，无法删除");
        }
        removeById(categoryId);
    }
}
