import { post, get } from './http'

/**
 * 用户登录（账号 + 密码 + 验证码）
 *
 * @param {string} username - 登录账号（学号/工号）
 * @param {string} password - 登录密码
 * @param {string} captcha - 图形验证码文本
 * @param {string} captchaKey - 验证码缓存键
 * @returns {Promise} Promise，解析为登录结果（含 JWT token）
 */
export const login = (username, password, captcha, captchaKey) => {
  return post('/api/auth/login', null, { params: { username, password, captcha, captchaKey } })
}

/**
 * 学生自主注册
 *
 * @param {Object} data - 注册表单数据（username、password、realName 等）
 * @returns {Promise} Promise，解析为注册结果
 */
export const register = (data) => post('/api/auth/register', data)

/**
 * 获取图形验证码
 *
 * @returns {Promise} Promise，解析为验证码数据（captchaKey + Base64 SVG 图片）
 */
export const getCaptcha = () => get('/api/auth/captcha')

/**
 * 退出登录（使当前 JWT token 失效）
 *
 * @returns {Promise} Promise，解析为登出结果
 */
export const logout = () => post('/api/auth/logout')

/**
 * 获取当前登录用户的个人信息
 *
 * @returns {Promise} Promise，解析为当前用户详情
 */
export const getCurrentUser = () => get('/api/auth/me')
