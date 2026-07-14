package com.zipper.librarymanagement.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 分类树节点值对象
 * <p>用于前端展示无限级分类树形组件，支持递归嵌套 children</p>
 */
@Data
public class CategoryTreeVO {

    /** 分类 ID */
    private Long id;

    /** 父分类 ID（0 为顶级节点） */
    private Long parentId;

    /** 分类名称 */
    private String name;

    /** 显示顺序 */
    private Integer sort;

    /** 子分类列表（递归嵌套，末级节点为空列表） */
    private List<CategoryTreeVO> children = new ArrayList<>();
}
