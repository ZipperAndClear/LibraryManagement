/**
 * main.js — 应用入口文件
 *
 * 启动流程：
 *   1. 创建 Vue 应用实例
 *   2. 注册全局插件（Pinia 状态管理、Vue Router 路由、Element Plus UI 库）
 *   3. 从 localStorage 恢复登录状态
 *   4. 挂载应用到 #app 根节点
 */
import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css';   // Element Plus 全局样式
import './style.css'                      // 自定义全局样式（CSS 变量、布局等）
import { useAuthStore } from './stores/auth'

// 1. 创建 Vue 应用实例，以 App.vue 作为根组件
const app = createApp(App)

// 2. 创建 Pinia 实例（全局状态管理）
const pinia = createPinia()

// 3. 注册插件（顺序：Pinia → Router → Element Plus）
app.use(pinia)          // 必须先 use(pinia) 才能使用 useAuthStore()
app.use(router)         // 路由
app.use(ElementPlus)    // Element Plus UI 组件库

// 4. 从 localStorage 恢复上一次的登录状态
//    页面刷新后 token 不会丢失，依然保持登录态
const authStore = useAuthStore()
authStore.loadFromStorage()

// 5. 将应用挂载到 index.html 中 id="app" 的 div 上
app.mount('#app')
