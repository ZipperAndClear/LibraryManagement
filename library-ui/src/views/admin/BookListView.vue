<!--
  图书列表页 - BookListView
  功能：图书的搜索、展示、状态管理和数据导入导出
  - 多条件搜索：书名/作者/ISBN 关键词 + 分类下拉 + 状态筛选
  - 分页表格展示图书列表（ISBN、书名、作者、分类、库存、状态、创建时间）
  - 行操作：编辑、状态变更（下拉菜单，禁止当前状态重复选择）、删除
  - 工具栏：Excel 导入、Excel 导出
  - 状态映射：1=在库 / 2=全部借出 / 3=下架 / 4=遗失
-->
<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
// 图书相关 API：分页搜索、删除、状态更新、导入导出
import { bookSearch, deleteBook, updateBookStatus, importBooks, exportBooks } from '../../api/book'
// 分类 API：获取分类树，然后扁平化给下拉框使用
import { categoryTree } from '../../api/category'
// Element Plus 图标
import {
  Search, Refresh, Plus, Edit, Delete, Download, Upload, ArrowDown
} from '@element-plus/icons-vue'
// Element Plus 消息提示和确认弹窗
import { ElMessage, ElMessageBox } from 'element-plus'

// Vue Router 实例，用于页面跳转
const router = useRouter()

// 图书状态映射表：状态码 -> { 中文标签, Element Tag 颜色类型 }
const statusMap = {
  1: { label: '在库', type: 'success' },
  2: { label: '全部借出', type: 'warning' },
  3: { label: '下架', type: 'info' },
  4: { label: '遗失', type: 'danger' },
}

// 状态筛选下拉框和操作下拉菜单的选项
const allStatusOptions = [
  { label: '在库', value: 1 },
  { label: '全部借出', value: 2 },
  { label: '下架', value: 3 },
  { label: '遗失', value: 4 },
]

// === 响应式状态 ===
const loading = ref(false)       // 表格加载中的 loading 状态
const tableData = ref([])        // 当前页图书数据
const total = ref(0)             // 总记录数（用于分页组件）
const currentPage = ref(1)       // 当前页码
const pageSize = ref(10)         // 每页条数
const flatCategories = ref([])   // 扁平化后的分类列表（含层级前缀，用于下拉框）

// 搜索表单（使用 reactive 绑定多个字段）
const searchForm = reactive({
  keyword: '',       // 关键词搜索（书名/作者/ISBN）
  categoryId: null,  // 分类筛选
  status: null,      // 状态筛选
})

/**
 * 递归扁平化分类树，为子节点添加层级前缀，用于下拉框显示
 * 例如：文学 / 小说
 */
function flattenTree(nodes, prefix = '') {
  const result = []
  for (const node of nodes) {
    const label = prefix + node.name
    result.push({ id: node.id, name: label })
    if (node.children && node.children.length) {
      result.push(...flattenTree(node.children, label + ' / '))
    }
  }
  return result
}

// 加载分类树并扁平化，供分类下拉框使用
async function loadCategories() {
  try {
    const res = await categoryTree()
    flatCategories.value = flattenTree(res.data || [])
  } catch {
    // 已处理
  }
}

/**
 * 核心数据加载：调用分页搜索 API，将结果写入 tableData 和 total
 * 会根据 searchForm 中的条件动态组装请求参数
 */
async function loadBooks() {
  loading.value = true
  try {
    // 构建分页 + 搜索参数
    const params = {
      page: currentPage.value,
      size: pageSize.value,
    }
    if (searchForm.keyword) params.keyword = searchForm.keyword
    if (searchForm.categoryId) params.categoryId = searchForm.categoryId
    if (searchForm.status != null && searchForm.status !== '') params.status = searchForm.status
    const res = await bookSearch(params)
    const page = res.data
    tableData.value = page.records || []
    total.value = page.total || 0
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 搜索：重置到第 1 页并重新加载数据
function handleSearch() {
  currentPage.value = 1
  loadBooks()
}

// 重置搜索条件：清空表单回到第 1 页
function handleReset() {
  searchForm.keyword = ''
  searchForm.categoryId = null
  searchForm.status = null
  currentPage.value = 1
  loadBooks()
}

// 分页页码变化时重新加载
function handlePageChange(page) {
  currentPage.value = page
  loadBooks()
}

// 每页条数变化时重置到第 1 页并重新加载
function handleSizeChange(size) {
  pageSize.value = size
  currentPage.value = 1
  loadBooks()
}

// 跳转到新增图书页面
function goAdd() {
  router.push('/books/add')
}

// 跳转到编辑图书页面（带 id 路由参数）
function goEdit(id) {
  router.push(`/books/${id}/edit`)
}

/**
 * 删除图书：弹出确认框 -> 调用 API -> 成功时处理当前页数据为空则回退一页
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${row.name}」吗？删除后不可恢复。`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await deleteBook(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      // 如果当前页只剩一条数据且不是第一页，则回退一页
      if (tableData.value.length === 1 && currentPage.value > 1) {
        currentPage.value--
      }
      loadBooks()
    }
  } catch (err) {
    if (err !== 'cancel' && err?.message) {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

/**
 * 变更图书状态：弹出确认框 -> 调用 updateBookStatus API
 * 下拉菜单的 command 值即为新状态码
 */
async function handleStatusChange(row, newStatus) {
  try {
    const statusLabel = statusMap[newStatus]?.label || newStatus
    await ElMessageBox.confirm(
      `确定将「${row.name}」状态改为「${statusLabel}」吗？`,
      '状态变更',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await updateBookStatus(row.id, newStatus)
    if (res.code === 200) {
      ElMessage.success('状态更新成功')
      loadBooks()
    }
  } catch (err) {
    if (err !== 'cancel' && err?.message) {
      ElMessage.error(err.message || '操作失败')
    }
  }
}

// Excel 导入 loading 状态
const importLoading = ref(false)

/**
 * 处理 Excel 导入：el-upload 的 onChange 触发
 * 传入 file.raw（原始 File 对象）调用 importBooks API
 */
async function handleImport(file) {
  importLoading.value = true
  try {
    const res = await importBooks(file.raw)
    if (res.code === 200) {
      const result = res.data
      const msg = result
        ? `成功 ${result.successCount || 0} 条，失败 ${result.failCount || 0} 条`
        : '导入完成'
      ElMessage.success(msg)
      loadBooks()
    }
  } catch {
    // 已处理
  } finally {
    importLoading.value = false
  }
}

/**
 * 导出 Excel：根据当前搜索条件导出
 * exportBooks 使用原生 fetch 下载文件，后端直接响应二进制流
 */
function handleExport() {
  exportBooks(
    searchForm.keyword || undefined,
    searchForm.categoryId || undefined
  ).catch(() => {
    // exportBooks uses native fetch, errors surface here
  })
}

// 页面挂载时加载分类列表和图书数据
onMounted(() => {
  loadCategories()
  loadBooks()
})
</script>

<!-- ==================== 模板 ==================== -->
<template>
  <div class="book-list-page">
    <!-- 顶部标题栏：标题 + 新增按钮 -->
    <div class="page-header">
      <h1 class="page-title">图书管理</h1>
      <el-button type="primary" :icon="Plus" @click="goAdd">新增图书</el-button>
    </div>

    <!-- 搜索区域：关键词输入框 + 分类下拉 + 状态下拉 + 搜索/重置按钮 -->
    <div class="search-section">
      <div class="search-row">
        <el-input
          v-model="searchForm.keyword"
          placeholder="书名 / 作者 / ISBN"
          clearable
          :prefix-icon="Search"
          class="search-input"
          @keyup.enter="handleSearch"
        />
        <el-select
          v-model="searchForm.categoryId"
          placeholder="全部分类"
          clearable
          class="search-select"
        >
          <el-option
            v-for="cat in flatCategories"
            :key="cat.id"
            :label="cat.name"
            :value="cat.id"
          />
        </el-select>
        <el-select
          v-model="searchForm.status"
          placeholder="全部状态"
          clearable
          class="search-select-sm"
        >
          <el-option
            v-for="opt in allStatusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
      <!-- 工具栏：Excel 导入 / 导出按钮 -->
      <div class="tool-row">
        <el-upload
          :auto-upload="false"
          :show-file-list="false"
          :on-change="handleImport"
          accept=".xlsx,.xls"
        >
          <el-button :icon="Upload" :loading="importLoading">导入Excel</el-button>
        </el-upload>
        <el-button :icon="Download" @click="handleExport">导出Excel</el-button>
      </div>
    </div>

    <!-- 数据表格区域 -->
    <div class="table-section">
      <el-table :data="tableData" v-loading="loading" stripe border style="width: 100%">
        <el-table-column prop="isbn" label="ISBN" width="160" show-overflow-tooltip />
        <el-table-column prop="name" label="书名" min-width="180" show-overflow-tooltip />
        <el-table-column prop="author" label="作者" width="120" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="130" show-overflow-tooltip />
        <el-table-column prop="stock" label="库存" width="70" align="center" />
        <!-- 状态列：使用 el-tag 显示，颜色类型由 statusMap 决定 -->
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type || 'info'" size="small">
              {{ statusMap[row.status]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <!-- 操作列固定在右侧：编辑、状态变更加下拉菜单、删除 -->
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link :icon="Edit" @click="goEdit(row.id)">编辑</el-button>
            <el-dropdown trigger="click" @command="(status) => handleStatusChange(row, status)">
              <el-button type="warning" link>变更状态<el-icon class="el-icon--right"><ArrowDown /></el-icon></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="opt in allStatusOptions"
                    :key="opt.value"
                    :command="opt.value"
                    :disabled="opt.value === row.status"
                  >
                    {{ opt.label }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
            <el-button type="danger" link :icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态：非加载中且无数据时显示 -->
      <div v-if="!loading && tableData.length === 0" class="empty-state">
        <el-empty description="暂无图书数据" :image-size="120" />
      </div>

      <!-- 分页器：仅在有数据时显示 -->
      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination
          :current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          background
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>
  </div>
</template>

<!-- ==================== 样式（scoped） ==================== -->
<style scoped>
.book-list-page {
  padding: 0;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0;
}

.search-section {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.search-row {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input {
  width: 220px;
}

.search-select {
  width: 180px;
}

.search-select-sm {
  width: 140px;
}

.tool-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.table-section {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 20px;
}

.empty-state {
  padding: 40px 0;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
