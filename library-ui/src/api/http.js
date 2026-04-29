import axios from 'axios'

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL

const service = axios.create({
  baseURL: apiBaseUrl,
  timeout: 10000,
})

export const request = (options) => {
  return service(options)
}

export const get = (url, params, config = {}) => {
  return request({
    url,
    method: 'get',
    params,
    ...config,
  })
}

export const post = (url, data, config = {}) => {
  return request({
    url,
    method: 'post',
    data,
    ...config,
  })
}

export default {
  request,
  get,
  post
}
