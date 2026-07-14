import { get, post, put, del } from './http'

/**
 * 获取图书分类树（含父子层级关系）
 *
 * @returns {Promise} Promise，解析为分类树结构数据
 */
export const categoryTree = () => get('/api/category/tree')

/**
 * 新增分类节点
 *
 * @param {Object} data - 分类数据（name、parentId、sort 等）
 * @returns {Promise} Promise，解析为新增结果
 */
export const addCategory = (data) => post('/api/category/add', data)

/**
 * 编辑已有分类信息
 *
 * @param {Object} data - 更新后的分类数据，必须包含 id 字段
 * @returns {Promise} Promise，解析为更新结果
 */
export const updateCategory = (data) => put('/api/category/update', data)

/**
 * 删除分类节点
 *
 * @param {string|number} id - 待删除的分类 ID
 * @returns {Promise} Promise，解析为删除结果
 */
export const deleteCategory = (id) => del(`/api/category/delete/${id}`)
