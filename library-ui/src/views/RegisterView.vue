<!--
  RegisterView.vue - 用户注册页面
  全屏居中布局，包含背景图、Logo 头像、注册表单（用户名、密码、确认密码、真实姓名、图形验证码）。
  页面挂载时自动加载验证码图片，用户可点击刷新。注册成功后跳转登录页。
-->
<script setup>
// ============================================================
//  Imports & Dependencies
// ============================================================
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { register, getCaptcha } from '../api/auth'   // 注册接口 & 获取验证码接口
import { ElMessage } from 'element-plus'              // Element Plus 消息提示
import bgUrl from '@/assets/images/background.png?url'
import adminAvatar from '@/assets/images/avatars/Admin.png?url'
import { Refresh } from '@element-plus/icons-vue'     // 刷新验证码按钮图标

const router = useRouter()

// ============================================================
//  Reactive State
// ============================================================

// 注册表单数据：5 个字段，全部用 v-model 绑定
const form = reactive({
  username: '',        // 用户名/学号
  password: '',        // 密码
  confirmPassword: '', // 确认密码（用于前端比对）
  realName: '',        // 真实姓名
  captcha: '',         // 用户输入的验证码
})

const captchaKey = ref('')       // 验证码 key，提交注册时需一并发送给后端
const captchaImage = ref('')     // 验证码图片 Base64 URL
const captchaLoading = ref(false) // 验证码加载中的 loading 态
const loading = ref(false)       // 注册提交按钮的 loading 态
const errorMsg = ref('')         // 错误提示信息

// ============================================================
//  API / Data Loading
// ============================================================

/**
 * 加载图形验证码
 * 调用后端 /api/auth/captcha 接口获取 captchaKey 和 base64 图片
 * 在页面挂载时自动调用，也可由用户点击刷新触发
 */
async function loadCaptcha() {
  captchaLoading.value = true
  try {
    const res = await getCaptcha()
    // code === 200 表示成功获取验证码
    if (res.code === 200) {
      captchaKey.value = res.data.captchaKey     // 保存 key 用于提交验证
      captchaImage.value = res.data.captchaImage  // base64 图片数据
    }
  } catch {
    errorMsg.value = '验证码加载失败'
  } finally {
    captchaLoading.value = false
  }
}

// ============================================================
//  Event Handlers
// ============================================================

/**
 * 注册表单提交处理函数
 * 1. 前端逐字段校验（非空、密码长度、两次密码一致性）
 * 2. 调用后端 register 接口
 * 3. 成功：弹出成功提示并跳转登录页
 * 4. 失败：重新加载验证码并清空验证码输入（后端错误由 http 拦截器统一提示）
 */
async function handleSubmit() {
  errorMsg.value = ''

  // 前端表单逐字段校验，任一项不通过则终止
  if (!form.username.trim()) { errorMsg.value = '请输入用户名'; return }
  if (!form.password) { errorMsg.value = '请输入密码'; return }
  if (form.password.length < 6) { errorMsg.value = '密码长度不能少于6位'; return }
  if (form.password !== form.confirmPassword) { errorMsg.value = '两次密码输入不一致'; return }
  if (!form.realName.trim()) { errorMsg.value = '请输入真实姓名'; return }
  if (!form.captcha.trim()) { errorMsg.value = '请输入验证码'; return }

  loading.value = true
  try {
    const res = await register({
      username: form.username.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
      realName: form.realName.trim(),
      captcha: form.captcha.trim(),
      captchaKey: captchaKey.value,   // 一并发送验证码 key
    })
    // code === 200 表示注册成功
    if (res.code === 200) {
      ElMessage.success('注册成功，请登录')
      router.push('/login')
    }
  } catch {
    // 注册失败时自动刷新验证码并清空用户已输入的验证码
    loadCaptcha()
    form.captcha = ''
  } finally {
    loading.value = false
  }
}

// ============================================================
//  Lifecycle Hooks
// ============================================================

// 页面挂载完成后自动加载验证码
onMounted(loadCaptcha)
</script>

<!-- ============================================================
     Template - 注册页面 UI
     登录页同款布局，额外包含确认密码、真实姓名、验证码行
============================================================ -->
<template>
  <!-- 全屏背景容器 -->
  <div class="register-wrapper" :style="{ backgroundImage: `url(${bgUrl})` }">
    <!-- 居中毛玻璃卡片 -->
    <div class="register-card">
      <!-- 卡片头部 -->
      <div class="register-header">
        <div class="logo-avatar">
          <img :src="adminAvatar" alt="logo" />
        </div>
        <h1>用户注册</h1>
        <p class="subtitle">Library Registration</p>
      </div>

      <!-- 注册表单 -->
      <form class="register-form" @submit.prevent="handleSubmit">
        <!-- 用户名 -->
        <div class="input-group">
          <span class="input-icon">
            <svg viewBox="0 0 16 16" fill="none" width="16" height="16">
              <circle cx="8" cy="5" r="3" stroke="currentColor" stroke-width="1.5"/>
              <path d="M3 14c0-2.76 2.24-5 5-5s5 2.24 5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </span>
          <input v-model="form.username" type="text" placeholder="请输入用户名/学号" autocomplete="username" />
        </div>

        <!-- 密码 -->
        <div class="input-group">
          <span class="input-icon">
            <svg viewBox="0 0 16 16" fill="none" width="16" height="16">
              <rect x="3" y="7" width="10" height="6" rx="1.5" stroke="currentColor" stroke-width="1.5"/>
              <path d="M5 7V5a3 3 0 016 0v2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </span>
          <input v-model="form.password" type="password" placeholder="请输入密码（至少6位）" autocomplete="new-password" />
        </div>

        <!-- 确认密码 -->
        <div class="input-group">
          <span class="input-icon">
            <svg viewBox="0 0 16 16" fill="none" width="16" height="16">
              <rect x="3" y="7" width="10" height="6" rx="1.5" stroke="currentColor" stroke-width="1.5"/>
              <path d="M5 7V5a3 3 0 016 0v2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </span>
          <input v-model="form.confirmPassword" type="password" placeholder="请确认密码" autocomplete="new-password" />
        </div>

        <!-- 真实姓名 -->
        <div class="input-group">
          <span class="input-icon">
            <svg viewBox="0 0 16 16" fill="none" width="16" height="16">
              <path d="M2 14s0-4 6-4 6 4 6 4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
              <circle cx="8" cy="5" r="3" stroke="currentColor" stroke-width="1.5"/>
            </svg>
          </span>
          <input v-model="form.realName" type="text" placeholder="请输入真实姓名" autocomplete="name" />
        </div>

        <!-- 验证码行：输入框 + 图片 + 刷新按钮 -->
        <div class="input-group captcha-row">
          <span class="input-icon">
            <svg viewBox="0 0 16 16" fill="none" width="16" height="16">
              <rect x="2" y="4" width="12" height="8" rx="1.5" stroke="currentColor" stroke-width="1.5"/>
              <path d="M6 10l2-3 1.5 1.5L12 6" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </span>
          <!-- 验证码输入框，最多 6 位，关闭自动补全防止干扰 -->
          <input v-model="form.captcha" type="text" placeholder="请输入验证码" maxlength="6" autocomplete="off" />
          <!-- 验证码图片，点击可刷新；loading 类名控制加载态样式 -->
          <div class="captcha-img" :class="{ loading: captchaLoading }" @click="loadCaptcha">
            <!-- 图片加载成功时展示 base64 验证码图 -->
            <img v-if="captchaImage" :src="captchaImage" alt="验证码" />
            <!-- 图片未加载时显示加载中文字 -->
            <span v-else class="captcha-loading-text">加载中...</span>
          </div>
          <!-- 刷新验证码按钮 -->
          <button type="button" class="captcha-refresh" title="刷新验证码" :disabled="captchaLoading" @click="loadCaptcha">
            <el-icon :size="15"><Refresh /></el-icon>
          </button>
        </div>

        <!-- 错误提示 -->
        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

        <!-- 提交按钮 -->
        <button type="submit" :disabled="loading" class="register-btn">
          <span v-if="loading" class="spinner"></span>
          {{ loading ? '注册中...' : '注 册' }}
        </button>

        <!-- 跳转登录页 -->
        <p class="login-link">
          已有账号？<router-link to="/login">去登录</router-link>
        </p>
      </form>
    </div>
  </div>
</template>

<!-- ============================================================
     Style - 注册页样式（scoped 作用域隔离）
     与登录页共享大部分输入框、按钮样式，额外包含验证码行样式
============================================================ -->
<style scoped>
/* ------ 全屏背景容器 ------ */
.register-wrapper {
  position: fixed;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  overflow: auto;          /* 注册表单内容可能更长，允许滚动 */
  z-index: 1;
}

/* ------ 毛玻璃卡片 ------ */
.register-card {
  position: relative;
  width: 420px;
  max-width: 94vw;
  background: rgba(255,255,255,0.92);
  backdrop-filter: blur(16px);
  border-radius: 16px;
  padding: 36px 36px 28px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15);
  margin: 24px 0;          /* 上下留白，避免贴边 */
}

/* ------ 卡片头部 ------ */
.register-header {
  text-align: center;
  margin-bottom: 24px;
}

.logo-avatar {
  width: 56px;
  height: 56px;
  margin: 0 auto 12px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid #e2e8f0;
}

.logo-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.register-header h1 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 1px;
}

.subtitle {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
  letter-spacing: 1.5px;
  text-transform: uppercase;
}

/* ------ 表单区域 ------ */
.register-form {
  display: flex;
  flex-direction: column;
  gap: 14px;              /* 比登录页稍紧凑 */
}

/* 输入框组 */
.input-group {
  position: relative;
  display: flex;
  align-items: center;
  background: #f8fafc;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-group:focus-within {
  border-color: #2563eb;
  box-shadow: 0 0 0 3px rgba(37,99,235,0.1);
  background: #fff;
}

.input-icon {
  display: flex;
  align-items: center;
  padding-left: 14px;
  color: #94a3b8;
  flex-shrink: 0;
}

.input-group:focus-within .input-icon { color: #2563eb; }

.input-group input {
  width: 100%;
  padding: 12px 14px 12px 10px;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #334155;
  outline: none;
  border-radius: 10px;
}

.input-group input::placeholder { color: #cbd5e1; }

/* ------ 验证码行特殊样式 ------ */
/* 验证码行的输入框宽度固定 120px，不随容器伸缩 */
.captcha-row input {
  width: 120px;
  flex: 0 0 auto;
}

/* 验证码图片区域 */
.captcha-img {
  width: 82px;
  height: 36px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;        /* 点击可刷新 */
  background: #e2e8f0;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

/* 加载中遮罩 */
.captcha-img.loading { opacity: 0.6; pointer-events: none; }
.captcha-loading-text { font-size: 10px; color: #94a3b8; }

/* 刷新按钮 */
.captcha-refresh {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 30px;
  height: 30px;
  margin: 0 8px;
  padding: 0;
  border: none;
  background: #f1f5f9;
  border-radius: 6px;
  color: #64748b;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s;
}

.captcha-refresh:hover { background: #e2e8f0; }
.captcha-refresh:disabled { opacity: 0.5; cursor: not-allowed; }

/* ------ 错误提示 ------ */
.error-msg {
  margin: -2px 0 0;
  padding: 10px 14px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 13px;
}

/* ------ 注册按钮 ------ */
.register-btn {
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 12px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  letter-spacing: 2px;
  transition: transform 0.15s, box-shadow 0.2s;
}

.register-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(37,99,235,0.35);
}

.register-btn:active { transform: translateY(0); }

.register-btn:disabled {
  background: #93c5fd;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

/* 跳转登录链接 */
.login-link {
  text-align: center;
  font-size: 13px;
  color: var(--muted);
  margin: 0;
}

.login-link a {
  color: #2563eb;
  font-weight: 500;
}

/* 旋转动画元素 */
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }
</style>
