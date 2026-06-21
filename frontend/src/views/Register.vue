<template>
  <div class="register-page">
    <div class="register-box">
      <div class="register-header">
        <h2>邀请码注册</h2>
        <p>使用平台发放的邀请码开通新租户，租户编号将自动生成</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" class="register-form" label-width="96px">
        <el-form-item label="邀请码" prop="inviteCode">
          <el-input v-model="form.inviteCode" placeholder="12位邀请码" maxlength="32" />
        </el-form-item>
        <el-form-item label="租户名称" prop="tenantName">
          <el-input v-model="form.tenantName" placeholder="如：张三果蔬批发" maxlength="100" />
        </el-form-item>
        <el-form-item label="管理员账号" prop="username">
          <el-input v-model="form.username" placeholder="全局唯一用户名" maxlength="50" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="选填" maxlength="50" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password maxlength="50" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password maxlength="50" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="loading" class="submit-btn" @click="handleRegister">
            提交注册
          </el-button>
        </el-form-item>
        <div class="footer-link">
          已有账号？<router-link to="/login">返回登录</router-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { postRegister } from '@/api/user'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const loading = ref(false)

const form = reactive({
  inviteCode: '',
  tenantName: '',
  username: '',
  realName: '',
  password: '',
  confirmPassword: ''
})

const validateConfirm = (_rule, value, callback) => {
  if (value !== form.password) callback(new Error('两次密码不一致'))
  else callback()
}

const rules = {
  inviteCode: [{ required: true, message: '请填写邀请码', trigger: 'blur' }],
  tenantName: [{ required: true, message: '请填写租户名称', trigger: 'blur' }],
  username: [
    { required: true, message: '请填写用户名', trigger: 'blur' },
    { min: 2, max: 50, message: '长度 2-50', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请填写密码', trigger: 'blur' },
    { min: 6, message: '至少 6 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

onMounted(() => {
  const code = route.query.inviteCode
  if (code) {
    form.inviteCode = String(code).trim().toUpperCase()
  }
})

async function handleRegister() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await postRegister({
      inviteCode: form.inviteCode.trim(),
      tenantName: form.tenantName.trim(),
      username: form.username.trim(),
      realName: form.realName.trim() || undefined,
      password: form.password
    })
    const data = res.data
    await ElMessageBox.alert(
        `请使用账号「${data.username}」登录`,
        '注册成功',
        { confirmButtonText: '去登录' }
    )
    await router.push({ path: '/login', query: { username: data.username } })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.register-page {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 24px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.register-box {
  width: 100%;
  max-width: 520px;
  padding: 36px 32px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.register-header {
  text-align: center;
  margin-bottom: 24px;

  h2 {
    font-size: 22px;
    color: #333;
    margin-bottom: 8px;
  }

  p {
    color: #666;
    font-size: 14px;
  }
}

.submit-btn {
  width: 100%;
}

.footer-link {
  text-align: center;
  font-size: 14px;
  color: #666;

  a {
    color: var(--el-color-primary);
    text-decoration: none;
  }
}
</style>
