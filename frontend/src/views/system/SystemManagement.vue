<template>
  <div class="system-management">
    <el-row :gutter="20">
      <el-col :span="8" v-for="item in modules" :key="item.path">
        <el-card class="module-card" :body-style="{ padding: '20px' }" @click="$router.push(item.path)">
          <div class="module-content">
            <el-icon :size="40" :color="item.color">
              <component :is="item.icon" />
            </el-icon>
            <div class="module-info">
              <div class="module-title">{{ item.title }}</div>
              <div class="module-desc">{{ item.desc }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <router-view />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { User, Document, Folder, Setting } from '@element-plus/icons-vue'

const modules = ref([
  {
    title: '用户管理',
    desc: '用户账号、角色权限管理',
    icon: User,
    color: '#409EFF',
    path: '/system/users'
  },
  {
    title: '日志审计',
    desc: '操作日志、安全审计',
    icon: Document,
    color: '#67C23A',
    path: '/system/logs'
  },
  {
    title: '文件管理',
    desc: '文件上传、下载、管理',
    icon: Folder,
    color: '#E6A23C',
    path: '/system/files'
  },
  {
    title: '系统配置',
    desc: '系统参数、基础数据配置',
    icon: Setting,
    color: '#909399',
    path: '/system/config'
  }
])
</script>

<style scoped lang="scss">
.system-management {
  .module-card {
    margin-bottom: 20px;
    cursor: pointer;
    transition: all 0.3s;

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
    }

    .module-content {
      display: flex;
      align-items: center;
      gap: 20px;

      .module-info {
        .module-title {
          font-size: 18px;
          font-weight: bold;
          margin-bottom: 8px;
        }

        .module-desc {
          font-size: 14px;
          color: #909399;
        }
      }
    }
  }
}
</style>