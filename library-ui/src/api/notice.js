import { get, post, put, del } from './http'

/**
 * 分页查询公告列表（支持标题关键词搜索和状态筛选）
 *
 * @param {Object} [params] - 查询参数（keyword、status、page、size 等）
 * @returns {Promise} Promise，解析为分页公告列表
 */
export const noticeList = (params) => get('/api/notice/list', params)

/**
 * 获取所有已发布的公告列表（学生端展示用）
 *
 * @returns {Promise} Promise，解析为已发布公告列表
 */
export const publishedNotices = () => get('/api/notice/published')

/**
 * 新增公告（默认草稿状态）
 *
 * @param {Object} data - 公告数据（title、content、isTop 等）
 * @returns {Promise} Promise，解析为新增结果
 */
export const addNotice = (data) => post('/api/notice/add', data)

/**
 * 编辑已有公告
 *
 * @param {Object} data - 更新后的公告数据，必须包含 id 字段
 * @returns {Promise} Promise，解析为更新结果
 */
export const updateNotice = (data) => put('/api/notice/update', data)

/**
 * 发布公告（草稿 → 已发布）
 *
 * @param {string|number} id - 公告 ID
 * @returns {Promise} Promise，解析为发布结果
 */
export const publishNotice = (id) => put(`/api/notice/publish/${id}`)

/**
 * 撤回公告（已发布 → 草稿）
 *
 * @param {string|number} id - 公告 ID
 * @returns {Promise} Promise，解析为撤回结果
 */
export const unpublishNotice = (id) => put(`/api/notice/unpublish/${id}`)

/**
 * 删除公告
 *
 * @param {string|number} id - 公告 ID
 * @returns {Promise} Promise，解析为删除结果
 */
export const deleteNotice = (id) => del(`/api/notice/delete/${id}`)
