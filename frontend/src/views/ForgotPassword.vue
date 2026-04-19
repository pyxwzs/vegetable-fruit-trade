<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>重置密码</h2>
        <p>向注册邮箱发送验证码</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" class="login-form" label-position="top">
        <el-form-item label="注册邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入注册时使用的邮箱" size="large" />
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="6位数字" maxlength="6" size="large" />
            <el-button
              size="large"
              class="code-btn"
              :disabled="sending || cooldown > 0"
              :loading="sending"
              @click="sendCode"
            >
              {{ cooldown > 0 ? `${cooldown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password placeholder="至少6位" size="large" />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password placeholder="再次输入新密码" size="large" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" class="login-button" size="large" :loading="loading" @click="submit">
            确认重置
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <el-link type="primary" @click="$router.push('/login')">返回登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { forgotPassword, resetPasswordByEmail } from '@/api/user'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const sending = ref(false)
const cooldown = ref(0)

let cooldownTimer = null

const clearCooldown = () => {
  if (cooldownTimer) {
    clearInterval(cooldownTimer)
    cooldownTimer = null
  }
  cooldown.value = 0
}

const startCooldown = () => {
  clearCooldown()
  cooldown.value = 60
  cooldownTimer = setInterval(() => {
    cooldown.value -= 1
    if (cooldown.value <= 0) {
      clearCooldown()
    }
  }, 1000)
}

onUnmounted(() => {
  clearCooldown()
})

const form = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirm = (rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 64, message: '密码长度为6-64位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' }
  ]
}

const sendCode = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validateField('email')
  } catch {
    return
  }
  sending.value = true
  startCooldown()
  try {
    await forgotPassword(form.email.trim())
    ElMessage.success('若该邮箱已注册，将收到验证码邮件，请查收（含垃圾箱）')
  } catch (e) {
    clearCooldown()
    ElMessage.error(e.response?.data?.message || '发送失败')
  } finally {
    sending.value = false
  }
}

const submit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await resetPasswordByEmail({
      email: form.email.trim(),
      code: form.code.trim(),
      newPassword: form.newPassword
    })
    ElMessage.success('密码已重置，请使用新密码登录')
    await router.push('/login')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '重置失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-box {
  width: 420px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.login-header {
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

.login-button {
  width: 100%;
}

.code-row {
  display: flex;
  gap: 10px;
  width: 100%;
  align-items: center;
  .el-input {
    flex: 1;
    min-width: 0;
  }
  .code-btn {
    flex-shrink: 0;
    min-width: 120px;
  }
}

.login-footer {
  text-align: center;
  margin-top: 16px;
}
</style>
