package com.zipper.librarymanagement.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 新增图书数据传输对象
 * <p>管理员录入新书时提交的表单数据</p>
 */
@Data
public class AddBookDTO {

    /** ISBN 国际标准书号（必填，需唯一） */
    private String isbn;

    /** 图书名称（必填） */
    private String name;

    /** 作者 */
    private String author;

    /** 出版社 */
    private String publisher;

    /** 图书价格 */
    private BigDecimal price;

    /** 初始库存数量 */
    private Integer stock;

    /** 所属分类 ID */
    private Long categoryId;

    /** 封面图片 URL */
    private String coverUrl;

    /** 内容简介 */
    private String introduction;
}
