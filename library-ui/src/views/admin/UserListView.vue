<!--
  用户列表页 - UserListView
  功能：用户管理，含搜索、禁用/启用、重置密码、删除
  - 搜索：用户名/真实姓名关键词 + 状态筛选（正常/禁用）
  - 分页表格：用户名、真实姓名、邮箱、手机号、角色、状态、创建时间
  - 行操作：编辑、重置密码（弹窗输入新密码）、启用/禁用切换、删除
  - 状态映射：1=正常 / 0=禁用
-->
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
// 用户 API：列表、删除、状态更新、重置密码
import { userList, deleteUser, updateUserStatus, resetPassword } from '../../api/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Search, Refresh } from '@element-plus/icons-vue'

const router = useRouter()

// === 响应式状态 ===
const loading = ref(false)       // 表格 loading
const tableData = ref([])        // 当前页用户数据
const total = ref(0)             // 总记录数
const currentPage = ref(1)       // 当前页码
const pageSize = ref(10)         // 每页条数

// 搜索表单
const searchForm = reactive({ keyword: '', status: null })

// 状态筛选选项
const statusOptions = [
  { label: '全部状态', value: null },
  { label: '正常', value: 1 },
  { label: '禁用', value: 0 },
]

// 状态映射表：状态码 -> { 标签, Tag 类型 }
const userStatus = { 1: { label: '正常', type: 'success' }, 0: { label: '禁用', type: 'danger' } }

/**
 * 加载用户列表：分页 + 可选搜索条件
 */
async function loadUsers() {
  loading.value = true
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.status != null && searchForm.status !== '') params.status = searchForm.status
    const res = await userList(params)
    const page = res.data || {}
    tableData.value = page.records || []
    total.value = page.total || 0
  } catch { tableData.value = []; total.value = 0 }
  finally { loading.value = false }
}

// 搜索：重置到第 1 页
function handleSearch() { currentPage.value = 1; loadUsers() }
// 重置搜索条件
function handleReset() { searchForm.keyword = ''; searchForm.status = null; currentPage.value = 1; loadUsers() }
// 分页事件
function handlePageChange(p) { currentPage.value = p; loadUsers() }
function handleSizeChange(s) { pageSize.value = s; currentPage.value = 1; loadUsers() }

// 跳转新增用户页
function goAdd() { router.push('/users/add') }

/**
 * 跳转编辑用户页
 * 通过路由 state 传递当前行数据（用户名、真实姓名等），避免再次请求详情
 */
function goEdit(row) {
  router.push(`/users/${row.id}/edit`)
}

/**
 * 重置密码：弹出输入框 -> 调用 resetPassword API
 */
async function handleResetPwd(row) {
  try {
    const { value } = await ElMessageBox.prompt(
      `为「${row.realName || row.username}」设置新密码`,
      '重置密码',
      { confirmButtonText: '确定', cancelButtonText: '取消', inputType: 'password', inputPlaceholder: '请输入新密码（至少6位）', inputPattern: /^.{6,}$/, inputErrorMessage: '密码至少6位' }
    )
    const res = await resetPassword(row.id, value)
    if (res.code === 200) ElMessage.success('密码重置成功')
  } catch (err) {
    if (err !== 'cancel' && err?.message) ElMessage.error(err.message || '操作失败')
  }
}

/**
 * 切换用户启用/禁用状态：确认框 -> updateUserStatus API
 * 按钮文字根据当前状态动态切换
 */
async function handleToggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  const label = newStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `确定${label}用户「${row.realName || row.username}」吗？`,
      '确认操作',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await updateUserStatus(row.id, newStatus)
    if (res.code === 200) { ElMessage.success(`${label}成功`); loadUsers() }
  } catch (err) {
    if (err !== 'cancel' && err?.message) ElMessage.error(err.message || '操作失败')
  }
}

/**
 * 删除用户：确认框 -> deleteUser API
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定删除用户「${row.realName || row.username}」吗？`, '删除确认', { type: 'warning' })
    const res = await deleteUser(row.id)
    if (res.code === 200) { ElMessage.success('删除成功'); loadUsers() }
  } catch (err) {
    if (err !== 'cancel' && err?.message) ElMessage.error(err.message || '操作失败')
  }
}

// 页面挂载时加载用户列表
onMounted(loadUsers)
</script>

<!-- ==================== 模板 ==================== -->
<template>
  <div class="user-list-page">
    <!-- 顶部标题栏 + 新增按钮 -->
    <div class="page-header">
      <h1 class="page-title">用户管理</h1>
      <el-button type="primary" :icon="Plus" @click="goAdd">新增用户</el-button>
    </div>

    <!-- 搜索区域 -->
    <div class="search-section">
      <div class="search-row">
        <el-input v-model="searchForm.keyword" placeholder="用户名 / 真实姓名" clearable :prefix-icon="Search" class="search-input" @keyup.enter="handleSearch" />
        <el-select v-model="searchForm.status" placeholder="全部状态" clearable class="search-select-sm">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 数据表格 -->
    <div class="table-section">
      <el-table :data="tableData" v-loading="loading" stripe border style="width:100%">
        <el-table-column prop="username" label="用户名" width="140" show-overflow-tooltip />
        <el-table-column prop="realName" label="真实姓名" width="120" show-overflow-tooltip />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="130" />
        <!-- 角色列：遍历 roleNames 数组，每个角色用 el-tag 展示 -->
        <el-table-column label="角色" width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-for="r in row.roleNames" :key="r" size="small" style="margin-right:4px;margin-bottom:2px">{{ r }}</el-tag>
          </template>
        </el-table-column>
        <!-- 状态列：使用 userStatus 映射表 -->
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="userStatus[row.status]?.type" size="small">{{ userStatus[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <!-- 操作列：编辑、重置密码、启用/禁用、删除 -->
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="goEdit(row)">编辑</el-button>
            <el-button type="warning" link @click="handleResetPwd(row)">重置密码</el-button>
            <el-button :type="row.status === 1 ? 'warning' : 'success'" link @click="handleToggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <div v-if="!loading && tableData.length === 0" class="empty"><el-empty description="暂无用户数据" :image-size="100" /></div>

      <!-- 分页器 -->
      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination :current-page="currentPage" :page-size="pageSize" :total="total" :page-sizes="[10,20,50,100]" layout="total,sizes,prev,pager,next,jumper" background @current-change="handlePageChange" @size-change="handleSizeChange" />
      </div>
    </div>
  </div>
</template>

<!-- ==================== 样式（scoped） ==================== -->
<style scoped>
.user-list-page { padding: 0 }
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
