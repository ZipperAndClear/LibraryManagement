import { get, put } from './http'

/**
 * 获取全部系统配置键值对列表
 *
 * @returns {Promise} Promise，解析为配置列表
 */
export const configList = () => get('/api/config/list')

/**
 * 根据配置键更新单个配置值
 *
 * @param {string} configKey - 配置键名（如 sys.borrow.max）
 * @param {string} configValue - 新的配置值
 * @returns {Promise} Promise，解析为更新结果
 */
export const updateConfig = (configKey, configValue) => put('/api/config/update', null, { params: { configKey, configValue } })

/**
 * 批量更新多项配置
 *
 * @param {Object} data - 配置键值对对象
 * @returns {Promise} Promise，解析为批量更新结果
 */
export const batchUpdateConfig = (data) => put('/api/config/batch-update', data)
