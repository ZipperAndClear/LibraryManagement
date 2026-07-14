<!--
  系统配置页 - ConfigView
  功能：以表格形式展示系统配置项，支持逐行内联编辑
  - 加载 configList API 获取配置列表
  - 每行配置值列默认以文本展示，点击编辑按钮进入编辑模式
  - 编辑模式：inline input + 保存/取消按钮
  - 保存调用 updateConfig API（传 configKey + 新值）
  - 编辑状态通过 editMap 对象管理（key=配置id, value=当前编辑值）
-->
<script setup>
import { ref, reactive, onMounted } from 'vue'
// 配置 API：列表、更新
import { configList, updateConfig, batchUpdateConfig } from '../../api/config'
import { ElMessage } from 'element-plus'
import { Edit, Check } from '@element-plus/icons-vue'

// === 响应式状态 ===
const loading = ref(false)       // 加载配置列表的 loading
const configs = ref([])          // 配置项列表
// 编辑映射表：{ [配置id]: 当前编辑中的值 }
// 使用 reactive 保证 vue 能追踪对象属性的增删
const editMap = reactive({})

// 加载配置列表
async function loadConfigs() {
  loading.value = true
  try {
    const res = await configList()
    configs.value = res.data || []
  } catch { configs.value = [] }
  finally { loading.value = false }
}

/**
 * 开启编辑模式：将该配置的当前值存入 editMap
 * 模板中检查 editMap[row.id] 是否为 undefined 来判断是否处于编辑中
 */
function startEdit(config) {
  editMap[config.id] = config.configValue
}

/**
 * 取消编辑模式：从 editMap 中删除该配置的编辑状态
 */
function cancelEdit(config) {
  delete editMap[config.id]
}

/**
 * 保存编辑：如果值未变化则直接取消；否则调用 updateConfig API
 * 成功后更新 config.configValue 为新值，并退出编辑模式
 */
async function saveEdit(config) {
  const newVal = editMap[config.id]
  if (newVal === undefined || newVal === config.configValue) {
    cancelEdit(config)
    return
  }
  try {
    // 使用 configKey 作为定位键调用 API
    const res = await updateConfig(config.configKey, newVal)
    if (res.code === 200) {
      config.configValue = newVal
      ElMessage.success('保存成功')
      cancelEdit(config)
    }
  } catch { /* */ }
}

/**
 * 判断某配置是否处于编辑状态
 */
function isEditing(config) {
  return editMap[config.id] !== undefined
}

onMounted(loadConfigs)
</script>

<!-- ==================== 模板 ==================== -->
<template>
  <div class="config-page">
    <!-- 顶部标题栏 + 刷新按钮 -->
    <div class="page-header">
      <h1 class="page-title">系统配置</h1>
      <el-button :icon="Check" @click="loadConfigs" :loading="loading">刷新</el-button>
    </div>

    <!-- 配置表格卡片 -->
    <div class="config-card" v-loading="loading">
      <el-table :data="configs" stripe border style="width:100%">
        <el-table-column prop="configName" label="配置项" width="180" />
        <!-- 配置键：以 code 样式显示 -->
        <el-table-column prop="configKey" label="配置键" width="200">
          <template #default="{row}"><code style="font-size:12px;color:var(--muted)">{{ row.configKey }}</code></template>
        </el-table-column>
        <!-- 配置值列：编辑模式下显示 input + 保存/取消；非编辑模式显示文本 -->
        <el-table-column label="配置值" min-width="240">
          <template #default="{row}">
            <div v-if="isEditing(row)" class="edit-row">
              <el-input v-model="editMap[row.id]" size="small" style="flex:1" />
              <el-button type="primary" link :icon="Check" size="small" @click="saveEdit(row)">保存</el-button>
              <el-button link size="small" @click="cancelEdit(row)">取消</el-button>
            </div>
            <span v-else>{{ row.configValue }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="200" show-overflow-tooltip />
        <!-- 操作列：非编辑模式时显示编辑按钮 -->
        <el-table-column label="操作" width="80" align="center">
          <template #default="{row}">
            <el-button v-if="!isEditing(row)" type="primary" link :icon="Edit" size="small" @click="startEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 空状态 -->
      <div v-if="!loading && configs.length === 0" class="empty"><el-empty description="暂无配置" :image-size="100" /></div>
    </div>
  </div>
</template>

<!-- ==================== 样式（scoped） ==================== -->
<style scoped>
.config-page { padding:0 }
.page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px }
.page-title { font-size:20px; font-weight:700; margin:0 }
.config-card { background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:20px }
.edit-row { display:flex; align-items:center; gap:6px }
.empty { padding:30px 0 }
</style>
