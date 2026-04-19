<template>
  <div class="dashboard">
    <!-- 日报摘要条 -->
    <el-alert
        v-if="dailyReport"
        type="info"
        show-icon
        :closable="false"
        class="daily-strip"
        :title="`经营日报（${reportDate}）`"
    >
      <template #default>
        <span class="daily-meta">
          <template v-if="hasPerm('sales:view')">
            今日出库销售额 <strong>¥{{ fmtMoney(dailyReport.realizedSales) }}</strong>
            ｜ 新销售单 {{ dailyReport.newOrderCount }} 笔
          </template>
          <template v-if="hasPerm('purchase:view')">
            <template v-if="hasPerm('sales:view')">｜ </template>
            新采购单 {{ dailyReport.newPurchaseOrderCount }} 笔
            ｜ 待审采购 {{ dailyReport.pendingPurchaseCount }}
          </template>
          <template v-if="hasPerm('sales:view')">
            ｜ 待审销售 {{ dailyReport.pendingSalesCount }}
          </template>
          <template v-if="hasInventoryPerm">
            ｜ 库存预警 {{ dailyReport.lowStockLineCount }} 条
            ｜ 临期 {{ dailyReport.expiringSkuCount }} 条
          </template>
          <template v-if="hasPerm('sales:view')">
            ｜ 授信预警 {{ dailyReport.creditWarningCount }} 户
          </template>
        </span>
      </template>
    </el-alert>

    <!-- 统计卡片 -->
    <el-row :gutter="20">
      <el-col :span="6" v-for="item in statistics" :key="item.title">
        <el-card class="stat-card" :body-style="{ padding: '20px' }">
          <div class="stat-item">
            <div class="stat-info">
              <div class="stat-title">{{ item.title }}</div>
              <div class="stat-value">{{ item.value }}</div>
            </div>
            <div class="stat-icon" :style="{ background: item.color + '20' }">
              <el-icon :size="32" :color="item.color">
                <component :is="item.icon" />
              </el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表 + 预警 -->
    <el-row :gutter="20" class="chart-row">
      <!-- 销售趋势：仅有销售权限才显示 -->
      <el-col :span="hasInventoryPerm ? 16 : 24" v-if="hasPerm('sales:view')">
        <el-card>
          <template #header>
            <span>销售趋势（近7日出库额）</span>
          </template>
          <div ref="salesChart" style="height: 300px"></div>
        </el-card>
      </el-col>

      <!-- 库存预警：有库存权限才显示 -->
      <el-col :span="hasPerm('sales:view') ? 8 : 24" v-if="hasInventoryPerm">
        <el-card>
          <template #header>
            <span>库存预警</span>
          </template>
          <div class="warning-list">
            <div v-for="item in warnings" :key="item.id" class="warning-item">
              <div class="warning-info">
                <div class="warning-name">{{ item.productName }}</div>
                <div class="warning-desc">
                  <el-tag :type="item.type" size="small">{{ item.message }}</el-tag>
                </div>
              </div>
            </div>
            <el-empty v-if="warnings.length === 0" description="暂无预警" />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 待办订单表格 -->
    <el-row :gutter="20" class="table-row" v-if="hasPerm('purchase:view') || hasPerm('sales:view')">
      <!-- 采购待办 -->
      <el-col
          :span="hasPerm('purchase:view') && hasPerm('sales:view') ? 12 : 24"
          v-if="hasPerm('purchase:view')"
      >
        <el-card>
          <template #header>
            <div class="card-header">
              <span>待办采购订单</span>
              <el-button type="primary" link @click="$router.push('/data-management/purchase')">
                查看更多
              </el-button>
            </div>
          </template>
          <el-table :data="pendingPurchases" style="width: 100%">
            <el-table-column prop="orderNo" label="订单号" width="170" />
            <el-table-column label="供应商" min-width="120">
              <template #default="{ row }">{{ row.supplier?.name || '—' }}</template>
            </el-table-column>
            <el-table-column label="金额">
              <template #default="{ row }">¥{{ Number(row.totalAmount || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 销售待办 -->
      <el-col
          :span="hasPerm('purchase:view') && hasPerm('sales:view') ? 12 : 24"
          v-if="hasPerm('sales:view')"
      >
        <el-card>
          <template #header>
            <div class="card-header">
              <span>待处理销售订单</span>
              <el-button type="primary" link @click="$router.push('/data-management/sales')">
                查看更多
              </el-button>
            </div>
          </template>
          <el-table :data="pendingSales" style="width: 100%">
            <el-table-column prop="orderNo" label="订单号" width="170" />
            <el-table-column label="客户" min-width="120">
              <template #default="{ row }">{{ row.customer?.name || '—' }}</template>
            </el-table-column>
            <el-table-column label="金额">
              <template #default="{ row }">¥{{ Number(row.totalAmount || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态">
              <template #default="{ row }">
                <el-tag :type="getStatusType(row.status)">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useStore } from 'vuex'
import * as echarts from 'echarts'
import {
  ShoppingCart,
  Money,
  Goods,
  User,
  WarningFilled,
  Clock
} from '@element-plus/icons-vue'
import { getExpiringProducts, getLowStockProducts } from '@/api/inventory'
import { getPurchaseOrders } from '@/api/purchase'
import { getSalesOrders } from '@/api/sales'
import { getHomeSummary } from '@/api/analytics'

const store = useStore()
const hasPerm = (code) => store.getters['user/hasPermission'](code)

const hasInventoryPerm = computed(
  () => hasPerm('inventory:view') || hasPerm('inventory:inbound') || hasPerm('inventory:outbound') || hasPerm('user:manage')
)

// ── 原始数据 ──────────────────────────────────────────
const homeData = ref(null)
const dailyReport = ref(null)
const weekTrend = ref([])
const warnings = ref([])
const pendingPurchases = ref([])
const pendingSales = ref([])

const reportDate = computed(() => dailyReport.value?.date || '')
const fmtMoney = (v) => (v != null ? Number(v).toFixed(2) : '0.00')
const mk = (title, value, icon, color) => ({ title, value: String(value ?? 0), icon, color })

// ── 角色自适应统计卡片 ────────────────────────────────
const statistics = computed(() => {
  const s = homeData.value
  const dr = s?.dailyReport
  if (!dr) {
    return [
      mk('今日销售额', '¥ 0', Money, '#409EFF'),
      mk('今日采购新单', 0, ShoppingCart, '#67C23A'),
      mk('库存商品数', 0, Goods, '#E6A23C'),
      mk('启用客户数', 0, User, '#F56C6C')
    ]
  }

  const canPurchase = hasPerm('purchase:view')
  const canSales = hasPerm('sales:view')

  if (canPurchase && !canSales) {
    // 采购员：采购相关统计
    return [
      mk('今日采购新单', dr.newPurchaseOrderCount, ShoppingCart, '#67C23A'),
      mk('待审采购', dr.pendingPurchaseCount, ShoppingCart, '#E6A23C'),
      mk('低库存预警', dr.lowStockLineCount, WarningFilled, '#F56C6C'),
      mk('临期预警', dr.expiringSkuCount, Clock, '#FA8C16')
    ]
  }

  if (canSales && !canPurchase) {
    // 销售员：销售相关统计
    return [
      { title: '今日销售额', value: '¥ ' + fmtMoney(dr.realizedSales), icon: Money, color: '#409EFF' },
      mk('今日新销售单', dr.newOrderCount, Goods, '#67C23A'),
      mk('待审销售', dr.pendingSalesCount, Goods, '#E6A23C'),
      mk('授信预警', dr.creditWarningCount, WarningFilled, '#F56C6C')
    ]
  }

  if (!canPurchase && !canSales) {
    // 仓管员：库存相关统计
    return [
      mk('库存商品数', s.stockKeepingProductCount, Goods, '#409EFF'),
      mk('低库存预警', dr.lowStockLineCount, WarningFilled, '#F56C6C'),
      mk('临期预警', dr.expiringSkuCount, Clock, '#E6A23C'),
      { title: '今日出库额', value: '¥ ' + fmtMoney(dr.realizedSales), icon: Money, color: '#67C23A' }
    ]
  }

  // 管理员 / 财务（同时有采购+销售视图权限）
  return [
    { title: '今日销售额', value: '¥ ' + fmtMoney(dr.realizedSales), icon: Money, color: '#409EFF' },
    mk('今日采购新单', dr.newPurchaseOrderCount, ShoppingCart, '#67C23A'),
    mk('库存商品数', s.stockKeepingProductCount, Goods, '#E6A23C'),
    mk('启用客户数', s.activeCustomerCount, User, '#F56C6C')
  ]
})

// ── 图表 ──────────────────────────────────────────────
const salesChart = ref(null)
let salesChartInst = null
let resizeHandler = null

const renderSalesChart = () => {
  if (!salesChart.value || !hasPerm('sales:view')) return
  if (!salesChartInst) salesChartInst = echarts.init(salesChart.value)
  const pts = weekTrend.value || []
  salesChartInst.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: pts.map((p) => (p.date ? String(p.date).slice(5) : '')) },
    yAxis: { type: 'value', name: '金额(元)' },
    series: [{
      name: '出库销售额',
      type: 'line',
      data: pts.map((p) => Number(p.amount || 0)),
      smooth: true,
      symbol: 'circle',
      lineStyle: { color: '#409EFF', width: 3 },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: 'rgba(64,158,255,0.5)' },
          { offset: 1, color: 'rgba(64,158,255,0.1)' }
        ])
      }
    }]
  })
}

// ── 数据加载 ──────────────────────────────────────────
const loadHome = async () => {
  try {
    const res = await getHomeSummary()
    homeData.value = res.data
    dailyReport.value = res.data?.dailyReport
    weekTrend.value = res.data?.weekSalesTrend || []
    renderSalesChart()
  } catch (e) {
    console.error('加载首页数据失败', e)
  }
}

const loadWarnings = async () => {
  if (!hasInventoryPerm.value) return
  try {
    const [expiring, lowStock] = await Promise.all([
      getExpiringProducts(),
      getLowStockProducts()
    ])
    warnings.value = [
      ...(expiring.data || []).map((item) => ({
        id: 'e-' + item.id,
        productName: item.product?.name || item.productName,
        type: 'danger',
        message: '即将过期'
      })),
      ...(lowStock.data || []).map((item) => ({
        id: 'l-' + item.id,
        productName: item.product?.name || item.productName,
        type: 'warning',
        message: '库存不足'
      }))
    ]
  } catch (e) {
    console.error('加载预警失败', e)
  }
}

const loadPendingOrders = async () => {
  try {
    const calls = []
    if (hasPerm('purchase:view')) calls.push(getPurchaseOrders({ status: 'PENDING', page: 0, size: 8 }))
    if (hasPerm('sales:view'))    calls.push(getSalesOrders({ status: 'PENDING', page: 0, size: 8 }))
    const results = await Promise.all(calls)
    let i = 0
    if (hasPerm('purchase:view')) pendingPurchases.value = results[i++]?.data?.content || []
    if (hasPerm('sales:view'))    pendingSales.value    = results[i++]?.data?.content || []
  } catch (e) {
    console.error('加载待办订单失败', e)
  }
}

const getStatusType = (status) => ({
  PENDING: 'info', APPROVED: 'success', SHIPPED: 'warning',
  DELIVERED: 'primary', COMPLETED: 'success', CANCELLED: 'danger'
}[status] || 'info')

const statusText = (status) => ({
  PENDING: '待审核', APPROVED: '已审核', SHIPPED: '已发货',
  RECEIVED: '已收货', DELIVERED: '已送达', COMPLETED: '已完成', CANCELLED: '已取消'
}[status] || status)

onMounted(() => {
  loadHome()
  loadWarnings()
  loadPendingOrders()
  resizeHandler = () => salesChartInst?.resize()
  window.addEventListener('resize', resizeHandler)
})

onUnmounted(() => {
  if (resizeHandler) window.removeEventListener('resize', resizeHandler)
  salesChartInst?.dispose()
})
</script>

<style scoped lang="scss">
.dashboard {
  .daily-strip { margin-bottom: 16px; }
  .daily-meta {
    font-size: 13px;
    line-height: 1.6;
    strong { color: var(--el-color-primary); }
  }
  .stat-card .stat-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .stat-info {
      .stat-title { font-size: 14px; color: #909399; margin-bottom: 8px; }
      .stat-value { font-size: 24px; font-weight: bold; color: #303133; }
    }
    .stat-icon {
      width: 60px; height: 60px; border-radius: 8px;
      display: flex; align-items: center; justify-content: center;
    }
  }
  .chart-row  { margin-top: 20px; }
  .table-row  { margin-top: 20px; }
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .warning-list {
    .warning-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 0;
      border-bottom: 1px solid #ebeef5;
      &:last-child { border-bottom: none; }
      .warning-info {
        .warning-name { font-weight: 500; margin-bottom: 4px; }
        .warning-desc { font-size: 12px; }
      }
    }
  }
}
</style>
