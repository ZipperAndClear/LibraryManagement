import { get, post, put, del } from './http'

/**
 * 分页查询用户列表（支持关键词和状态筛选）
 *
 * @param {Object} [params] - 查询参数（keyword、status、page、size 等）
 * @returns {Promise} Promise，解析为分页用户列表
 */
export const userList = (params) => get('/api/user/list', params)

export const userDetail = (id) => get(`/api/user/detail/${id}`)

/**
 * 新增用户（管理员操作）
 *
 * @param {Object} data - 用户数据（username、password、roleIds 等）
 * @returns {Promise} Promise，解析为新增结果
 */
export const addUser = (data) => post('/api/user/add', data)

/**
 * 编辑已有用户信息
 *
 * @param {Object} data - 更新后的用户数据，必须包含 id 字段
 * @returns {Promise} Promise，解析为更新结果
 */
export const updateUser = (data) => put('/api/user/update', data)

/**
 * 重置用户密码（管理员操作，无需旧密码）
 *
 * @param {string|number} userId - 目标用户 ID
 * @param {string} newPassword - 新密码
 * @returns {Promise} Promise，解析为重置结果
 */
export const resetPassword = (userId, newPassword) => put('/api/user/reset-password', null, { params: { userId, newPassword } })

/**
 * 当前用户修改自己的密码（需验证旧密码）
 *
 * @param {string|number} userId - 用户 ID
 * @param {string} oldPassword - 旧密码
 * @param {string} newPassword - 新密码
 * @returns {Promise} Promise，解析为修改结果
 */
export const changePassword = (userId, oldPassword, newPassword) => put('/api/user/change-password', null, { params: { userId, oldPassword, newPassword } })

/**
 * 启用 / 禁用用户账号
 *
 * @param {string|number} userId - 目标用户 ID
 * @param {string|number} status - 状态值：1-正常 0-禁用
 * @returns {Promise} Promise，解析为状态更新结果
 */
export const updateUserStatus = (userId, status) => put('/api/user/status', null, { params: { userId, status } })

/**
 * 删除用户（逻辑删除，同时清理角色关联）
 *
 * @param {string|number} id - 待删除的用户 ID
 * @returns {Promise} Promise，解析为删除结果
 */
export const deleteUser = (id) => del(`/api/user/delete/${id}`)
