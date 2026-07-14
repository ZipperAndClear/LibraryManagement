<!--
  DashboardView.vue - 数据大盘页面（首页）
  展示 4 个统计卡片（总藏书量、今日借出、当前借阅中、逾期未还）、
  热门图书 Top 10 列表、最新公告列表。
  管理员角色额外显示"快捷操作"区域（新增图书、新增用户等）。
  数据通过 3 个 API 并发请求加载。
-->
<script setup>
// ============================================================
//  Imports & Dependencies
// ============================================================
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { dashboardStats, hotBooks } from '../api/dashboard' // 大盘统计 & 热门图书 API
import { publishedNotices } from '../api/notice'             // 公告列表 API
import {
  Document, Reading, User, WarningFilled,
  TrendCharts, Bell, Plus, Edit, Check,
  Collection, Avatar, Timer
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()

const apiBase = import.meta.env.VITE_API_BASE_URL || ''
function coverUrl(raw) {
  if (!raw) return ''
  if (raw.startsWith('http://') || raw.startsWith('https://')) return raw
  return apiBase + raw
}

// ============================================================
//  Reactive State
// ============================================================

// 统计数据：由 API 返回填充 { totalBooks, todayBorrows, currentBorrowing, overdue }
const stats = ref({ totalBooks: 0, todayBorrows: 0, currentBorrowing: 0, overdue: 0 })
const hotBookList = ref([])    // 热门图书数组
const noticeList = ref([])     // 公告数组
const loading = ref(true)      // 页面整体 loading 状态

// ============================================================
//  Computed Properties
// ============================================================

// 判断当前用户是否为管理员（admin 或 librarian 角色）
const isAdmin = computed(() => authStore.roles.some(r => r === 'admin' || r === 'librarian'))

// 4 个统计卡片的配置数组，每个卡片包含图标、颜色、背景色
const statCards = computed(() => [
  {
    label: '总藏书量',
    value: stats.value.totalBooks,
    icon: Collection,
    color: '#007DFF',
    bg: '#DAECFF',
  },
  {
    label: '今日借出',
    value: stats.value.todayBorrows,
    icon: Reading,
    color: '#00B578',
    bg: '#E1F7E8',
  },
  {
    label: '当前借阅中',
    value: stats.value.currentBorrowing,
    icon: Timer,
    color: '#FF8F1F',
    bg: '#FFF0E0',
  },
  {
    label: '逾期未还',
    value: stats.value.overdue,
    icon: WarningFilled,
    color: '#F53F3F',
    bg: '#FFE8E8',
  },
])

// 快捷操作按钮配置：仅管理员显示，每个按钮有图标、路由、Element Plus 按钮类型
const quickActions = computed(() => isAdmin.value ? [
  { label: '新增图书', icon: Plus, route: '/books/add', type: 'primary' },
  { label: '新增用户', icon: Avatar, route: '/users/add', type: 'success' },
  { label: '处理逾期', icon: WarningFilled, route: '/borrows', type: 'warning' },
  { label: '发布公告', icon: Edit, route: '/notices/add', type: 'info' },
] : [])

// ============================================================
//  API / Data Loading
// ============================================================

/**
 * 并发请求 3 个 API 加载仪表盘数据
 * - 统计数据
 * - 热门图书（截取前 10）
 * - 最新公告（截取前 5）
 * 使用 Promise.all 并发执行减少等待时间
 */
async function loadData() {
  loading.value = true
  try {
    // 并发发起 3 个请求
    const [statsRes, hotRes, noticeRes] = await Promise.all([
      dashboardStats(),
      hotBooks(),
      publishedNotices(),
    ])
    // 填充统计数据，兜底为空对象
    stats.value = statsRes.data || {}
    // 热门图书仅显示前 10 条
    hotBookList.value = (hotRes.data || []).slice(0, 10)
    // 公告仅显示前 5 条
    noticeList.value = (noticeRes.data || []).slice(0, 5)
  } catch {
    // 错误由 http 拦截器统一处理（例如 ElMessage.error），此处静默
  } finally {
    loading.value = false
  }
}

// ============================================================
//  Utility Functions
// ============================================================

// 路由跳转辅助函数
function go(route) {
  router.push(route)
}

// ============================================================
//  Lifecycle Hooks
// ============================================================

// 页面挂载后加载数据
onMounted(loadData)
</script>

<!-- ============================================================
     Template - 数据大盘 UI
     统计卡片行 → 热门图书 & 公告 + 快捷操作（双列布局）
============================================================ -->
<template>
  <div class="dashboard">
    <!-- 页面头部：标题 + 刷新按钮 -->
    <div class="page-header">
      <h1 class="page-title">数据大盘</h1>
      <el-button text :icon="TrendCharts" @click="loadData" :loading="loading">刷新数据</el-button>
    </div>

    <!-- 仪表盘主体，v-loading 控制全局 loading 遮罩 -->
    <div v-loading="loading" class="dashboard-body">
      <!-- 第一行：4 个统计卡片，响应式栅格（移动端每行 2 个，桌面端每行 4 个） -->
      <el-row :gutter="16">
        <el-col v-for="(card, i) in statCards" :key="i" :xs="12" :sm="6">
          <div class="stat-card" :style="{ '--card-color': card.color, '--card-bg': card.bg }">
            <!-- 卡片图标：背景色和图标色由 CSS 变量控制 -->
            <div class="stat-icon" :style="{ background: card.bg, color: card.color }">
              <el-icon :size="22"><component :is="card.icon" /></el-icon>
            </div>
            <!-- 卡片数据 -->
            <div class="stat-info">
              <div class="stat-value">{{ card.value }}</div>
              <div class="stat-label">{{ card.label }}</div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 第二行：热门图书（左 14/24）+ 公告 & 快捷操作（右 10/24） -->
      <el-row :gutter="16" class="dashboard-sections">
        <!-- 左列：热门图书 Top 10 -->
        <el-col :xs="24" :md="14">
          <div class="section-card">
            <div class="section-header">
              <el-icon color="#007DFF"><TrendCharts /></el-icon>
              <span>热门图书 Top 10</span>
            </div>
            <!-- 有数据时渲染列表 -->
            <div v-if="hotBookList.length" class="hot-list">
              <div v-for="(book, i) in hotBookList" :key="book.id" class="hot-item">
                <!-- 排名序号，前 3 名高亮 -->
                <div class="hot-rank" :class="{ top3: i < 3 }">{{ i + 1 }}</div>
                <!-- 图书封面：有 coverUrl 展示图片，否则展示占位图标 -->
                <div class="hot-cover">
                  <img v-if="book.coverUrl" :src="coverUrl(book.coverUrl)" alt="" />
                  <div v-else class="cover-placeholder">
                    <el-icon :size="20"><Document /></el-icon>
                  </div>
                </div>
                <!-- 图书信息：名称、作者 + 分类 -->
                <div class="hot-info">
                  <div class="hot-name">{{ book.name }}</div>
                  <div class="hot-meta">{{ book.author }} · {{ book.categoryName || '未分类' }}</div>
                </div>
                <!-- 在库册数 -->
                <div class="hot-stock">{{ book.stock }} 册</div>
              </div>
            </div>
            <!-- 无数据时展示空状态 -->
            <el-empty v-else description="暂无热门图书" :image-size="80" />
          </div>
        </el-col>

        <!-- 右列：最新公告 + 快捷操作（仅管理员） -->
        <el-col :xs="24" :md="10">
          <!-- 公告区域 -->
          <div class="section-card">
            <div class="section-header">
              <el-icon color="#FF8F1F"><Bell /></el-icon>
              <span>最新公告</span>
            </div>
            <div v-if="noticeList.length" class="notice-list">
              <div v-for="notice in noticeList" :key="notice.id" class="notice-item">
                <!-- 置顶公告标红色标签 -->
                <el-tag v-if="notice.isTop" size="small" type="danger" class="notice-tag">置顶</el-tag>
                <div class="notice-title">{{ notice.title }}</div>
                <!-- 发布时间，取前 10 位（yyyy-MM-dd） -->
                <div class="notice-time">{{ notice.createTime?.slice(0, 10) }}</div>
              </div>
            </div>
            <el-empty v-else description="暂无公告" :image-size="80" />
          </div>

          <!-- 快捷操作区域：仅管理员可见 -->
          <div v-if="isAdmin" class="section-card" style="margin-top: 16px">
            <div class="section-header">
              <el-icon color="#00B578"><Plus /></el-icon>
              <span>快捷操作</span>
            </div>
            <!-- 2 列网格布局的操作按钮 -->
            <div class="action-grid">
              <div
                v-for="action in quickActions"
                :key="action.label"
                class="action-btn"
                @click="go(action.route)"
              >
                <!-- Element Plus 圆形图标按钮 -->
                <el-button :type="action.type" circle :icon="action.icon" size="large" />
                <span>{{ action.label }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<!-- ============================================================
     Style - 数据大盘样式（scoped 作用域隔离）
     统计卡片、热门图书列表、公告列表、快捷操作网格
============================================================ -->
<style scoped>
/* ------ 页面容器 ------ */
.dashboard {
  padding: 0;
}

/* 页面头部：标题左对齐 + 刷新按钮右对齐 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0;
}

/* loading 时保证最小高度 */
.dashboard-body {
  min-height: 200px;
}

/* ========== 统计卡片 ========== */
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 12px;
  background: var(--panel);            /* CSS 变量：白色面板 */
  border: 1px solid var(--line);       /* 边框颜色 */
  margin-bottom: 16px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);         /* 悬停微上浮 */
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
}

/* 卡片左侧图标容器 */
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;                     /* 不压缩图标 */
}

/* 卡片右侧信息区域 */
.stat-info {
  flex: 1;
  min-width: 0;                       /* 允许文字截断 */
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
  color: var(--text);
}

.stat-label {
  font-size: 13px;
  color: var(--muted);
  margin-top: 2px;
}

/* ========== 双列区域 ========== */
.dashboard-sections {
  margin-top: 4px;
}

/* 通用 section 卡片 */
.section-card {
  border-radius: 12px;
  background: var(--panel);
  border: 1px solid var(--line);
  padding: 20px;
}

/* section 头部（图标 + 标题） */
.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  padding-bottom: 14px;
  border-bottom: 1px solid var(--line);
  margin-bottom: 14px;
}

/* ========== 热门图书列表 ========== */
.hot-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 10px;
  border-radius: 8px;
  transition: background 0.15s;
  cursor: default;
}

.hot-item:hover {
  background: #f8faff;
}

/* 排名序号 */
.hot-rank {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  background: #f0f2f5;
  color: var(--muted);
  flex-shrink: 0;
}

/* 前三名特殊高亮（橙金色渐变） */
.hot-rank.top3 {
  background: linear-gradient(135deg, #FF8F1F, #FFB800);
  color: #fff;
}

/* 图书缩略图 */
.hot-cover {
  width: 36px;
  height: 48px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f0f2f5;
}

.hot-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 无封面时的占位容器 */
.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #c0c4cc;
}

/* 图书信息 */
.hot-info {
  flex: 1;
  min-width: 0;
}

.hot-name {
  font-size: 14px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;     /* 文字溢出显示省略号 */
  white-space: nowrap;
}

.hot-meta {
  font-size: 12px;
  color: var(--muted);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 库存册数 */
.hot-stock {
  font-size: 12px;
  color: var(--muted);
  white-space: nowrap;
  flex-shrink: 0;
}

/* ========== 公告列表 ========== */
.notice-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  transition: background 0.15s;
  cursor: default;
}

.notice-item:hover {
  background: #f8faff;
}

.notice-tag {
  flex-shrink: 0;
}

.notice-title {
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.notice-time {
  font-size: 12px;
  color: var(--muted);
  white-space: nowrap;
  flex-shrink: 0;
}

/* ========== 快捷操作 ========== */
.action-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);   /* 2 列等宽网格 */
  gap: 12px;
}

.action-btn {
  display: flex;
  flex-direction: column;                   /* 图标在上，文字在下 */
  align-items: center;
  gap: 6px;
  padding: 16px 8px;
  border-radius: 10px;
  cursor: pointer;
  transition: background 0.15s;
}

.action-btn:hover {
  background: #f0f5ff;
}

.action-btn span {
  font-size: 12px;
  color: var(--muted);
}
</style>
