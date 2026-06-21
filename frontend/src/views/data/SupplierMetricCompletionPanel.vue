<template>
  <div class="completion-panel" v-loading="loading">
    <div class="search-bar">
      <el-radio-group v-model="periodType" @change="handleSearch">
        <el-radio-button label="MONTH">月指标</el-radio-button>
        <el-radio-button label="YEAR">年指标</el-radio-button>
      </el-radio-group>
      <el-date-picker
          v-if="periodType === 'MONTH'"
          v-model="monthPeriod"
          type="month"
          placeholder="选择月份"
          value-format="YYYY-M"
          class="filter-picker"
          @change="handleSearch"
      />
      <el-date-picker
          v-else
          v-model="yearPeriod"
          type="year"
          placeholder="选择年份"
          value-format="YYYY"
          class="filter-picker filter-picker--year"
          @change="handleSearch"
      />
      <el-select v-model="supplierId" placeholder="全部供应商" clearable filterable class="filter-select">
        <el-option v-for="s in supplierOptions" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>

    <div v-if="report" class="summary-grid">
      <div class="summary-card wide period-card">
        <div class="label">周期</div>
        <div class="value">{{ report.periodLabel }}</div>
        <div class="summary-tip">各商品单位不同，仅统计种数与达标情况，数量请查看下方明细。</div>
      </div>
      <div class="summary-card">
        <div class="label">供应商</div>
        <div class="value">{{ summaryStats.supplierCount }}</div>
      </div>
      <div class="summary-card">
        <div class="label">指标数</div>
        <div class="value">{{ summaryStats.metricCount }}</div>
      </div>
      <div class="summary-card">
        <div class="label">已达标</div>
        <div class="value primary">{{ summaryStats.completedCount }}</div>
      </div>
      <div class="summary-card">
        <div class="label">未达标</div>
        <div class="value shortfall">{{ summaryStats.pendingCount }}</div>
      </div>
      <div class="summary-card wide">
        <div class="label">达标率 {{ summaryStats.reachRate }}%</div>
        <el-progress
            :percentage="summaryStats.reachRate"
            :status="summaryStats.reachRate >= 100 ? 'success' : undefined"
            :stroke-width="12"
        />
      </div>
    </div>

    <el-empty v-if="!loading && !groups.length" description="该周期暂无订量指标，请先在「订量设置」中添加" />

    <el-collapse v-if="!loading && groups.length" v-model="openSuppliers" class="supplier-collapse">
      <el-collapse-item
          v-for="g in groups"
          :key="g.supplierId"
          :name="g.supplierId"
      >
        <template #title>
          <div class="supplier-head">
            <span class="name">{{ g.supplierName }}</span>
            <span class="meta">共 {{ groupStats(g).productCount }} 种 · 已达标 {{ groupStats(g).completedCount }} 种</span>
          </div>
        </template>

        <div v-if="isMobile" class="mobile-card-list">
          <div v-for="row in g.items" :key="row.id" class="item-card">
            <div class="item-card-title">{{ productTitle(row) }}</div>
            <div class="card-row">
              <span class="label">订量</span>
              <span class="value">{{ qtyFmt(row.targetQty, row) }}</span>
            </div>
            <div class="card-row">
              <span class="label">实际</span>
              <span class="value">{{ qtyFmt(row.actualQty, row) }}</span>
            </div>
            <div class="card-row">
              <span class="label">差额</span>
              <span class="value" :class="gapClass(row.gapQty)">{{ gapFmt(row.gapQty, row) }}</span>
            </div>
            <div class="progress-row">
              <span class="label">完成度 {{ row.completionPercent }}%</span>
              <el-progress
                  :percentage="Math.min(100, row.completionPercent || 0)"
                  :status="row.completionPercent >= 100 ? 'success' : undefined"
                  :stroke-width="8"
              />
            </div>
          </div>
        </div>
        <div v-else class="table-wrap">
          <el-table :data="g.items" border size="small" class="item-table">
            <el-table-column label="商品" min-width="120">
              <template #default="{ row }">{{ productTitle(row) }}</template>
            </el-table-column>
            <el-table-column label="订量" width="100">
              <template #default="{ row }">{{ qtyFmt(row.targetQty, row) }}</template>
            </el-table-column>
            <el-table-column label="实际" width="100">
              <template #default="{ row }">{{ qtyFmt(row.actualQty, row) }}</template>
            </el-table-column>
            <el-table-column label="差额" width="100">
              <template #default="{ row }">
                <span :class="gapClass(row.gapQty)">{{ gapFmt(row.gapQty, row) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="完成度" min-width="140">
              <template #default="{ row }">
                <el-progress
                    :percentage="Math.min(100, row.completionPercent || 0)"
                    :status="row.completionPercent >= 100 ? 'success' : undefined"
                    :stroke-width="8"
                />
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getSupplierMetricCompletion } from '@/api/supplierProductMetric'
import { getActiveSuppliers } from '@/api/supplier'
import { usePageRefresh } from '@/composables/usePageRefresh'
import { useIsMobile } from '@/composables/useIsMobile'

const isMobile = useIsMobile()
const now = new Date()
const loading = ref(false)
const report = ref(null)
const periodType = ref('MONTH')
const monthPeriod = ref(`${now.getFullYear()}-${now.getMonth() + 1}`)
const yearPeriod = ref(String(now.getFullYear()))
const supplierId = ref(null)
const supplierOptions = ref([])
const openSuppliers = ref([])

const groups = computed(() => report.value?.groups || [])

const summaryStats = computed(() => {
  const items = groups.value.flatMap(g => g.items || [])
  const metricCount = items.length
  const completedCount = items.filter(row => Number(row.completionPercent || 0) >= 100).length
  return {
    supplierCount: groups.value.length,
    metricCount,
    completedCount,
    pendingCount: metricCount - completedCount,
    reachRate: metricCount > 0 ? Math.round(completedCount / metricCount * 100) : 0
  }
})

const numFmt = (v) => Number(v || 0).toFixed(1)

const productTitle = (row) => {
  const name = row?.productName || '—'
  return row?.productUnit ? `${name}（${row.productUnit}）` : name
}

const groupStats = (group) => {
  const items = group?.items || []
  const productCount = items.length
  const completedCount = items.filter(row => Number(row.completionPercent || 0) >= 100).length
  return { productCount, completedCount }
}

const qtyFmt = (v, row) => {
  const n = numFmt(v)
  return row?.productUnit ? `${n} ${row.productUnit}` : n
}

const gapFmt = (v, row) => {
  const text = Math.abs(Number(v || 0)).toFixed(1)
  return row?.productUnit ? `${text} ${row.productUnit}` : text
}

const gapClass = (v) => {
  const n = Number(v || 0)
  if (n > 0) return 'text-excess'
  if (n < 0) return 'text-shortfall'
  return ''
}

const loadOptions = async () => {
  try {
    const res = await getActiveSuppliers()
    supplierOptions.value = res.data || []
  } catch {
    supplierOptions.value = []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      periodType: periodType.value,
      supplierId: supplierId.value || undefined
    }
    if (periodType.value === 'MONTH') {
      const [y, m] = (monthPeriod.value || '').split('-').map(Number)
      params.year = y
      params.month = m
    } else {
      params.year = Number(yearPeriod.value)
    }
    const res = await getSupplierMetricCompletion(params)
    report.value = res.data || { groups: [] }
    openSuppliers.value = groups.value.map(g => g.supplierId)
  } catch {
    report.value = { groups: [] }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => loadData()

usePageRefresh(loadData)

defineExpose({ reload: loadData })

onMounted(loadOptions)
</script>

<style scoped lang="scss">
.completion-panel {
  padding: 4px 2px 8px;
}

.search-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  align-items: center;
}

.filter-picker {
  width: 140px;
}

.filter-picker--year {
  width: 120px;
}

.filter-select {
  width: 160px;
  min-width: 140px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 16px;

  @media (min-width: 768px) {
    grid-template-columns: repeat(4, 1fr);
  }
}

.summary-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 14px 16px;

  &.wide {
    grid-column: 1 / -1;
  }

  &.period-card .value {
    margin-bottom: 6px;
  }

  .summary-tip {
    font-size: 12px;
    color: #909399;
    line-height: 1.5;
    margin-top: 4px;
  }

  .label {
    font-size: 12px;
    color: #909399;
    margin-bottom: 8px;
  }

  .value {
    font-size: 17px;
    font-weight: 700;
    color: #303133;
    line-height: 1.35;
    word-break: break-all;

    &.primary { color: #409eff; }
    &.excess { color: #e6a23c; }
    &.shortfall { color: #f56c6c; }
  }
}

.supplier-collapse {
  border: none;

  :deep(.el-collapse-item) {
    margin-bottom: 10px;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    overflow: hidden;
    background: #fff;
  }

  :deep(.el-collapse-item__header) {
    height: auto;
    line-height: 1.45;
    padding: 14px 16px;
    border-bottom: none;
  }

  :deep(.el-collapse-item__wrap) {
    border-top: 1px solid #f0f0f0;
  }

  :deep(.el-collapse-item__content) {
    padding: 12px 16px 16px;
  }
}

.supplier-head {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
  width: 100%;
  padding-right: 8px;

  .name {
    font-weight: 600;
    color: #303133;
    font-size: 15px;
  }

  .meta {
    font-size: 12px;
    color: #909399;
    font-weight: normal;
    line-height: 1.4;
  }
}

.group-summary {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  font-size: 13px;
  color: #606266;
  margin-bottom: 12px;
  line-height: 1.5;
}

.item-card {
  background: #f9fafb;
  border: 1px solid #eef0f3;
  border-radius: 8px;
  padding: 12px 14px;
}

.item-card-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
}

.card-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  margin-bottom: 6px;
  line-height: 1.45;

  .label {
    color: #909399;
    flex-shrink: 0;
  }

  .value {
    color: #303133;
    text-align: right;
    word-break: break-all;
  }
}

.progress-row {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid #eef0f3;

  .label {
    display: block;
    font-size: 12px;
    color: #909399;
    margin-bottom: 6px;
  }
}

.table-wrap {
  overflow-x: auto;
}

.item-table {
  width: 100%;

  :deep(.el-table__cell) {
    padding: 10px 12px;
  }

  :deep(.cell) {
    padding: 0 4px;
    line-height: 1.45;
  }
}

.text-excess { color: #e6a23c; font-weight: 600; }
.text-shortfall { color: #f56c6c; font-weight: 600; }

@media (max-width: 767px) {
  .completion-panel {
    padding: 0 0 8px;
  }

  .filter-picker,
  .filter-picker--year,
  .filter-select {
    width: 100%;
    min-width: 0;
  }

  .summary-card .value {
    font-size: 16px;
  }
}
</style>
