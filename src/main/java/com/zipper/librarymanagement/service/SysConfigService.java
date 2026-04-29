package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.entity.SysConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 系统配置业务接口
 * <p>维护系统运行所需的动态可配参数，如最大借阅数、借阅天数、每日罚金等。
 * 配置值采用内存缓存 + 数据库双存储，高频读取时优先走缓存。</p>
 */
public interface SysConfigService extends IService<SysConfig> {

    /**
     * 根据配置键获取字符串值
     * @param configKey 配置键名（如 "sys.borrow.max"）
     * @return 配置值字符串
     */
    String getValueByKey(String configKey);

    /**
     * 根据配置键获取整数值
     * @param configKey 配置键名
     * @return 转换后的整数值
     */
    Integer getIntByKey(String configKey);

    /**
     * 根据配置键获取金额值
     * @param configKey 配置键名
     * @return 转换后的 BigDecimal 金额
     */
    BigDecimal getDecimalByKey(String configKey);

    /**
     * 获取全部系统配置列表（管理员端配置管理页面使用）
     */
    List<SysConfig> listAllConfigs();

    /**
     * 更新单个配置项的值
     * @param configKey   配置键名
     * @param configValue 新的配置值
     */
    void updateConfig(String configKey, String configValue);

    /**
     * 批量更新配置项
     * @param configMap 配置键值对 Map
     */
    void batchUpdateConfigs(Map<String, String> configMap);

    /**
     * 刷新配置缓存
     * <p>清除内存缓存并重新从数据库加载全部配置，在配置被其他方式修改后调用</p>
     */
    void refreshCache();
}
