<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '../stores/app'
import { useAuthStore } from '../stores/auth'
import {
  DataBoard, Reading, User, Document, Tickets, Setting, UserFilled,
  Search, Timer, WarningFilled, Bell,
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const authStore = useAuthStore()

const activeIndex = computed(() => route.path)

const isAdmin = computed(() => authStore.roles.some(r => r === 'admin' || r === 'librarian'))
const isSuperAdmin = computed(() => authStore.roles.includes('admin'))
const isStudent = computed(() => authStore.roles.includes('student'))

const superAdminSection = [
  { label: '用户管理', icon: User, children: [
    { label: '用户列表', path: '/users' },
    { label: '新增用户', path: '/users/add' },
  ]},
  { label: '系统配置', icon: Setting, path: '/config' },
]

const adminSection = [
  { label: '图书管理', icon: Reading, children: [
    { label: '图书列表', path: '/books' },
    { label: '新增图书', path: '/books/add' },
    { label: '分类管理', path: '/categories' },
  ]},
  { label: '借阅管理', icon: Tickets, children: [
    { label: '借阅记录', path: '/borrows' },
    { label: '罚款记录', path: '/fines' },
  ]},
  { label: '公告管理', icon: Document, children: [
    { label: '公告列表', path: '/notices' },
    { label: '新增公告', path: '/notices/add' },
  ]},
]

const studentSection = [
  { label: '图书检索与借阅', icon: Search, path: '/student/books' },
  { label: '我的借阅记录', icon: Timer, path: '/student/borrows' },
  { label: '我的罚款', icon: WarningFilled, path: '/student/fines' },
  { label: '公告查看', icon: Bell, path: '/student/notices' },
]

const menus = computed(() => {
  const items = []
  if (isAdmin.value) {
    items.push({ label: '数据大盘', icon: DataBoard, path: '/dashboard' })
  }
  if (isSuperAdmin.value) { items.push(...superAdminSection) }
  if (isAdmin.value) { items.push(...adminSection) }
  if (isStudent.value) { items.push(...studentSection) }
  items.push({ label: '个人中心', icon: UserFilled, path: '/profile' })
  return items
})

const handleSelect = (index) => { router.push(index) }
</script>

<template>
  <el-menu :default-active="activeIndex" :collapse="appStore.sidebarCollapsed" class="menu" @select="handleSelect">
    <template v-for="item in menus" :key="item.path || item.label">
      <el-sub-menu v-if="item.children" :index="item.label">
        <template #title>
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </template>
        <el-menu-item v-for="child in item.children" :key="child.path" :index="child.path">
          <span>{{ child.label }}</span>
        </el-menu-item>
      </el-sub-menu>
      <el-menu-item v-else :index="item.path">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<style scoped>
.menu {
  border-right: none;
  background: transparent;
}
</style>
