<template>
  <div class="menu-settings-page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>菜单设置</span>
          <span class="hint">{{ isMobile ? '点击分组展开子菜单，可收起；勾选控制显示/隐藏' : '从左到右展开：左侧为分组，右侧为子菜单；可整组禁用，也可单独禁用某项' }}</span>
        </div>
      </template>

      <div class="actions">
        <el-button size="small" @click="selectAll">全部启用</el-button>
        <el-button size="small" @click="selectNone">全部关闭</el-button>
        <el-button v-if="isMobile" size="small" @click="expandAllMobile">全部展开</el-button>
        <el-button v-if="isMobile" size="small" @click="collapseAllMobile">全部收起</el-button>
      </div>

      <div class="menu-forest">
        <div
            v-for="group in MENU_GROUPS"
            :key="group.key"
            class="menu-branch"
            :class="{
              'is-single': group.type === 'item',
              'is-expanded': group.type === 'group' && isExpanded(group.key)
            }"
        >
          <div
              class="branch-parent"
              :class="{ 'can-expand': isMobile && group.type === 'group' }"
              @click="onParentRowClick(group)"
          >
            <el-checkbox
                :model-value="isGroupChecked(group)"
                :indeterminate="isGroupIndeterminate(group)"
                @click.stop
                @change="(v) => toggleGroup(group, v)"
            >
              <span class="parent-label">{{ group.title }}</span>
            </el-checkbox>
            <el-icon
                v-if="isMobile && group.type === 'group'"
                class="expand-icon"
                :class="{ rotated: isExpanded(group.key) }"
            >
              <ArrowDown />
            </el-icon>
          </div>

          <template v-if="group.type === 'group' && showChildren(group)">
            <div class="branch-connector" aria-hidden="true">
              <span class="connector-line" />
              <span class="connector-arrow">›</span>
            </div>
            <div class="branch-children">
              <el-checkbox
                  v-for="child in group.children"
                  :key="child.menuKey"
                  :model-value="isChecked(child.menuKey)"
                  @click.stop
                  @change="(v) => toggleKey(child.menuKey, v)"
              >
                {{ child.title }}
              </el-checkbox>
            </div>
          </template>
        </div>
      </div>

      <div class="footer">
        <el-button type="primary" :loading="saving" @click="handleSave">保存设置</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useStore } from 'vuex'
import { ElMessage } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import { ALL_MENU_KEYS, MENU_GROUPS } from '@/config/menu'
import { updateMenuKeys } from '@/api/user'
import { useIsMobile } from '@/composables/useIsMobile'

const store = useStore()
const isMobile = useIsMobile()
const saving = ref(false)
const selectedKeys = ref([...ALL_MENU_KEYS])
const expandedKeys = ref([])

watch(
    () => store.state.user.userInfo?.menuKeys,
    (keys) => {
      selectedKeys.value = keys?.length ? [...keys] : [...ALL_MENU_KEYS]
    },
    { immediate: true }
)

const groupKeys = MENU_GROUPS.filter(g => g.type === 'group').map(g => g.key)

const isExpanded = (key) => expandedKeys.value.includes(key)

const showChildren = (group) => {
  if (group.type !== 'group') return false
  if (!isMobile.value) return true
  return isExpanded(group.key)
}

const toggleExpand = (key) => {
  if (isExpanded(key)) {
    expandedKeys.value = expandedKeys.value.filter(k => k !== key)
  } else {
    expandedKeys.value = [...expandedKeys.value, key]
  }
}

const onParentRowClick = (group) => {
  if (isMobile.value && group.type === 'group') {
    toggleExpand(group.key)
  }
}

const expandAllMobile = () => {
  expandedKeys.value = [...groupKeys]
}

const collapseAllMobile = () => {
  expandedKeys.value = []
}

const groupChildKeys = (group) =>
  group.type === 'item' ? [group.menuKey] : group.children.map(c => c.menuKey)

const isChecked = (key) => selectedKeys.value.includes(key)

const isGroupChecked = (group) =>
  groupChildKeys(group).every(k => selectedKeys.value.includes(k))

const isGroupIndeterminate = (group) => {
  const keys = groupChildKeys(group)
  const n = keys.filter(k => selectedKeys.value.includes(k)).length
  return n > 0 && n < keys.length
}

const toggleGroup = (group, checked) => {
  const keys = groupChildKeys(group)
  if (checked) {
    selectedKeys.value = [...new Set([...selectedKeys.value, ...keys])]
  } else {
    selectedKeys.value = selectedKeys.value.filter(k => !keys.includes(k))
  }
}

const toggleKey = (key, checked) => {
  if (checked) {
    if (!selectedKeys.value.includes(key)) {
      selectedKeys.value = [...selectedKeys.value, key]
    }
  } else {
    selectedKeys.value = selectedKeys.value.filter(k => k !== key)
  }
}

const selectAll = () => {
  selectedKeys.value = [...ALL_MENU_KEYS]
}

const selectNone = () => {
  selectedKeys.value = []
}

const handleSave = async () => {
  const menuKeys = selectedKeys.value.filter(k => ALL_MENU_KEYS.includes(k))
  if (menuKeys.length === 0) {
    ElMessage.warning('请至少启用一项功能菜单')
    return
  }
  saving.value = true
  try {
    const res = await updateMenuKeys({ menuKeys })
    store.commit('user/SET_USER_INFO', res.data)
    selectedKeys.value = res.data?.menuKeys?.length ? [...res.data.menuKeys] : [...ALL_MENU_KEYS]
    ElMessage.success('菜单设置已保存')
  } catch {
    /* 错误由拦截器提示 */
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.menu-settings-page {
  max-width: 100%;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 4px;

  .hint {
    font-size: 13px;
    color: #909399;
    font-weight: normal;
    line-height: 1.45;
  }
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.menu-forest {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.menu-branch {
  display: flex;
  align-items: stretch;
  min-height: 72px;
  background: #fafafa;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: hidden;

  &.is-single {
    max-width: 320px;
  }
}

.branch-parent {
  flex: 0 0 168px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 16px 18px;
  background: linear-gradient(180deg, #f5f7fa 0%, #eef1f6 100%);
  border-right: 1px solid #e4e7ed;

  :deep(.el-checkbox) {
    height: auto;
    align-items: center;
    flex: 1;
    min-width: 0;
  }

  .parent-label {
    font-size: 15px;
    font-weight: 600;
    color: #303133;
  }
}

.expand-icon {
  flex-shrink: 0;
  font-size: 16px;
  color: #909399;
  transition: transform 0.2s ease;

  &.rotated {
    transform: rotate(180deg);
  }
}

.branch-connector {
  flex: 0 0 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  background: #fff;

  .connector-line {
    position: absolute;
    left: 0;
    top: 50%;
    width: 100%;
    height: 2px;
    background: linear-gradient(90deg, #dcdfe6 0%, #c0c4cc 100%);
    transform: translateY(-50%);
  }

  .connector-arrow {
    position: relative;
    z-index: 1;
    width: 28px;
    height: 28px;
    line-height: 26px;
    text-align: center;
    border-radius: 50%;
    background: #fff;
    border: 1px solid #dcdfe6;
    color: #909399;
    font-size: 18px;
    font-weight: 600;
  }
}

.branch-children {
  flex: 1;
  display: flex;
  flex-wrap: wrap;
  align-content: center;
  align-items: center;
  gap: 12px 28px;
  padding: 16px 24px;
  background: #fff;

  :deep(.el-checkbox) {
    margin-right: 0;
    height: auto;
  }

  :deep(.el-checkbox__label) {
    font-size: 14px;
    color: #606266;
  }
}

.footer {
  margin-top: 20px;
}

@media (min-width: 768px) {
  .branch-parent {
    flex: 0 0 180px;
  }

  .branch-children {
    gap: 14px 36px;
    padding: 18px 32px;
  }
}

@media (max-width: 767px) {
  .menu-branch {
    flex-direction: column;
    min-height: 0;

    &.is-single {
      max-width: none;
    }
  }

  .branch-parent {
    flex: none;
    width: 100%;
    border-right: none;
    padding: 14px 16px;

    &.can-expand {
      cursor: pointer;
      user-select: none;

      &:active {
        background: #ebeef5;
      }
    }
  }

  .branch-connector {
    display: none;
  }

  .branch-children {
    flex: none;
    width: 100%;
    flex-direction: column;
    align-items: stretch;
    flex-wrap: nowrap;
    gap: 0;
    padding: 0 16px 12px 40px;
    background: #fff;
    border-top: 1px solid #f0f2f5;

    :deep(.el-checkbox) {
      display: flex;
      align-items: center;
      width: 100%;
      min-height: 44px;
      margin: 0;
      padding: 8px 0;
      border-bottom: 1px solid #f5f5f5;

      &:last-child {
        border-bottom: none;
        padding-bottom: 4px;
      }
    }

    :deep(.el-checkbox__label) {
      font-size: 14px;
      line-height: 1.45;
      padding-left: 10px;
    }
  }
}
</style>
