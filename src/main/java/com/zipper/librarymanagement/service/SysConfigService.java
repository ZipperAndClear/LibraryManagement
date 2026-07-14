package com.zipper.librarymanagement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zipper.librarymanagement.entity.SysConfig;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 系统配置业务接口
 * <p>
 * 维护图书馆管理系统运行所需的动态可配参数，如最大借阅数、借阅天数、每日罚金等。
 * 采用<b>内存缓存 + 数据库双存储</b>架构：高频读取时优先从内存缓存获取，避免频繁查询数据库；
 * 写入操作同时更新数据库和缓存，保证数据一致性与性能兼备。
 * </p>
 *
 * <h3>常用配置键说明</h3>
 * <ul>
 *   <li>{@code sys.borrow.max} — 每位用户最大同时借阅数（整数）</li>
 *   <li>{@code sys.borrow.days} — 单次借阅的默认天数（整数）</li>
 *   <li>{@code sys.fine.per.day} — 每日逾期罚金金额（金额）</li>
 *   <li>{@code sys.renew.max} — 每本书最大续借次数（整数）</li>
 * </ul>
 *
 * <h3>主要功能</h3>
 * <ul>
 *   <li><b>配置读取</b> — 通过配置键获取字符串、整数或金额类型的值</li>
 *   <li><b>配置列表</b> — 获取全部配置项供管理员配置页面使用</li>
 *   <li><b>配置更新</b> — 支持单个或批量更新配置值，自动同步数据库与缓存</li>
 *   <li><b>缓存刷新</b> — 手动清除并重建缓存，用于配置被外部修改后的同步场景</li>
 * </ul>
 *
 * @author zipper
 */
public interface SysConfigService extends IService<SysConfig> {

    /**
     * 根据配置键获取字符串类型的配置值
     * <p>
     * 优先从内存缓存中读取，缓存未命中时从数据库加载并写入缓存。
     * </p>
     *
     * @param key 配置键名，如 {@code "sys.borrow.max"}、{@code "sys.borrow.days"}
     * @return 配置值字符串；若该键不存在则返回 {@code null}
     */
    String getValueByKey(String key);

    /**
     * 根据配置键获取整数类型的配置值
     * <p>
     * 内部调用 {@link #getValueByKey(String)} 获取字符串值后转换为 {@link Integer}。
     * 该方法用于读取借阅上限、借阅天数、续借次数等整数类型配置。
     * </p>
     *
     * @param key 配置键名，如 {@code "sys.borrow.max"}、{@code "sys.renew.max"}
     * @return 转换后的整数值；若该键不存在或值无法解析为整数则返回 {@code null}
     */
    Integer getIntByKey(String key);

    /**
     * 根据配置键获取金额类型的配置值
     * <p>
     * 内部调用 {@link #getValueByKey(String)} 获取字符串值后转换为 {@link BigDecimal}。
     * 该方法用于读取每日罚金等金额类型配置。
     * </p>
     *
     * @param key 配置键名，如 {@code "sys.fine.per.day"}
     * @return 转换后的 {@link BigDecimal} 金额值；若该键不存在或值无法解析则返回 {@code null}
     */
    BigDecimal getDecimalByKey(String key);

    /**
     * 获取全部系统配置项的列表
     * <p>
     * 查询数据库中所有未被逻辑删除的配置记录，供管理员端配置管理页面展示和编辑。
     * 返回结果按配置键排序，不依赖缓存以确保数据的完整性和实时性。
     * </p>
     *
     * @return 全部系统配置实体列表；无任何配置时返回空列表
     */
    List<SysConfig> listAllConfigs();

    /**
     * 更新单个配置项的值
     * <p>
     * 同时更新数据库记录和内存缓存，保证后续读取立即生效。
     * 若指定的配置键在数据库中不存在，将抛出 {@code BusinessException}。
     * </p>
     *
     * @param key   要更新的配置键名，如 {@code "sys.borrow.max"}
     * @param value 新的配置值字符串
     */
    void updateConfig(String key, String value);

    /**
     * 批量更新配置项
     * <p>
     * 一次性更新多个配置项，所有更新在同一事务中执行，保证原子性。
     * 更新完成后统一刷新缓存，确保所有变更同时生效。
     * </p>
     * <p>
     * 该方法适用于管理员配置页面一次性提交多个修改的场景。
     * </p>
     *
     * @param configMap 配置键值对 Map，key 为配置键名，value 为新的配置值
     */
    void batchUpdateConfigs(Map<String, String> configMap);

    /**
     * 刷新配置缓存
     * <p>
     * 清除当前内存中的全部配置缓存，并从数据库重新加载所有配置项到缓存中。
     * 在以下场景中需要调用此方法：
     * </p>
     * <ul>
     *   <li>配置被其他方式（如数据库直连、SQL 脚本）直接修改后</li>
     *   <li>多实例部署场景中，某一实例修改配置后通知其他实例刷新</li>
     *   <li>缓存数据异常需要重建时</li>
     * </ul>
     */
    void refreshCache();
}
