/**
 * auth.js — 认证状态管理 Store（Pinia）
 *
 * 管理前端的登录/登出状态和当前用户信息，供路由守卫、侧边栏菜单、顶栏等全局使用。
 * 登录状态基于 localStorage 中的 token 持久化，刷新页面后自动恢复。
 */
import { ref, computed } from 'vue'
import { defineStore } from 'pinia'

/**
 * 认证状态管理 Store
 * 使用 Pinia 组合式 API（Setup Store）定义
 * 暴露 token、isLoggedIn、user、roles 等响应式状态
 * 以及 login、logout、loadFromStorage 等操作方法
 */
export const useAuthStore = defineStore('auth', () => {
  // localStorage 键名，用于存储登录凭证
  const AUTH_KEY = 'LibraryManagementAuth'

  // ============================================================
  //  State - 响应式状态
  // ============================================================

  /** JWT 令牌，用于请求头 Authorization 携带 */
  const token = ref('')
  /** 是否已登录，用于路由守卫和 UI 条件渲染 */
  const isLoggedIn = ref(false)
  /** 当前用户信息对象：{ userId, username, realName, avatar, roles } */
  const user = ref(null)
  /** 计算属性：从 user 中提取 roles 数组，user 为空时返回空数组 */
  const roles = computed(() => user.value?.roles || [])

  // ============================================================
  //  Actions - 状态操作函数（以下称"action"）
  // ============================================================

  /**
   * 从 localStorage 恢复登录态
   * 应用启动时（main.js 中）调用，用于刷新页面后保持登录状态
   * 解析失败时清空状态
   */
  function loadFromStorage() {
    try {
      const raw = localStorage.getItem(AUTH_KEY)
      if (!raw) return  // 无本地数据，保持未登录态
      const auth = JSON.parse(raw)
      // 有 token 才恢复登录，避免空 token 被误认为已登录
      if (auth.token) {
        token.value = auth.token
        user.value = auth.user || null
        isLoggedIn.value = true
      }
    } catch {
      // 解析异常（如数据被篡改）时清空状态
      clearState()
    }
  }

  /**
   * 登录操作
   * 接收登录接口返回的用户数据和 JWT token
   * 同时写入响应式状态和 localStorage 持久化
   * @param {Object} userData - 用户信息对象
   * @param {string} jwtToken - JWT 令牌
   */
  function login(userData, jwtToken) {
    token.value = jwtToken
    user.value = userData
    isLoggedIn.value = true
    // 持久化到 localStorage，刷新后可通过 loadFromStorage 恢复
    localStorage.setItem(AUTH_KEY, JSON.stringify({ token: jwtToken, user: userData }))
  }

  /**
   * 登出操作
   * 清除 localStorage 缓存并重置所有响应式状态
   */
  function logout() {
    localStorage.removeItem(AUTH_KEY)
    clearState()
  }

  /**
   * 清理内存中的认证状态（仅内部使用，被 login/logout 调用）
   * 将所有状态重置为初始值
   */
  function clearState() {
    token.value = ''
    isLoggedIn.value = false
    user.value = null
  }

  // ============================================================
  //  Exports - 对外暴露的接口
  // ============================================================

  return {
    token,
    isLoggedIn,
    user,
    roles,
    loadFromStorage,
    login,
    logout,
  }
})
