/**
 * app.js — 应用 UI 状态 Store（Pinia）
 *
 * 管理侧边栏折叠/展开、全局 loading 等与具体业务无关的 UI 状态。
 * 不涉及持久化，刷新后恢复默认（侧边栏展开）。
 */
import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 应用 UI 状态管理 Store
 * 使用 Pinia 组合式 API 定义
 * 主要用于 AppLayout 和 SideMenu 组件间共享侧边栏折叠状态
 */
export const useAppStore = defineStore('app', () => {
  // ============================================================
  //  State
  // ============================================================

  /** 侧边栏是否折叠（true=折叠，false=展开） */
  const sidebarCollapsed = ref(false)

  // ============================================================
  //  Actions
  // ============================================================

  /**
   * 切换侧边栏折叠状态
   * 顶栏折叠按钮点击时调用
   */
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  /**
   * 收起侧边栏（折叠）
   * 用于需要在特定场景下强制折叠侧边栏
   */
  function collapseSidebar() {
    sidebarCollapsed.value = true
  }

  /**
   * 展开侧边栏
   * 用于需要在特定场景下强制展开侧边栏
   */
  function expandSidebar() {
    sidebarCollapsed.value = false
  }

  // ============================================================
  //  Exports
  // ============================================================

  return {
    sidebarCollapsed,
    toggleSidebar,
    collapseSidebar,
    expandSidebar,
  }
})
