/**
 * router/index.js — Vue Router 路由配置
 *
 * 定义整个应用的路由表：
 *   - 公开路由：/login（登录）、/register（注册）
 *   - 主布局路由："/" 包裹 AppLayout，包含管理员端和学生端所有子路由
 *
 * 路由使用懒加载（动态 import）实现代码分割，按路由拆分 chunk。
 */
import { createRouter, createWebHistory } from 'vue-router'

/**
 * 创建路由实例
 * history: createWebHistory(BASE_URL) — 使用 HTML5 History 模式
 *   BASE_URL 来自 Vite 的 import.meta.env.BASE_URL，默认 '/'
 */
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: () => import('../views/RegisterView.vue'),
    },
    {
      path: '/',
      component: () => import('../components/AppLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'dashboard', component: () => import('../views/DashboardView.vue') },
        { path: 'books', name: 'books', component: () => import('../views/admin/BookListView.vue') },
        { path: 'books/add', name: 'books-add', component: () => import('../views/admin/BookFormView.vue') },
        { path: 'books/:id/edit', name: 'books-edit', component: () => import('../views/admin/BookFormView.vue') },
        { path: 'categories', name: 'categories', component: () => import('../views/admin/CategoryView.vue') },
        { path: 'users', name: 'users', component: () => import('../views/admin/UserListView.vue') },
        { path: 'users/add', name: 'users-add', component: () => import('../views/admin/UserFormView.vue') },
        { path: 'users/:id/edit', name: 'users-edit', component: () => import('../views/admin/UserFormView.vue') },
        { path: 'borrows', name: 'borrows', component: () => import('../views/admin/BorrowListView.vue') },
        { path: 'fines', name: 'fines', component: () => import('../views/admin/FineListView.vue') },
        { path: 'notices', name: 'notices', component: () => import('../views/admin/NoticeListView.vue') },
        { path: 'notices/add', name: 'notices-add', component: () => import('../views/admin/NoticeFormView.vue') },
        { path: 'notices/:id/edit', name: 'notices-edit', component: () => import('../views/admin/NoticeFormView.vue') },
        { path: 'config', name: 'config', component: () => import('../views/admin/ConfigView.vue') },
        { path: 'profile', name: 'profile', component: () => import('../views/admin/ProfileView.vue') },
        { path: 'student/books', name: 'student-books', component: () => import('../views/student/BookView.vue') },
        { path: 'student/borrows', name: 'student-borrows', component: () => import('../views/student/BorrowView.vue') },
        { path: 'student/fines', name: 'student-fines', component: () => import('../views/student/FineView.vue') },
        { path: 'student/notices', name: 'student-notices', component: () => import('../views/student/NoticeView.vue') },
      ],
    },
  ],
})

const AUTH_KEY = 'LibraryManagementAuth'
const PUBLIC_ROUTES = ['/login', '/register']

router.beforeEach((to, from, next) => {
  const raw = localStorage.getItem(AUTH_KEY)
  let isLoggedIn = false
  let userRoles = []

  if (raw) {
    try {
      const auth = JSON.parse(raw)
      isLoggedIn = !!(auth.token && auth.user)
      userRoles = auth.user?.roles || []
    } catch { /* */ }
  }

  const isPublic = PUBLIC_ROUTES.includes(to.path)

  if (!isLoggedIn && !isPublic) {
    next('/login')
    return
  }

  if (isLoggedIn && isPublic) {
    next('/dashboard')
    return
  }

  const isStudentOnly = userRoles.length === 1 && userRoles[0] === 'student'
  const isAdminPath = !to.path.startsWith('/student/') && to.path !== '/profile'

  if (isStudentOnly && isAdminPath) {
    next('/student/books')
    return
  }

  next()
})

export default router
