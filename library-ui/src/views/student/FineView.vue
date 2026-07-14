<!--
  我的罚款页面 (FineView.vue)
  - 展示当前学生的罚款记录（关联图书、金额、原因、状态、缴费时间）
  - 状态为"未缴费"(status===0) 的记录可缴纳
  - 支持分页
-->
<script setup>
// ====================================================================
// 导入
// ====================================================================
import { ref, onMounted } from 'vue'
import { fineList, payFine } from '../../api/fine'          // 罚款列表 + 缴纳罚款 API
import { useAuthStore } from '../../stores/auth'            // 当前登录用户信息
import { ElMessage, ElMessageBox } from 'element-plus'

// ====================================================================
// 认证 store —— 获取当前学生的 userId
// ====================================================================
const authStore = useAuthStore()

// ====================================================================
// 响应式状态
// ====================================================================
const loading = ref(false)          // 列表加载状态
const records = ref([])             // 当前页罚款记录列表
const total = ref(0)                // 符合条件的罚款记录总数
const currentPage = ref(1)          // 当前页码
const pageSize = ref(10)            // 每页条数

// ====================================================================
// 常量映射
// ====================================================================

/**
 * 罚款状态枚举映射
 * 0: 未缴费  1: 已缴费  2: 已免除
 */
const statusMap = {
  0: { label: '未缴费', type: 'danger' },
  1: { label: '已缴费', type: 'success' },
  2: { label: '已免除', type: 'info' },
}

// ====================================================================
// 数据加载
// ====================================================================

/**
 * 根据当前分页参数加载当前学生的罚款记录
 * API 调用时传入 userId 确保只查询自己的记录
 */
async function loadData() {
  loading.value = true
  try {
    const res = await fineList({ page: currentPage.value, size: pageSize.value, userId: authStore.user.userId })
    const p = res.data || {}
    records.value = p.records || []
    total.value = p.total || 0
  } catch { records.value = []; total.value = 0 }
  finally { loading.value = false }
}

// ====================================================================
// 分页事件处理
// ====================================================================

/** 分页页码改变 */
function handlePageChange(p) { currentPage.value = p; loadData() }

/** 分页每页条数改变（重置到第1页） */
function handleSizeChange(s) { pageSize.value = s; currentPage.value = 1; loadData() }

// ====================================================================
// 缴费操作
// ====================================================================

/**
 * 缴纳罚款
 * 仅"未缴费"(status===0) 的记录可缴纳
 * 确认后调用 payFine API，成功后刷新列表
 * @param {Object} row - 当前行罚款记录
 */
async function handlePay(row) {
  try {
    await ElMessageBox.confirm(`确认缴纳 ¥${row.fineAmount} 罚款？`, '缴费确认', { type: 'warning' })
    const res = await payFine(row.id)
    if (res.code === 200) { ElMessage.success('缴费成功'); loadData() }
  } catch (err) { if (err !== 'cancel' && err?.message) ElMessage.error(err.message) }
}

// ====================================================================
// 生命周期
// ====================================================================

/** 组件挂载后加载罚款记录 */
onMounted(loadData)
</script>

<template>
  <div class="student-fine-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">我的罚款</h1>
    </div>

    <!-- 罚款记录表格区域 -->
    <div class="table-section">
      <el-table :data="records" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="bookName" label="关联图书" min-width="200" show-overflow-tooltip />
        <!-- 金额列：红色高亮显示 -->
        <el-table-column label="罚款金额" width="120" align="center">
          <template #default="{row}"><span style="font-weight:600;color:#f56c6c">¥{{ row.fineAmount }}</span></template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="220" show-overflow-tooltip />
        <!-- 状态列：根据 statusMap 渲染颜色标签 -->
        <el-table-column label="状态" width="100" align="center">
          <template #default="{row}">
            <el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <!-- 缴费时间可能为空，显示 "-" -->
        <el-table-column label="缴费时间" width="160">
          <template #default="{row}">{{ row.payTime?.replace('T',' ') || '-' }}</template>
        </el-table-column>
        <!-- 操作列：仅"未缴费"状态显示缴费按钮 -->
        <el-table-column label="操作" width="100" align="center">
          <template #default="{row}">
            <el-button v-if="row.status === 0" type="success" link size="small" @click="handlePay(row)">立即缴费</el-button>
            <span v-else style="color:var(--muted);font-size:12px">—</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 非加载中且无数据时显示空状态 -->
      <div v-if="!loading && records.length === 0" class="empty"><el-empty description="暂无罚款记录" :image-size="100" /></div>

      <!-- 有数据时显示分页器 -->
      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination :current-page="currentPage" :page-size="pageSize" :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" background @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.student-fine-page { padding:0 }
.page-header { margin-bottom:16px }
.page-title { font-size:20px; font-weight:700; margin:0 }
.table-section { background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:20px; min-height:200px }
.empty { padding:30px 0 }
.pagination-wrap { display:flex; justify-content:flex-end; margin-top:16px }
</style>
