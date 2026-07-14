<!--
  公告列表页 - NoticeListView
  功能：公告的列表展示、发布/撤回管理
  - 搜索：公告标题关键词 + 状态筛选（草稿/已发布）
  - 分页表格：标题、置顶标记、状态、创建时间
  - 行操作：编辑、发布(草稿->已发布)、撤回(已发布->草稿)、删除
  - 删除后处理当前页为空则回退一页
-->
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
// 公告 API：列表、发布、撤回、删除
import { noticeList, publishNotice, unpublishNotice, deleteNotice } from '../../api/notice'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'

const router = useRouter()

// === 响应式状态 ===
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 搜索表单
const searchForm = reactive({ keyword: '', status: null })

// 搜索、重置、分页
function handleSearch() { currentPage.value = 1; loadData() }
function handleReset() { searchForm.keyword = ''; searchForm.status = null; currentPage.value = 1; loadData() }
function handlePageChange(p) { currentPage.value = p; loadData() }
function handleSizeChange(s) { pageSize.value = s; currentPage.value = 1; loadData() }

// 加载公告列表
async function loadData() {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.status != null && searchForm.status !== '') params.status = searchForm.status
    const res = await noticeList(params)
    const page = res.data || {}
    tableData.value = page.records || []
    total.value = page.total || 0
  } catch { tableData.value = []; total.value = 0 }
  finally { loading.value = false }
}

// 跳转新增公告页
function goAdd() { router.push('/notices/add') }

/**
 * 跳转编辑公告页
 * 通过路由 state 传递标题、内容、置顶状态，避免额外请求
 */
function goEdit(row) {
  router.push({
    path: `/notices/${row.id}/edit`,
    state: { title: row.title, content: row.content, isTop: row.isTop },
  })
}

/**
 * 发布公告（草稿 -> 已发布）
 */
async function handlePublish(row) {
  try {
    await ElMessageBox.confirm(`确定发布「${row.title}」吗？`, '发布确认', { type: 'info' })
    const res = await publishNotice(row.id)
    if (res.code === 200) { ElMessage.success('已发布'); loadData() }
  } catch (err) { if (err !== 'cancel' && err?.message) ElMessage.error(err.message) }
}

/**
 * 撤回公告（已发布 -> 草稿），撤回后学生端不可见
 */
async function handleUnpublish(row) {
  try {
    await ElMessageBox.confirm(`确定撤回「${row.title}」吗？撤回后学生端不可见。`, '撤回确认', { type: 'warning' })
    const res = await unpublishNotice(row.id)
    if (res.code === 200) { ElMessage.success('已撤回'); loadData() }
  } catch (err) { if (err !== 'cancel' && err?.message) ElMessage.error(err.message) }
}

/**
 * 删除公告：确认框 -> deleteNotice API
 * 删除后若当前页仅剩一条且非第一页则回退一页
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title}」吗？`, '删除确认', { type: 'warning' })
    const res = await deleteNotice(row.id)
    if (res.code === 200) { ElMessage.success('已删除'); if (tableData.value.length === 1 && currentPage.value > 1) currentPage.value--; loadData() }
  } catch (err) { if (err !== 'cancel' && err?.message) ElMessage.error(err.message) }
}

onMounted(loadData)
</script>

<!-- ==================== 模板 ==================== -->
<template>
  <div class="notice-list-page">
    <!-- 顶部标题栏 + 新增按钮 -->
    <div class="page-header">
      <h1 class="page-title">公告列表</h1>
      <el-button type="primary" :icon="Plus" @click="goAdd">新增公告</el-button>
    </div>

    <!-- 搜索区域 -->
    <div class="search-section">
      <div class="search-row">
        <el-input v-model="searchForm.keyword" placeholder="公告标题" clearable :prefix-icon="Search" class="search-input" @keyup.enter="handleSearch" />
        <el-select v-model="searchForm.status" placeholder="全部状态" clearable class="search-select-sm">
          <el-option label="草稿" :value="0" />
          <el-option label="已发布" :value="1" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
        <!-- 置顶标记：使用 el-tag 红色显示 -->
        <el-table-column label="置顶" width="70" align="center">
          <template #default="{row}"><el-tag v-if="row.isTop" size="small" type="danger">置顶</el-tag><span v-else style="color:var(--muted)">—</span></template>
        </el-table-column>
        <!-- 状态列：status=1 为已发布 / 0 为草稿 -->
        <el-table-column label="状态" width="90" align="center">
          <template #default="{row}">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '已发布' : '草稿' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170">
          <template #default="{row}">{{ row.createTime?.replace('T',' ') }}</template>
        </el-table-column>
        <!-- 操作列：编辑、发布/撤回（互斥显示）、删除 -->
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{row}">
            <el-button type="primary" link :icon="Edit" @click="goEdit(row)">编辑</el-button>
            <el-button v-if="row.status === 0" type="success" link @click="handlePublish(row)">发布</el-button>
            <el-button v-if="row.status === 1" type="warning" link @click="handleUnpublish(row)">撤回</el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <div v-if="!loading && tableData.length === 0" class="empty"><el-empty description="暂无公告" :image-size="100" /></div>

      <!-- 分页器 -->
      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination :current-page="currentPage" :page-size="pageSize" :total="total" :page-sizes="[10,20,50,100]" layout="total,sizes,prev,pager,next,jumper" background @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </div>
  </div>
</template>

<!-- ==================== 样式（scoped） ==================== -->
<style scoped>
.notice-list-page { padding:0 }
.page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px }
.page-title { font-size:20px; font-weight:700; margin:0 }
.search-section { background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:16px 20px; margin-bottom:16px }
.search-row { display:flex; align-items:center; gap:12px; flex-wrap:wrap }
.search-input { width:240px }
.search-select-sm { width:130px }
.table-section { background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:20px }
.empty { padding:30px 0 }
.pagination-wrap { display:flex; justify-content:flex-end; margin-top:16px }
</style>
