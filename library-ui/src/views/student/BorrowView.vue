<!--
  我的借阅记录页面 (BorrowView.vue)
  - 展示当前学生的借阅记录列表（图书名称、ISBN、借/还应/归还时间、续借次数、状态）
  - 状态为"借阅中"(status===0) 的记录可续借
  - 支持分页
-->
<script setup>
// ====================================================================
// 导入
// ====================================================================
import { ref, onMounted } from 'vue'
import { borrowList, renewBook } from '../../api/borrow'  // 借阅列表 + 续借 API
import { useAuthStore } from '../../stores/auth'          // 当前登录用户信息
import { ElMessage, ElMessageBox } from 'element-plus'

// ====================================================================
// 认证 store —— 获取当前学生的 userId
// ====================================================================
const authStore = useAuthStore()

// ====================================================================
// 响应式状态
// ====================================================================
const loading = ref(false)          // 列表加载状态，控制表格 loading 和空状态展示
const records = ref([])             // 当前页借阅记录列表
const total = ref(0)                // 符合条件的借阅记录总数
const currentPage = ref(1)          // 当前页码
const pageSize = ref(10)            // 每页条数

// ====================================================================
// 常量映射
// ====================================================================

/**
 * 借阅状态枚举映射
 * 0: 借阅中  1: 已归还  2: 逾期未还  3: 逾期已还  4: 图书遗失
 */
const statusMap = {
  0: { label: '借阅中', type: 'primary' },
  1: { label: '已归还', type: 'success' },
  2: { label: '逾期未还', type: 'danger' },
  3: { label: '逾期已还', type: 'warning' },
  4: { label: '图书遗失', type: 'info' },
}

// ====================================================================
// 数据加载
// ====================================================================

/**
 * 根据当前分页参数加载当前学生的借阅记录
 * API 调用时传入 userId 确保只查询自己的记录
 */
async function loadData() {
  loading.value = true
  try {
    const res = await borrowList({ page: currentPage.value, size: pageSize.value, userId: authStore.user.userId })
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
// 续借操作
// ====================================================================

/**
 * 续借图书
 * 仅"借阅中"(status===0) 的记录可续借
 * 确认后调用 renewBook API，成功后刷新列表
 * @param {Object} row - 当前行借阅记录
 */
async function handleRenew(row) {
  try {
    await ElMessageBox.confirm('确认续借该图书吗？', '续借确认', { type: 'info' })
    const res = await renewBook(row.id, row.userId)
    if (res.code === 200) { ElMessage.success('续借成功'); loadData() }
  } catch (err) { if (err !== 'cancel' && err?.message) ElMessage.error(err.message) }
}

// ====================================================================
// 生命周期
// ====================================================================

/** 组件挂载后加载借阅记录 */
onMounted(loadData)
</script>

<template>
  <div class="student-borrow-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1 class="page-title">我的借阅记录</h1>
    </div>

    <!-- 借阅记录表格区域 -->
    <div class="table-section">
      <el-table :data="records" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="bookName" label="图书名称" min-width="200" show-overflow-tooltip />
        <el-table-column prop="bookIsbn" label="ISBN" width="150" show-overflow-tooltip />
        <!-- 时间列：将后端 "T" 分隔符替换为空格 -->
        <el-table-column label="借出时间" width="160">
          <template #default="{row}">{{ row.borrowTime?.replace('T',' ') }}</template>
        </el-table-column>
        <el-table-column label="应还时间" width="160">
          <template #default="{row}">{{ row.expectReturnTime?.replace('T',' ') }}</template>
        </el-table-column>
        <!-- 归还时间可能为空，显示 "-" -->
        <el-table-column label="归还时间" width="160">
          <template #default="{row}">{{ row.actualReturnTime?.replace('T',' ') || '-' }}</template>
        </el-table-column>
        <el-table-column prop="renewCount" label="续借次数" width="90" align="center" />
        <!-- 状态列：根据 statusMap 渲染颜色标签 -->
        <el-table-column label="状态" width="100" align="center">
          <template #default="{row}">
            <el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <!-- 操作列：仅"借阅中"状态显示续借按钮 -->
        <el-table-column label="操作" width="100" align="center">
          <template #default="{row}">
            <el-button v-if="row.status === 0" type="primary" link size="small" @click="handleRenew(row)">续借</el-button>
            <span v-else style="color:var(--muted);font-size:12px">—</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 非加载中且无数据时显示空状态 -->
      <div v-if="!loading && records.length === 0" class="empty"><el-empty description="暂无借阅记录" :image-size="100" /></div>

      <!-- 有数据时显示分页器 -->
      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination :current-page="currentPage" :page-size="pageSize" :total="total" :page-sizes="[10,20,50]" layout="total,sizes,prev,pager,next" background @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.student-borrow-page { padding:0 }
.page-header { margin-bottom:16px }
.page-title { font-size:20px; font-weight:700; margin:0 }
.table-section { background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:20px; min-height:200px }
.empty { padding:30px 0 }
.pagination-wrap { display:flex; justify-content:flex-end; margin-top:16px }
</style>
