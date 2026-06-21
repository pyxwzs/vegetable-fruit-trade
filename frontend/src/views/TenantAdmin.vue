<template>
  <div class="tenant-admin-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>租户管理</span>
          <span class="hint">生成一次性邀请码，用户在小程序注册页手动填写</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="租户列表" name="tenants">
          <el-table :data="tenants" v-loading="loadingTenants" border style="width: 100%">
            <el-table-column prop="code" label="租户编号" width="140" />
            <el-table-column prop="name" label="租户名称" min-width="160" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
                  {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createTime" label="创建时间" width="170" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button
                    v-if="row.id !== 1"
                    link
                    :type="row.status === 'ACTIVE' ? 'danger' : 'primary'"
                    @click="toggleTenant(row)"
                >
                  {{ row.status === 'ACTIVE' ? '停用' : '启用' }}
                </el-button>
                <span v-else class="muted">系统默认</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="邀请码" name="invites">
          <div class="invite-actions">
            <el-input
                v-model="inviteRemark"
                placeholder="备注（选填）"
                style="max-width: 280px"
                clearable
            />
            <el-button type="primary" :loading="generating" @click="handleGenerate">
              生成邀请码
            </el-button>
          </div>

          <el-table :data="invites" v-loading="loadingInvites" border style="width: 100%">
            <el-table-column prop="code" label="邀请码" width="150">
              <template #default="{ row }">
                <span class="code-text">{{ row.code }}</span>
                <el-button link type="primary" @click="copyText(row.code, '邀请码')">复制</el-button>
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.status === 'UNUSED' ? 'success' : 'info'">
                  {{ row.status === 'UNUSED' ? '未使用' : '已使用' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="remark" label="备注" min-width="120" show-overflow-tooltip />
            <el-table-column prop="usedByTenantName" label="使用租户" min-width="140" />
            <el-table-column prop="createTime" label="创建时间" width="170" />
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="inviteDialogVisible" title="邀请码已生成" width="480px">
      <div v-if="latestInvite" class="invite-result">
        <div class="result-row">
          <span class="label">邀请码</span>
          <span class="value code-text">{{ latestInvite.code }}</span>
          <el-button link type="primary" @click="copyText(latestInvite.code, '邀请码')">复制</el-button>
        </div>
        <p class="tip">一次性使用，请将邀请码发给用户在小程序「邀请码注册」页填写</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateInviteCode, listInviteCodes, listTenants, updateTenantStatus } from '@/api/admin'
import { usePageRefresh } from '@/composables/usePageRefresh'

const activeTab = ref('tenants')
const tenants = ref([])
const invites = ref([])
const loadingTenants = ref(false)
const loadingInvites = ref(false)
const generating = ref(false)
const inviteRemark = ref('')
const inviteDialogVisible = ref(false)
const latestInvite = ref(null)

async function loadTenants() {
  loadingTenants.value = true
  try {
    const res = await listTenants()
    tenants.value = res.data || []
  } finally {
    loadingTenants.value = false
  }
}

async function loadInvites() {
  loadingInvites.value = true
  try {
    const res = await listInviteCodes()
    invites.value = res.data || []
  } finally {
    loadingInvites.value = false
  }
}

async function toggleTenant(row) {
  const next = row.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE'
  const action = next === 'DISABLED' ? '停用' : '启用'
  try {
    await ElMessageBox.confirm(`确定${action}租户「${row.name}」？`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await updateTenantStatus(row.id, next)
  ElMessage.success(`已${action}`)
  await loadTenants()
}

async function handleGenerate() {
  generating.value = true
  try {
    const res = await generateInviteCode(inviteRemark.value.trim() || undefined)
    latestInvite.value = res.data
    inviteRemark.value = ''
    inviteDialogVisible.value = true
    await loadInvites()
    activeTab.value = 'invites'
  } finally {
    generating.value = false
  }
}

async function copyText(text, label) {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(`已复制${label}`)
  } catch {
    ElMessage.info(text)
  }
}

watch(activeTab, (tab) => {
  if (tab === 'tenants') loadTenants()
  else loadInvites()
})

onMounted(() => {
  loadTenants()
})

usePageRefresh(() => {
  if (activeTab.value === 'tenants') return loadTenants()
  return loadInvites()
})
</script>

<style scoped lang="scss">
.tenant-admin-page {
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
  }
}

.invite-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.code-text {
  font-family: monospace;
  font-weight: 600;
  margin-right: 8px;
}

.muted {
  color: var(--el-text-color-secondary);
  font-size: 13px;
}

.invite-result {
  display: flex;
  flex-direction: column;
  gap: 14px;

  .result-row {
    display: flex;
    align-items: flex-start;
    gap: 8px;
    flex-wrap: wrap;
  }

  .label {
    flex: 0 0 72px;
    color: var(--el-text-color-secondary);
  }

  .value {
    flex: 1;
    min-width: 0;
    word-break: break-all;
  }

  .tip {
    margin: 0;
    font-size: 13px;
    color: var(--el-text-color-secondary);
    line-height: 1.5;
  }
}
</style>
