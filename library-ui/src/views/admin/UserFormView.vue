<!--
  用户表单页 - UserFormView（新增 / 编辑用户）
  功能：
  - 新增模式：含用户名、真实姓名、密码、确认密码、邮箱、手机号、角色多选
  - 编辑模式：密码字段不显示（仅新增时设密码），数据从路由 state 回填
  - 编辑回填：角色通过 roleNames + roleNameToId 映射还原为 roleIds 数组
  - 提交：新增调用 addUser，编辑调用 updateUser
-->
<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// 用户 API：新增、编辑
import { addUser, updateUser, userDetail } from '../../api/user'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

// 编辑模式判断：路由是否有 id 参数
const isEdit = computed(() => !!route.params.id)
// 动态标题
const title = computed(() => isEdit.value ? '编辑用户' : '新增用户')

const submitting = ref(false)   // 提交 loading
const loading = ref(false)      // 编辑时加载用户详情的 loading
const formRef = ref(null)       // 表单引用

// 角色选项（checkbox 数据源）
const roleOptions = [
  { id: 1, roleName: '超级管理员', roleCode: 'admin' },
  { id: 2, roleName: '图书管理员', roleCode: 'librarian' },
  { id: 3, roleName: '普通学生', roleCode: 'student' },
]

const form = reactive({
  username: '',
  password: '',           // 仅新增时使用
  confirmPassword: '',    // 仅新增时用于确认
  realName: '',
  email: '',
  phone: '',
  roleIds: [],            // checkbox-group 绑定的角色 ID 数组
})

// 基础验证规则（用户名、真实姓名必填）
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
}

// 密码字段验证规则（仅新增时使用）
const pwdRules = { required: true, message: '请输入密码', trigger: 'blur' }
// 确认密码验证规则：必填 + 与密码一致
const confirmRules = [
  { required: true, message: '请确认密码', trigger: 'blur' },
  { validator: (_, v, cb) => v === form.password ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' },
]

/**
 * 编辑模式下初始化表单数据
 * 调用 GET /api/user/detail/{id} 获取用户信息并回填表单
 */
async function initEditData() {
  if (!isEdit.value) return
  loading.value = true
  try {
    const res = await userDetail(route.params.id)
    if (res.code === 200 && res.data) {
      const u = res.data
      form.username = u.username || ''
      form.realName = u.realName || ''
      form.email = u.email || ''
      form.phone = u.phone || ''
      form.roleIds = u.roleIds || []
    }
  } catch { /* */ }
  finally { loading.value = false }
}

/**
 * 表单提交：验证 -> 确认密码一致性（新增时）-> 调用对应 API
 */
async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (valid === false) return

  // 新增模式下额外校验密码一致性
  if (!isEdit.value && form.password !== form.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      // 编辑：不传密码，调用 updateUser
      const res = await updateUser({
        id: Number(route.params.id),
        username: form.username,
        realName: form.realName,
        email: form.email,
        phone: form.phone,
        roleIds: form.roleIds.length ? form.roleIds : null,
      })
      if (res.code === 200) { ElMessage.success('保存成功'); router.push('/users') }
    } else {
      // 新增：含密码，调用 addUser
      const res = await addUser({
        username: form.username,
        password: form.password,
        realName: form.realName,
        email: form.email,
        phone: form.phone,
        roleIds: form.roleIds,
      })
      if (res.code === 200) { ElMessage.success('新增成功'); router.push('/users') }
    }
  } catch { /* */ }
  finally { submitting.value = false }
}

// 返回用户列表
function goBack() { router.push('/users') }

// 页面挂载时初始化编辑数据
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
        <el-row :gutter="24">
          <!-- 用户名 -->
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" placeholder="请输入用户名" maxlength="50" />
            </el-form-item>
          </el-col>
          <!-- 真实姓名 -->
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="请输入真实姓名" maxlength="50" />
            </el-form-item>
          </el-col>
          <!-- 密码和确认密码：仅新增模式显示 -->
          <template v-if="!isEdit">
            <el-col :span="12">
              <el-form-item label="密码" prop="password" :rules="pwdRules">
                <el-input v-model="form.password" type="password" placeholder="请输入密码（至少6位）" maxlength="100" show-password />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="确认密码" prop="confirmPassword" :rules="confirmRules">
                <el-input v-model="form.confirmPassword" type="password" placeholder="请再次输入密码" maxlength="100" show-password />
              </el-form-item>
            </el-col>
          </template>
          <!-- 邮箱 -->
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" placeholder="请输入邮箱" maxlength="100" />
            </el-form-item>
          </el-col>
          <!-- 手机号 -->
          <el-col :span="12">
            <el-form-item label="手机号">
              <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="20" />
            </el-form-item>
          </el-col>
          <!-- 角色分配：多选复选框 -->
          <el-col :span="24">
            <el-form-item label="角色分配">
              <el-checkbox-group v-model="form.roleIds">
                <el-checkbox v-for="role in roleOptions" :key="role.id" :label="role.id" :value="role.id">
                  {{ role.roleName }}
                </el-checkbox>
              </el-checkbox-group>
              <div v-if="form.roleIds.length === 0" class="role-hint">请至少选择一个角色</div>
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
.form-page { max-width: 800px }
.page-header { display:flex; align-items:center; justify-content:space-between; margin-bottom:16px }
.page-title { font-size:20px; font-weight:700; margin:0 }
.form-card { background:var(--panel); border:1px solid var(--line); border-radius:10px; padding:28px 32px }
.role-hint { font-size:12px; color:var(--muted); margin-top:4px }
</style>
