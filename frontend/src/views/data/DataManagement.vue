<template>
  <div class="data-management">
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in modules" :key="item.path">
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
import {
  Goods,
  Box,
  ShoppingCart,
  Sell,
  Van,  // 使用 Van 代替 Truck（货车图标）
  User
} from '@element-plus/icons-vue'

const modules = ref([
  {
    title: '商品管理',
    desc: '商品信息维护、分类管理',
    icon: Goods,
    color: '#409EFF',
    path: '/data-management/products'
  },
  {
    title: '库存管理',
    desc: '库存查询、入库出库、盘点',
    icon: Box,
    color: '#67C23A',
    path: '/data-management/inventory'
  },
  {
    title: '采购管理',
    desc: '采购订单、供应商管理',
    icon: ShoppingCart,
    color: '#E6A23C',
    path: '/data-management/purchase'
  },
  {
    title: '销售管理',
    desc: '销售订单、客户管理',
    icon: Sell,
    color: '#F56C6C',
    path: '/data-management/sales'
  },
  {
    title: '供应商管理',
    desc: '供应商信息、信用评估',
    icon: Van,
    color: '#909399',
    path: '/data-management/suppliers'
  },
  {
    title: '客户管理',
    desc: '客户信息、信用等级',
    icon: User,
    color: '#722ed1',
    path: '/data-management/customers'
  }
])
</script>

<style scoped lang="scss">
.data-management {
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