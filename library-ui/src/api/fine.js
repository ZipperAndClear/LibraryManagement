import { get, post } from './http'

/**
 * 分页查询罚款记录列表
 *
 * @param {Object} [params] - 查询参数（userId、status、page、size 等）
 * @returns {Promise} Promise，解析为分页罚款列表
 */
export const fineList = (params) => get('/api/fine/list', params)

/**
 * 缴纳指定罚款
 *
 * @param {string|number} fineRecordId - 罚款记录 ID
 * @returns {Promise} Promise，解析为缴纳结果
 */
export const payFine = (fineRecordId) => post('/api/fine/pay', null, { params: { fineRecordId } })

/**
 * 豁免指定罚款（管理员操作，标记为已免除）
 *
 * @param {string|number} fineRecordId - 罚款记录 ID
 * @returns {Promise} Promise，解析为豁免结果
 */
export const exemptFine = (fineRecordId) => post('/api/fine/exempt', null, { params: { fineRecordId } })
