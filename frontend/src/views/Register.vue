<template>
  <div class="register-page">
    <div class="register-box">
      <div class="header">
        <h2>用户注册</h2>
        <p>填写信息，邮箱验证码用于激活账号</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="register-form"
        @submit.prevent
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="3-50 个字符" maxlength="50" size="large" />
        </el-form-item>

        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" size="large" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="用于接收验证码" size="large" />
        </el-form-item>

        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="11 位中国大陆手机号" maxlength="11" size="large" />
        </el-form-item>

        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="选填" size="large" />
        </el-form-item>

        <el-form-item label="邮箱验证码" prop="code">
          <div class="code-row">
            <el-input
              v-model="form.code"
              maxlength="6"
              placeholder="先点击右侧获取验证码"
              size="large"
              clearable
            />
            <el-button
              size="large"
              :disabled="sending || cooldown > 0"
              :loading="sending"
              class="code-btn"
              @click="sendRegisterCode"
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
            class="w100"
            size="large"
            @click="submitRegister"
          >
            注册
          </el-button>
        </el-form-item>
      </el-form>

      <div class="footer">
        <el-link type="primary" @click="router.push('/login')">已有账号？去登录</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { registerChallenge, registerVerify } from '@/api/user'

/** 中国大陆手机号：1 开头，第二位 3-9，共 11 位 */
const CN_MOBILE = /^1[3-9]\d{9}$/

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const sending = ref(false)
const cooldown = ref(0)
const sessionId = ref('')

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
  username: '',
  password: '',
  email: '',
  phone: '',
  realName: '',
  code: ''
})

const validatePhone = (rule, value, callback) => {
  const v = (value || '').trim()
  if (!v) {
    callback(new Error('请输入手机号'))
    return
  }
  if (!CN_MOBILE.test(v)) {
    callback(new Error('请输入正确的 11 位手机号码'))
    return
  }
  callback()
}

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '长度为 3-50 个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 100, message: '密码长度为 6-100 位', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  phone: [{ required: true, validator: validatePhone, trigger: 'blur' }],
  realName: [{ max: 50, message: '姓名不超过 50 字', trigger: 'blur' }],
  code: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { pattern: /^\d{6}$/, message: '验证码为 6 位数字', trigger: 'blur' }
  ]
}

/** 获取验证码前：只校验除验证码外的字段 */
const validateBeforeSend = async () => {
  if (!formRef.value) return false
  const fields = ['username', 'password', 'email', 'phone', 'realName']
  try {
    for (const f of fields) {
      await formRef.value.validateField(f)
    }
    return true
  } catch {
    return false
  }
}

const sendRegisterCode = async () => {
  const ok = await validateBeforeSend()
  if (!ok) return

  sending.value = true
  startCooldown()
  try {
    const res = await registerChallenge({
      username: form.username.trim(),
      password: form.password,
      email: form.email.trim(),
      phone: form.phone.trim(),
      realName: form.realName?.trim() || undefined
    })
    const d = res.data
    sessionId.value = d.sessionId
    ElMessage.success('验证码已发送至邮箱，请查收（含垃圾箱）')
  } catch (e) {
    clearCooldown()
    ElMessage.error(e.response?.data?.message || e.message || '发送失败')
  } finally {
    sending.value = false
  }
}

const submitRegister = async () => {
  if (!sessionId.value) {
    ElMessage.warning('请先点击「获取验证码」')
    return
  }
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await registerVerify({
      sessionId: sessionId.value,
      code: form.code.trim()
    })
    ElMessage.success('注册成功，请登录')
    await router.push('/login')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || e.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 24px 16px;
}

.register-box {
  width: 100%;
  max-width: 440px;
  padding: 36px 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.1);
}

.header {
  text-align: center;
  margin-bottom: 24px;
  h2 {
    font-size: 22px;
    color: #333;
    margin-bottom: 8px;
  }
  p {
    font-size: 14px;
    color: #666;
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

.w100 {
  width: 100%;
}

.footer {
  text-align: center;
  margin-top: 16px;
}
</style>
