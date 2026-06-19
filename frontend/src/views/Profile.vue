<template>
  <div class="profile-page">
    <el-card shadow="never" class="card">
      <template #header>
        <span>个人资料</span>
      </template>

      <el-form ref="formRef" :model="form" label-width="120px" class="form">
        <el-form-item label="用户名">
          <span>{{ userInfo?.username }}</span>
        </el-form-item>

        <el-form-item label="姓名">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>

        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存资料</el-button>
        </el-form-item>
      </el-form>

      <el-divider content-position="left">修改密码</el-divider>

      <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="120px" class="form">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入原密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码（6-20位）" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="changingPwd" @click="handleChangePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { updateMe, changePassword } from '@/api/user'

const store = useStore()
const formRef = ref(null)
const pwdFormRef = ref(null)
const saving = ref(false)
const changingPwd = ref(false)

const userInfo = computed(() => store.state.user.userInfo)

const form = reactive({ realName: '', phone: '' })

watch(userInfo, (u) => {
  if (u) {
    form.realName = u.realName || ''
    form.phone = u.phone || ''
  }
}, { immediate: true })

const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_, value, cb) => {
        if (value !== pwdForm.newPassword) cb(new Error('两次密码不一致'))
        else cb()
      },
      trigger: 'blur'
    }
  ]
}

onMounted(async () => {
  if (!userInfo.value) {
    await store.dispatch('user/getUserInfo')
  }
})

const handleSave = async () => {
  saving.value = true
  try {
    const res = await updateMe({ realName: form.realName, phone: form.phone })
    store.commit('user/SET_USER_INFO', res.data)
    ElMessage.success('资料已保存')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const handleChangePassword = async () => {
  if (!pwdFormRef.value) return
  try {
    await pwdFormRef.value.validate()
  } catch {
    return
  }
  changingPwd.value = true
  try {
    await changePassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      confirmPassword: pwdForm.confirmPassword
    })
    ElMessage.success('密码已修改，请重新登录')
    store.dispatch('user/logout')
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '修改失败')
  } finally {
    changingPwd.value = false
  }
}
</script>

<style scoped lang="scss">
.profile-page {
  padding: 16px;
}
.card {
  max-width: 600px;
}
</style>
