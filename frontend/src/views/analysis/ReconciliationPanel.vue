<template>
  <div class="recon-panel" v-loading="loading">

    <div class="toolbar">
      <el-select
        :model-value="entityId"
        :placeholder="`全部${entityLabel}`"
        clearable
        filterable
        style="width: 180px"
        @update:model-value="v => { $emit('update:entityId', v ?? null); $emit('query') }"
      >
        <el-option v-for="e in entities" :key="e.id" :label="e.name" :value="e.id" />
      </el-select>

      <el-select
        :model-value="year"
        style="width: 100px"
        @update:model-value="v => { $emit('update:year', v); $emit('query') }"
      >
        <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
      </el-select>

      <el-select
        v-if="viewMode === 'month'"
        :model-value="month"
        style="width: 90px"
        @update:model-value="v => { $emit('update:month', v); $emit('query') }"
      >
        <el-option v-for="m in 12" :key="m" :label="`${m}月`" :value="m" />
      </el-select>

      <el-radio-group :model-value="viewMode" @update:model-value="onViewModeChange">
        <el-radio-button label="year">按年</el-radio-button>
        <el-radio-button label="month">按月</el-radio-button>
      </el-radio-group>

      <el-button type="primary" @click="$emit('query')">查询</el-button>
      <el-button type="success" @click="$emit('export')">{{ exportLabel }}</el-button>
    </div>

    <!-- 未选对象：供应商/客户汇总 -->
    <template v-if="!entityId">
      <div class="summary-section">
        <div class="section-title">
          {{ periodTitle }} · 各{{ entityLabel }}{{ type === 'purchase' ? '应付' : '应收' }}
          <span class="sub total-hint">
            合计 {{ pendingLabel }} ¥{{ fmt(partnerReport?.pendingAmount) }}
          </span>
        </div>
        <div class="table-wrap">
          <el-table
            :data="partnerRowsPaged"
            border
            size="small"
            highlight-current-row
            style="cursor: pointer"
            @row-click="row => $emit('select-partner', row.entityId)"
          >
            <el-table-column prop="name" :label="entityLabel" min-width="120" />
            <el-table-column prop="orderCount" label="单数" width="60" />
            <el-table-column :label="type === 'purchase' ? '采购额' : '销售额'">
              <template #default="{ row }">¥{{ fmt(row.totalAmount) }}</template>
            </el-table-column>
            <el-table-column :label="type === 'purchase' ? '已付' : '已收'" width="100">
              <template #default="{ row }">¥{{ fmt(row.settledAmount) }}</template>
            </el-table-column>
            <el-table-column :label="pendingLabel" width="100">
              <template #default="{ row }">
                <span :class="{ unpaid: row.pendingAmount > 0 }">¥{{ fmt(row.pendingAmount) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-pagination
          v-if="partnerRows.length > partnerPageSize"
          v-model:current-page="partnerPage"
          v-model:page-size="partnerPageSize"
          :total="partnerRows.length"
          :page-sizes="PAGE_SIZES"
          layout="total, sizes, prev, pager, next"
          class="pagination"
          @size-change="onPartnerPageSizeChange"
        />
        <div class="hint">点击某{{ entityLabel }}查看明细</div>
      </div>
    </template>

    <!-- 已选对象 + 按年：有数据的月份 -->
    <template v-else-if="viewMode === 'year'">
      <div class="summary-section">
        <div class="section-title">{{ entityName }} · {{ year }}年 各月汇总</div>
        <div class="table-wrap">
          <el-table
            v-if="summaryRows.length"
            :data="summaryRowsPaged"
            border
            size="small"
            highlight-current-row
            style="cursor: pointer"
            @row-click="row => $emit('select-month', row.month)"
          >
            <el-table-column prop="month" label="月份" width="70">
              <template #default="{ row }">
                <span :class="{ 'active-row': row.month === selectedMonth }">{{ row.month }}月</span>
              </template>
            </el-table-column>
            <el-table-column prop="orderCount" label="单数" width="60" />
            <el-table-column :label="type === 'purchase' ? '采购额' : '销售额'">
              <template #default="{ row }">¥{{ fmt(row.totalAmount) }}</template>
            </el-table-column>
            <el-table-column :label="pendingLabel" width="100">
              <template #default="{ row }">
                <span :class="{ unpaid: row.pendingAmount > 0 }">¥{{ fmt(row.pendingAmount) }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="该年度暂无记录" :image-size="60" />
        </div>
        <el-pagination
          v-if="summaryRows.length > summaryPageSize"
          v-model:current-page="summaryPage"
          v-model:page-size="summaryPageSize"
          :total="summaryRows.length"
          :page-sizes="PAGE_SIZES"
          layout="total, sizes, prev, pager, next"
          class="pagination"
          @size-change="onSummaryPageSizeChange"
        />
      </div>
      <div v-if="items.length" class="detail-section">
        <div class="section-title">品种明细 <span class="sub">· {{ selectedMonth }}月</span></div>
        <ReconciliationDetail :items="items" />
      </div>
    </template>

    <!-- 已选对象 + 按月：每日 -->
    <template v-else>
      <div class="summary-section">
        <div class="section-title">{{ entityName }} · {{ year }}年{{ month }}月 每日汇总</div>
        <div class="table-wrap">
          <el-table
            v-if="dailyRows.length"
            :data="dailyRowsPaged"
            border
            size="small"
            highlight-current-row
            style="cursor: pointer"
            @row-click="row => $emit('update:selectedDay', row.date || '')"
          >
            <el-table-column label="日期" min-width="100">
              <template #default="{ row }">
                <span :class="{ 'active-row': row.date === selectedDay }">{{ row.date }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="orderCount" label="单数" width="60" />
            <el-table-column label="金额">
              <template #default="{ row }">¥{{ fmt(row.totalAmount) }}</template>
            </el-table-column>
            <el-table-column :label="pendingLabel" width="100">
              <template #default="{ row }">
                <span :class="{ unpaid: row.pendingAmount > 0 }">¥{{ fmt(row.pendingAmount) }}</span>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="该月暂无记录" :image-size="60" />
        </div>
        <el-pagination
          v-if="dailyRows.length > dailyPageSize"
          v-model:current-page="dailyPage"
          v-model:page-size="dailyPageSize"
          :total="dailyRows.length"
          :page-sizes="PAGE_SIZES"
          layout="total, sizes, prev, pager, next"
          class="pagination"
          @size-change="onDailyPageSizeChange"
        />
        <div v-if="dayDates.length" class="day-filter">
          <span :class="['date-tag', { active: !selectedDay }]" @click="$emit('update:selectedDay', '')">全月</span>
          <span
            v-for="d in dayDatesOnPage"
            :key="d"
            :class="['date-tag', { active: selectedDay === d }]"
            @click="$emit('update:selectedDay', d)"
          >{{ d.slice(8) }}日</span>
        </div>
      </div>
      <div v-if="items.length" class="detail-section">
        <div class="section-title">
          品种明细
          <span v-if="selectedDay" class="sub">· {{ selectedDay }}</span>
          <span v-else class="sub">· {{ month }}月全月</span>
        </div>
        <ReconciliationDetail :items="items" :filter-date="selectedDay" />
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import ReconciliationDetail from './ReconciliationDetail.vue'

const props = defineProps({
  type: { type: String, required: true },
  entities: { type: Array, default: () => [] },
  entityLabel: { type: String, default: '' },
  entityId: { type: Number, default: null },
  entityName: { type: String, default: '' },
  year: { type: Number, required: true },
  month: { type: Number, required: true },
  selectedMonth: { type: Number, default: 1 },
  viewMode: { type: String, default: 'year' },
  selectedDay: { type: String, default: '' },
  summaryRows: { type: Array, default: () => [] },
  dailyRows: { type: Array, default: () => [] },
  partnerReport: { type: Object, default: null },
  items: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false }
})

const emit = defineEmits([
  'update:entityId', 'update:year', 'update:month', 'update:viewMode', 'update:selectedDay',
  'query', 'select-month', 'select-partner', 'export'
])

const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 5 }, (_, i) => currentYear - i)

const PAGE_SIZES = [5, 10, 20]

const fmt = (v) => Number(v || 0).toFixed(2)
const pendingLabel = computed(() => props.type === 'purchase' ? '未付' : '未收')
const partnerRows = computed(() => props.partnerReport?.rows || [])

const periodTitle = computed(() =>
  props.viewMode === 'year' ? `${props.year}年` : `${props.year}年${props.month}月`
)

const exportLabel = computed(() => {
  if (!props.entityId) return '导出汇总'
  return props.viewMode === 'year' ? '导出年报' : '导出月报'
})

const dayDates = computed(() =>
  [...new Set((props.dailyRows || []).map(r => r.date).filter(Boolean))].sort()
)

const partnerPage = ref(1)
const partnerPageSize = ref(5)
const summaryPage = ref(1)
const summaryPageSize = ref(5)
const dailyPage = ref(1)
const dailyPageSize = ref(5)

watch(
  () => [props.partnerReport, props.year, props.month, props.viewMode],
  () => { partnerPage.value = 1 }
)

watch(
  () => [props.summaryRows, props.year, props.entityId],
  () => { summaryPage.value = 1 }
)

watch(
  () => [props.dailyRows, props.month, props.year, props.entityId],
  () => { dailyPage.value = 1 }
)

watch(partnerPageSize, () => { partnerPage.value = 1 })
watch(summaryPageSize, () => { summaryPage.value = 1 })
watch(dailyPageSize, () => { dailyPage.value = 1 })

const slicePage = (rows, page, size) => {
  const start = (page - 1) * size
  return rows.slice(start, start + size)
}

const partnerRowsPaged = computed(() =>
  slicePage(partnerRows.value, partnerPage.value, partnerPageSize.value)
)

const summaryRowsPaged = computed(() =>
  slicePage(props.summaryRows || [], summaryPage.value, summaryPageSize.value)
)

const dailyRowsPaged = computed(() =>
  slicePage(props.dailyRows || [], dailyPage.value, dailyPageSize.value)
)

const dayDatesOnPage = computed(() =>
  dailyRowsPaged.value.map(r => r.date).filter(Boolean)
)

const onPartnerPageSizeChange = () => { partnerPage.value = 1 }
const onSummaryPageSizeChange = () => { summaryPage.value = 1 }
const onDailyPageSizeChange = () => { dailyPage.value = 1 }

const onViewModeChange = (mode) => {
  emit('update:viewMode', mode)
  emit('update:selectedDay', '')
  emit('query')
}
</script>

<style scoped lang="scss">
.toolbar {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.summary-section,
.detail-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 8px;
  .sub { font-weight: 400; color: #909399; font-size: 13px; }
  .total-hint { margin-left: 8px; color: #f56c6c; }
}

.hint {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 8px;
  text-align: center;
}

.table-wrap {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.active-row { color: #409eff; font-weight: 700; }
.unpaid { color: #f56c6c; font-weight: 500; }

.day-filter {
  display: flex;
  flex-wrap: nowrap;
  gap: 6px;
  margin-top: 10px;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  padding-bottom: 4px;
}

.date-tag {
  flex-shrink: 0;
  padding: 3px 10px;
  border-radius: 12px;
  border: 1px solid #dcdfe6;
  font-size: 12px;
  color: #606266;
  cursor: pointer;
  user-select: none;
  &.active { background: #409eff; border-color: #409eff; color: #fff; }
}

.pagination {
  margin-top: 10px;
  justify-content: flex-end;
}
</style>
