package com.zipper.librarymanagement.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zipper.librarymanagement.common.BusinessException;
import com.zipper.librarymanagement.entity.SysConfig;
import com.zipper.librarymanagement.mapper.SysConfigMapper;
import com.zipper.librarymanagement.service.SysConfigService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置业务实现类。
 *
 * <h3>核心职责</h3>
 * <ul>
 *   <li>提供系统配置项的读取（支持 {@link String}、{@link Integer}、{@link BigDecimal} 类型）</li>
 *   <li>单个配置项更新</li>
 *   <li>批量配置项更新</li>
 *   <li>缓存管理与全量刷新</li>
 * </ul>
 *
 * <h3>缓存策略</h3>
 * <p>使用 {@link ConcurrentHashMap} 做本地内存缓存（key=configKey, value=configValue）。
 * 读取路径：先查缓存 → 缓存未命中查数据库 → 回填缓存。
 * 更新配置时同步清除对应的缓存键，确保读到的永远是最新值。
 * 批量更新后可通过 {@link #refreshCache()} 全量刷新。</p>
 *
 * <p><b>注意：</b>当前实现使用本地缓存，多实例部署时缓存不一致问题需通过 Redis 等分布式缓存解决。</p>
 *
 * <h3>关键依赖</h3>
 * <ul>
 *   <li>{@link SysConfigMapper}：系统配置表的数据访问</li>
 * </ul>
 *
 * <h3>事务边界</h3>
 * <p>只有写操作（{@link #updateConfig}、{@link #batchUpdateConfigs}）使用
 * {@code @Transactional}。读操作及缓存刷新操作不加事务。</p>
 *
 * @see SysConfigService
 * @see SysConfig
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    /** 配置值本地缓存（key=configKey, value=configValue） */
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    /**
     * 根据配置键获取配置值（{@link String} 类型）。
     *
     * <h4>缓存读取流程</h4>
     * <ol>
     *   <li>从 {@link #configCache} 中查询缓存</li>
     *   <li>缓存命中 → 直接返回</li>
     *   <li>缓存未命中 → 查询数据库</li>
     *   <li>若数据库中也无此配置 → 抛出 {@link BusinessException}</li>
     *   <li>将数据库值存入缓存后返回</li>
     * </ol>
     *
     * @param configKey 配置键（如 {@code "sys.borrow.max"}）
     * @return 配置值字符串
     * @throws BusinessException 若配置不存在
     */
    @Override
    public String getValueByKey(String configKey) {
        String cached = configCache.get(configKey);
        if (cached != null) {
            return cached;
        }
        SysConfig config = lambdaQuery().eq(SysConfig::getConfigKey, configKey).one();
        if (config == null) {
            throw new BusinessException("系统配置不存在: " + configKey);
        }
        configCache.put(configKey, config.getConfigValue());
        return config.getConfigValue();
    }

    /**
     * 根据配置键获取配置值（{@link Integer} 类型）。
     *
     * <p>内部调用 {@link #getValueByKey} 获取字符串值后解析为整数。
     * 常用于读取数量/次数类配置（如最大借阅数量、续借次数上限）。</p>
     *
     * @param configKey 配置键
     * @return 整型配置值
     * @throws BusinessException 若配置不存在
     * @throws NumberFormatException 若配置值无法解析为整数
     */
    @Override
    public Integer getIntByKey(String configKey) {
        String value = getValueByKey(configKey);
        return Integer.valueOf(value);
    }

    /**
     * 根据配置键获取配置值（{@link BigDecimal} 类型）。
     *
     * <p>内部调用 {@link #getValueByKey} 获取字符串值后解析为 BigDecimal。
     * 常用于读取金额类配置（如每日罚款金额）。</p>
     *
     * @param configKey 配置键
     * @return BigDecimal 配置值
     * @throws BusinessException 若配置不存在
     * @throws NumberFormatException 若配置值不是有效数字
     */
    @Override
    public BigDecimal getDecimalByKey(String configKey) {
        String value = getValueByKey(configKey);
        return new BigDecimal(value);
    }

    /**
     * 列出全部系统配置项。
     *
     * @return 所有配置实体列表
     */
    @Override
    public List<SysConfig> listAllConfigs() {
        return list();
    }

    /**
     * 更新单个配置项的值。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>查数据库确认配置项存在，否则抛出异常</li>
     *   <li>更新 {@code configValue} 并持久化</li>
     *   <li><b>清除缓存</b>：从 {@link #configCache} 中移除该键，
     *       确保下次读取时重新从数据库加载最新值</li>
     * </ol>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}。</p>
     *
     * @param configKey   配置键
     * @param configValue 新的配置值
     * @throws BusinessException 若配置不存在
     */
    @Override
    @Transactional
    public void updateConfig(String configKey, String configValue) {
        SysConfig config = lambdaQuery().eq(SysConfig::getConfigKey, configKey).one();
        if (config == null) {
            throw new BusinessException("配置不存在: " + configKey);
        }
        config.setConfigValue(configValue);
        updateById(config);
        configCache.remove(configKey);
    }

    /**
     * 批量更新配置项。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>遍历 Map 中的每个键值对</li>
     *   <li>逐条调用 {@link #updateConfig}（每条独立事务提交）</li>
     * </ol>
     *
     * <p><b>注意：</b>当前实现是逐个调用 {@link #updateConfig}，
     * 每条配置更新在各自的事务中提交，而非整体事务。</p>
     *
     * <h4>事务</h4>
     * <p>标注 {@code @Transactional}，但内部每个 {@link #updateConfig} 调用
     * 由于 Spring AOP 自调用问题，可能不启用事务。
     * 生产环境建议重构为内部统一事务方式。</p>
     *
     * @param configMap 配置键值对（key=configKey, value=configValue）
     */
    @Override
    @Transactional
    public void batchUpdateConfigs(Map<String, String> configMap) {
        configMap.forEach(this::updateConfig);
    }

    /**
     * 全量刷新本地缓存。
     *
     * <h4>业务逻辑</h4>
     * <ol>
     *   <li>清空 {@link #configCache}</li>
     *   <li>从数据库加载所有配置项</li>
     *   <li>逐一放入缓存</li>
     * </ol>
     *
     * <p>适用场景：配置在外部被直接修改了数据库，或定时任务全量同步缓存。</p>
     */
    @Override
    public void refreshCache() {
        configCache.clear();
        List<SysConfig> all = list();
        for (SysConfig config : all) {
            configCache.put(config.getConfigKey(), config.getConfigValue());
        }
    }
}
