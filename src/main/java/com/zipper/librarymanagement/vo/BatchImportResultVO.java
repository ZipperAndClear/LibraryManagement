package com.zipper.librarymanagement.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入图书结果值对象
 * <p>返回给前端的 Excel 批量导入结果，包含成功/失败数量和失败明细</p>
 */
@Data
public class BatchImportResultVO {

    /** 成功导入的图书数量 */
    private int successCount;

    /** 导入失败的图书数量 */
    private int failCount;

    /** 失败原因列表（每条对应一行导入失败的记录及原因） */
    private List<String> failMessages = new ArrayList<>();
}
