<template>
  <div class="recon-detail">
    <div v-if="groupedDays.length" class="detail-scroll">
      <div v-for="day in groupedDays" :key="day.date" class="day-block">
        <div class="day-head">
          <span class="day-date">{{ day.date }}</span>
          <span class="day-total">当日 ¥{{ day.total.toFixed(2) }}</span>
        </div>
        <div class="product-table">
          <div class="product-header">
            <span class="col-name">品种</span>
            <span class="col-price">单价</span>
            <span class="col-qty">重量</span>
            <span class="col-amt">金额(元)</span>
          </div>
          <div v-for="(row, i) in day.items" :key="i" class="product-row">
            <span class="col-name">{{ row.productName }}</span>
            <span class="col-price">{{ priceText(row) }}</span>
            <span class="col-qty">{{ qtyText(row) }}</span>
            <span class="col-amt">¥{{ Number(row.amount).toFixed(2) }}</span>
          </div>
        </div>
      </div>
      <div class="month-total">
        <span>本月合计</span>
        <b>¥{{ monthTotal.toFixed(2) }}</b>
      </div>
    </div>
    <el-empty v-else description="暂无明细" :image-size="60" />
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  items: { type: Array, default: () => [] },
  filterDate: { type: String, default: '' }
})

const filtered = computed(() => {
  if (!props.filterDate) return props.items || []
  return (props.items || []).filter(i => i.date === props.filterDate)
})

const groupedDays = computed(() => {
  const map = new Map()
  for (const item of filtered.value) {
    const d = item.date || '未知'
    if (!map.has(d)) map.set(d, { date: d, items: [], total: 0 })
    const g = map.get(d)
    g.items.push(item)
    g.total += Number(item.amount || 0)
  }
  return [...map.values()].sort((a, b) => String(a.date).localeCompare(String(b.date)))
})

const monthTotal = computed(() =>
  groupedDays.value.reduce((s, d) => s + d.total, 0)
)

const qtyText = (row) => {
  const q = Number(row.quantity).toFixed(3)
  return row.unit ? `${q} ${row.unit}` : q
}

const priceText = (row) => {
  const p = Number(row.price).toFixed(2)
  return row.unit ? `${p} 元/${row.unit}` : `${p} 元`
}
</script>

<style scoped lang="scss">
.recon-detail {
  .detail-scroll {
    max-height: 480px;
    overflow-y: auto;
    @media (max-width: 767px) { max-height: none; }
  }
}

.day-block {
  margin-bottom: 12px;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  overflow: hidden;
}

.day-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;

  .day-date { font-weight: 600; font-size: 13px; color: #303133; }
  .day-total { font-size: 12px; color: #409eff; font-weight: 600; }
}

.product-table { font-size: 13px; }

.product-header,
.product-row {
  display: grid;
  grid-template-columns: 1fr 72px 88px 72px;
  gap: 4px;
  padding: 6px 12px;
  align-items: center;
}

.product-header {
  background: #fafafa;
  color: #909399;
  font-size: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.product-row {
  border-bottom: 1px solid #f5f5f5;
  &:last-child { border-bottom: none; }
}

.col-name { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-price, .col-qty, .col-amt { text-align: right; }
.col-amt { font-weight: 600; }

.month-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  background: #ecf5ff;
  border-radius: 6px;
  font-size: 14px;
  b { font-size: 18px; color: #409eff; }
}
</style>
