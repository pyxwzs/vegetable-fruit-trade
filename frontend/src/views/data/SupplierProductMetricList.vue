<template>
  <div class="metric-page">
    <el-tabs v-model="activeTab" class="metric-tabs" @tab-change="onTabChange">
      <el-tab-pane label="完成情况" name="completion">
        <SupplierMetricCompletionPanel ref="completionRef" />
      </el-tab-pane>
      <el-tab-pane label="订量设置" name="manage">
        <SupplierMetricManagePanel ref="manageRef" @changed="refreshCompletion" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import SupplierMetricCompletionPanel from './SupplierMetricCompletionPanel.vue'
import SupplierMetricManagePanel from './SupplierMetricManagePanel.vue'
import { usePageRefresh } from '@/composables/usePageRefresh'

const activeTab = ref('completion')
const completionRef = ref(null)
const manageRef = ref(null)

const refreshCompletion = () => completionRef.value?.reload?.()
const refreshManage = () => manageRef.value?.reload?.()

const onTabChange = (name) => {
  if (name === 'completion') refreshCompletion()
  else if (name === 'manage') refreshManage()
}

usePageRefresh(() => {
  if (activeTab.value === 'completion') refreshCompletion()
  else refreshManage()
})
</script>

<style scoped lang="scss">
.metric-page {
  :deep(.metric-tabs > .el-tabs__header) {
    margin-bottom: 12px;
    padding: 0 4px;
  }

  :deep(.el-tabs__content) {
    padding: 0 4px 8px;
  }

  @media (max-width: 767px) {
    :deep(.el-tabs__item) {
      padding: 0 14px;
    }

    :deep(.el-tabs__content) {
      padding: 0 0 8px;
    }
  }
}
</style>
