<template>
  <el-container class="layout-container">

    <!-- 桌面侧边栏 -->
    <el-aside v-if="!isMobile" :width="isCollapse ? '64px' : '200px'" class="aside">
      <div class="logo">
        <img :src="logoUrl" alt="logo" />
        <span v-if="!isCollapse">{{ siteName }}</span>
      </div>
      <AppSidebarMenu :collapse="isCollapse" />
    </el-aside>

    <!-- 移动端抽屉侧边栏 -->
    <el-drawer
        v-if="isMobile"
        v-model="drawerOpen"
        direction="ltr"
        :show-close="false"
        :with-header="false"
        size="220px"
        class="mobile-drawer"
    >
      <div class="logo drawer-logo">
        <img :src="logoUrl" alt="logo" />
        <span>{{ siteName }}</span>
      </div>
      <AppSidebarMenu @select="drawerOpen = false" />
    </el-drawer>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon v-if="isMobile" class="collapse-btn" @click="drawerOpen = true">
            <Expand />
          </el-icon>
          <el-icon v-else class="collapse-btn" @click="isCollapse = !isCollapse">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb v-if="!isMobile" separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
          <span v-else class="mobile-page-title">{{ currentTitle }}</span>
        </div>

        <div class="header-right">
          <!-- 桌面端：下拉菜单 -->
          <el-dropdown v-if="!isMobile" trigger="click" @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="28" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
              <span class="user-name">{{ userInfo?.realName || userInfo?.username }}</span>
              <span v-if="tenantName" class="tenant-name">{{ tenantName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>

          <!-- 移动端：底部操作栏，避免下拉闪退 -->
          <div v-else class="user-info mobile-avatar" @click.stop="userSheetOpen = true">
            <el-avatar :size="32" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
          </div>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <ProfileDialog v-model="profileVisible" />

    <el-drawer
        v-if="isMobile"
        v-model="userSheetOpen"
        direction="btt"
        :with-header="false"
        :size="sheetHeight"
        class="user-sheet-drawer"
        append-to-body
    >
      <div class="user-sheet">
        <div class="user-sheet-head">
          <el-avatar :size="44" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
          <div class="user-sheet-meta">
            <div class="user-sheet-name">{{ userInfo?.realName || userInfo?.username || '用户' }}</div>
            <div class="user-sheet-account">@{{ userInfo?.username }}</div>
          </div>
        </div>
        <div class="user-sheet-actions">
          <button type="button" class="sheet-btn" @click="openProfile">个人资料</button>
          <button type="button" class="sheet-btn danger" @click="confirmLogout">退出登录</button>
        </div>
        <button type="button" class="sheet-cancel" @click="userSheetOpen = false">取消</button>
      </div>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessageBox } from 'element-plus'
import { Expand, Fold, ArrowDown } from '@element-plus/icons-vue'
import AppSidebarMenu from '@/components/AppSidebarMenu.vue'
import ProfileDialog from '@/components/ProfileDialog.vue'

const router = useRouter()
const route = useRoute()
const store = useStore()

const isCollapse = ref(false)
const drawerOpen = ref(false)
const profileVisible = ref(false)
const userSheetOpen = ref(false)
const isMobile = ref(window.innerWidth < 768)
const sheetHeight = '220px'

const userInfo = computed(() => store.state.user.userInfo)
const tenantName = computed(() => store.state.tenant.tenantName)
const siteName = computed(() => store.state.site.siteName)
const logoUrl = computed(() => store.getters['site/logoUrl'])
const breadcrumbs = computed(() => route.matched.filter(item => item.meta.title))
const currentTitle = computed(() => {
  const last = route.matched.slice().reverse().find(r => r.meta?.title)
  return last?.meta?.title || ''
})

const onResize = () => {
  isMobile.value = window.innerWidth < 768
  if (!isMobile.value) {
    drawerOpen.value = false
    userSheetOpen.value = false
  }
}
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

const openProfile = () => {
  userSheetOpen.value = false
  profileVisible.value = true
}

const confirmLogout = () => {
  userSheetOpen.value = false
  handleLogout()
}

const handleCommand = (command) => {
  if (command === 'profile') profileVisible.value = true
  else if (command === 'logout') handleLogout()
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
    })
    store.dispatch('user/logout')
    router.push('/login')
  } catch { /* cancel */ }
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.aside {
  background-color: #304156;
  transition: width 0.3s;
  overflow: hidden;
  flex-shrink: 0;

  .logo {
    height: 56px;
    line-height: 56px;
    color: #fff;
    font-size: 15px;
    font-weight: bold;
    text-align: center;
    background-color: #2b3a4a;
    overflow: hidden;
    white-space: nowrap;

    img { width: 28px; height: 28px; vertical-align: middle; margin-right: 8px; }
  }
}

:deep(.mobile-drawer) {
  .el-drawer__body { padding: 0; background: #304156; }
}

.drawer-logo {
  height: 56px;
  line-height: 56px;
  color: #fff;
  font-size: 15px;
  font-weight: bold;
  text-align: center;
  background-color: #2b3a4a;
  img { width: 28px; height: 28px; vertical-align: middle; margin-right: 8px; }
}

:deep(.menu) {
  border-right: none;
  background-color: #304156;

  .el-menu-item,
  .el-sub-menu__title {
    color: #bfcbd9;
    &:hover { background-color: #263445; }
    &.is-active { color: #409eff; background-color: #263445; }
  }
  .el-sub-menu .el-menu-item {
    background-color: #1f2d3d;
    &:hover { background-color: #263445; }
  }
}

.header {
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 12px;
  height: 50px;
  flex-shrink: 0;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;
    min-width: 0;

    .collapse-btn {
      font-size: 20px;
      cursor: pointer;
      flex-shrink: 0;
    }

    .mobile-page-title {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    flex-shrink: 0;

    .user-info {
      display: flex;
      align-items: center;
      gap: 6px;
      cursor: pointer;
      .user-name { font-size: 14px; color: #303133; }
      .tenant-name {
        font-size: 12px;
        color: #909399;
        margin-left: 6px;
        padding-left: 6px;
        border-left: 1px solid #ebeef5;
      }
    }

    .mobile-avatar {
      padding: 4px;
      border-radius: 50%;
      -webkit-tap-highlight-color: transparent;

      &:active {
        background: #f5f7fa;
      }
    }
  }
}

:deep(.user-sheet-drawer) {
  .el-drawer__body {
    padding: 0;
  }
}

.user-sheet {
  padding: 16px 16px calc(16px + env(safe-area-inset-bottom));
}

.user-sheet-head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 4px 16px;
  border-bottom: 1px solid #f0f2f5;
  margin-bottom: 12px;
}

.user-sheet-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.user-sheet-account {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.user-sheet-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.sheet-btn {
  width: 100%;
  height: 44px;
  border: none;
  border-radius: 8px;
  background: #f5f7fa;
  color: #303133;
  font-size: 15px;
  cursor: pointer;

  &.danger {
    color: #f56c6c;
  }

  &:active {
    opacity: 0.85;
  }
}

.sheet-cancel {
  width: 100%;
  height: 44px;
  margin-top: 10px;
  border: none;
  border-radius: 8px;
  background: #fff;
  color: #909399;
  font-size: 15px;
  cursor: pointer;
}

.main {
  background-color: #f0f2f5;
  padding: 12px;
  overflow-y: auto;

  @media (min-width: 768px) {
    padding: 20px;
  }
}
</style>
