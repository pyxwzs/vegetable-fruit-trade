<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '200px'" class="aside">
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

        <el-sub-menu index="announcement">
          <template #title>
            <el-icon><Bell /></el-icon>
            <span>公告通知</span>
          </template>
          <el-menu-item index="/announcements">公告列表</el-menu-item>
          <el-menu-item v-if="hasPerm('announcement:manage')" index="/announcements/create">发布公告</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="showDataMenu" index="data">
          <template #title>
            <el-icon><DataLine /></el-icon>
            <span>数据管理</span>
          </template>
          <el-menu-item v-if="hasPerm('product:view') || hasPerm('product:create') || hasPerm('user:manage')" index="/data-management/products">商品管理</el-menu-item>
          <el-menu-item v-if="hasPerm('inventory:inbound') || hasPerm('inventory:outbound') || hasPerm('user:manage')" index="/data-management/inventory">库存管理</el-menu-item>
          <el-menu-item v-if="hasPerm('purchase:view')" index="/data-management/purchase">采购管理</el-menu-item>
          <el-menu-item v-if="hasPerm('sales:view')" index="/data-management/sales">销售管理</el-menu-item>
          <el-menu-item v-if="hasPerm('purchase:view')" index="/data-management/suppliers">供应商管理</el-menu-item>
          <el-menu-item v-if="hasPerm('sales:view')" index="/data-management/customers">客户管理</el-menu-item>
          <el-menu-item v-if="showReturnMenu" index="/data-management/return-requests">退货申请</el-menu-item>
        </el-sub-menu>

        <el-sub-menu v-if="showFinanceMenu" index="finance">
          <template #title>
            <el-icon><Wallet /></el-icon>
            <span>财务管理</span>
          </template>
          <el-menu-item v-if="hasPerm('purchase:pay') || hasPerm('sales:collect') || hasPerm('user:manage')" index="/data-management/finance-summary">资金汇总</el-menu-item>
          <el-menu-item v-if="hasPerm('purchase:pay')" index="/data-management/purchase">采购付款</el-menu-item>
          <el-menu-item v-if="hasPerm('sales:collect')" index="/data-management/sales">销售收款</el-menu-item>
          <el-menu-item v-if="hasPerm('purchase:pay') || hasPerm('sales:collect')" index="/data-management/return-requests">退货审批</el-menu-item>
        </el-sub-menu>

        <el-menu-item v-if="hasPerm('user:manage')" index="/analysis">
          <el-icon><PieChart /></el-icon>
          <span>经营分析</span>
        </el-menu-item>

        <el-sub-menu v-if="showSystemMenu" index="system">
          <template #title>
            <el-icon><Setting /></el-icon>
            <span>系统管理</span>
          </template>
          <el-menu-item v-if="hasPerm('user:manage')" index="/system/users">用户管理</el-menu-item>
          <el-menu-item v-if="hasPerm('log:view')" index="/system/logs">日志审计</el-menu-item>
          <el-menu-item v-if="hasPerm('file:manage')" index="/system/files">文件管理</el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="toggleCollapse">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
              {{ item.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" class="notification">
            <el-icon @click="showNotifications"><Bell /></el-icon>
          </el-badge>

          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" :src="userAvatar" />
              <span>{{ userInfo?.realName || userInfo?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
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
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessageBox } from 'element-plus'
import {
  Odometer,
  Bell,
  DataLine,
  PieChart,
  Setting,
  Expand,
  Fold,
  ArrowDown,
  Wallet
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const store = useStore()

const isCollapse = ref(false)
const unreadCount = ref(0)

const userInfo = computed(() => store.state.user.userInfo)
const userAvatar = computed(() => 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png')

/** 与后端 /auth/me 中 roles[].permissions[].code 一致；多角色时 Vuex 已合并为并集 */
const hasPerm = (code) => store.getters['user/hasPermission'](code)

/**
 * 数据管理菜单：只对有数据操作权限的角色显示（采购员、销售员、仓管员、管理员）。
 * 刻意排除财务角色：财务仅有 purchase:view / sales:view / pay / collect，
 * 没有 product:view / create / approve / inbound 等，因此不会出现在此菜单。
 */
const showDataMenu = computed(
  () =>
    hasPerm('product:view') ||
    hasPerm('purchase:create') ||
    hasPerm('purchase:approve') ||
    hasPerm('sales:create') ||
    hasPerm('sales:approve') ||
    hasPerm('inventory:inbound') ||
    hasPerm('inventory:outbound')
)

const showFinanceMenu = computed(() => hasPerm('purchase:pay') || hasPerm('sales:collect'))

const showReturnMenu = computed(
  () =>
    hasPerm('purchase:create') || hasPerm('purchase:approve') ||
    hasPerm('sales:create') || hasPerm('sales:approve') ||
    hasPerm('inventory:inbound') || hasPerm('inventory:outbound') ||
    hasPerm('purchase:pay') || hasPerm('sales:collect')
)

const showSystemMenu = computed(
  () => hasPerm('user:manage') || hasPerm('log:view') || hasPerm('file:manage')
)

const activeMenu = computed(() => route.path)

const breadcrumbs = computed(() => {
  return route.matched.filter(item => item.meta.title)
})

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleCommand = (command) => {
  switch (command) {
    case 'profile':
      router.push('/profile')
      break
    case 'password':
      // 修改密码
      break
    case 'logout':
      handleLogout()
      break
  }
}

const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    store.dispatch('user/logout')
    router.push('/login')
  } catch (error) {
    // 取消退出
  }
}

const showNotifications = () => {
  // 显示通知中心
}

onMounted(() => {
  // 获取未读通知数量
})
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;

  .aside {
    background-color: #304156;
    transition: width 0.3s;
    overflow: hidden;

    .logo {
      height: 60px;
      line-height: 60px;
      color: #fff;
      font-size: 16px;
      font-weight: bold;
      text-align: center;
      background-color: #2b3a4a;
      overflow: hidden;
      white-space: nowrap;

      img {
        width: 32px;
        height: 32px;
        vertical-align: middle;
        margin-right: 10px;
      }
    }

    .menu {
      border-right: none;
      background-color: #304156;

      :deep(.el-menu-item),
      :deep(.el-sub-menu__title) {
        color: #bfcbd9;

        &:hover {
          background-color: #263445;
        }

        &.is-active {
          color: #409eff;
          background-color: #263445;
        }
      }
    }
  }

  .header {
    background-color: #fff;
    border-bottom: 1px solid #e6e6e6;
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 20px;

    .header-left {
      display: flex;
      align-items: center;

      .collapse-btn {
        font-size: 20px;
        margin-right: 20px;
        cursor: pointer;
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      gap: 20px;

      .notification {
        cursor: pointer;
      }

      .user-info {
        display: flex;
        align-items: center;
        gap: 8px;
        cursor: pointer;
      }
    }
  }

  .main {
    background-color: #f0f2f5;
    padding: 20px;
  }
}
</style>