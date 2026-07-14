<!--
  罚款记录页 - FineListView
  功能：查看和管理所有罚款记录
  - 搜索：状态筛选（未缴费/已缴费/已免除）
  - 分页表格：用户、关联图书、罚款金额、原因、状态、缴费时间、创建时间
  - 行操作（仅未缴费状态可用）：缴纳、豁免
  - 状态映射：0=未缴费 / 1=已缴费 / 2=已免除
-->
<script setup>
import { ref, reactive, onMounted } from 'vue'
// 罚款 API：列表、缴纳、豁免
import { fineList, payFine, exemptFine } from '../../api/fine'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh } from '@element-plus/icons-vue'

// === 响应式状态 ===
const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

// 搜索表单：仅按状态筛选
const searchForm = reactive({ status: null })

// 罚款状态映射表
const statusMap = {
  0: { label: '未缴费', type: 'danger' },
  1: { label: '已缴费', type: 'success' },
  2: { label: '已免除', type: 'info' },
}

// 状态筛选选项
const statusOptions = [
  { value: 0, label: '未缴费' },
  { value: 1, label: '已缴费' },
  { value: 2, label: '已免除' },
]

// 加载罚款列表
async function loadData() {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (searchForm.status != null && searchForm.status !== '') params.status = searchForm.status
    const res = await fineList(params)
    const page = res.data || {}
    tableData.value = page.records || []
    total.value = page.total || 0
  } catch { tableData.value = []; total.value = 0 }
  finally { loading.value = false }
}

// 搜索、重置、分页
function handleSearch() { currentPage.value = 1; loadData() }
function handleReset() { searchForm.status = null; currentPage.value = 1; loadData() }
function handlePageChange(p) { currentPage.value = p; loadData() }
function handleSizeChange(s) { pageSize.value = s; currentPage.value = 1; loadData() }

/**
 * 缴纳罚款：确认框 -> payFine API
 * 仅"未缴费"状态的记录可操作
 */
async function handlePay(row) {
  try {
    await ElMessageBox.confirm(
      `确认「${row.userRealName}」缴纳 ¥${row.fineAmount} 罚款？`,
      '缴纳确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await payFine(row.id)
    if (res.code === 200) { ElMessage.success('已标记为已缴费'); loadData() }
  } catch (err) { if (err !== 'cancel' && err?.message) ElMessage.error(err.message) }
}

/**
 * 豁免罚款：确认框 -> exemptFine API
 * 仅"未缴费"状态的记录可操作
 */
async function handleExempt(row) {
  try {
    await ElMessageBox.confirm(
      `确认免除「${row.userRealName}」¥${row.fineAmount} 的罚款？`,
      '豁免确认', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await exemptFine(row.id)
    if (res.code === 200) { ElMessage.success('已豁免'); loadData() }
  } catch (err) { if (err !== 'cancel' && err?.message) ElMessage.error(err.message) }
}

onMounted(loadData)
</script>

<!-- ==================== 模板 ==================== -->
<template>
  <div class="fine-list-page">
    <!-- 顶部标题栏 -->
    <div class="page-header">
      <h1 class="page-title">罚款记录</h1>
    </div>

    <!-- 搜索区域：仅含状态筛选 -->
    <div class="search-section">
      <div class="search-row">
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
        <el-table-column prop="userRealName" label="用户" width="110" show-overflow-tooltip />
        <el-table-column prop="bookName" label="关联图书" min-width="180" show-overflow-tooltip />
        <!-- 罚款金额列：红色高亮显示 -->
        <el-table-column label="罚款金额" width="110" align="center">
          <template #default="{row}"><span style="font-weight:600;color:#f56c6c">¥{{ row.fineAmount }}</span></template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="200" show-overflow-tooltip />
        <!-- 状态列 -->
        <el-table-column label="状态" width="100" align="center">
          <template #default="{row}">
            <el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="缴费时间" width="160">
          <template #default="{row}">{{ row.payTime?.replace('T',' ') || '-' }}</template>
        </el-table-column>
        <el-table-column label="创建时间" width="160">
          <template #default="{row}">{{ row.createTime?.replace('T',' ') }}</template>
        </el-table-column>
        <!-- 操作列：仅"未缴费"状态下显示 -->
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{row}">
            <template v-if="row.status === 0">
              <el-button type="success" link size="small" @click="handlePay(row)">缴纳</el-button>
              <el-button type="warning" link size="small" @click="handleExempt(row)">豁免</el-button>
            </template>
            <span v-else style="color:var(--muted);font-size:12px">—</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <div v-if="!loading && tableData.length === 0" class="empty"><el-empty description="暂无罚款记录" :image-size="100" /></div>

      <!-- 分页器 -->
      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination :current-page="currentPage" :page-size="pageSize" :total="total" :page-sizes="[10,20,50,100]" layout="total,sizes,prev,pager,next,jumper" background @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </div>
  </div>
</template>

<!-- ==================== 样式（scoped） ==================== -->
<style scoped>
.fine-list-page { padding:0 }
.page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px }
.page-title { font-size:20px; font-weight:700; margin:0 }
.search-section { background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:16px 20px; margin-bottom:16px }
.search-row { display:flex; align-items:center; gap:12px; flex-wrap:wrap }
.search-select-sm { width:140px }
.table-section { background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:20px }
.empty { padding:30px 0 }
.pagination-wrap { display:flex; justify-content:flex-end; margin-top:16px }
</style>
