import { get } from './http'

/**
 * 获取数据大盘统计概览数据
 *
 * 包含馆藏总数、今日借出、当前借阅中、逾期未还等聚合指标。
 *
 * @returns {Promise} Promise，解析为统计数据对象
 */
export const dashboardStats = () => get('/api/dashboard/stats')

/**
 * 获取热门借阅图书榜单
 *
 * @returns {Promise} Promise，解析为热门图书列表
 */
export const hotBooks = () => get('/api/dashboard/hot-books')
