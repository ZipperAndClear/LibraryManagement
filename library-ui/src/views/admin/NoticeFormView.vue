<!--
  公告表单页 - NoticeFormView（新增 / 编辑公告）
  功能：
  - 新增模式：空白表单，提交调用 addNotice API
  - 编辑模式：通过路由 state 回填标题、内容、置顶状态，提交调用 updateNotice API
  - 表单字段：标题、内容（多行文本）、置顶开关
  - 表单验证：标题和内容为必填项
-->
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// 公告 API：新增、编辑
import { addNotice, updateNotice } from '../../api/notice'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

// 是否为编辑模式
const isEdit = computed(() => !!route.params.id)
// 动态页面标题
const title = computed(() => isEdit.value ? '编辑公告' : '新增公告')

const submitting = ref(false)   // 提交 loading
const formRef = ref(null)       // 表单引用

// 公告表单数据
const form = reactive({
  title: '',
  content: '',
  isTop: 0,   // 是否置顶：1=置顶 / 0=不置顶
})

// 表单验证规则
const rules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
}

/**
 * 编辑模式下从路由 state 回填表单数据
 * state 由列表页 goEdit 时传入
 */
function initEditData() {
  if (!isEdit.value) return
  const state = window.history.state
  if (state) {
    form.title = state.title || ''
    form.content = state.content || ''
    form.isTop = state.isTop || 0
  }
}

/**
 * 表单提交：验证 -> 根据模式调用新增/编辑 API -> 成功后跳转列表
 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (valid === false) return

  submitting.value = true
  try {
    const data = { title: form.title, content: form.content, isTop: form.isTop }
    let res
    if (isEdit.value) {
      data.id = Number(route.params.id)
      res = await updateNotice(data)
    } else {
      res = await addNotice(data)
    }
    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '保存成功' : '新增成功')
      router.push('/notices')
    }
  } catch { /* */ }
  finally { submitting.value = false }
}

// 返回公告列表頁
function goBack() { router.push('/notices') }

onMounted(initEditData)
</script>

<!-- ==================== 模板 ==================== -->
<template>
  <div class="form-page">
    <!-- 顶部标题栏 -->
    <div class="page-header">
      <h1 class="page-title">{{ title }}</h1>
      <el-button @click="goBack">返回列表</el-button>
    </div>

    <!-- 表单卡片 -->
    <div class="form-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="default">
        <!-- 公告标题 -->
        <el-form-item label="公告标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="200" />
        </el-form-item>

        <!-- 公告内容：多行文本域 -->
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="请输入公告正文内容" maxlength="10000" show-word-limit />
        </el-form-item>

        <!-- 置顶显示开关：el-switch 绑定 1/0 -->
        <el-form-item label="置顶显示">
          <el-switch v-model="form.isTop" :active-value="1" :inactive-value="0" />
          <span style="margin-left:10px;font-size:13px;color:var(--muted)">开启后此公告将排在列表最前面</span>
        </el-form-item>

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
.form-page { max-width: 860px }
.page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px }
.page-title { font-size:20px; font-weight:700; margin:0 }
.form-card { background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:28px 32px }
</style>
