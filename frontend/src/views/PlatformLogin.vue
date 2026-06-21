<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>平台管理后台</h2>
        <p>租户管理 · 邀请码发放（仅平台管理员）</p>
      </div>

      <el-form
        ref="formRef"
        :model="loginForm"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item prop="account">
          <el-input
            v-model="loginForm.account"
            placeholder="管理员账号"
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
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="hint">
        租户用户请使用微信小程序登录，不在此入口操作。
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { PLATFORM_ADMIN_HOME } from '@/config/menu'

const router = useRouter()
const store = useStore()
const formRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  account: '',
  password: '',
  rememberMe: false
})

const rules = {
  account: [{ required: true, message: '请输入管理员账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const handleLogin = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await store.dispatch('user/loginPlatform', {
      account: loginForm.account.trim(),
      password: loginForm.password,
      rememberMe: loginForm.rememberMe
    })
    ElMessage.success('登录成功')
    await router.push(PLATFORM_ADMIN_HOME)
  } catch (error) {
    const msg = error.response?.data?.message || error.message || '账号或密码错误'
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
  background: linear-gradient(135deg, #1f2937 0%, #374151 100%);
}

.login-box {
  width: 400px;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.15);
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
}

.login-form .login-button {
  width: 100%;
}

.hint {
  margin-top: 16px;
  text-align: center;
  font-size: 13px;
  color: #909399;
}
</style>
