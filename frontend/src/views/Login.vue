<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>果蔬批发商贸管理系统</h2>
        <p v-if="step === 1">欢迎登录</p>
        <p v-else>邮箱验证{{ maskedEmail ? `（${maskedEmail}）` : '' }}</p>
        <p v-if="step === 1 && mfaFeatureEnabled" class="hint">
          若已在个人资料中开启登录二次验证，登录时将向绑定邮箱发送验证码。
        </p>
      </div>

      <!-- 第一步：账号密码 -->
      <el-form
        v-show="step === 1"
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleStep1Login"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="loginForm.rememberMe">记住我（30天内保持登录）</el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            class="login-button"
            size="large"
            @click="handleStep1Login"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 第二步：验证码 -->
      <el-form
        v-show="step === 2"
        ref="mfaFormRef"
        :model="mfaForm"
        :rules="mfaRules"
        class="login-form"
        @keyup.enter="handleStep2"
      >
        <el-form-item prop="code" label="邮箱验证码">
          <div class="code-row">
            <el-input
              v-model="mfaForm.code"
              placeholder="6位数字"
              maxlength="6"
              size="large"
              clearable
            />
            <el-button
              size="large"
              :disabled="sending || cooldown > 0"
              :loading="sending"
              class="code-btn"
              @click="sendLoginCode"
            >
              {{ cooldown > 0 ? `${cooldown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            :loading="loading"
            :disabled="!sessionId"
            class="login-button"
            size="large"
            @click="handleStep2"
          >
            {{ loading ? '登录中...' : '登录' }}
          </el-button>
        </el-form-item>

        <el-form-item>
          <el-button text type="primary" @click="backToStep1">返回上一步</el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <el-link type="primary" @click="router.push('/register')">注册账号</el-link>
        <span class="sep">|</span>
        <el-link type="primary" @click="router.push('/forgot-password')">忘记密码？</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { getAuthConfig, loginMfaChallenge } from '@/api/user'

const router = useRouter()
const store = useStore()
const formRef = ref(null)
const mfaFormRef = ref(null)
const loading = ref(false)
const sending = ref(false)
const cooldown = ref(0)
const step = ref(1)
const sessionId = ref('')
const maskedEmail = ref('')
const mfaFeatureEnabled = ref(false)

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

const loginForm = reactive({
  username: 'admin',
  password: 'admin123',
  rememberMe: false
})

const mfaForm = reactive({
  code: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const mfaRules = {
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位数字', trigger: 'blur' }
  ]
}

onMounted(async () => {
  localStorage.removeItem('token')
  localStorage.removeItem('refreshToken')
  try {
    const res = await getAuthConfig()
    mfaFeatureEnabled.value = !!res.data?.mfaFeatureEnabled
  } catch {
    mfaFeatureEnabled.value = false
  }
})

/** 第一步：提交账号密码；服务端决定直接发 token 或进入二次验证 */
const handleStep1Login = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    const r = await store.dispatch('user/loginStep1', {
      username: loginForm.username,
      password: loginForm.password,
      rememberMe: loginForm.rememberMe
    })
    if (r.needMfa) {
      step.value = 2
      sessionId.value = r.sessionId
      maskedEmail.value = r.maskedEmail || ''
      mfaForm.code = ''
      clearCooldown()
      ElMessage.success('请输入邮箱验证码')
    } else {
      ElMessage.success('登录成功')
      await router.push('/')
    }
  } catch (error) {
    const msg = error.response?.data?.message || error.message || '登录失败'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

const sendLoginCode = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  sending.value = true
  startCooldown()
  try {
    const res = await loginMfaChallenge({
      username: loginForm.username,
      password: loginForm.password,
      rememberMe: loginForm.rememberMe
    })
    const d = res.data
    sessionId.value = d.sessionId
    maskedEmail.value = d.maskedEmail || ''
    ElMessage.success('验证码已发送至邮箱，请查收（含垃圾箱）')
  } catch (error) {
    clearCooldown()
    const msg = error.response?.data?.message || error.message || '发送失败'
    ElMessage.error(msg)
  } finally {
    sending.value = false
  }
}

const backToStep1 = () => {
  step.value = 1
  sessionId.value = ''
  mfaForm.code = ''
  clearCooldown()
}

const handleStep2 = async () => {
  if (!sessionId.value) {
    ElMessage.warning('请先点击「获取验证码」')
    return
  }
  if (!mfaFormRef.value) return
  try {
    await mfaFormRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await store.dispatch('user/login', {
      sessionId: sessionId.value,
      code: mfaForm.code.trim()
    })
    ElMessage.success('登录成功')
    await router.push('/')
  } catch (error) {
    const msg = error.response?.data?.message || error.message || '登录失败'
    ElMessage.error(msg)
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
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;

  h2 {
    font-size: 24px;
    color: #333;
    margin-bottom: 10px;
  }

  p {
    color: #666;
    font-size: 14px;
  }

  .hint {
    margin-top: 8px;
    font-size: 12px;
    color: #909399;
    line-height: 1.5;
  }
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

.login-form {
  .login-button {
    width: 100%;
  }
}

.login-footer {
  text-align: center;
  margin-top: 20px;
  .sep {
    margin: 0 8px;
    color: #ccc;
  }
}
</style>
