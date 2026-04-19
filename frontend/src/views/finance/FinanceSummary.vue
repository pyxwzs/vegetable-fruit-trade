<template>
  <div class="finance-summary">
    <el-row :gutter="16" class="kpi-row">
      <!-- 采购支出 -->
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="kpi-card purchase">
          <div class="kpi-label">采购总额</div>
          <div class="kpi-value">¥{{ fmt(data.totalPurchaseAmount) }}</div>
          <div class="kpi-sub">
            已付 <span class="paid">¥{{ fmt(data.totalPaidAmount) }}</span>
            &nbsp;·&nbsp;
            待付 <span class="warn">¥{{ fmt(data.totalUnpaidAmount) }}</span>
          </div>
        </el-card>
      </el-col>

      <!-- 销售收入 -->
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="kpi-card sales">
          <div class="kpi-label">销售总额</div>
          <div class="kpi-value">¥{{ fmt(data.totalSalesAmount) }}</div>
          <div class="kpi-sub">
            已收 <span class="paid">¥{{ fmt(data.totalReceivedAmount) }}</span>
            &nbsp;·&nbsp;
            待收 <span class="warn">¥{{ fmt(data.totalUnreceivedAmount) }}</span>
          </div>
        </el-card>
      </el-col>

      <!-- 毛利 -->
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="kpi-card profit" :class="{ negative: grossProfitNeg }">
          <div class="kpi-label">毛利（已收 - 已付）</div>
          <div class="kpi-value" :class="grossProfitNeg ? 'red' : 'green'">
            ¥{{ fmt(data.grossProfit) }}
          </div>
          <div class="kpi-sub">毛利率 {{ data.grossMarginPercent }}%</div>
        </el-card>
      </el-col>

      <!-- 净资金缺口 -->
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="kpi-card gap">
          <div class="kpi-label">应收 - 应付（资金缺口）</div>
          <div class="kpi-value" :class="gapNeg ? 'red' : 'green'">
            ¥{{ fmt(gap) }}
          </div>
          <div class="kpi-sub">
            应收 ¥{{ fmt(data.totalUnreceivedAmount) }} &nbsp;·&nbsp; 应付 ¥{{ fmt(data.totalUnpaidAmount) }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 近6个月趋势 -->
    <el-row :gutter="16" style="margin-top:20px">
      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header><span>近6个月采购支出趋势</span></template>
          <div v-if="purchaseChartData.length" class="chart-wrap">
            <div v-for="row in purchaseChartData" :key="row.month" class="bar-row">
              <span class="bar-label">{{ row.month }}</span>
              <div class="bar-track">
                <div class="bar-fill purchase-bar" :style="{ width: row.totalPct + '%' }">
                  <span v-if="row.totalPct > 15" class="bar-text">¥{{ fmtShort(row.total) }}</span>
                </div>
                <div class="bar-fill paid-bar" :style="{ width: row.paidPct + '%' }">
                  <span v-if="row.paidPct > 15" class="bar-text">¥{{ fmtShort(row.settled) }}</span>
                </div>
              </div>
              <span class="bar-amount">¥{{ fmtShort(row.total) }}</span>
            </div>
            <div class="chart-legend">
              <span class="legend-dot purchase-dot"></span>采购总额
              <span class="legend-dot paid-dot" style="margin-left:16px"></span>已付
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="60" />
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card>
          <template #header><span>近6个月销售收入趋势</span></template>
          <div v-if="salesChartData.length" class="chart-wrap">
            <div v-for="row in salesChartData" :key="row.month" class="bar-row">
              <span class="bar-label">{{ row.month }}</span>
              <div class="bar-track">
                <div class="bar-fill sales-bar" :style="{ width: row.totalPct + '%' }">
                  <span v-if="row.totalPct > 15" class="bar-text">¥{{ fmtShort(row.total) }}</span>
                </div>
                <div class="bar-fill recv-bar" :style="{ width: row.paidPct + '%' }">
                  <span v-if="row.paidPct > 15" class="bar-text">¥{{ fmtShort(row.settled) }}</span>
                </div>
              </div>
              <span class="bar-amount">¥{{ fmtShort(row.total) }}</span>
            </div>
            <div class="chart-legend">
              <span class="legend-dot sales-dot"></span>销售总额
              <span class="legend-dot recv-dot" style="margin-left:16px"></span>已收
            </div>
          </div>
          <el-empty v-else description="暂无数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 仓库资产 -->
    <el-row :gutter="16" style="margin-top:20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div style="display:flex;align-items:center;justify-content:space-between">
              <span>仓库库存资产</span>
              <span style="font-size:14px;color:#606266">
                总资产：<strong style="color:#409eff;font-size:16px">¥{{ fmt(data.totalWarehouseAsset) }}</strong>
              </span>
            </div>
          </template>
          <el-table
            v-if="data.warehouseAssets && data.warehouseAssets.length"
            :data="data.warehouseAssets"
            style="width:100%"
            size="small"
          >
            <el-table-column prop="warehouseName" label="仓库名称" min-width="150" />
            <el-table-column label="库存资产价值" min-width="160" align="right">
              <template #default="{ row }">
                <span style="font-weight:600;color:#303133">¥{{ fmt(row.assetValue) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="占比" min-width="120" align="right">
              <template #default="{ row }">
                <span style="color:#909399">
                  {{ totalAssetGt0 ? (Number(row.assetValue) / Number(data.totalWarehouseAsset) * 100).toFixed(1) + '%' : '-' }}
                </span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="暂无库存资产数据" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const data = ref({
  totalPurchaseAmount: 0,
  totalPaidAmount: 0,
  totalUnpaidAmount: 0,
  totalSalesAmount: 0,
  totalReceivedAmount: 0,
  totalUnreceivedAmount: 0,
  grossProfit: 0,
  grossMarginPercent: 0,
  totalWarehouseAsset: 0,
  warehouseAssets: [],
  purchaseByMonth: [],
  salesByMonth: []
})

const loading = ref(false)

const fmt = (v) => Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const fmtShort = (v) => {
  const n = Number(v || 0)
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return n.toFixed(0)
}

const grossProfitNeg = computed(() => Number(data.value.grossProfit || 0) < 0)
const gap = computed(() => Number(data.value.totalUnreceivedAmount || 0) - Number(data.value.totalUnpaidAmount || 0))
const gapNeg = computed(() => gap.value < 0)
const totalAssetGt0 = computed(() => Number(data.value.totalWarehouseAsset || 0) > 0)

const makeChartData = (rows) => {
  if (!rows?.length) return []
  const maxTotal = Math.max(...rows.map(r => Number(r.total || 0)), 1)
  return rows.map(r => {
    const total = Number(r.total || 0)
    const settled = Number(r.settled || 0)
    return {
      month: r.month,
      total,
      settled,
      totalPct: Math.round((total / maxTotal) * 90),
      paidPct: Math.round((settled / maxTotal) * 90)
    }
  })
}

const purchaseChartData = computed(() => makeChartData(data.value.purchaseByMonth))
const salesChartData = computed(() => makeChartData(data.value.salesByMonth))

const loadData = async () => {
  loading.value = true
  try {
    const res = await request({ url: '/finance/summary', method: 'get' })
    data.value = res.data
  } catch {
    ElMessage.error('加载资金汇总失败')
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.finance-summary {
  .kpi-row { margin-bottom: 4px; }

  .kpi-card {
    text-align: center;
    padding: 8px 0;

    .kpi-label {
      font-size: 13px;
      color: #909399;
      margin-bottom: 6px;
    }
    .kpi-value {
      font-size: 28px;
      font-weight: 700;
      color: #303133;
      margin-bottom: 6px;
    }
    .kpi-sub {
      font-size: 12px;
      color: #606266;
    }
    .paid  { color: #67c23a; font-weight: 600; }
    .warn  { color: #e6a23c; font-weight: 600; }
    .green { color: #67c23a; }
    .red   { color: #f56c6c; }
  }

  .chart-wrap {
    padding: 4px 0;

    .bar-row {
      display: flex;
      align-items: center;
      margin-bottom: 14px;
      gap: 8px;

      .bar-label {
        width: 62px;
        font-size: 12px;
        color: #606266;
        flex-shrink: 0;
      }

      .bar-track {
        flex: 1;
        position: relative;
        height: 22px;
      }

      .bar-fill {
        position: absolute;
        height: 100%;
        display: flex;
        align-items: center;
        border-radius: 3px;
        transition: width 0.4s;
      }
      .bar-text {
        font-size: 11px;
        color: #fff;
        padding-left: 6px;
        white-space: nowrap;
      }

      .purchase-bar { background: #409eff; top: 0; height: 11px; }
      .paid-bar     { background: #67c23a; top: 11px; height: 11px; }
      .sales-bar    { background: #e6a23c; top: 0; height: 11px; }
      .recv-bar     { background: #67c23a; top: 11px; height: 11px; }

      .bar-amount {
        width: 60px;
        font-size: 12px;
        color: #303133;
        text-align: right;
        flex-shrink: 0;
      }
    }

    .chart-legend {
      font-size: 12px;
      color: #909399;
      display: flex;
      align-items: center;
      gap: 4px;
      margin-top: 8px;

      .legend-dot {
        display: inline-block;
        width: 10px;
        height: 10px;
        border-radius: 50%;
      }
      .purchase-dot { background: #409eff; }
      .paid-dot     { background: #67c23a; }
      .sales-dot    { background: #e6a23c; }
      .recv-dot     { background: #67c23a; }
    }
  }
}
</style>
