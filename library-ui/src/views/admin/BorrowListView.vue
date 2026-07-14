<!--
  借阅记录页 - BorrowListView
  功能：查看和管理所有图书借阅记录
  - 搜索：图书名称关键词 + 状态筛选
  - 分页表格：借阅人、图书名称、ISBN、借出/应还/归还时间、续借次数、状态、罚款金额
  - 状态映射：0=借阅中/1=正常归还/2=逾期未还/3=逾期已还/4=图书遗失
  - 行操作（仅活跃状态可用）：归还、续借（仅借阅中可续借）、标记遗失
  - 标记遗失后会产生赔偿罚款
-->
<script setup>
import { ref, reactive, onMounted } from 'vue'
// 借阅 API：列表、归还、续借、标记遗失
import { borrowList, returnBook, renewBook, markLost } from '../../api/borrow'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'

// === 响应式状态 ===
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 搜索表单：关键词（图书名称）+ 状态筛选
const searchForm = reactive({ keyword: '', status: null })

// 借阅状态映射表
const statusMap = {
  0: { label: '借阅中', type: 'primary' },
  1: { label: '正常归还', type: 'success' },
  2: { label: '逾期未还', type: 'danger' },
  3: { label: '逾期已还', type: 'warning' },
  4: { label: '图书遗失', type: 'info' },
}

// 状态筛选下拉框选项
const statusOptions = [
  { label: '借阅中', value: 0 },
  { label: '正常归还', value: 1 },
  { label: '逾期未还', value: 2 },
  { label: '逾期已还', value: 3 },
  { label: '图书遗失', value: 4 },
]

// 加载借阅记录列表
async function loadData() {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.status != null && searchForm.status !== '') params.status = searchForm.status
    const res = await borrowList(params)
    const page = res.data || {}
    tableData.value = page.records || []
    total.value = page.total || 0
  } catch { tableData.value = []; total.value = 0 }
  finally { loading.value = false }
}

// 搜索、重置、分页事件
function handleSearch() { currentPage.value = 1; loadData() }
function handleReset() { searchForm.keyword = ''; searchForm.status = null; currentPage.value = 1; loadData() }
function handlePageChange(p) { currentPage.value = p; loadData() }
function handleSizeChange(s) { pageSize.value = s; currentPage.value = 1; loadData() }

/**
 * 判断记录是否处于"活跃"状态（借阅中或逾期未还）
 * 活跃状态下才显示操作按钮（归还、续借、标记遗失）
 */
function isActive(status) { return status === 0 || status === 2 }

/**
 * 归还图书：确认框 -> returnBook API
 */
async function handleReturn(row) {
  try {
    await ElMessageBox.confirm(`确定归还「${row.bookName}」吗？`, '归还确认', { type: 'warning' })
    const res = await returnBook(row.id, row.userId)
    if (res.code === 200) { ElMessage.success('归还成功'); loadData() }
  } catch (err) { if (err !== 'cancel' && err?.message) ElMessage.error(err.message) }
}

/**
 * 续借图书：确认框 -> renewBook API
 * 仅在"借阅中"状态下可选
 */
async function handleRenew(row) {
  try {
    await ElMessageBox.confirm(`确定续借「${row.bookName}」吗？`, '续借确认', { type: 'info' })
    const res = await renewBook(row.id, row.userId)
    if (res.code === 200) { ElMessage.success('续借成功'); loadData() }
  } catch (err) { if (err !== 'cancel' && err?.message) ElMessage.error(err.message) }
}

/**
 * 标记遗失：确认框 -> markLost API
 * 标记后将产生赔偿罚款记录
 */
async function handleMarkLost(row) {
  try {
    await ElMessageBox.confirm(
      `确认将「${row.bookName}」标记为遗失吗？将产生赔偿罚款。`,
      '标记遗失', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await markLost(row.id)
    if (res.code === 200) { ElMessage.success('已标记为遗失'); loadData() }
  } catch (err) { if (err !== 'cancel' && err?.message) ElMessage.error(err.message) }
}

onMounted(loadData)
</script>

<!-- ==================== 模板 ==================== -->
<template>
  <div class="borrow-list-page">
    <!-- 顶部标题栏 -->
    <div class="page-header">
      <h1 class="page-title">借阅记录</h1>
    </div>

    <!-- 搜索区域 -->
    <div class="search-section">
      <div class="search-row">
        <el-input v-model="searchForm.keyword" placeholder="图书名称" clearable :prefix-icon="Search" class="search-input" @keyup.enter="handleSearch" />
        <el-select v-model="searchForm.status" placeholder="全部状态" clearable class="search-select-sm">
          <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="userRealName" label="借阅人" width="110" show-overflow-tooltip />
        <el-table-column prop="bookName" label="图书名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="bookIsbn" label="ISBN" width="150" show-overflow-tooltip />
        <!-- 时间列：将后端 ISO 格式中的 'T' 替换为空格显示 -->
        <el-table-column label="借出时间" width="160">
          <template #default="{row}">{{ row.borrowTime?.replace('T',' ') }}</template>
        </el-table-column>
        <el-table-column label="应还时间" width="160">
          <template #default="{row}">{{ row.expectReturnTime?.replace('T',' ') }}</template>
        </el-table-column>
        <el-table-column label="归还时间" width="160">
          <template #default="{row}">{{ row.actualReturnTime?.replace('T',' ') || '-' }}</template>
        </el-table-column>
        <el-table-column prop="renewCount" label="续借" width="60" align="center" />
        <!-- 状态列：el-tag 展示 -->
        <el-table-column label="状态" width="100" align="center">
          <template #default="{row}">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <!-- 罚款金额 -->
        <el-table-column label="罚款" width="90" align="center">
          <template #default="{row}">{{ row.fineAmount != null ? '¥' + row.fineAmount : '-' }}</template>
        </el-table-column>
        <!-- 操作列：仅活跃状态（借阅中/逾期未还）下显示 -->
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{row}">
            <template v-if="isActive(row.status)">
              <el-button type="success" link size="small" @click="handleReturn(row)">归还</el-button>
              <!-- 续借仅借阅中（status=0）时可用 -->
              <el-button v-if="row.status === 0" type="primary" link size="small" @click="handleRenew(row)">续借</el-button>
              <el-button type="danger" link size="small" @click="handleMarkLost(row)">标记遗失</el-button>
            </template>
            <span v-else style="color:var(--muted);font-size:12px">—</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <div v-if="!loading && tableData.length === 0" class="empty"><el-empty description="暂无借阅记录" :image-size="100" /></div>

      <!-- 分页器 -->
      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination :current-page="currentPage" :page-size="pageSize" :total="total" :page-sizes="[10,20,50,100]" layout="total,sizes,prev,pager,next,jumper" background @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </div>
  </div>
</template>

<!-- ==================== 样式（scoped） ==================== -->
<style scoped>
.borrow-list-page { padding:0 }
.page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px }
.page-title { font-size:20px; font-weight:700; margin:0 }
.search-section { background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:16px 20px; margin-bottom:16px }
.search-row { display:flex; align-items:center; gap:12px; flex-wrap:wrap }
.search-input { width:220px }
.search-select-sm { width:140px }
.table-section { background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:20px }
.empty { padding:30px 0 }
.pagination-wrap { display:flex; justify-content:flex-end; margin-top:16px }
</style>
