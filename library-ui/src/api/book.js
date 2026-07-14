import { get, post, put, del } from './http'

/**
 * 分页搜索图书，支持多条件筛选
 *
 * @param {Object} [params] - 筛选参数（如 keyword、categoryId、page、size 等）
 * @returns {Promise} Promise，解析为分页搜索结果
 */
export const bookSearch = (params) => get('/api/book/search', params)

/**
 * 获取指定图书的详细信息
 *
 * @param {string|number} id - 图书 ID
 * @returns {Promise} Promise，解析为图书详情
 */
export const bookDetail = (id) => get(`/api/book/detail/${id}`)

/**
 * 获取热门借阅图书列表
 *
 * @param {number} [topN] - 返回前 N 本
 * @returns {Promise} Promise，解析为热门图书列表
 */
export const hotBooks = (topN) => get('/api/book/hot', { topN })

/**
 * 新增图书
 *
 * @param {Object} data - 图书数据（isbn、书名、作者、价格、库存等）
 * @returns {Promise} Promise，解析为新增结果
 */
export const addBook = (data) => post('/api/book/add', data)

/**
 * 编辑已有图书信息
 *
 * @param {Object} data - 更新后的图书数据，必须包含 id 字段
 * @returns {Promise} Promise，解析为更新结果
 */
export const updateBook = (data) => put('/api/book/update', data)

/**
 * 更新图书状态（在库 / 借出 / 下架 / 遗失）
 *
 * @param {string|number} bookId - 图书 ID
 * @param {string|number} status - 新状态值
 * @returns {Promise} Promise，解析为状态更新结果
 */
export const updateBookStatus = (bookId, status) => put('/api/book/status', null, { params: { bookId, status } })

/**
 * 删除图书（逻辑删除）
 *
 * @param {string|number} id - 待删除的图书 ID
 * @returns {Promise} Promise，解析为删除结果
 */
export const deleteBook = (id) => del(`/api/book/delete/${id}`)

/**
 * 通过上传 Excel 文件批量导入图书
 *
 * 以 multipart/form-data 格式发送 POST 请求，上传 .xlsx 文件，
 * 后端解析并逐行导入，返回成功 / 失败数量及明细。
 *
 * @param {File} file - 待上传的 Excel 文件对象
 * @returns {Promise} Promise，解析为导入结果
 */
export const importBooks = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return post('/api/book/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}

/**
 * 按条件导出图书列表为 Excel 文件并触发浏览器下载
 *
 * 使用原生 Fetch API 以 Blob 形式接收文件流，
 * 创建临时 a 标签触发浏览器下载，文件名格式为 "图书列表_YYYY-MM-DD.xlsx"。
 * 不走 axios 拦截器以避免 JSON 解析对二进制文件的干扰。
 *
 * @param {string} [keyword] - 导出筛选条件：关键词
 * @param {string|number} [categoryId] - 导出筛选条件：分类 ID
 * @returns {Promise<void>} Promise，下载触发后即完成
 */
export const exportBooks = (keyword, categoryId) => {
  const raw = localStorage.getItem('LibraryManagementAuth')
  const token = raw ? JSON.parse(raw).token : ''
  const params = new URLSearchParams()
  if (keyword) params.append('keyword', keyword)
  if (categoryId != null) params.append('categoryId', categoryId)
  const apiBase = import.meta.env.VITE_API_BASE_URL
  return fetch(`${apiBase}/api/book/export?${params}`, {
    headers: { Authorization: `Bearer ${token}` },
  }).then((res) => {
    if (!res.ok) throw new Error('导出失败')
    return res.blob()
  }).then((blob) => {
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `图书列表_${new Date().toISOString().slice(0, 10)}.xlsx`
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)
  })
}
