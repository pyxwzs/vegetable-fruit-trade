<template>
  <div class="analysis-dashboard" v-loading="loading">
    <el-card class="daily-card" shadow="never">
      <div class="daily-head">
        <span class="daily-title">经营日报</span>
        <el-date-picker
            v-model="reportDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择日期"
            @change="loadDailyReport"
        />
      </div>
      <p v-if="dailyReport" class="daily-lines">
        当日出库销售额 <strong>¥{{ fmt(dailyReport.realizedSales) }}</strong>
        ｜ 新销售单 {{ dailyReport.newOrderCount }} 笔
        ｜ 新采购单 {{ dailyReport.newPurchaseOrderCount }} 笔
        ｜ 待审采购 <el-tag size="small" type="warning">{{ dailyReport.pendingPurchaseCount }}</el-tag>
        ｜ 待审销售 <el-tag size="small" type="warning">{{ dailyReport.pendingSalesCount }}</el-tag>
        ｜ 库存预警 {{ dailyReport.lowStockLineCount }} 条
        ｜ 临期 {{ dailyReport.expiringSkuCount }} 条
        ｜ 授信预警 {{ dailyReport.creditWarningCount }} 户
      </p>
    </el-card>

    <el-row :gutter="20">
      <el-col :span="6" v-for="(item, idx) in kpiCards" :key="idx">
        <el-card class="kpi-card" :body-style="{ padding: '20px' }">
          <div class="kpi-item">
            <div class="kpi-info">
              <div class="kpi-title">{{ item.title }}</div>
              <div class="kpi-value">{{ item.value }}</div>
              <div v-if="item.trend != null" class="kpi-trend" :class="item.trend >= 0 ? 'up' : 'down'">
                环比 {{ item.trend >= 0 ? '+' : '' }}{{ item.trend }}%
              </div>
            </div>
            <div class="kpi-icon" :style="{ background: item.color + '20' }">
              <el-icon :size="32" :color="item.color">
                <component :is="item.icon" />
              </el-icon>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>销售趋势（已出库/完成订单额）</span>
              <el-radio-group v-model="salesRange" size="small" @change="loadAnalytics">
                <el-radio-button label="week">本周</el-radio-button>
                <el-radio-button label="month">本月</el-radio-button>
                <el-radio-button label="quarter">本季</el-radio-button>
                <el-radio-button label="year">本年</el-radio-button>
              </el-radio-group>
            </div>
          </template>
          <div ref="salesChartRef" style="height: 350px"></div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>商品销售额排行</span>
            </div>
          </template>
          <div ref="productRankRef" style="height: 350px"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>毛利与品类结构</span>
            </div>
          </template>
          <div v-if="profitSummary" class="profit-text">
            <p>营业收入 <strong>¥{{ fmt(profitSummary.revenue) }}</strong></p>
            <p>估算成本（采购参考价×销量）<strong>¥{{ fmt(profitSummary.estimatedCost) }}</strong></p>
            <p>毛利 <strong>¥{{ fmt(profitSummary.grossProfit) }}</strong>，毛利率 <strong>{{ (Number(profitSummary.grossMargin || 0) * 100).toFixed(1) }}%</strong></p>
          </div>
          <div ref="profitChartRef" style="height: 260px"></div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>采购补货建议</span>
              <span class="sub">全仓可用合计 &lt; 10 的商品（可按建议量采购）</span>
            </div>
          </template>
          <el-table v-if="replenishList.length" :data="replenishList" border size="small" max-height="320">
            <el-table-column prop="productName" label="商品" min-width="120" />
            <el-table-column prop="unit" label="单位" width="60" />
            <el-table-column label="当前可用" width="100">
              <template #default="{ row }">{{ Number(row.currentAvailableTotal).toFixed(3) }}</template>
            </el-table-column>
            <el-table-column label="建议补货" width="100">
              <template #default="{ row }">{{ Number(row.suggestedQty).toFixed(3) }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无补货建议" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="table-row">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>客户价值（按出库毛利）</span>
            </div>
          </template>
          <el-table :data="customerRows" style="width: 100%" border>
            <el-table-column prop="rank" label="排名" width="80" align="center" />
            <el-table-column prop="customerName" label="客户名称" min-width="150" />
            <el-table-column prop="orderCount" label="订单数" width="100" align="center" />
            <el-table-column label="销售额" width="130">
              <template #default="{ row }">¥{{ fmt(row.revenue) }}</template>
            </el-table-column>
            <el-table-column label="估算成本" width="130">
              <template #default="{ row }">¥{{ fmt(row.estimatedCost) }}</template>
            </el-table-column>
            <el-table-column label="毛利" width="130">
              <template #default="{ row }">¥{{ fmt(row.grossProfit) }}</template>
            </el-table-column>
            <el-table-column label="利润率" width="100">
              <template #default="{ row }">{{ (Number(row.profitMargin || 0) * 100).toFixed(1) }}%</template>
            </el-table-column>
            <el-table-column prop="creditLevel" label="信用" width="90">
              <template #default="{ row }">
                <el-tag :type="creditTag(row.creditLevel)" size="small">{{ row.creditLevel }}级</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Money, ShoppingCart, Goods, User } from '@element-plus/icons-vue'
import {
  getOverviewKpi,
  getSalesTrend,
  getProductRanking,
  getProfitSummary,
  getReplenishment,
  getCustomerRanking,
  getDailyReport
} from '@/api/analytics'

const loading = ref(false)
const salesRange = ref('month')
const salesChartRef = ref(null)
const productRankRef = ref(null)
const profitChartRef = ref(null)

let chartSales = null
let chartRank = null
let chartProfit = null

const kpiCards = ref([
  { title: '区间销售额', value: '¥ 0', trend: null, icon: Money, color: '#409EFF' },
  { title: '区间毛利', value: '¥ 0', trend: null, icon: ShoppingCart, color: '#67C23A' },
  { title: '区间订单数', value: '0', trend: null, icon: Goods, color: '#E6A23C' },
  { title: '有交易客户数', value: '0', trend: null, icon: User, color: '#F56C6C' }
])

const trendPoints = ref([])
const rankList = ref([])
const profitSummary = ref(null)
const replenishList = ref([])
const customerRows = ref([])

const reportDate = ref(null)
const dailyReport = ref(null)

const fmt = (v) => (v != null ? Number(v).toFixed(2) : '0.00')

const creditTag = (lv) =>
  ({ A: 'success', B: 'primary', C: 'warning', D: 'danger' }[lv] || 'info')

const loadDailyReport = async () => {
  try {
    const res = await getDailyReport(reportDate.value || undefined)
    dailyReport.value = res.data
  } catch {
    dailyReport.value = null
  }
}

const renderSalesChart = () => {
  if (!salesChartRef.value) return
  if (!chartSales) chartSales = echarts.init(salesChartRef.value)
  const pts = trendPoints.value || []
  const dates = pts.map((p) => {
    const d = p.date
    if (!d) return ''
    return typeof d === 'string' ? d.slice(5, 10) : String(d).slice(5, 10)
  })
  const amounts = pts.map((p) => Number(p.amount || 0))
  chartSales.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: dates },
    yAxis: { type: 'value', name: '金额(元)' },
    series: [
      {
        name: '销售额',
        type: 'line',
        data: amounts,
        smooth: true,
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64,158,255,0.4)' },
            { offset: 1, color: 'rgba(64,158,255,0.05)' }
          ])
        }
      }
    ]
  })
}

const renderRankChart = () => {
  if (!productRankRef.value) return
  if (!chartRank) chartRank = echarts.init(productRankRef.value)
  const list = (rankList.value || []).slice(0, 8).reverse()
  const names = list.map((x) => x.productName)
  const vals = list.map((x) => Number(x.amount || 0))
  chartRank.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '8%', bottom: '3%', containLabel: true },
    xAxis: { type: 'value', name: '元' },
    yAxis: { type: 'category', data: names },
    series: [
      {
        name: '销售额',
        type: 'bar',
        data: vals,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: '#409EFF' },
            { offset: 1, color: '#36cfc9' }
          ])
        }
      }
    ]
  })
}

const renderProfitChart = () => {
  if (!profitChartRef.value) return
  if (!chartProfit) chartProfit = echarts.init(profitChartRef.value)
  const cats = profitSummary.value?.salesByCategory || []
  const data = cats.map((c) => ({ value: Number(c.amount || 0), name: c.categoryName || '未分类' }))
  chartProfit.setOption({
    tooltip: { trigger: 'item' },
    legend: { orient: 'vertical', left: 'left', top: 'middle' },
    series: [
      {
        name: '品类销售额',
        type: 'pie',
        radius: ['40%', '70%'],
        avoidLabelOverlap: true,
        data
      }
    ]
  })
}

const loadAnalytics = async () => {
  loading.value = true
  try {
    const r = salesRange.value
    const [kpi, trend, rank, profit, rep, cust] = await Promise.all([
      getOverviewKpi(r),
      getSalesTrend(r),
      getProductRanking(r, 10),
      getProfitSummary(r),
      getReplenishment(),
      getCustomerRanking(r, 20)
    ])
    const k = kpi.data
    kpiCards.value = [
      {
        title: '区间销售额',
        value: '¥ ' + fmt(k?.totalSales),
        trend: k?.salesTrendPercent != null ? Number(k.salesTrendPercent) : null,
        icon: Money,
        color: '#409EFF'
      },
      {
        title: '区间毛利',
        value: '¥ ' + fmt(k?.grossProfit),
        trend: k?.profitTrendPercent != null ? Number(k.profitTrendPercent) : null,
        icon: ShoppingCart,
        color: '#67C23A'
      },
      {
        title: '区间订单数',
        value: String(k?.orderCount ?? 0),
        trend: k?.orderTrendPercent != null ? Number(k.orderTrendPercent) : null,
        icon: Goods,
        color: '#E6A23C'
      },
      {
        title: '有交易客户数',
        value: String(k?.tradingCustomerCount ?? 0),
        trend: k?.customerTrendPercent != null ? Number(k.customerTrendPercent) : null,
        icon: User,
        color: '#F56C6C'
      }
    ]
    trendPoints.value = trend.data || []
    rankList.value = rank.data || []
    profitSummary.value = profit.data
    replenishList.value = rep.data || []
    customerRows.value = cust.data || []

    await nextTick()
    renderSalesChart()
    renderRankChart()
    renderProfitChart()
  } finally {
    loading.value = false
  }
}

const onResize = () => {
  chartSales?.resize()
  chartRank?.resize()
  chartProfit?.resize()
}

onMounted(async () => {
  await loadDailyReport()
  await loadAnalytics()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  chartSales?.dispose()
  chartRank?.dispose()
  chartProfit?.dispose()
})
</script>

<style scoped lang="scss">
.analysis-dashboard {
  .daily-card {
    margin-bottom: 20px;
    .daily-head {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-bottom: 10px;
    }
    .daily-title {
      font-weight: 600;
      font-size: 16px;
    }
    .daily-lines {
      margin: 0;
      font-size: 13px;
      color: var(--el-text-color-regular);
      line-height: 1.7;
      strong {
        color: var(--el-color-primary);
      }
    }
  }

  .kpi-card {
    margin-bottom: 20px;

    .kpi-item {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .kpi-info {
        .kpi-title {
          font-size: 14px;
          color: #909399;
          margin-bottom: 8px;
        }

        .kpi-value {
          font-size: 22px;
          font-weight: bold;
          color: #303133;
          margin-bottom: 8px;
        }

        .kpi-trend {
          font-size: 12px;

          &.up {
            color: #67c23a;
          }

          &.down {
            color: #f56c6c;
          }
        }
      }

      .kpi-icon {
        width: 56px;
        height: 56px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }
  }

  .chart-row,
  .table-row {
    margin-top: 20px;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
    .sub {
      font-size: 12px;
      color: var(--el-text-color-secondary);
      font-weight: normal;
    }
  }

  .profit-text {
    margin-bottom: 8px;
    font-size: 13px;
    p {
      margin: 4px 0;
    }
  }
}
</style>
