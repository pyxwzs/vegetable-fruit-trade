<template>
  <el-dialog
      v-model="visible"
      title="个人资料"
      :width="isMobile ? '92%' : '420px'"
      destroy-on-close
      class="profile-dialog"
      @open="onOpen"
  >
    <div class="profile-head">
      <el-avatar :size="48" class="head-avatar">{{ avatarText }}</el-avatar>
      <div class="head-info">
        <div class="head-name">{{ displayName }}</div>
        <div v-if="userInfo?.username" class="head-user">{{ userInfo.username }}</div>
      </div>
    </div>

    <el-form ref="formRef" :model="form" label-position="top" class="profile-form">
      <el-form-item v-if="isPlatformAdmin" label="管理员账号">
        <div class="readonly-field">{{ userInfo?.username || '—' }}</div>
      </el-form-item>
      <el-form-item v-if="!isPlatformAdmin" label="微信昵称">
        <div class="readonly-field">{{ userInfo?.wxNickname || '—' }}</div>
      </el-form-item>
      <el-form-item label="姓名">
        <el-input v-model="form.realName" placeholder="请输入姓名" clearable />
      </el-form-item>
      <el-form-item v-if="!isPlatformAdmin" label="手机号">
        <el-input v-model="form.phone" placeholder="请输入手机号" clearable maxlength="11" />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button v-if="!isPlatformAdmin" type="primary" :loading="saving" @click="handleSave">
        保存资料
      </el-button>
      <el-button v-else type="primary" @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { updateMe } from '@/api/user'
import { useIsMobile } from '@/composables/useIsMobile'
import { isPlatformAdminRole } from '@/config/menu'

const props = defineProps({
  modelValue: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue'])

const store = useStore()
const isMobile = useIsMobile()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v)
})

const formRef = ref(null)
const saving = ref(false)

const userInfo = computed(() => store.state.user.userInfo)
const isPlatformAdmin = computed(() => isPlatformAdminRole(userInfo.value))
const displayName = computed(() => userInfo.value?.realName || userInfo.value?.username || userInfo.value?.wxNickname || '未设置姓名')
const avatarText = computed(() => {
  const name = displayName.value
  return name.slice(0, 1).toUpperCase()
})

const form = reactive({ realName: '', phone: '' })

watch(userInfo, (u) => {
  if (u) {
    form.realName = u.realName || ''
    form.phone = u.phone || ''
  }
}, { immediate: true })

const onOpen = async () => {
  if (!userInfo.value) {
    await store.dispatch('user/getUserInfo')
  }
}

const handleSave = async () => {
  saving.value = true
  try {
    const res = await updateMe({ realName: form.realName, phone: form.phone })
    store.commit('user/SET_USER_INFO', res.data)
    ElMessage.success('资料已保存')
    visible.value = false
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.profile-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 16px;
  background: #f5f7fa;
  border-radius: 8px;
}

.head-avatar {
  flex-shrink: 0;
  background: #409eff;
  color: #fff;
  font-weight: 700;
}

.head-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  word-break: break-all;
}

.head-user {
  margin-top: 2px;
  font-size: 12px;
  color: #909399;
}

.profile-form {
  :deep(.el-form-item) {
    margin-bottom: 14px;
  }

  :deep(.el-form-item__label) {
    padding-bottom: 4px;
    line-height: 1.4;
    font-weight: 500;
  }

  :deep(.el-input) {
    width: 100%;
  }
}

.readonly-field {
  width: 100%;
  min-height: 32px;
  line-height: 32px;
  padding: 0 12px;
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  color: #606266;
  font-size: 14px;
}
</style>

<style lang="scss">
.profile-dialog {
  .el-dialog__body {
    padding-top: 8px;
    padding-bottom: 8px;
  }

  .el-dialog__footer {
    padding-top: 8px;
  }
}
</style>
