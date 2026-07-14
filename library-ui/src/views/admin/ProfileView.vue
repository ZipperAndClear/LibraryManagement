<!--
  个人中心页 - ProfileView
  功能：当前登录用户的信息展示与密码修改
  - 左栏：头像（支持上传替换）+ 用户信息只读展示（用户名、姓名、邮箱、手机、角色）
  - 右栏：修改密码表单（原密码 + 新密码 + 确认密码）
  - 头像上传：调用 uploadFile API，成功后同步更新 authStore 和 localStorage
  - 密码修改：调用 changePassword API
-->
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { changePassword, updateUser } from '../../api/user'
import { uploadFile } from '../../api/file'
import { getCurrentUser } from '../../api/auth'
import { ElMessage } from 'element-plus'
import { Camera } from '@element-plus/icons-vue'

// 获取认证 store 实例
const authStore = useAuthStore()
// 当前登录用户（computed 保证响应式）
const user = computed(() => authStore.user || {})
const formRef = ref(null)
const submitting = ref(false)     // 密码修改提交 loading
const uploadLoading = ref(false)  // 头像上传 loading

// API 基础 URL，用于拼接相对路径为完整 URL
const apiBase = import.meta.env.VITE_API_BASE_URL || ''

/**
 * 头像地址计算属性：
 * - 如果没有头像，返回空字符串（el-avatar 将显示文字首字母）
 * - 如果是完整 URL（http/https 开头），直接使用
 * - 否则拼接 API 基础路径
 */
const avatarSrc = computed(() => {
  const avatar = user.value.avatar
  if (!avatar) return ''
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) return avatar
  return apiBase + avatar
})

async function loadUserInfo() {
  try {
    const res = await getCurrentUser()
    if (res.code === 200 && res.data) {
      const u = res.data
      authStore.user = {
        ...authStore.user,
        userId: u.id,
        username: u.username,
        realName: u.realName,
        avatar: u.avatar,
        email: u.email,
        phone: u.phone,
        status: u.status,
        roles: u.roleCodes || u.roles,
      }
      const raw = localStorage.getItem('LibraryManagementAuth')
      if (raw) {
        const auth = JSON.parse(raw)
        auth.user = authStore.user
        localStorage.setItem('LibraryManagementAuth', JSON.stringify(auth))
      }
    }
  } catch { /* */ }
}

// 修改密码表单
const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

// 密码修改验证规则
const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    // 自定义校验：必须与新密码一致
    { validator: (_, v, cb) => v === form.newPassword ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' },
  ],
}

/**
 * 提交密码修改：表单验证 -> changePassword API -> 清空表单
 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (valid === false) return

  submitting.value = true
  try {
    const res = await changePassword(user.value.userId, form.oldPassword, form.newPassword)
    if (res.code === 200) {
      ElMessage.success('密码修改成功')
      form.oldPassword = ''
      form.newPassword = ''
      form.confirmPassword = ''
      formRef.value.resetFields()
    }
  } catch { /* */ }
  finally { submitting.value = false }
}

/**
 * 头像上传处理：el-upload 的 onChange 触发
 * 1. 上传文件到服务器
 * 2. 调用 updateUser 将新头像 URL 保存到用户信息
 * 3. 同步更新 authStore.user.avatar 和 localStorage 持久化数据
 */
async function handleAvatarUpload(file) {
  uploadLoading.value = true
  try {
    // 如果有旧头像，移除 API 前缀后传给 uploadFile 以替换旧文件
    const oldUrl = user.value.avatar
      ? user.value.avatar.replace(apiBase, '')
      : undefined
    const res = await uploadFile(file.raw, oldUrl, 'avatars')
    if (res.code === 200 && res.data) {
      // 更新用户信息中的头像字段
      const updateRes = await updateUser({ id: user.value.userId, avatar: res.data.url })
      if (updateRes.code === 200) {
        // 更新 store 中的头像
        authStore.user.avatar = apiBase + res.data.url
        // 同步更新 localStorage 中的持久化 auth 数据
        const raw = localStorage.getItem('LibraryManagementAuth')
        if (raw) {
          const auth = JSON.parse(raw)
          auth.user = authStore.user
          localStorage.setItem('LibraryManagementAuth', JSON.stringify(auth))
        }
        ElMessage.success('头像更新成功')
      }
    }
  } catch { /* */ }
  finally { uploadLoading.value = false }
}

onMounted(loadUserInfo)
</script>

<!-- ==================== 模板 ==================== -->
<template>
  <div class="profile-page">
    <h1 class="page-title">个人中心</h1>

    <el-row :gutter="20">
      <!-- 左栏：用户信息卡片 -->
      <el-col :span="10">
        <div class="profile-card">
          <!-- 头像区域：Avatar + 悬浮上传按钮 -->
          <div class="profile-avatar">
            <div class="avatar-wrap">
              <!-- el-avatar：有图片则显示图片，无图片则显示用户首字母 -->
              <el-avatar :size="80" :src="avatarSrc" :style="{ background: avatarSrc ? undefined : '#007DFF' }">
                {{ user.realName?.charAt(0) || user.username?.charAt(0) }}
              </el-avatar>
              <!-- 上传组件：绝对定位在头像右下角，点击触发 handleAvatarUpload -->
              <el-upload
                :auto-upload="false"
                :show-file-list="false"
                @change="handleAvatarUpload"
                accept="image/*"
                class="avatar-upload"
              >
                <div class="upload-overlay" :class="{ loading: uploadLoading }">
                  <el-icon :size="18"><Camera /></el-icon>
                </div>
              </el-upload>
            </div>
          </div>
          <!-- 用户信息列表 -->
          <div class="profile-info">
            <div class="profile-row"><span class="profile-label">用户名</span><span>{{ user.username }}</span></div>
            <div class="profile-row"><span class="profile-label">姓名</span><span>{{ user.realName }}</span></div>
            <div class="profile-row"><span class="profile-label">邮箱</span><span>{{ user.email || '未设置' }}</span></div>
            <div class="profile-row"><span class="profile-label">手机</span><span>{{ user.phone || '未设置' }}</span></div>
            <!-- 角色展示：将角色代码映射为中文名后用 el-tag 展示 -->
            <div class="profile-row"><span class="profile-label">角色</span>
              <span>
                <el-tag v-for="r in user.roles" :key="r" size="small" style="margin-right:4px">{{ r === 'admin' ? '超级管理员' : r === 'librarian' ? '图书管理员' : r === 'student' ? '学生' : r }}</el-tag>
              </span>
            </div>
          </div>
        </div>
      </el-col>

      <!-- 右栏：修改密码表单 -->
      <el-col :span="14">
        <div class="profile-card">
          <h3 style="margin:0 0 16px;font-size:16px">修改密码</h3>
          <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" size="default">
            <el-form-item label="原密码" prop="oldPassword">
              <el-input v-model="form.oldPassword" type="password" placeholder="请输入原密码" show-password maxlength="100" />
            </el-form-item>
            <el-form-item label="新密码" prop="newPassword">
              <el-input v-model="form.newPassword" type="password" placeholder="请输入新密码（至少6位）" show-password maxlength="100" />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" show-password maxlength="100" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleSubmit">确认修改</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<!-- ==================== 样式（scoped） ==================== -->
<style scoped>
.profile-page { max-width: 900px }
.page-title { font-size:20px; font-weight:700; margin:0 0 16px }
.profile-card { background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:28px 30px; margin-bottom:16px }
.profile-avatar { display:flex; justify-content:center; margin-bottom:20px }
.avatar-wrap { position:relative; display:inline-block }
/* 上传按钮定位在头像右下角 */
.avatar-upload { position:absolute; right:0; bottom:0 }
.upload-overlay { width:30px; height:30px; border-radius:50%; background:rgba(0,0,0,0.45); color:#fff; display:flex; align-items:center; justify-content:center; cursor:pointer; transition:background 0.2s }
.upload-overlay:hover { background:rgba(0,0,0,0.65) }
.upload-overlay.loading { opacity:0.6; pointer-events:none }
.profile-info { display:flex; flex-direction:column; gap:14px }
.profile-row { display:flex; align-items:center; font-size:14px }
.profile-label { width:60px; color:var(--muted); flex-shrink:0 }
</style>
