<template>
  <div class="site-settings-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>站点设置</span>
          <span class="hint">修改侧边栏显示的系统名称与 Logo</span>
        </div>
      </template>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" class="settings-form">
        <el-form-item label="系统名称" prop="siteName">
          <el-input
              v-model="form.siteName"
              maxlength="100"
              show-word-limit
              placeholder="显示在导航栏的名称"
              class="site-name-input"
          />
        </el-form-item>

        <el-form-item label="Logo">
          <div class="logo-block">
            <div class="logo-preview">
              <img :src="previewLogoUrl" alt="logo" @error="onPreviewError" />
            </div>

            <div class="logo-actions">
              <el-radio-group v-model="logoMode" class="logo-mode">
                <el-radio-button label="upload">上传图片</el-radio-button>
                <el-radio-button label="url">链接地址</el-radio-button>
              </el-radio-group>

              <div v-if="logoMode === 'upload'" class="logo-panel">
                <el-upload
                    :show-file-list="false"
                    accept="image/png,image/jpeg,image/webp,image/gif,image/svg+xml"
                    :before-upload="beforeUpload"
                    :http-request="handleUpload"
                >
                  <el-button :loading="uploading">选择图片</el-button>
                </el-upload>
                <p class="upload-tip">支持 png、jpg、webp、gif、svg，不超过 2MB</p>
              </div>

              <div v-else class="logo-panel">
                <div class="url-row">
                  <el-input
                      v-model="form.logoUrl"
                      placeholder="https://example.com/logo.png"
                      clearable
                      class="logo-url-input"
                  />
                  <el-button type="primary" :loading="savingUrl" @click="handleSaveUrl">保存链接</el-button>
                </div>
                <p class="upload-tip">填写可公开访问的图片链接，须以 http:// 或 https:// 开头</p>
              </div>

              <el-button
                  v-if="hasCustomLogo"
                  link
                  type="danger"
                  class="clear-btn"
                  :loading="clearing"
                  @click="handleClearLogo"
              >
                恢复默认 Logo
              </el-button>
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存名称</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import defaultLogo from '@/assets/logo.png'

const store = useStore()
const formRef = ref()
const saving = ref(false)
const uploading = ref(false)
const savingUrl = ref(false)
const clearing = ref(false)
const logoMode = ref('upload')
const previewBroken = ref(false)

const form = reactive({
  siteName: '',
  logoUrl: ''
})

const rules = {
  siteName: [
    { required: true, message: '请填写系统名称', trigger: 'blur' },
    { max: 100, message: '最多 100 字', trigger: 'blur' }
  ]
}

const hasCustomLogo = computed(() => !!store.state.site.logoPath)

const previewLogoUrl = computed(() => {
  if (previewBroken.value) {
    return defaultLogo
  }
  if (logoMode.value === 'url' && form.logoUrl.trim()) {
    return form.logoUrl.trim()
  }
  return store.getters['site/logoUrl']
})

function syncFormFromStore() {
  form.siteName = store.state.site.siteName
  const path = store.state.site.logoPath
  if (path && /^https?:\/\//i.test(path)) {
    logoMode.value = 'url'
    form.logoUrl = path
  } else {
    logoMode.value = 'upload'
    form.logoUrl = ''
  }
  previewBroken.value = false
}

onMounted(async () => {
  if (!store.state.site.loaded) {
    await store.dispatch('site/load')
  }
  syncFormFromStore()
})

function onPreviewError() {
  previewBroken.value = true
}

function beforeUpload(file) {
  const allowed = ['image/png', 'image/jpeg', 'image/webp', 'image/gif', 'image/svg+xml']
  if (!allowed.includes(file.type)) {
    ElMessage.warning('请选择 png、jpg、webp、gif 或 svg 图片')
    return false
  }
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('Logo 不能超过 2MB')
    return false
  }
  return true
}

async function handleUpload({ file }) {
  uploading.value = true
  previewBroken.value = false
  try {
    await store.dispatch('site/uploadLogo', file)
    syncFormFromStore()
    ElMessage.success('Logo 已更新')
  } finally {
    uploading.value = false
  }
}

function validateLogoUrl(url) {
  if (!url) {
    ElMessage.warning('请填写 Logo 链接')
    return false
  }
  if (!/^https?:\/\//i.test(url)) {
    ElMessage.warning('链接须以 http:// 或 https:// 开头')
    return false
  }
  return true
}

async function handleSaveUrl() {
  const url = form.logoUrl.trim()
  if (!validateLogoUrl(url)) return

  savingUrl.value = true
  previewBroken.value = false
  try {
    await store.dispatch('site/updateLogoUrl', url)
    syncFormFromStore()
    ElMessage.success('Logo 链接已保存')
  } finally {
    savingUrl.value = false
  }
}

async function handleClearLogo() {
  try {
    await ElMessageBox.confirm('确定恢复为系统默认 Logo？', '提示', { type: 'warning' })
  } catch {
    return
  }

  clearing.value = true
  previewBroken.value = false
  try {
    await store.dispatch('site/clearLogo')
    syncFormFromStore()
    ElMessage.success('已恢复默认 Logo')
  } finally {
    clearing.value = false
  }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    await store.dispatch('site/updateName', form.siteName.trim())
    ElMessage.success('系统名称已保存')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.site-settings-page {
  max-width: 100%;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .hint {
    font-size: 13px;
    color: var(--el-text-color-secondary);
    font-weight: normal;
    line-height: 1.45;
  }
}

.settings-form {
  margin-top: 8px;
  max-width: 100%;

  :deep(.el-form-item__content) {
    flex: 1;
    min-width: 0;
  }
}

.site-name-input {
  width: 100%;
  max-width: 520px;
}

.logo-block {
  display: flex;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 20px;
  width: 100%;
}

.logo-preview {
  width: 88px;
  height: 88px;
  flex-shrink: 0;
  border-radius: 8px;
  background: var(--el-fill-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;

  img {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
  }
}

.logo-actions {
  flex: 1;
  min-width: 240px;
}

.logo-mode {
  margin-bottom: 12px;
}

.logo-panel {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 8px;
}

.url-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  width: 100%;
  max-width: 640px;
}

.logo-url-input {
  flex: 1;
  min-width: 200px;
}

.upload-tip {
  margin: 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.45;
}

.clear-btn {
  margin-top: 12px;
  padding-left: 0;
}

@media (max-width: 767px) {
  .settings-form {
    :deep(.el-form-item) {
      margin-bottom: 18px;
    }

    :deep(.el-form-item__label) {
      padding-bottom: 6px;
    }
  }

  .site-name-input {
    max-width: none;
  }

  .logo-block {
    flex-direction: column;
    gap: 12px;
  }

  .url-row {
    flex-direction: column;
    max-width: none;
  }

  .logo-url-input {
    width: 100%;
  }
}
</style>
