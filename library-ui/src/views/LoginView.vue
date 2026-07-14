<!--
  LoginView.vue - 登录页面
  全屏居中布局，包含背景图、Logo 头像、用户名密码表单。
  登录成功后调用 authStore.login() 保存 token 和用户信息，然后跳转到首页。
  登录失败展示后端返回的错误信息。
-->
<script setup>
// ============================================================
//  Imports & Dependencies
// ============================================================
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { post } from '../api/http'                // axios 封装，发起登录 POST 请求
import { useAuthStore } from '../stores/auth'       // 认证状态管理
import bgUrl from '@/assets/images/background.png?url' // 背景图片（Vite 静态资源导入）
import adminAvatar from '@/assets/images/avatars/Admin.png?url' // Logo 头像

// ============================================================
//  Reactive State
// ============================================================

// 登录表单数据：用户名与密码 — 用 reactive 包裹以支持 v-model 双向绑定
const form = reactive({
  username: '', // 用户输入的学号/工号
  password: '', // 用户输入的密码
})

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)   // 登录按钮 loading 状态，防止重复提交
const errorMsg = ref('')     // 错误提示文字，非空时在表单下方红色展示

// ============================================================
//  Event Handlers
// ============================================================

/**
 * 表单提交处理函数
 * 1. 清除上一次的错误信息
 * 2. 前端校验非空
 * 3. 调用后端登录接口，GET 方式通过 params 传参（避免 body 明文）
 * 4. 成功：写入 authStore 并路由跳转至 '/'
 * 5. 失败：展示后端返回的 message 或兜底提示
 */
const handleSubmit = async () => {
  // 先清空错误提示
  errorMsg.value = ''
  // 前端表单校验：用户名和密码均不能为空
  if (!form.username.trim() || !form.password.trim()) {
    errorMsg.value = '请输入用户名和密码'
    return
  }
  loading.value = true
  try {
    // 使用 GET /api/auth/login?username=xxx&password=xxx 方式登录
    const res = await post('/api/auth/login', null, {
      params: { username: form.username.trim(), password: form.password },
    })
    // code === 200 表示登录成功
    if (res.code === 200) {
      // 将用户数据和 JWT token 保存到 Pinia store（并持久化到 localStorage）
      authStore.login(res.data, res.data.token)
      // 跳转到企栈首页（/ 会重定向到 /dashboard）
      await router.push('/')
    } else {
      // 后端返回了非 200 的业务错误码
      errorMsg.value = res.message || '登录失败'
    }
  } catch (err) {
    // 网络异常或 HTTP 错误（如 500）时，优先取后端返回的 message
    errorMsg.value = err?.response?.data?.message || err.message || '请求失败，请检查后端是否启动'
  } finally {
    loading.value = false
  }
}
</script>

<!-- ============================================================
     Template - 登录页面 UI
     包含：背景图层、毛玻璃卡片、Logo 头像、表单、错误提示、跳转注册
============================================================ -->
<template>
  <!-- 全屏背景容器，background-image 使用 import 的 bgUrl -->
  <div class="login-wrapper" :style="{ backgroundImage: `url(${bgUrl})` }">
    <!-- 居中毛玻璃卡片 -->
    <div class="login-card">
      <!-- 卡片头部：Logo 头像 + 标题 + 英文副标题 -->
      <div class="login-header">
        <div class="logo-avatar">
          <img :src="adminAvatar" alt="logo" />
        </div>
        <h1>图书管理系统</h1>
        <p class="subtitle">Library Management System</p>
      </div>

      <!-- 登录表单，@submit.prevent 阻止默认提交行为 -->
      <form class="login-form" @submit.prevent="handleSubmit">
        <!-- 用户名输入框，带 SVG 人物图标 -->
        <div class="input-group">
          <span class="input-icon">
            <svg viewBox="0 0 16 16" fill="none" width="16" height="16">
              <circle cx="8" cy="5" r="3" stroke="currentColor" stroke-width="1.5"/>
              <path d="M3 14c0-2.76 2.24-5 5-5s5 2.24 5 5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </span>
          <!-- v-model 绑定到 form.username -->
          <input id="username" v-model="form.username" type="text" placeholder="请输入学号/工号" autocomplete="username" />
        </div>

        <!-- 密码输入框，带 SVG 锁头图标 -->
        <div class="input-group">
          <span class="input-icon">
            <svg viewBox="0 0 16 16" fill="none" width="16" height="16">
              <rect x="3" y="7" width="10" height="6" rx="1.5" stroke="currentColor" stroke-width="1.5"/>
              <path d="M5 7V5a3 3 0 016 0v2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </span>
          <!-- v-model 绑定到 form.password -->
          <input id="password" v-model="form.password" type="password" placeholder="请输入密码" autocomplete="current-password" />
        </div>

        <!-- 错误提示区域：仅当 errorMsg 非空时渲染 -->
        <p v-if="errorMsg" class="error-msg">{{ errorMsg }}</p>

        <!-- 提交按钮，loading 时禁用并展示旋转动画 -->
        <button type="submit" :disabled="loading" class="login-btn">
          <span v-if="loading" class="spinner"></span>
          {{ loading ? '登录中...' : '登 录' }}
        </button>

        <!-- 跳转注册页链接 -->
        <p class="register-link">
          没有账号？<router-link to="/register">去注册</router-link>
        </p>
      </form>
    </div>
  </div>
</template>

<!-- ============================================================
     Style - 登录页样式（scoped 作用域隔离）
     核心：全屏背景、毛玻璃卡片、输入框聚焦动效、按钮渐变
============================================================ -->
<style scoped>
/* ------ 全屏背景容器 ------ */
.login-wrapper {
  position: fixed;          /* 固定定位覆盖整个视口 */
  inset: 0;                 /* 等价于 top/right/bottom/left: 0 */
  display: flex;
  align-items: center;
  justify-content: center;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  overflow: hidden;
  z-index: 1;
}

/* ------ 毛玻璃卡片（登录表单容器）------ */
.login-card {
  position: relative;
  width: 400px;
  max-width: 92vw;                          /* 小屏幕下不超过视口宽度的 92% */
  background: rgba(255,255,255,0.92);       /* 半透明白色背景 */
  backdrop-filter: blur(16px);               /* 毛玻璃模糊效果 */
  border-radius: 16px;
  padding: 44px 36px 36px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.15); /* 柔和投影 */
}

/* ------ 卡片头部：Logo + 标题 ------ */
.login-header {
  text-align: center;
  margin-bottom: 32px;
}

/* Logo 圆形头像区域 */
.logo-avatar {
  width: 64px;
  height: 64px;
  margin: 0 auto 16px;
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

.login-header h1 {
  margin: 0 0 6px;
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
  letter-spacing: 1px;
}

.subtitle {
  margin: 0;
  font-size: 12px;
  color: #94a3b8;
  letter-spacing: 2px;
  text-transform: uppercase;  /* 英文全大写 */
}

/* ------ 表单区域 ------ */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 16px;  /* 各表单项之间的间距 */
}

/* 输入框组：图标 + input 的组合容器 */
.input-group {
  position: relative;
  display: flex;
  align-items: center;
  background: #f8fafc;
  border: 1.5px solid #e2e8f0;
  border-radius: 10px;
  transition: border-color 0.2s, box-shadow 0.2s; /* 聚焦时的平滑过渡 */
}

.input-group:focus-within {
  border-color: #2563eb;                          /* 聚焦时边框变蓝 */
  box-shadow: 0 0 0 3px rgba(37,99,235,0.1);     /* 蓝色外发光 */
  background: #fff;
}

/* 输入框内的图标 */
.input-icon {
  display: flex;
  align-items: center;
  padding-left: 14px;
  color: #94a3b8;
  flex-shrink: 0;  /* 不因 input 伸缩而压缩 */
}

.input-group:focus-within .input-icon { color: #2563eb; }  /* 聚焦时图标跟随变蓝 */

/* 输入框本身 */
.input-group input {
  width: 100%;
  padding: 13px 14px 13px 10px;
  border: none;
  background: transparent;
  font-size: 14px;
  color: #334155;
  outline: none;       /* 去除默认 outline，由父容器 border 控制 */
  border-radius: 10px;
}

.input-group input::placeholder { color: #cbd5e1; }  /* placeholder 颜色 */

/* ------ 错误提示 ------ */
.error-msg {
  margin: -4px 0 0;      /* 负 margin 拉近与上方输入框的距离 */
  padding: 10px 14px;
  background: #fef2f2;   /* 浅红色背景 */
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;        /* 红色文字 */
  font-size: 13px;
}

/* ------ 登录按钮 ------ */
.login-btn {
  margin-top: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  width: 100%;
  padding: 13px;
  border: none;
  border-radius: 10px;
  background: linear-gradient(135deg, #2563eb, #3b82f6);  /* 蓝色渐变背景 */
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  letter-spacing: 2px;
  transition: transform 0.15s, box-shadow 0.2s;
}

.login-btn:hover {
  transform: translateY(-1px);                     /* 悬停微上浮 */
  box-shadow: 0 6px 20px rgba(37,99,235,0.35);    /* 悬停投影增强 */
}

.login-btn:active { transform: translateY(0); }    /* 点击时回落 */

.login-btn:disabled {
  background: #93c5fd;       /* 禁用态变浅蓝 */
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

/* 跳转注册链接 */
.register-link {
  text-align: center;
  font-size: 13px;
  color: var(--muted);
  margin: 4px 0 0;
}

.register-link a {
  color: #2563eb;
  font-weight: 500;
}

/* 加载旋转动画元素 */
.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;            /* 上方边框白色，其余半透明 */
  border-radius: 50%;
  animation: spin 0.6s linear infinite;  /* 无限旋转 */
}

/* 旋转关键帧 */
@keyframes spin { to { transform: rotate(360deg); } }
</style>
