<template>
  <div class="user-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <div>
            <el-button type="primary" @click="openCreate">新增用户</el-button>
            <el-button @click="handleExport">导出</el-button>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-input
            v-model="searchKeyword"
            placeholder="请输入用户名/姓名/邮箱/手机号"
            style="width: 300px"
            @keyup.enter="handleSearch"
        />
        <el-select v-model="status" placeholder="状态" clearable style="width: 120px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
          <el-option label="锁定" value="LOCKED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="users" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="username" label="用户名" width="150" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="email" label="邮箱" width="200" />
        <el-table-column prop="phone" label="手机号" width="150" />
        <el-table-column prop="roles" label="角色" width="200">
          <template #default="{ row }">
            <el-tag v-for="role in row.roles" :key="role.id" size="small" style="margin-right: 5px">
              {{ role.name }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'ENABLED'" type="success" link @click="handleSetStatus(row, 'ENABLED')">启用</el-button>
            <el-button v-if="row.status !== 'DISABLED'" type="info" link @click="handleSetStatus(row, 'DISABLED')">禁用</el-button>
            <el-button v-if="row.status !== 'LOCKED'" type="warning" link @click="handleSetStatus(row, 'LOCKED')">锁定</el-button>
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
            <el-button type="success" link @click="handleResetPwd(row)">重置密码</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
          class="pagination"
      />
    </el-card>

    <el-dialog
        v-model="dialogVisible"
        :title="editingId ? '编辑用户' : '新增用户'"
        width="520px"
        destroy-on-close
        @closed="resetFormModel"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="登录名" maxlength="50" show-word-limit :disabled="!!editingId" />
        </el-form-item>
        <el-form-item :label="editingId ? '新密码' : '密码'" prop="password">
          <el-input
              v-model="form.password"
              type="password"
              show-password
              :placeholder="editingId ? '不填则不修改密码' : '至少 6 位'"
              autocomplete="new-password"
          />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" maxlength="50" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="邮箱" />
        </el-form-item>
        <el-form-item label="手机" prop="phone">
          <el-input v-model="form.phone" placeholder="手机号" maxlength="20" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option
                v-for="r in roleOptions"
                :key="r.id"
                :label="`${r.name}${r.description ? '（' + r.description + '）' : ''}`"
                :value="r.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="账号状态" style="width: 100%">
            <el-option label="启用" value="ENABLED" />
            <el-option label="禁用" value="DISABLED" />
            <el-option label="锁定" value="LOCKED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getUsers,
  getUser,
  createUser,
  updateUser,
  deleteUser,
  updateUserStatus,
  resetUserPassword
} from '@/api/user'
import { listRoles } from '@/api/role'
import { formatDateTime } from '@/utils/date'

const loading = ref(false)
const submitting = ref(false)
const users = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const status = ref('')

const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const roleOptions = ref([])

const form = reactive({
  username: '',
  password: '',
  realName: '',
  email: '',
  phone: '',
  roleIds: [],
  status: 'ENABLED'
})

const validatePassword = (rule, value, callback) => {
  if (editingId.value) {
    if (value && value.length > 0 && value.length < 6) {
      callback(new Error('密码至少 6 位'))
      return
    }
  } else if (!value || value.length < 6) {
    callback(new Error('请输入至少 6 位密码'))
    return
  }
  callback()
}

const formRules = computed(() => ({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '长度 3～50 个字符', trigger: 'blur' }
  ],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  roleIds: [{ type: 'array', required: true, min: 1, message: '请至少选择一个角色', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }]
}))

const loadRoleOptions = async () => {
  try {
    const res = await listRoles()
    roleOptions.value = res.data || []
  } catch {
    roleOptions.value = []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value - 1,
      size: size.value,
      keyword: searchKeyword.value || undefined,
      status: status.value || undefined
    }
    const response = await getUsers(params)
    users.value = response.data.content
    total.value = response.data.totalElements
  } catch (error) {
    console.error('加载用户失败:', error)
    ElMessage.error('加载用户失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const resetSearch = () => {
  searchKeyword.value = ''
  status.value = ''
  page.value = 1
  loadData()
}

const resetFormModel = () => {
  editingId.value = null
  form.username = ''
  form.password = ''
  form.realName = ''
  form.email = ''
  form.phone = ''
  form.roleIds = []
  form.status = 'ENABLED'
  formRef.value?.resetFields()
}

const openCreate = () => {
  resetFormModel()
  editingId.value = null
  dialogVisible.value = true
}

const openEdit = async (row) => {
  editingId.value = row.id
  dialogVisible.value = true
  try {
    const res = await getUser(row.id)
    const u = res.data
    form.username = u.username || ''
    form.password = ''
    form.realName = u.realName || ''
    form.email = u.email || ''
    form.phone = u.phone || ''
    form.status = u.status || 'ENABLED'
    form.roleIds = (u.roles || []).map((r) => r.id)
  } catch {
    ElMessage.error('加载用户信息失败')
    dialogVisible.value = false
  }
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        username: form.username.trim(),
        realName: form.realName?.trim() || undefined,
        email: form.email?.trim() || undefined,
        phone: form.phone?.trim() || undefined,
        roleIds: form.roleIds,
        status: form.status
      }
      if (form.password) {
        payload.password = form.password
      }
      if (editingId.value) {
        await updateUser(editingId.value, payload)
        ElMessage.success('保存成功')
      } else {
        await createUser({ ...payload, password: form.password })
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } catch {
      // 错误由拦截器提示
    } finally {
      submitting.value = false
    }
  })
}

const statusActionLabels = {
  ENABLED: '启用',
  DISABLED: '禁用',
  LOCKED: '锁定'
}

const handleSetStatus = (row, nextStatus) => {
  const label = statusActionLabels[nextStatus]
  ElMessageBox.confirm(
    `确定要将用户「${row.username}」设为「${label}」吗？禁用或锁定后该账号将无法登录；锁定多用于风险管控，禁用多用于离职停权。`,
    '修改账号状态',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: nextStatus === 'ENABLED' ? 'success' : 'warning'
    }
  ).then(async () => {
    try {
      await updateUserStatus(row.id, nextStatus)
      ElMessage.success('状态已更新')
      loadData()
    } catch {
      // 错误提示由 axios 拦截器统一展示
    }
  }).catch(() => {})
}

const handleResetPwd = (row) => {
  ElMessageBox.prompt(`请输入用户「${row.username}」的新密码（至少 6 位）`, '重置密码', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    inputType: 'password',
    inputPlaceholder: '新密码',
    inputValidator: (val) => {
      if (!val || val.length < 6) {
        return '密码至少 6 位'
      }
      return true
    }
  }).then(async ({ value }) => {
    try {
      await resetUserPassword(row.id, value)
      ElMessage.success('密码已重置')
    } catch {
      // 拦截器
    }
  }).catch(() => {})
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除用户「${row.username}」吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteUser(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch {
      // 拦截器
    }
  }).catch(() => {})
}

const getStatusType = (s) => {
  const types = {
    ENABLED: 'success',
    DISABLED: 'info',
    LOCKED: 'danger'
  }
  return types[s] || 'info'
}

const getStatusText = (s) => {
  const texts = {
    ENABLED: '启用',
    DISABLED: '禁用',
    LOCKED: '锁定'
  }
  return texts[s] || s
}

const handleExport = () => {
  ElMessage.info('导出功能开发中')
}

onMounted(() => {
  loadRoleOptions()
  loadData()
})
</script>

<style scoped lang="scss">
.user-list {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .search-bar {
    margin-bottom: 20px;
    display: flex;
    gap: 10px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}
</style>
