import { get, post } from './http'

/**
 * 借阅图书：为用户借出指定图书
 *
 * @param {string|number} userId - 借阅人（用户）ID
 * @param {string|number} bookId - 借阅图书 ID
 * @returns {Promise} Promise，解析为借阅结果
 */
export const borrowBook = (userId, bookId) => post('/api/borrow/borrow', null, { params: { userId, bookId } })

/**
 * 归还图书：归还已借出的图书，逾期则自动计算罚款
 *
 * @param {string|number} borrowRecordId - 借阅记录 ID
 * @param {string|number} userId - 归还人（用户）ID
 * @returns {Promise} Promise，解析为归还结果
 */
export const returnBook = (borrowRecordId, userId) => post('/api/borrow/return', null, { params: { borrowRecordId, userId } })

/**
 * 续借图书：延长应还时间
 *
 * @param {string|number} borrowRecordId - 借阅记录 ID
 * @param {string|number} userId - 续借人（用户）ID
 * @returns {Promise} Promise，解析为续借结果
 */
export const renewBook = (borrowRecordId, userId) => post('/api/borrow/renew', null, { params: { borrowRecordId, userId } })

/**
 * 标记图书丢失（管理员操作）
 *
 * @param {string|number} borrowRecordId - 借阅记录 ID
 * @returns {Promise} Promise，解析为标记结果
 */
export const markLost = (borrowRecordId) => post('/api/borrow/mark-lost', null, { params: { borrowRecordId } })

/**
 * 分页查询借阅记录列表（支持多条件筛选）
 *
 * @param {Object} [params] - 查询参数（userId、status、keyword、page、size 等）
 * @returns {Promise} Promise，解析为分页借阅记录列表
 */
export const borrowList = (params) => get('/api/borrow/list', params)

/**
 * 获取全部逾期未还的借阅记录列表（管理员催还用）
 *
 * @returns {Promise} Promise，解析为逾期记录列表
 */
export const overdueList = () => get('/api/borrow/overdue')
