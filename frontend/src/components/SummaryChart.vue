<template>
  <el-card class="summary-chart-card" shadow="never">
    <!-- 标题栏 -->
    <template #header>
      <div class="chart-header">
        <span class="chart-title">{{ props.type === 'purchase' ? '采购统计' : '销售统计' }}</span>
        <div class="chart-controls">
          <el-radio-group v-model="dateMode" size="small" @change="loadData">
            <el-radio-button label="day">按日</el-radio-button>
            <el-radio-button label="month">按月</el-radio-button>
          </el-radio-group>
          <el-date-picker
            v-if="dateMode === 'day'"
            v-model="selectedDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            size="small"
            style="width: 140px"
            @change="loadData"
          />
          <el-date-picker
            v-else
            v-model="selectedMonth"
            type="month"
            value-format="YYYY-MM"
            placeholder="选择月份"
            size="small"
            style="width: 120px"
            @change="loadData"
          />
        </div>
      </div>

      <!-- KPI 双格 -->
      <div class="kpi-row">
        <!-- 左：今日/本月采购额或销售额（可点击过滤列表） -->
        <div class="kpi-item kpi-clickable period" @click="emitPeriodFilter">
          <span class="kpi-label">{{ dateMode === 'day' ? (type === 'purchase' ? '今日采购额' : '今日销售额') : (type === 'purchase' ? '本月采购额' : '本月销售额') }}</span>
          <span class="kpi-value" :class="type === 'purchase' ? 'blue' : 'green'">
            ¥{{ activeSummary.totalAmount.toFixed(2) }}
          </span>
          <span class="kpi-sub">点击查看{{ dateMode === 'day' ? '当日' : '当月' }}订单 ›</span>
        </div>
        <div class="kpi-divider"></div>
        <!-- 右：本年待付/待收款（可点击过滤） -->
        <div class="kpi-item kpi-clickable" @click="$emit('filter-unpaid')">
          <span class="kpi-label">{{ type === 'purchase' ? '本年待付款' : '本年待收款' }}</span>
          <span class="kpi-value red">¥{{ pendingAmount.toFixed(2) }}</span>
          <span class="kpi-sub">点击查看未付订单 ›</span>
        </div>
      </div>
    </template>

    <div v-loading="loading">
      <!-- 双图区：v-show 保持 DOM 节点，避免 ECharts 更新时 parentNode 为 null -->
      <div class="charts-row" v-show="activeSummary.items.length">
        <div class="chart-wrap">
          <div class="chart-label">商品占比</div>
          <v-chart :option="pieOption" :init-options="{ renderer: 'canvas' }" autoresize style="height: 200px" />
        </div>
        <div class="chart-wrap">
          <div class="chart-label">商品金额</div>
          <v-chart :option="barOption" :init-options="{ renderer: 'canvas' }" autoresize style="height: 200px" />
        </div>
      </div>
      <el-empty
        v-show="!loading && !activeSummary.items.length"
        :description="dateMode === 'day' ? '当日暂无记录' : '当月暂无记录'"
        :image-size="50"
      />
    </div>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { use } from 'echarts/core'
import { PieChart, BarChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, GridComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { getPurchaseItems, getSalesItems, getYearlyBalance } from '@/api/analytics'

use([PieChart, BarChart, TooltipComponent, LegendComponent, GridComponent, CanvasRenderer])

const props = defineProps({
  type: { type: String, default: 'purchase' }
})
const emit = defineEmits(['filter-unpaid', 'filter-period'])

const now = new Date()
const todayStr = now.toISOString().slice(0, 10)
const thisMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`

const dateMode = ref('day')
const selectedDate = ref(todayStr)
const selectedMonth = ref(thisMonth)
const loading = ref(false)

const empty = () => ({ orderCount: 0, totalAmount: 0, totalQty: 0, items: [] })
const purchaseSummary = ref(empty())
const salesSummary = ref(empty())
const pendingAmount = ref(0)

const activeSummary = computed(() =>
  props.type === 'purchase' ? purchaseSummary.value : salesSummary.value
)

const PIE_COLORS = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
  '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#48cae4'
]

const pieOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: p => `${p.name}<br/>¥${Number(p.value).toFixed(2)} (${p.percent}%)`
  },
  legend: { show: false },
  color: PIE_COLORS,
  series: [{
    type: 'pie',
    radius: ['38%', '68%'],
    center: ['50%', '46%'],
    label: {
      show: true,
      formatter: p => p.name.length > 4 ? p.name.slice(0, 4) + '…' : p.name,
      fontSize: 11
    },
    data: activeSummary.value.items.map(i => ({ name: i.productName, value: i.amount }))
  }]
}))

const barOption = computed(() => {
  const items = [...activeSummary.value.items].reverse()
  const accent = props.type === 'purchase' ? '#5470c6' : '#91cc75'
  return {
    tooltip: {
      trigger: 'axis',
      formatter: params => `${params[0].name}<br/>¥${Number(params[0].value).toFixed(2)}`
    },
    grid: { left: 80, right: 20, top: 10, bottom: 10 },
    xAxis: { type: 'value', axisLabel: { fontSize: 10, formatter: v => v >= 1000 ? (v / 1000).toFixed(1) + 'k' : v } },
    yAxis: {
      type: 'category',
      data: items.map(i => i.productName.length > 5 ? i.productName.slice(0, 5) + '…' : i.productName),
      axisLabel: { fontSize: 11 }
    },
    series: [{
      type: 'bar',
      data: items.map(i => i.amount),
      itemStyle: { color: accent, borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right', formatter: p => `¥${Number(p.value).toFixed(0)}`, fontSize: 10 }
    }]
  }
})

const aggregate = (rawItems) => {
  const map = {}
  rawItems.forEach(i => {
    const key = i.productName
    if (!map[key]) map[key] = { productName: i.productName, unit: i.unit, quantity: 0, amount: 0 }
    map[key].quantity += Number(i.quantity)
    map[key].amount += Number(i.amount)
  })
  const items = Object.values(map).sort((a, b) => b.amount - a.amount)
  const totalAmount = items.reduce((s, r) => s + r.amount, 0)
  const totalQty = items.reduce((s, r) => s + r.quantity, 0)
  const orderNos = new Set(rawItems.map(i => i.orderNo).filter(Boolean))
  return { orderCount: orderNos.size, totalAmount, totalQty, items }
}

const loadData = async () => {
  loading.value = true
  try {
    let year, month, filterDate
    if (dateMode.value === 'day') {
      const d = new Date(selectedDate.value)
      year = d.getFullYear()
      month = d.getMonth() + 1
      filterDate = selectedDate.value
    } else {
      const parts = selectedMonth.value.split('-')
      year = Number(parts[0])
      month = Number(parts[1])
      filterDate = null
    }

    const [pRes, sRes] = await Promise.all([
      getPurchaseItems({ year, month }),
      getSalesItems({ year, month })
    ])

    const pAll = pRes.data?.items || []
    const sAll = sRes.data?.items || []
    const pFiltered = filterDate ? pAll.filter(i => i.date === filterDate) : pAll
    const sFiltered = filterDate ? sAll.filter(i => i.date === filterDate) : sAll

    purchaseSummary.value = aggregate(pFiltered)
    salesSummary.value = aggregate(sFiltered)
  } catch {
    purchaseSummary.value = empty()
    salesSummary.value = empty()
  } finally {
    loading.value = false
  }
}

const monthRange = (ym) => {
  const [y, m] = ym.split('-').map(Number)
  const mm = String(m).padStart(2, '0')
  const last = new Date(y, m, 0).getDate()
  return {
    startDate: `${y}-${mm}-01`,
    endDate: `${y}-${mm}-${String(last).padStart(2, '0')}`
  }
}

const emitPeriodFilter = () => {
  if (dateMode.value === 'day') {
    emit('filter-period', { mode: 'day', startDate: selectedDate.value, endDate: selectedDate.value, label: selectedDate.value })
  } else {
    const { startDate, endDate } = monthRange(selectedMonth.value)
    emit('filter-period', { mode: 'month', startDate, endDate, label: selectedMonth.value })
  }
}

const loadYearlyBalance = async () => {
  try {
    const res = await getYearlyBalance({ year: now.getFullYear() })
    const data = res.data || {}
    pendingAmount.value = props.type === 'purchase'
      ? Number(data.unpaidToFarmers || 0)
      : Number(data.uncollectedFromCustomers || 0)
  } catch { pendingAmount.value = 0 }
}

const refresh = () => {
  loadData()
  loadYearlyBalance()
}

onMounted(refresh)

defineExpose({ refresh })
</script>

<style scoped lang="scss">
.summary-chart-card { margin-bottom: 12px; }

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.chart-title { font-size: 15px; font-weight: 600; color: #303133; }
.chart-controls { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

/* 双 KPI */
.kpi-row {
  display: flex;
  align-items: stretch;
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
  gap: 0;
}
.kpi-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 4px 8px;
}
.kpi-divider {
  width: 1px;
  background: #f0f0f0;
  margin: 0 4px;
}
.kpi-label { font-size: 12px; color: #909399; }
.kpi-clickable {
  cursor: pointer;
  border-radius: 6px;
  transition: background 0.2s;
  &:hover { background: #fff1f0; }
  &.period:hover { background: #ecf5ff; }
}
.kpi-value { font-size: 18px; font-weight: 700; }
.kpi-value.blue { color: #409eff; }
.kpi-value.green { color: #67c23a; }
.kpi-value.red { color: #f56c6c; }
.kpi-sub { font-size: 11px; color: #c0c4cc; }

/* 双图布局 */
.charts-row {
  display: flex;
  gap: 8px;
}
.chart-wrap {
  flex: 1;
  min-width: 0;
  .chart-label {
    font-size: 12px;
    color: #909399;
    text-align: center;
    margin-bottom: 2px;
  }
}

@media (max-width: 600px) {
  .charts-row { flex-direction: column; }
  .kpi-value { font-size: 16px; }
}
</style>
