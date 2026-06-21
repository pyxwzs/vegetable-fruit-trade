<template>
  <div class="stats-panel" v-loading="loading">
    <div class="toolbar">
      <el-select :model-value="year" style="width: 100px" @update:model-value="v => $emit('update:year', v)">
        <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
      </el-select>
      <el-select :model-value="month" clearable placeholder="全年" style="width: 90px"
        @update:model-value="v => $emit('update:month', v ?? null)">
        <el-option v-for="m in 12" :key="m" :label="`${m}月`" :value="m" />
      </el-select>
      <el-select :model-value="entityId" clearable filterable :placeholder="`全部${entityLabel}`"
        style="width: 160px" @update:model-value="v => $emit('update:entityId', v ?? null)">
        <el-option v-for="e in entities" :key="e.id" :label="e.name" :value="e.id" />
      </el-select>
      <el-button type="primary" @click="$emit('query')">查询</el-button>
    </div>

    <div v-if="report" class="summary-row">
      <div class="summary-card">
        <span class="label">{{ type === 'purchase' ? '采购' : '销售' }}总额</span>
        <b>¥{{ fmt(report.grandTotalAmount) }}</b>
      </div>
      <div class="summary-card">
        <span class="label">商品总量</span>
        <b>{{ qtyFmt(report.grandTotalQuantity) }}</b>
      </div>
      <div class="summary-card muted">
        <span class="label">{{ entityLabel }}数</span>
        <b>{{ report.groups?.length || 0 }}</b>
      </div>
    </div>

    <el-empty v-if="!loading && !(report?.groups?.length)" description="暂无统计数据" :image-size="70" />

    <el-collapse v-else-if="report?.groups?.length" v-model="expanded" class="group-collapse">
      <el-collapse-item v-for="g in report.groups" :key="g.entityId" :name="g.entityId">
        <template #title>
          <div class="group-title">
            <span class="name">{{ g.entityName }}</span>
            <span class="meta">共 {{ g.products?.length || 0 }} 种 · 总量 {{ qtyFmt(g.totalQuantity) }}</span>
            <span class="amount">¥{{ fmt(g.totalAmount) }}</span>
          </div>
        </template>
        <div class="table-wrap">
          <el-table :data="g.products" border size="small">
            <el-table-column prop="productName" label="商品" min-width="120" />
            <el-table-column label="数量" width="110">
              <template #default="{ row }">{{ qtyRow(row) }}</template>
            </el-table-column>
            <el-table-column label="金额" width="110">
              <template #default="{ row }">
                <span class="amount-cell">¥{{ fmt(row.totalAmount) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-collapse-item>
    </el-collapse>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  type: { type: String, required: true },
  year: { type: Number, required: true },
  month: { type: Number, default: null },
  entityId: { type: Number, default: null },
  entities: { type: Array, default: () => [] },
  entityLabel: { type: String, default: '' },
  report: { type: Object, default: null },
  loading: { type: Boolean, default: false }
})

defineEmits(['update:year', 'update:month', 'update:entityId', 'query'])

const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 5 }, (_, i) => currentYear - i)
const expanded = ref([])

watch(() => props.report, (r) => {
  expanded.value = (r?.groups || []).map(g => g.entityId)
}, { immediate: true })

const fmt = (v) => Number(v || 0).toFixed(2)
const qtyFmt = (v) => Number(v || 0).toFixed(1)
const qtyRow = (row) => {
  const q = Number(row.quantity || 0).toFixed(1)
  return row.unit ? `${q} ${row.unit}` : q
}
</script>

<style scoped lang="scss">
.toolbar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 14px;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin-bottom: 14px;
  @media (max-width: 767px) { grid-template-columns: 1fr; }
}

.summary-card {
  padding: 12px 14px;
  border-radius: 8px;
  background: linear-gradient(135deg, #ecf5ff, #f5f9ff);
  border: 1px solid #d9ecff;
  .label { display: block; font-size: 12px; color: #909399; margin-bottom: 4px; }
  b { font-size: 20px; color: #409eff; }
  &.muted b { color: #606266; font-size: 18px; }
}

.group-collapse {
  border: none;
  :deep(.el-collapse-item__header) {
    height: auto;
    min-height: 44px;
    line-height: 1.4;
    padding: 6px 0;
  }
}

.group-title {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding-right: 8px;
  flex-wrap: wrap;
  .name { font-weight: 600; color: #303133; }
  .meta { font-size: 12px; color: #909399; flex: 1; }
  .amount { font-weight: 700; color: #409eff; }
}

.table-wrap { overflow-x: auto; }
.amount-cell { font-weight: 600; }
</style>
