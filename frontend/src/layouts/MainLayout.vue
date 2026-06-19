<template>
  <el-container class="layout-container">

    <!-- 桌面侧边栏 -->
    <el-aside v-if="!isMobile" :width="isCollapse ? '64px' : '200px'" class="aside">
      <div class="logo">
        <img src="@/assets/logo.png" alt="logo" />
        <span v-if="!isCollapse">果蔬批发</span>
      </div>
      <el-menu
          :default-active="activeMenu"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          class="menu"
      >
        <el-menu-item index="/">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>

        <el-sub-menu index="trade">
          <template #title>
            <el-icon><ShoppingCart /></el-icon>
            <span>进销管理</span>
          </template>
          <el-menu-item index="/purchase">采购管理</el-menu-item>
          <el-menu-item index="/sales">销售管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="stock">
          <template #title>
            <el-icon><Box /></el-icon>
            <span>商品库存</span>
          </template>
          <el-menu-item index="/products">商品管理</el-menu-item>
          <el-menu-item index="/inventory">库存查看</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="partner">
          <template #title>
            <el-icon><UserFilled /></el-icon>
            <span>往来管理</span>
          </template>
          <el-menu-item index="/suppliers">供应商管理</el-menu-item>
          <el-menu-item index="/customers">客户管理</el-menu-item>
          <el-menu-item index="/expenses">支出记录</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="reconcile">
          <template #title>
            <el-icon><Calendar /></el-icon>
            <span>月度对账</span>
          </template>
                    <el-menu-item index="/monthly-report">农户/客户对账</el-menu-item>
        </el-sub-menu>
      </el-menu>
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
        <img src="@/assets/logo.png" alt="logo" />
        <span>果蔬批发</span>
      </div>
      <el-menu
          :default-active="activeMenu"
          :collapse="false"
          :collapse-transition="false"
          router
          class="menu"
          @select="drawerOpen = false"
      >
        <el-menu-item index="/">
          <el-icon><Odometer /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>

        <el-sub-menu index="trade">
          <template #title>
            <el-icon><ShoppingCart /></el-icon>
            <span>进销管理</span>
          </template>
          <el-menu-item index="/purchase">采购管理</el-menu-item>
          <el-menu-item index="/sales">销售管理</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="stock">
          <template #title>
            <el-icon><Box /></el-icon>
            <span>商品库存</span>
          </template>
          <el-menu-item index="/products">商品管理</el-menu-item>
          <el-menu-item index="/inventory">库存查看</el-menu-item>
        </el-sub-menu>

        <el-sub-menu index="partner">
          <template #title>
            <el-icon><UserFilled /></el-icon>
            <span>往来管理</span>
          </template>
          <el-menu-item index="/suppliers">供应商管理</el-menu-item>
          <el-menu-item index="/customers">客户管理</el-menu-item>
          <el-menu-item index="/expenses">支出记录</el-menu-item>
        </el-sub-menu>
        <el-sub-menu index="reconcile">
          <template #title>
            <el-icon><Calendar /></el-icon>
            <span>月度对账</span>
          </template>
                    <el-menu-item index="/monthly-report">农户/客户对账</el-menu-item>
        </el-sub-menu>
      </el-menu>
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
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="28" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
              <span v-if="!isMobile" class="user-name">{{ userInfo?.realName || userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessageBox } from 'element-plus'
import {
  Odometer, ShoppingCart, Box, UserFilled, Calendar,
  Expand, Fold, ArrowDown
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const store = useStore()

const isCollapse = ref(false)
const drawerOpen = ref(false)
const isMobile = ref(window.innerWidth < 768)

const userInfo = computed(() => store.state.user.userInfo)
const activeMenu = computed(() => route.path)
const breadcrumbs = computed(() => route.matched.filter(item => item.meta.title))
const currentTitle = computed(() => {
  const last = route.matched.slice().reverse().find(r => r.meta?.title)
  return last?.meta?.title || ''
})

const onResize = () => {
  isMobile.value = window.innerWidth < 768
  if (!isMobile.value) drawerOpen.value = false
}
onMounted(() => window.addEventListener('resize', onResize))
onUnmounted(() => window.removeEventListener('resize', onResize))

const handleCommand = (command) => {
  if (command === 'profile') router.push('/profile')
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
  height: calc(100vh - 56px);
  overflow-y: auto;

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
    }
  }
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
