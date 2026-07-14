<!--
  AppLayout.vue - 主布局组件（Shell 布局）
  应用登录后的主框架，包含三部分：
  - 左侧侧边栏（el-aside）：Logo + SideMenu 导航菜单
  - 顶部顶栏（el-header）：折叠按钮 + 用户名 + 退出按钮
  - 内容区（el-main）：<RouterView /> 渲染匹配的子路由页面
  侧边栏宽度可折叠/展开，状态由 appStore 驱动
-->
<script setup>
// ============================================================
//  Imports & Dependencies
// ============================================================
import { computed } from 'vue'
import { RouterView, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'     // 获取当前用户信息
import { useAppStore } from '../stores/app'       // 获取侧边栏折叠状态
import { Fold, Expand, SwitchButton } from '@element-plus/icons-vue'
import SideMenu from './SideMenu.vue'              // 左侧导航菜单组件

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

// ============================================================
//  Computed Properties
// ============================================================

// 优先展示用户的真实姓名（realName），其次用户名（username），都没有则空字符串
const userName = computed(() => authStore.user?.realName || authStore.user?.username || '')

// 动态侧边栏宽度：折叠时 64px（仅显示图标），展开时 220px
const asideWidth = computed(() => appStore.sidebarCollapsed ? '64px' : '220px')

// ============================================================
//  Event Handlers
// ============================================================

/**
 * 退出登录
 * 调用 authStore.logout() 清除 token 和用户信息
 * 然后跳转到登录页
 */
function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<!-- ============================================================
     Template - Shell 布局
     左侧 aside + 右侧（header + main）
============================================================ -->
<template>
  <div class="shell">
    <!-- ====== 左侧侧边栏 ====== -->
    <!-- :width 动态绑定，实现平滑折叠/展开过渡（CSS transition） -->
    <el-aside :width="asideWidth" class="shell-aside">
      <!-- 品牌/Logo 区域：折叠时显示简写 "Library"，展开时显示完整名称 -->
      <div class="brand">{{ appStore.sidebarCollapsed ? 'Library' : '图书管理系统' }}</div>
      <!-- 导航菜单 -->
      <SideMenu />
    </el-aside>

    <!-- ====== 右侧区域 ====== -->
    <div class="shell-right">
      <!-- ====== 顶部顶栏 ====== -->
      <el-header class="shell-header">
        <!-- 左侧：折叠/展开侧边栏的按钮 -->
        <div class="header-left">
          <!-- 根据折叠状态切换图标：折叠时显示"展开"图标，展开时显示"折叠"图标 -->
          <el-button text @click="appStore.toggleSidebar">
            <el-icon><component :is="appStore.sidebarCollapsed ? Expand : Fold" /></el-icon>
          </el-button>
        </div>

        <!-- 右侧：用户名标签 + 退出按钮 -->
        <div class="header-actions">
          <!-- Element Plus 标签，展示当前登录用户名 -->
          <el-tag v-if="userName" type="primary" effect="plain">{{ userName }}</el-tag>
          <!-- 退出按钮，点击触发 handleLogout -->
          <el-button text type="danger" :icon="SwitchButton" @click="handleLogout">
            退出
          </el-button>
        </div>
      </el-header>

      <!-- ====== 内容区域 ====== -->
      <!-- RouterView 根据当前路由 path 渲染对应的子路由组件 -->
      <el-main class="shell-main">
        <RouterView />
      </el-main>
    </div>
  </div>
</template>

<!-- ============================================================
     Style - Shell 布局样式（scoped）
     补充 style.css 中未定义的组件级样式
============================================================ -->
<style scoped>
/* 侧边栏宽度过渡动画，对应 el-aside 的 :width 属性 */
.shell-aside {
  transition: width 0.3s;
  overflow-y: auto;
  overflow-x: hidden;
}

/* 顶栏左侧区域 */
.header-left {
  display: flex;
  align-items: center;
}
</style>
