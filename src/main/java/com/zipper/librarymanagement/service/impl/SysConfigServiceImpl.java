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
 * 系统配置业务实现类
 * <p>使用 ConcurrentHashMap 做本地缓存，避免频繁读取数据库。
 * 更新配置时同步清除对应的缓存键，批量更新后可通过 refreshCache 全量刷新。</p>
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    /** 配置值本地缓存（key=configKey, value=configValue） */
    private final Map<String, String> configCache = new ConcurrentHashMap<>();

    @Override
    public String getValueByKey(String configKey) {
        // 优先走缓存
        String cached = configCache.get(configKey);
        if (cached != null) {
            return cached;
        }
        // 缓存未命中，查数据库
        SysConfig config = lambdaQuery().eq(SysConfig::getConfigKey, configKey).one();
        if (config == null) {
            throw new BusinessException("系统配置不存在: " + configKey);
        }
        // 回填缓存
        configCache.put(configKey, config.getConfigValue());
        return config.getConfigValue();
    }

    @Override
    public Integer getIntByKey(String configKey) {
        String value = getValueByKey(configKey);
        return Integer.valueOf(value);
    }

    @Override
    public BigDecimal getDecimalByKey(String configKey) {
        String value = getValueByKey(configKey);
        return new BigDecimal(value);
    }

    @Override
    public List<SysConfig> listAllConfigs() {
        return list();
    }

    @Override
    @Transactional
    public void updateConfig(String configKey, String configValue) {
        // 查库确认配置存在
        SysConfig config = lambdaQuery().eq(SysConfig::getConfigKey, configKey).one();
        if (config == null) {
            throw new BusinessException("配置不存在: " + configKey);
        }
        config.setConfigValue(configValue);
        updateById(config);
        // 更新后清除缓存，下次读取时重新加载
        configCache.remove(configKey);
    }

    @Override
    @Transactional
    public void batchUpdateConfigs(Map<String, String> configMap) {
        configMap.forEach(this::updateConfig);
    }

    @Override
    public void refreshCache() {
        // 全量刷新：清空缓存后重新从数据库加载所有配置
        configCache.clear();
        List<SysConfig> all = list();
        for (SysConfig config : all) {
            configCache.put(config.getConfigKey(), config.getConfigValue());
        }
    }
}
