import axios from 'axios'
import { ElMessage } from 'element-plus'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL

const service = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
})

const AUTH_KEY = 'LibraryManagementAuth'

/**
 * 从 localStorage 读取认证令牌
 *
 * 使用 AUTH_KEY 键从 localStorage 获取存储的认证对象，
 * 解析 JSON 后返回 token 字段。未找到或解析失败时返回空字符串。
 *
 * @returns {string} 存储的 JWT 令牌，不可用时返回空字符串
 */
const readToken = () => {
  try {
    const raw = localStorage.getItem(AUTH_KEY)
    if (!raw) {
      return ''
    }
    const auth = JSON.parse(raw)
    return auth.token || ''
  } catch {
    return ''
  }
}

// 自动在请求头中注入 token
service.interceptors.request.use(
  function (config) {
    const token = readToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  function (error) {
    return Promise.reject(error)
  }
)

// 统一处理响应和错误
service.interceptors.response.use(
  function (response) {
    response = response.data
    if (response?.code === 200) {
      return response
    } else if (response?.code === 401) {
      ElMessage.error('登录状态已过期，请重新登录')
      localStorage.removeItem(AUTH_KEY)
      window.location.href = '/login'
      return Promise.reject(new Error('Unauthorized'))
    } else {
      ElMessage.error(response?.message || '请求失败')
      return Promise.reject(new Error(response?.message || 'Error'))
    }
  },
  function (error) {
    if (error?.response?.status === 401) {
      localStorage.removeItem(AUTH_KEY)
      ElMessage.error('登录状态已过期，请重新登录')
      setTimeout(() => {
        window.location.href = '/login'
      }, 1500)
    }
    return Promise.reject(error)
  }
)

/**
 * 发送 HTTP 请求（使用预配置的 axios 实例）
 *
 * axios 实例已配置请求拦截器（自动注入 token）和响应拦截器（统一 401 跳转 + ElementPlus 提示）。
 *
 * @param {import('axios').AxiosRequestConfig} options - axios 请求配置对象
 * @returns {Promise<import('axios').AxiosResponse>} Promise，解析为响应数据
 */
export const request = (options) => {
  return service(options)
}

/**
 * 发送 HTTP GET 请求
 *
 * @param {string} url - 请求地址
 * @param {Object} [params] - URL 查询参数
 * @param {import('axios').AxiosRequestConfig} [config={}] - 额外的 axios 配置
 * @returns {Promise} Promise，解析为响应数据
 */
export const get = (url, params, config = {}) => {
  return request({
    url,
    method: 'get',
    params,
    ...config,
  })
}

/**
 * 发送 HTTP POST 请求
 *
 * @param {string} url - 请求地址
 * @param {Object} [data] - 请求体数据
 * @param {import('axios').AxiosRequestConfig} [config={}] - 额外的 axios 配置
 * @returns {Promise} Promise，解析为响应数据
 */
export const post = (url, data, config = {}) => {
  return request({
    url,
    method: 'post',
    data,
    ...config,
  })
}

/**
 * 发送 HTTP PUT 请求
 *
 * @param {string} url - 请求地址
 * @param {Object} [data] - 请求体数据
 * @param {import('axios').AxiosRequestConfig} [config={}] - 额外的 axios 配置
 * @returns {Promise} Promise，解析为响应数据
 */
export const put = (url, data, config = {}) => {
  return request({
    url,
    method: 'put',
    data,
    ...config,
  })
}

/**
 * 发送 HTTP DELETE 请求
 *
 * @param {string} url - 请求地址
 * @param {import('axios').AxiosRequestConfig} [config={}] - 额外的 axios 配置
 * @returns {Promise} Promise，解析为响应数据
 */
export const del = (url, config = {}) => {
  return request({
    url,
    method: 'delete',
    ...config,
  })
}

export default {
  request,
  get,
  post,
  put,
  delete: del,
}
