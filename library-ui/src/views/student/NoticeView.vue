<!--
  公告页面 (NoticeView.vue)
  - 展示已发布的公告列表，以卡片形式逐条呈现
  - 置顶公告优先显示并带有"置顶"标签，其余按发布时间倒序排列
  - 提供手动刷新按钮
-->
<script setup>
// ====================================================================
// 导入
// ====================================================================
import { ref, onMounted } from 'vue'
import { publishedNotices } from '../../api/notice'  // 获取已发布公告列表 API

// ====================================================================
// 响应式状态
// ====================================================================
const loading = ref(false)        // 加载状态，控制 loading 遮罩和空状态展示
const notices = ref([])           // 已发布公告列表

// ====================================================================
// 数据加载
// ====================================================================

/**
 * 加载已发布的公告列表
 * 排序规则：1) 置顶公告优先 (isTop 为 true 的排前面)
 *          2) 同级别按创建时间倒序 (最新在前)
 */
async function loadData() {
  loading.value = true
  try {
    const res = await publishedNotices()
    notices.value = (res.data || []).sort((a, b) => {
      if (a.isTop !== b.isTop) return b.isTop - a.isTop
      return new Date(b.createTime) - new Date(a.createTime)
    })
  } catch { notices.value = [] }
  finally { loading.value = false }
}

// ====================================================================
// 生命周期
// ====================================================================

/** 组件挂载后加载公告数据 */
onMounted(loadData)
</script>

<template>
  <div class="student-notice-page">
    <!-- 页面标题 + 刷新按钮 -->
    <div class="page-header">
      <h1 class="page-title">公告</h1>
      <el-button :loading="loading" @click="loadData">刷新</el-button>
    </div>

    <!-- 公告卡片列表区域 -->
    <div v-loading="loading" class="notice-section">
      <!-- 有数据时渲染公告卡片 -->
      <div v-if="notices.length" class="notice-list">
        <div v-for="notice in notices" :key="notice.id" class="notice-card">
          <!-- 卡片头部：置顶标签 + 标题 + 发布日期 -->
          <div class="notice-head">
            <el-tag v-if="notice.isTop" size="small" type="danger" class="notice-tag">置顶</el-tag>
            <h3 class="notice-title">{{ notice.title }}</h3>
            <span class="notice-time">{{ notice.createTime?.slice(0, 10) }}</span>
          </div>
          <!-- 公告正文内容 -->
          <div class="notice-content">{{ notice.content }}</div>
        </div>
      </div>
      <!-- 无数据且非加载中时显示空状态 -->
      <el-empty v-else-if="!loading" description="暂无公告" :image-size="100" />
    </div>
  </div>
</template>

<style scoped>
.student-notice-page { padding:0 }

.page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px }
.page-title { font-size:20px; font-weight:700; margin:0 }

.notice-section { background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:20px; min-height:200px }
.notice-list { display:flex; flex-direction:column; gap:16px }
.notice-card { padding:16px 20px; border:1px solid var(--line); border-radius:8px; transition:box-shadow 0.2s }
.notice-card:hover { box-shadow:0 2px 8px rgba(0,0,0,0.04) }
.notice-head { display:flex; align-items:center; gap:10px; margin-bottom:10px }
.notice-tag { flex-shrink:0 }
.notice-title { flex:1; font-size:16px; font-weight:600; margin:0; overflow:hidden; text-overflow:ellipsis; white-space:nowrap }
.notice-time { font-size:12px; color:var(--muted); white-space:nowrap; flex-shrink:0 }
.notice-content { font-size:14px; line-height:1.7; color:var(--text); white-space:pre-wrap; word-break:break-word }
</style>
