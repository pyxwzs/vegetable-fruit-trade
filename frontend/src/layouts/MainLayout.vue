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

        <el-sub-menu index="data">
          <template #title>
            <el-icon><DataLine /></el-icon>
            <span>数据管理</span>
          </template>
          <el-menu-item index="/data-management/products">商品管理</el-menu-item>
          <el-menu-item index="/data-management/inventory">库存管理</el-menu-item>
          <el-menu-item index="/data-management/purchase">采购管理</el-menu-item>
          <el-menu-item index="/data-management/sales">销售管理</el-menu-item>
          <el-menu-item index="/data-management/suppliers">供应商管理</el-menu-item>
          <el-menu-item index="/data-management/customers">客户管理</el-menu-item>
        </el-sub-menu>

        <el-menu-item index="/analysis">
          <el-icon><PieChart /></el-icon>
          <span>经营分析</span>
        </el-menu-item>
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
          <el-dropdown @command="handleCommand">
            <div class="user-info">
              <el-avatar :size="32" src="https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png" />
              <span>{{ userInfo?.realName || userInfo?.username }}</span>
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
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { ElMessageBox } from 'element-plus'
import {
  Odometer,
  DataLine,
  PieChart,
  Expand,
  Fold,
  ArrowDown
} from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const store = useStore()

const isCollapse = ref(false)
const userInfo = computed(() => store.state.user.userInfo)
const activeMenu = computed(() => route.path)
const breadcrumbs = computed(() => route.matched.filter(item => item.meta.title))

const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

const handleCommand = (command) => {
  if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'logout') {
    handleLogout()
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
  } catch {
    // 取消退出
  }
}
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
