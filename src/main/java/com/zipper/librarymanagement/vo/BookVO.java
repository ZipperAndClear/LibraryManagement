package com.zipper.librarymanagement.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 图书信息值对象
 * <p>前端图书列表和详情展示使用的数据结构，相比 Entity 增加了分类名称字段</p>
 */
@Data
public class BookVO {

    /** 图书 ID */
    private Long id;

    /** 国际标准书号 (ISBN) */
    private String isbn;

    /** 图书名称 */
    private String name;

    /** 作者 */
    private String author;

    /** 出版社 */
    private String publisher;

    /** 图书价格（丢失赔偿参考价） */
    private BigDecimal price;

    /** 当前库存数量 */
    private Integer stock;

    /** 所属分类 ID */
    private Long categoryId;

    /** 分类名称（关联 sys_category 表查询得到，非 Entity 直接字段） */
    private String categoryName;

    /** 封面图片 URL */
    private String coverUrl;

    /** 内容简介 */
    private String introduction;

    /** 图书状态：1-在库 2-全部借出 3-下架 4-遗失 */
    private Integer status;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
