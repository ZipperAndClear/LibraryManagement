<!--
  图书表单页 - BookFormView（新增 / 编辑图书）
  功能：
  - 新增模式：空白表单，提交调用 addBook API
  - 编辑模式：通过路由参数 id 加载 bookDetail，回填表单后提交调用 updateBook API
  - 表单字段：ISBN、书名、作者、出版社、价格、库存、分类下拉、封面图片（URL 输入 + 上传）、内容简介
  - 表单验证：ISBN 和书名为必填，库存>=0，价格>=0
-->
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// 图书 API：详情查询、新增、编辑
import { bookDetail, addBook, updateBook } from '../../api/book'
// 分类 API：获取分类树
import { categoryTree } from '../../api/category'
// 文件上传 API
import { uploadFile, deleteFile } from '../../api/file'
import { ElMessage } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'

// 路由相关：获取当前路由参数判断新增/编辑模式
const route = useRoute()
const router = useRouter()

// 是否为编辑模式（路由有 id 参数则为编辑）
const isEdit = computed(() => !!route.params.id)
// 动态标题
const title = computed(() => isEdit.value ? '编辑图书' : '新增图书')

// === 响应式状态 ===
const loading = ref(false)        // 编辑时加载图书详情的 loading
const submitting = ref(false)     // 提交表单的 loading
const apiBase = import.meta.env.VITE_API_BASE_URL || ''

const coverPreviewUrl = computed(() => {
  if (!form.coverUrl) return ''
  if (form.coverUrl.startsWith('http://') || form.coverUrl.startsWith('https://')) return form.coverUrl
  return apiBase + form.coverUrl
})

const flatCategories = ref([])    // 扁平化分类列表
const formRef = ref(null)         // el-form 的引用，用于表单验证

// 图书表单数据（使用 reactive 以便双向绑定）
const form = reactive({
  isbn: '',
  name: '',
  author: '',
  publisher: '',
  price: undefined,     // 使用 undefined 让 el-input-number 默认空白
  stock: undefined,
  categoryId: null,
  coverUrl: '',
  introduction: '',
})

// 表单验证规则
const rules = {
  isbn: [{ required: true, message: '请输入ISBN', trigger: 'blur' }],
  name: [{ required: true, message: '请输入图书名称', trigger: 'blur' }],
  stock: [
    { required: true, message: '请输入库存数量', trigger: 'blur' },
    { type: 'number', min: 0, message: '库存不能为负数', trigger: 'blur' },
  ],
  price: [
    { type: 'number', min: 0, message: '价格不能为负数', trigger: 'blur' },
  ],
}

/**
 * 递归扁平化分类树，将层级结构的分类转为带前缀的一维数组
 * 用于 el-select 下拉框展示
 */
function flattenTree(nodes, prefix = '') {
  const result = []
  for (const node of nodes) {
    result.push({ id: node.id, name: prefix + node.name })
    if (node.children?.length) {
      result.push(...flattenTree(node.children, prefix + node.name + ' / '))
    }
  }
  return result
}

// 加载分类树数据
async function loadCategories() {
  try {
    const res = await categoryTree()
    flatCategories.value = flattenTree(res.data || [])
  } catch { /* */ }
}

/**
 * 编辑模式下加载图书详情，回填到 form 中
 * 加载失败则提示并返回上一页
 */
async function loadBook() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res = await bookDetail(route.params.id)
    const book = res.data
    form.isbn = book.isbn || ''
    form.name = book.name || ''
    form.author = book.author || ''
    form.publisher = book.publisher || ''
    form.price = book.price != null ? Number(book.price) : undefined
    form.stock = book.stock != null ? Number(book.stock) : undefined
    form.categoryId = book.categoryId || null
    form.coverUrl = book.coverUrl || ''
    form.introduction = book.introduction || ''
  } catch {
    ElMessage.error('图书信息加载失败')
    router.back()
  } finally {
    loading.value = false
  }
}

// 封面上传 loading 状态
const uploadLoading = ref(false)

/**
 * 封面上传处理：el-upload 的 onChange 触发
 * 上传 file.raw 到服务器，成功后将返回的 URL 写入 form.coverUrl
 */
async function handleCoverUpload(file) {
  uploadLoading.value = true
  try {
    const oldUrl = form.coverUrl ? form.coverUrl.replace(apiBase, '') : undefined
    const res = await uploadFile(file.raw, oldUrl, 'covers')
    if (res.code === 200) {
      form.coverUrl = res.data.url
      ElMessage.success('封面上传成功')
    }
  } catch { /* */ }
  finally { uploadLoading.value = false }
}

async function handleRemoveCover() {
  if (form.coverUrl) {
    const oldUrl = form.coverUrl.replace(apiBase, '')
    form.coverUrl = ''
    try { await deleteFile(oldUrl) } catch { /* */ }
  }
}

/**
 * 表单提交：先验证 -> 根据模式调用新增/编辑 API -> 成功后跳转列表
 */
async function handleSubmit() {
  // 表单验证，验证失败返回 false
  const valid = await formRef.value.validate().catch(() => false)
  if (valid === false) return

  submitting.value = true
  try {
    const data = {
      isbn: form.isbn,
      name: form.name,
      author: form.author,
      publisher: form.publisher,
      price: form.price,
      stock: form.stock,
      categoryId: form.categoryId,
      coverUrl: form.coverUrl,
      introduction: form.introduction,
    }
    let res
    if (isEdit.value) {
      data.id = Number(route.params.id)
      res = await updateBook(data)
    } else {
      res = await addBook(data)
    }
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '保存成功' : '新增成功')
      router.push('/books')
    }
  } catch { /* */ }
  finally { submitting.value = false }
}

// 返回图书列表页
function goBack() {
  router.push('/books')
}

// 页面挂载时加载分类和（如果是编辑模式）加载图书详情
onMounted(() => {
  loadCategories()
  loadBook()
})
</script>

<!-- ==================== 模板 ==================== -->
<template>
  <div class="form-page" v-loading="loading">
    <!-- 顶部标题栏 -->
    <div class="page-header">
      <h1 class="page-title">{{ title }}</h1>
      <el-button @click="goBack">返回列表</el-button>
    </div>

    <!-- 表单卡片 -->
    <div class="form-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" label-position="right" size="default">
        <el-row :gutter="24">
          <!-- ISBN -->
          <el-col :span="12">
            <el-form-item label="ISBN" prop="isbn">
              <el-input v-model="form.isbn" placeholder="请输入ISBN" maxlength="50" />
            </el-form-item>
          </el-col>
          <!-- 图书名称 -->
          <el-col :span="12">
            <el-form-item label="图书名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入图书名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <!-- 作者 -->
          <el-col :span="12">
            <el-form-item label="作者">
              <el-input v-model="form.author" placeholder="请输入作者" maxlength="50" />
            </el-form-item>
          </el-col>
          <!-- 出版社 -->
          <el-col :span="12">
            <el-form-item label="出版社">
              <el-input v-model="form.publisher" placeholder="请输入出版社" maxlength="100" />
            </el-form-item>
          </el-col>
          <!-- 价格：数字输入框 -->
          <el-col :span="12">
            <el-form-item label="价格">
              <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" placeholder="0.00" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <!-- 库存：数字输入框 -->
          <el-col :span="12">
            <el-form-item label="库存" prop="stock">
              <el-input-number v-model="form.stock" :min="0" :step="1" placeholder="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <!-- 分类下拉框 -->
          <el-col :span="12">
            <el-form-item label="分类">
              <el-select v-model="form.categoryId" placeholder="请选择分类" clearable style="width: 100%">
                <el-option v-for="cat in flatCategories" :key="cat.id" :label="cat.name" :value="cat.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <!-- 封面图片：手动输入 URL 或上传 -->
          <el-col :span="12">
            <el-form-item label="封面图片">
              <div class="cover-row">
                <el-input v-model="form.coverUrl" placeholder="图片URL 或上传" clearable style="flex:1" />
                <el-upload
                  :auto-upload="false"
                  :show-file-list="false"
                  @change="handleCoverUpload"
                  accept="image/*"
                >
                  <el-button :loading="uploadLoading" :icon="Plus">上传</el-button>
                </el-upload>
                <el-button v-if="form.coverUrl" :icon="Delete" @click="handleRemoveCover" />
              </div>
            </el-form-item>
          </el-col>
          <!-- 封面预览：仅在有 URL 时显示 -->
          <el-col v-if="form.coverUrl" :span="24">
            <el-form-item label="预览">
              <img :src="coverPreviewUrl" class="cover-preview" alt="封面预览" />
            </el-form-item>
          </el-col>
          <!-- 内容简介：多行文本 -->
          <el-col :span="24">
            <el-form-item label="内容简介">
              <el-input v-model="form.introduction" type="textarea" :rows="4" placeholder="请输入图书内容简介" maxlength="2000" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 操作按钮 -->
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="handleSubmit">
            {{ isEdit ? '保存修改' : '确认新增' }}
          </el-button>
          <el-button @click="goBack">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<!-- ==================== 样式（scoped） ==================== -->
<style scoped>
.form-page {
  max-width: 960px;
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

.form-card {
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 10px;
  padding: 28px 32px;
}

.cover-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.cover-preview {
  width: 100px;
  height: 140px;
  object-fit: cover;
  border-radius: 6px;
  border: 1px solid var(--line);
}
</style>
