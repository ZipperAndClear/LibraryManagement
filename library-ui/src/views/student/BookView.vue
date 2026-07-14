<!--
  图书检索与借阅页面 (BookView.vue)
  - 提供按关键字/分类搜索图书的功能
  - 以卡片网格形式展示图书列表（封面、书名、作者、分类、状态、库存）
  - 在库且有库存的图书可一键借阅
  - 支持分页浏览
-->
<script setup>
// ====================================================================
// 导入
// ====================================================================
import { ref, reactive, onMounted } from 'vue'
import { bookSearch } from '../../api/book'          // 图书搜索 API（分页+条件）
import { categoryTree } from '../../api/category'    // 分类树 API（用于下拉筛选）
import { borrowBook } from '../../api/borrow'        // 借阅图书 API
import { useAuthStore } from '../../stores/auth'     // 当前登录用户信息
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'

// ====================================================================
// 认证 store —— 获取当前学生的 userId
// ====================================================================
const authStore = useAuthStore()
const apiBase = import.meta.env.VITE_API_BASE_URL || ''

function coverUrl(raw) {
  if (!raw) return ''
  if (raw.startsWith('http://') || raw.startsWith('https://')) return raw
  return apiBase + raw
}

// ====================================================================
// 响应式状态
// ====================================================================
const loading = ref(false)                    // 列表加载状态，控制 loading 遮罩
const borrowing = ref({})                     // 借阅按钮 loading 状态，key=bookId, value=boolean
const books = ref([])                         // 当前页图书列表
const total = ref(0)                          // 符合条件的图书总数
const currentPage = ref(1)                    // 当前页码
const pageSize = ref(12)                      // 每页条数
const flatCategories = ref([])                // 扁平化的分类列表，用于 el-select 选项

// 搜索表单绑定（双向绑定至模板中的输入框）
const searchForm = reactive({ keyword: '', categoryId: null })

// ====================================================================
// 工具函数
// ====================================================================

/**
 * 将后端返回的分类树拍平为一维数组
 * @param {Array} nodes  - 树节点数组，每项包含 id, name, children
 * @param {string} prefix - 用于构建层级前缀（如 "文学 / 小说"）
 * @returns {Array} 拍平后的节点 [{id, name}, ...]
 */
function flattenTree(nodes, prefix = '') {
  const r = []
  for (const n of nodes) {
    r.push({ id: n.id, name: prefix + n.name })
    if (n.children?.length) r.push(...flattenTree(n.children, prefix + n.name + ' / '))
  }
  return r
}

// ====================================================================
// 数据加载
// ====================================================================

/**
 * 加载分类列表并拍平放入 flatCategories，供搜索区域下拉选择使用
 */
async function loadCategories() {
  try { const res = await categoryTree(); flatCategories.value = flattenTree(res.data || []) } catch { /* 静默失败 */ }
}

/**
 * 根据当前搜索条件和分页参数加载图书列表
 * 将结果写入 books 和 total，并控制 loading 遮罩
 */
async function loadBooks() {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.categoryId) params.categoryId = searchForm.categoryId
    const res = await bookSearch(params)
    const p = res.data || {}
    books.value = p.records || []
    total.value = p.total || 0
  } catch { books.value = []; total.value = 0 }
  finally { loading.value = false }
}

// ====================================================================
// 搜索/重置/分页 事件处理
// ====================================================================

/** 搜索：重置到第1页并重新加载图书列表 */
function handleSearch() { currentPage.value = 1; loadBooks() }

/** 重置搜索条件并重新加载 */
function handleReset() { searchForm.keyword = ''; searchForm.categoryId = null; currentPage.value = 1; loadBooks() }

/** 分页页码改变 */
function handlePageChange(p) { currentPage.value = p; loadBooks() }

/** 分页每页条数改变（重置到第1页） */
function handleSizeChange(s) { pageSize.value = s; currentPage.value = 1; loadBooks() }

// ====================================================================
// 借阅操作
// ====================================================================

/**
 * 借阅图书
 * 前置校验：图书状态必须为"在库"(status===1)且库存 > 0
 * 确认后调用 borrowBook API，成功后刷新列表
 * @param {Object} book - 当前行图书对象
 */
async function handleBorrow(book) {
  if (book.status !== 1 || !book.stock) { ElMessage.warning('该图书暂不可借阅'); return }
  try {
    await ElMessageBox.confirm(`确定借阅「${book.name}」吗？`, '借阅确认', { type: 'info' })
    borrowing.value[book.id] = true
    const res = await borrowBook(authStore.user.userId, book.id)
    if (res.code === 200) {
      ElMessage.success('借阅成功，请在规定时间内归还')
      loadBooks()
    }
  } catch (err) { if (err !== 'cancel' && err?.message) ElMessage.error(err.message) }
  finally { borrowing.value[book.id] = false }
}

// ====================================================================
// 生命周期
// ====================================================================

/** 组件挂载后加载分类和图书数据 */
onMounted(() => { loadCategories(); loadBooks() })
</script>

<template>
  <div class="student-book-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">图书检索与借阅</h1>
    </div>

    <!-- 搜索区域：关键字输入 + 分类下拉 + 搜索/重置按钮 -->
    <div class="search-section">
      <div class="search-row">
        <el-input v-model="searchForm.keyword" placeholder="书名 / 作者 / ISBN" clearable :prefix-icon="Search" class="search-input" @keyup.enter="handleSearch" />
        <el-select v-model="searchForm.categoryId" placeholder="全部分类" clearable class="search-select">
          <el-option v-for="c in flatCategories" :key="c.id" :label="c.name" :value="c.id" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 图书卡片网格区域 -->
    <div v-loading="loading" class="book-grid-section">
      <!-- 有数据时渲染卡片网格 -->
      <div v-if="books.length" class="book-grid">
        <div v-for="book in books" :key="book.id" class="book-card">
          <!-- 图书封面/占位 -->
          <div class="book-cover">
            <img v-if="book.coverUrl" :src="coverUrl(book.coverUrl)" alt="" />
            <div v-else class="cover-placeholder">{{ book.name?.charAt(0) }}</div>
          </div>
          <!-- 图书信息主体 -->
          <div class="book-body">
            <div class="book-name">{{ book.name }}</div>
            <div class="book-meta">{{ book.author }} · {{ book.categoryName || '未分类' }}</div>
            <!-- 底部：状态标签 + 库存 + 借阅按钮 -->
            <div class="book-bottom">
              <el-tag :type="book.status === 1 ? 'success' : book.status === 2 ? 'warning' : 'info'" size="small">
                {{ { 1: '在库', 2: '已借出', 3: '下架', 4: '遗失' }[book.status] || '未知' }}
              </el-tag>
              <span class="book-stock">库存 {{ book.stock }}</span>
              <!-- 在库且有库存时显示借阅按钮，否则显示"不可借"标签 -->
              <el-button
                v-if="book.status === 1 && book.stock > 0"
                type="primary"
                size="small"
                :loading="borrowing[book.id]"
                @click="handleBorrow(book)"
              >借阅</el-button>
              <el-tag v-else type="info" size="small" effect="dark" style="cursor:default">不可借</el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 无数据且非加载中时显示空状态 -->
      <el-empty v-else-if="!loading" description="暂无图书" :image-size="100" />

      <!-- 超过12条时显示分页器 -->
      <div v-if="total > 12" class="pagination-wrap">
        <el-pagination :current-page="currentPage" :page-size="pageSize" :total="total" :page-sizes="[12,24,48]" layout="total,sizes,prev,pager,next" background @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.student-book-page { padding:0 }
.page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px }
.page-title { font-size:20px; font-weight:700; margin:0 }
.search-section { background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:16px 20px; margin-bottom:16px }
.search-row { display:flex; align-items:center; gap:12px; flex-wrap:wrap }
.search-input { width:240px }
.search-select { width:180px }
.book-grid-section { background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:20px; min-height:300px }
.book-grid { display:grid; grid-template-columns:repeat(auto-fill,minmax(260px,1fr)); gap:16px }
.book-card { display:flex; gap:14px; padding:16px; border:1px solid var(--line); border-radius:10px; transition:box-shadow 0.2s }
.book-card:hover { box-shadow:0 2px 12px rgba(0,0,0,0.06) }
.book-cover { width:80px; height:110px; border-radius:6px; overflow:hidden; flex-shrink:0; background:#f0f2f5 }
.book-cover img { width:100%; height:100%; object-fit:cover }
.cover-placeholder { width:100%; height:100%; display:flex; align-items:center; justify-content:center; font-size:24px; font-weight:700; color:#c0c4cc }
.book-body { flex:1; display:flex; flex-direction:column; min-width:0 }
.book-name { font-size:15px; font-weight:600; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.book-meta { font-size:12px; color:var(--muted); margin-top:4px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.book-bottom { display:flex; align-items:center; gap:8px; margin-top:auto; padding-top:10px }
.book-stock { font-size:12px; color:var(--muted); margin-right:auto }
.pagination-wrap { display:flex; justify-content:center; margin-top:20px }
</style>
