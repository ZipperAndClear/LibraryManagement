package com.zipper.librarymanagement.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
// 核心：对应数据库的 sys_config 表
@TableName("sys_config")
public class SysConfig {
    /**
     * 参数主键 - 主键（自增）
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 参数名称
     */
    @TableField("config_name")
    private String configName;

    /**
     * 参数键名（唯一索引）
     */
    @TableField("config_key")
    private String configKey;

    /**
     * 参数键值
     */
    @TableField("config_value")
    private String configValue;

    /**
     * 备注
     */
    @TableField("remark")
    private String remark;

    /**
     * 创建时间（格式化输出）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("create_time")
    private LocalDateTime createTime;

    /**
     * 更新时间（格式化输出）
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField("update_time")
    private LocalDateTime updateTime;
}