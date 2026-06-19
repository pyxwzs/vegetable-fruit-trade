<template>
  <div class="inventory-list">

    <!-- 库存仪表盘 -->
    <el-card class="summary-card" shadow="never" v-loading="chartLoading">
      <template #header>
        <div class="chart-header">
          <span class="chart-title">库存总览</span>
        </div>
        <div class="kpi-row">
          <div class="kpi-item">
            <span class="kpi-label">商品种类</span>
            <span class="kpi-value purple">{{ stats.skuCount }}</span>
            <span class="kpi-sub">种</span>
          </div>
          <div class="kpi-divider"></div>
          <div class="kpi-item">
            <span class="kpi-label">库存总价值</span>
            <span class="kpi-value purple">¥{{ stats.totalValue.toFixed(2) }}</span>
            <span class="kpi-sub">按最近采购价</span>
          </div>
          <div class="kpi-divider"></div>
          <div class="kpi-item kpi-clickable purchase" @click="router.push('/purchase')">
            <span class="kpi-label">待入库</span>
            <span class="kpi-value blue">{{ purchasePending.count }} 笔</span>
            <span class="kpi-sub">¥{{ Number(purchasePending.totalAmount).toFixed(2) }} ›</span>
          </div>
          <div class="kpi-divider"></div>
          <div class="kpi-item kpi-clickable sales" @click="router.push('/sales')">
            <span class="kpi-label">待出库</span>
            <span class="kpi-value green">{{ salesPending.count }} 笔</span>
            <span class="kpi-sub">¥{{ Number(salesPending.totalAmount).toFixed(2) }} ›</span>
          </div>
        </div>
      </template>

      <div v-show="overviewItems.length" class="charts-row">
        <div class="chart-wrap">
          <div class="chart-label">分类价值占比</div>
          <v-chart :option="pieOption" :init-options="{ renderer: 'canvas' }" autoresize style="height:220px" />
        </div>
        <div class="chart-wrap">
          <div class="chart-label">商品库存价值 TOP</div>
          <v-chart :option="barOption" :init-options="{ renderer: 'canvas' }" autoresize style="height:220px" />
        </div>
      </div>
      <el-empty v-show="!chartLoading && !overviewItems.length" description="暂无库存数据" :image-size="50" />
    </el-card>

    <!-- 明细列表 -->
    <el-card>
      <div class="search-bar">
        <el-input
            v-model="searchKeyword"
            placeholder="商品名称"
            style="width: 220px"
            clearable
            @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <div v-if="isMobile" v-loading="loading" class="mobile-card-list">
        <MobileListCard v-for="row in inventories" :key="row.id" :title="row.product?.name || '—'">
          <template #tag>
            <el-tag v-if="row.product?.category" type="info" size="small">{{ row.product.category }}</el-tag>
          </template>
          <div class="card-row"><span class="label">库存</span><span class="value">{{ row.quantity }} {{ row.product?.unit }}</span></div>
        </MobileListCard>
        <el-empty v-if="!loading && !inventories.length" description="暂无数据" />
      </div>
      <div v-else class="table-wrap">
        <el-table :data="inventories" style="width: 100%" v-loading="loading" border>
          <el-table-column prop="product.name" label="商品名称" min-width="160" />
          <el-table-column prop="product.category" label="分类" width="100" />
          <el-table-column label="当前库存" width="120" align="right">
            <template #default="{ row }">{{ Number(row.quantity).toFixed(2) }} {{ row.product?.unit }}</template>
          </el-table-column>
        </el-table>
      </div>

      <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData"
          @current-change="loadData"
          class="pagination"
      />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { use } from 'echarts/core'
import { PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { getInventories, getInventoryOverview } from '@/api/inventory'
import { getPurchasePendingStats } from '@/api/purchase'
import { getSalesPendingStats } from '@/api/sales'
import { useIsMobile } from '@/composables/useIsMobile'
import MobileListCard from '@/components/MobileListCard.vue'

use([PieChart, BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const router = useRouter()
const isMobile = useIsMobile()

const PIE_COLORS = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452']

const purchasePending = ref({ count: 0, totalAmount: 0 })
const salesPending = ref({ count: 0, totalAmount: 0 })
const overviewItems = ref([])
const overviewStats = ref({ skuCount: 0, totalValue: 0 })
const chartLoading = ref(false)

const stats = computed(() => ({
  skuCount: overviewStats.value.skuCount,
  totalValue: Number(overviewStats.value.totalValue || 0)
}))

const loadDashboard = async () => {
  chartLoading.value = true
  try {
    const [overview, p, s] = await Promise.all([
      getInventoryOverview(),
      getPurchasePendingStats(),
      getSalesPendingStats()
    ])
    overviewItems.value = overview.data?.items || []
    overviewStats.value = {
      skuCount: Number(overview.data?.skuCount || 0),
      totalValue: overview.data?.totalValue || 0
    }
    purchasePending.value = { count: Number(p.data?.count || 0), totalAmount: Number(p.data?.totalAmount || 0) }
    salesPending.value = { count: Number(s.data?.count || 0), totalAmount: Number(s.data?.totalAmount || 0) }
  } catch {
    overviewItems.value = []
    overviewStats.value = { skuCount: 0, totalValue: 0 }
  } finally {
    chartLoading.value = false
  }
}

const pieOption = computed(() => {
  const catMap = {}
  overviewItems.value.forEach(i => {
    const cat = i.category || '未分类'
    catMap[cat] = (catMap[cat] || 0) + Number(i.value || 0)
  })
  return {
    tooltip: { trigger: 'item', formatter: p => `${p.name}<br/>¥${Number(p.value).toFixed(2)} (${p.percent}%)` },
    legend: { show: false },
    color: PIE_COLORS,
    series: [{
      type: 'pie',
      radius: ['38%', '68%'],
      center: ['50%', '46%'],
      label: { show: true, fontSize: 11, formatter: p => p.name.length > 4 ? p.name.slice(0, 4) + '…' : p.name },
      data: Object.entries(catMap).map(([name, value]) => ({ name, value }))
    }]
  }
})

const barOption = computed(() => {
  const sorted = [...overviewItems.value]
    .sort((a, b) => Number(b.value) - Number(a.value))
    .slice(0, 12)
    .reverse()
  return {
    tooltip: {
      trigger: 'axis',
      formatter: params => {
        const row = sorted[params[0].dataIndex]
        return `${params[0].name}: ¥${Number(params[0].value).toFixed(2)}<br/>${Number(row?.quantity || 0)} ${row?.unit || ''}`
      }
    },
    grid: { left: 80, right: 30, top: 10, bottom: 10 },
    xAxis: { type: 'value', axisLabel: { fontSize: 10, formatter: v => `¥${v}` } },
    yAxis: {
      type: 'category',
      data: sorted.map(i => {
        const n = i.productName || '—'
        return n.length > 6 ? n.slice(0, 6) + '…' : n
      }),
      axisLabel: { fontSize: 11 }
    },
    series: [{
      type: 'bar',
      data: sorted.map(i => Number(i.value)),
      itemStyle: { color: '#5470c6', borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right', fontSize: 10, formatter: p => `¥${Number(p.value).toFixed(0)}` }
    }]
  }
})

const loading = ref(false)
const inventories = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')

const loadData = async () => {
  loading.value = true
  try {
    const res = await getInventories({
      page: page.value - 1, size: size.value,
      keyword: searchKeyword.value || undefined
    })
    inventories.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } catch { ElMessage.error('加载库存失败') } finally { loading.value = false }
}

const handleSearch = () => { page.value = 1; loadData() }
const resetSearch = () => { searchKeyword.value = ''; page.value = 1; loadData() }

onMounted(() => {
  loadDashboard()
  loadData()
})
</script>

<style scoped lang="scss">
.inventory-list {
  .table-wrap { overflow-x: auto; -webkit-overflow-scrolling: touch; }
  .search-bar { margin-bottom: 12px; display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
  .pagination { margin-top: 16px; display: flex; justify-content: center; }
}

.summary-card { margin-bottom: 12px; }

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chart-title { font-size: 15px; font-weight: 600; color: #303133; }

.kpi-row {
  display: flex;
  align-items: stretch;
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}
.kpi-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 4px 6px;
  border-radius: 6px;
  transition: background 0.2s;
}
.kpi-divider { width: 1px; background: #f0f0f0; margin: 0 2px; }
.kpi-label { font-size: 12px; color: #909399; }
.kpi-value { font-size: 17px; font-weight: 700; }
.kpi-value.purple { color: #9a60b4; }
.kpi-value.blue { color: #409eff; font-size: 15px; }
.kpi-value.green { color: #67c23a; font-size: 15px; }
.kpi-sub { font-size: 11px; color: #c0c4cc; }
.kpi-clickable { cursor: pointer; }
.kpi-clickable.purchase:hover { background: #ecf5ff; }
.kpi-clickable.sales:hover { background: #f0f9eb; }

.charts-row { display: flex; gap: 8px; }
.chart-wrap {
  flex: 1;
  min-width: 0;
  .chart-label { font-size: 12px; color: #909399; text-align: center; margin-bottom: 4px; }
}

@media (max-width: 600px) {
  .kpi-row { flex-wrap: wrap; }
  .kpi-item { flex: 0 0 45%; margin-bottom: 8px; }
  .kpi-divider { display: none; }
  .charts-row { flex-direction: column; }
  .kpi-value { font-size: 15px; }
}
</style>
