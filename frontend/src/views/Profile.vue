<template>
  <div class="profile-page">
    <el-card shadow="never" class="card">
      <template #header>
        <span>个人资料 · 安全设置</span>
      </template>

      <el-form label-width="200px" class="form">
        <el-form-item label="用户名">
          <span>{{ userInfo?.username }}</span>
        </el-form-item>
        <el-form-item label="邮箱">
          <span>{{ userInfo?.email || '未绑定' }}</span>
        </el-form-item>

        <el-divider content-position="left">登录安全</el-divider>

        <el-form-item label="登录二次验证（邮箱验证码）">
          <el-switch
            v-model="form.mfaLoginEnabled"
            :disabled="!mfaFeatureEnabled || saving"
          />
          <span class="tip">
            <template v-if="!mfaFeatureEnabled">系统未开放此功能，无法开启。</template>
            <template v-else>开启后，在账号与系统均允许时使用邮箱验证码第二步登录。</template>
          </span>
        </el-form-item>

        <el-form-item label="登录提醒邮件">
          <el-switch v-model="form.loginAlertEmailEnabled" :disabled="saving" />
          <span class="tip">每次登录成功时向绑定邮箱发送提醒（含大致时间与 IP）。</span>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { getAuthConfig, updateMySecurity } from '@/api/user'

const store = useStore()
const saving = ref(false)
const mfaFeatureEnabled = ref(false)

const userInfo = computed(() => store.state.user.userInfo)

const form = reactive({
  mfaLoginEnabled: false,
  loginAlertEmailEnabled: true
})

function syncFormFromStore() {
  const u = userInfo.value
  if (!u) return
  form.mfaLoginEnabled = !!u.mfaLoginEnabled
  form.loginAlertEmailEnabled = u.loginAlertEmailEnabled !== false
}

watch(userInfo, syncFormFromStore, { immediate: true })

onMounted(async () => {
  if (!userInfo.value) {
    try {
      await store.dispatch('user/getUserInfo')
    } catch {
      /* 路由守卫应已拦截未登录 */
    }
  }
  syncFormFromStore()
  try {
    const res = await getAuthConfig()
    mfaFeatureEnabled.value = !!res.data?.mfaFeatureEnabled
  } catch {
    mfaFeatureEnabled.value = false
  }
})

const handleSave = async () => {
  saving.value = true
  try {
    const res = await updateMySecurity({
      mfaLoginEnabled: form.mfaLoginEnabled,
      loginAlertEmailEnabled: form.loginAlertEmailEnabled
    })
    store.commit('user/SET_USER_INFO', res.data)
    ElMessage.success('已保存')
  } catch (error) {
    const msg = error.response?.data?.message || error.message || '保存失败'
    ElMessage.error(msg)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.profile-page {
  padding: 16px;
}
.card {
  max-width: 720px;
}
.form {
  .tip {
    margin-left: 12px;
    font-size: 12px;
    color: #909399;
    line-height: 1.5;
  }
}
</style>
