<template>
  <el-container class="platform-layout">
    <el-header class="header">
      <div class="brand">
        <span class="brand-title">平台管理后台</span>
        <span class="brand-sub">租户与邀请码</span>
      </div>
      <div class="header-right">
        <span class="user-name">{{ userInfo?.realName || userInfo?.username }}</span>
        <el-button link type="primary" @click="handleLogout">退出</el-button>
      </div>
    </el-header>
    <el-main class="main">
      <router-view />
    </el-main>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessageBox } from 'element-plus'

const router = useRouter()
const store = useStore()
const userInfo = computed(() => store.state.user.userInfo)

const handleLogout = async () => {
  await ElMessageBox.confirm('确定退出登录？', '提示', { type: 'warning' })
  store.dispatch('user/logout')
  router.push('/platform/login')
}
</script>

<style scoped lang="scss">
.platform-layout {
  min-height: 100vh;
  background: #f5f7fa;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  height: 56px;
}

.brand-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-right: 12px;
}

.brand-sub {
  font-size: 13px;
  color: #909399;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  color: #606266;
  font-size: 14px;
}

.main {
  padding: 20px;
}
</style>
