<!--
  分类管理页 - CategoryView
  功能：树形分类结构的管理（增删改查）
  - 使用 el-tree 展示分类树，默认全部展开
  - 每个节点悬停显示操作按钮：新增子分类、编辑、删除
  - 支持新增顶级分类
  - 弹窗对话框：分类名称（必填）+ 排序字段
  - 删除前确认：若有子分类则后端会拒绝
-->
<script setup>
import { ref, reactive, onMounted } from 'vue'
// 分类 API：获取树、新增、编辑、删除
import { categoryTree, addCategory, updateCategory, deleteCategory } from '../../api/category'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, FolderAdd } from '@element-plus/icons-vue'

// === 响应式状态 ===
const treeData = ref([])         // 分类树数据
const loading = ref(false)       // 加载树的 loading
const dialogVisible = ref(false) // 新增/编辑弹窗可见性
const dialogTitle = ref('')      // 弹窗标题
const saving = ref(false)        // 弹窗保存 loading
const currentNode = ref(null)    // 当前操作的节点（用于编辑/新增子分类时确定父节点）

// 分类表单数据
const form = reactive({
  id: null,         // 编辑时才有 id
  name: '',
  parentId: 0,      // 0 表示顶级分类
  sort: 0,
})

// 加载分类树
async function loadTree() {
  loading.value = true
  try {
    const res = await categoryTree()
    treeData.value = res.data || []
  } catch { /* */ }
  finally { loading.value = false }
}

/**
 * 新增顶级分类：打开弹窗，parentId=0
 */
function handleAddRoot() {
  dialogTitle.value = '新增顶级分类'
  form.id = null
  form.name = ''
  form.parentId = 0
  form.sort = 0
  currentNode.value = null
  dialogVisible.value = true
}

/**
 * 在某分类下新增子分类：打开弹窗，parentId=该分类的 id
 */
function handleAddChild(data) {
  dialogTitle.value = `在「${data.name}」下新增子分类`
  form.id = null
  form.name = ''
  form.parentId = data.id
  form.sort = 0
  currentNode.value = data
  dialogVisible.value = true
}

/**
 * 编辑分类：打开弹窗，回填表单数据
 */
function handleEdit(data) {
  dialogTitle.value = `编辑分类「${data.name}」`
  form.id = data.id
  form.name = data.name
  form.parentId = data.parentId
  form.sort = data.sort ?? 0
  currentNode.value = data
  dialogVisible.value = true
}

/**
 * 删除分类：弹出确认框 -> 调用 deleteCategory API
 */
async function handleDelete(data) {
  try {
    await ElMessageBox.confirm(
      `确定删除「${data.name}」吗？如果存在子分类将无法删除。`,
      '删除确认',
      { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
    )
    const res = await deleteCategory(data.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadTree()
    }
  } catch (err) {
    if (err !== 'cancel' && err?.message) {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

/**
 * 弹窗提交：验证名称 -> 根据是否有 id 决定新增/编辑 -> 成功后关闭弹窗并刷新树
 */
async function handleSubmit() {
  if (!form.name.trim()) {
    ElMessage.warning('请输入分类名称')
    return
  }
  saving.value = true
  try {
    const data = {
      name: form.name.trim(),
      parentId: form.parentId,
      sort: form.sort,
    }
    let res
    if (form.id) {
      // 编辑模式：带上 id
      data.id = form.id
      res = await updateCategory(data)
    } else {
      // 新增模式
      res = await addCategory(data)
    }
    if (res.code === 200) {
      ElMessage.success(form.id ? '编辑成功' : '新增成功')
      dialogVisible.value = false
      loadTree()
    }
  } catch { /* */ }
  finally { saving.value = false }
}

// 页面挂载时加载分类树
onMounted(loadTree)
</script>

<!-- ==================== 模板 ==================== -->
<template>
  <div class="category-page">
    <!-- 顶部标题栏 -->
    <div class="page-header">
      <h1 class="page-title">分类管理</h1>
      <el-button type="primary" :icon="FolderAdd" @click="handleAddRoot">新增顶级分类</el-button>
    </div>

    <!-- 分类树卡片 -->
    <div class="tree-card" v-loading="loading">
      <!-- el-tree 默认全部展开，node-key 为 id，label 为 name -->
      <el-tree
        v-if="treeData.length"
        :data="treeData"
        node-key="id"
        default-expand-all
        :expand-on-click-node="false"
        :props="{ label: 'name', children: 'children' }"
      >
        <!-- 自定义树节点：名称 + 操作按钮（悬停显示） -->
        <template #default="{ data }">
          <div class="tree-node">
            <span class="tree-node-name">{{ data.name }}</span>
            <span class="tree-node-actions">
              <el-button link type="primary" size="small" :icon="Plus" @click.stop="handleAddChild(data)">子分类</el-button>
              <el-button link type="warning" size="small" :icon="Edit" @click.stop="handleEdit(data)">编辑</el-button>
              <el-button link type="danger" size="small" :icon="Delete" @click.stop="handleDelete(data)">删除</el-button>
            </span>
          </div>
        </template>
      </el-tree>
      <!-- 空状态 -->
      <el-empty v-else description="暂无分类数据，请添加" :image-size="80" />
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="420px" :close-on-click-modal="false">
      <el-form label-width="80px" @submit.prevent="handleSubmit">
        <el-form-item label="分类名称" required>
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :step="1" controls-position="right" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<!-- ==================== 样式（scoped） ==================== -->
<style scoped>
.category-page {
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

.tree-card {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 20px;
  min-height: 200px;
}

.tree-node {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 1;
  padding-right: 12px;
  line-height: 32px;
}

.tree-node-name {
  font-size: 14px;
  font-weight: 500;
}

/* 操作按钮默认隐藏，悬停时显示 */
.tree-node-actions {
  display: flex;
  gap: 4px;
  visibility: hidden;
}

.tree-node:hover .tree-node-actions {
  visibility: visible;
}
</style>
