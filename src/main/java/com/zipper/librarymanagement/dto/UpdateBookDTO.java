package com.zipper.librarymanagement.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 编辑图书数据传输对象
 * <p>管理员修改图书信息时提交的数据，包含 id 字段用于定位</p>
 */
@Data
public class UpdateBookDTO {

    /** 图书 ID（必填，用于定位要更新的图书记录） */
    private Long id;

    /** ISBN 国际标准书号 */
    private String isbn;

    /** 图书名称 */
    private String name;

    /** 作者 */
    private String author;

    /** 出版社 */
    private String publisher;

    /** 图书价格 */
    private BigDecimal price;

    /** 库存数量 */
    private Integer stock;

    /** 所属分类 ID */
    private Long categoryId;

    /** 封面图片 URL */
    private String coverUrl;

    /** 内容简介 */
    private String introduction;
}
