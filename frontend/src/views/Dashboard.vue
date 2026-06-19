<template>
  <div class="dashboard" v-loading="overviewLoading">

    <!-- 应该赚了 vs 实际赚了 -->
    <el-card class="hero-card" shadow="never">
      <div class="hero-month-bar">{{ monthLabel }}</div>
      <div class="hero-dual">
        <!-- 按单据 -->
        <div class="hero-profit book" :class="{ negative: bookProfit < 0 }">
          <div class="hero-label">应赚</div>
          <div class="hero-sub">销售 − 采购 − 支出</div>
          <div class="hero-value">
            {{ bookProfit >= 0 ? '+' : '' }}¥{{ fmt(bookProfit) }}
          </div>
          <div class="hero-breakdown">
            <div class="breakdown-item">
              <span>销售</span>
              <b>¥{{ fmt(overview.salesTotal) }}</b>
            </div>
            <div class="breakdown-sep">−</div>
            <div class="breakdown-item">
              <span>采购</span>
              <b>¥{{ fmt(overview.purchaseTotal) }}</b>
            </div>
            <div class="breakdown-sep">−</div>
            <div class="breakdown-item">
              <span>支出</span>
              <b>¥{{ fmt(overview.otherExpenses) }}</b>
            </div>
          </div>
        </div>

        <!-- 按收付 -->
        <div class="hero-profit cash" :class="{ negative: cashProfit < 0 }">
          <div class="hero-label">实赚</div>
          <div class="hero-sub">已收 − 已付 − 支出</div>
          <div class="hero-value">
            {{ cashProfit >= 0 ? '+' : '' }}¥{{ fmt(cashProfit) }}
          </div>
          <div class="hero-breakdown">
            <div class="breakdown-item">
              <span>已收</span>
              <b>¥{{ fmt(overview.collectedFromCustomers) }}</b>
            </div>
            <div class="breakdown-sep">−</div>
            <div class="breakdown-item">
              <span>已付</span>
              <b>¥{{ fmt(overview.paidToFarmers) }}</b>
            </div>
            <div class="breakdown-sep">−</div>
            <div class="breakdown-item">
              <span>支出</span>
              <b>¥{{ fmt(overview.otherExpenses) }}</b>
            </div>
          </div>
          <div class="pending-row">
            <span class="pending-chip">待收 ¥{{ fmt(overview.uncollectedFromCustomers) }}</span>
            <span class="pending-chip">待付 ¥{{ fmt(overview.unpaidToFarmers) }}</span>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 往来与库存 -->
    <el-card class="stat-card" shadow="never">
      <div class="stat-grid">
        <div class="stat-cell">
          <span class="stat-label">未收客户</span>
          <span class="stat-value text-danger">¥{{ fmt(overview.uncollectedFromCustomers) }}</span>
        </div>
        <div class="stat-cell">
          <span class="stat-label">未付农户</span>
          <span class="stat-value text-danger">¥{{ fmt(overview.unpaidToFarmers) }}</span>
        </div>
        <div class="stat-cell clickable" @click="router.push('/inventory')">
          <span class="stat-label">库存价值</span>
          <span class="stat-value text-purple">¥{{ fmt(inventoryValue) }}</span>
        </div>
        <div class="stat-cell">
          <span class="stat-label">应实差</span>
          <span class="stat-value" :class="profitGap >= 0 ? 'text-profit' : 'text-danger'">
            {{ profitGap >= 0 ? '+' : '' }}¥{{ fmt(profitGap) }}
          </span>
        </div>
      </div>
    </el-card>

    <!-- 待办 -->
    <el-card class="todo-card" shadow="never">
      <template #header>
        <span class="card-title">待办事项</span>
      </template>

      <div class="todo-grid">
        <section class="todo-block">
          <div class="todo-head">
            <div class="todo-head-left">
              <span class="todo-title">待入库</span>
              <span v-if="purchasePending.count" class="todo-badge">
                {{ purchasePending.count }} 笔 · ¥{{ fmt(purchasePending.totalAmount) }}
              </span>
            </div>
            <el-button type="primary" link @click="router.push('/purchase')">全部 ›</el-button>
          </div>
          <div v-if="pendingPurchases.length" class="todo-list">
            <div v-for="row in pendingPurchases" :key="row.id" class="todo-row">
              <span class="todo-name">{{ row.supplier?.name || '—' }}</span>
              <span class="todo-date">{{ row.orderDate }}</span>
              <span class="todo-amount">¥{{ fmt(row.totalAmount) }}</span>
            </div>
          </div>
          <div v-else class="todo-empty">暂无待入库</div>
        </section>

        <section class="todo-block">
          <div class="todo-head">
            <div class="todo-head-left">
              <span class="todo-title">待出库</span>
              <span v-if="salesPending.count" class="todo-badge">
                {{ salesPending.count }} 笔 · ¥{{ fmt(salesPending.totalAmount) }}
              </span>
            </div>
            <el-button type="primary" link @click="router.push('/sales')">全部 ›</el-button>
          </div>
          <div v-if="pendingSales.length" class="todo-list">
            <div v-for="row in pendingSales" :key="row.id" class="todo-row">
              <span class="todo-name">{{ row.customer?.name || '—' }}</span>
              <span class="todo-date">{{ row.orderDate }}</span>
              <span class="todo-amount">¥{{ fmt(row.totalAmount) }}</span>
            </div>
          </div>
          <div v-else class="todo-empty">暂无待出库</div>
        </section>

        <section class="todo-block">
          <div class="todo-head">
            <span class="todo-title">待付款农户</span>
            <el-button type="primary" link @click="router.push('/purchase')">去付款 ›</el-button>
          </div>
          <div v-if="farmerRanking.length" class="todo-list">
            <div v-for="(row, i) in farmerRanking" :key="i" class="todo-row">
              <span class="todo-name">{{ row.name }}</span>
              <span class="todo-amount text-danger">¥{{ fmt(row.unpaid) }}</span>
            </div>
          </div>
          <div v-else class="todo-empty">本月无欠款</div>
        </section>

        <section class="todo-block">
          <div class="todo-head">
            <span class="todo-title">待收款客户</span>
            <el-button type="primary" link @click="router.push('/sales')">去收款 ›</el-button>
          </div>
          <div v-if="customerRanking.length" class="todo-list">
            <div v-for="(row, i) in customerRanking" :key="i" class="todo-row">
              <span class="todo-name">{{ row.name }}</span>
              <span class="todo-amount text-danger">¥{{ fmt(row.unpaid) }}</span>
            </div>
          </div>
          <div v-else class="todo-empty">本月无欠款</div>
        </section>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getPurchaseOrders, getPurchasePendingStats } from '@/api/purchase'
import { getSalesOrders, getSalesPendingStats } from '@/api/sales'
import { getMonthlyOverview } from '@/api/analytics'
import { getInventoryOverview } from '@/api/inventory'

const router = useRouter()

const overviewLoading = ref(false)
const overview = ref({
  purchaseTotal: 0,
  salesTotal: 0,
  paidToFarmers: 0,
  collectedFromCustomers: 0,
  unpaidToFarmers: 0,
  uncollectedFromCustomers: 0,
  otherExpenses: 0,
  cashDifference: 0,
  netProfit: 0,
  farmerUnpaidRanking: [],
  customerUnreceivedRanking: []
})
const inventoryValue = ref(0)
const purchasePending = ref({ count: 0, totalAmount: 0 })
const salesPending = ref({ count: 0, totalAmount: 0 })
const pendingPurchases = ref([])
const pendingSales = ref([])

const fmt = (v) => Number(v || 0).toFixed(2)

const monthLabel = computed(() => {
  const now = new Date()
  return `${now.getFullYear()}年${now.getMonth() + 1}月`
})

const bookProfit = computed(() =>
  Number(overview.value.netProfit ?? (
    Number(overview.value.salesTotal || 0)
    - Number(overview.value.purchaseTotal || 0)
    - Number(overview.value.otherExpenses || 0)
  ))
)

const cashProfit = computed(() =>
  Number(overview.value.collectedFromCustomers || 0)
  - Number(overview.value.paidToFarmers || 0)
  - Number(overview.value.otherExpenses || 0)
)

const profitGap = computed(() => bookProfit.value - cashProfit.value)

const farmerRanking = computed(() =>
  (overview.value.farmerUnpaidRanking || [])
    .filter(r => Number(r.unpaid) > 0)
    .slice(0, 5)
)

const customerRanking = computed(() =>
  (overview.value.customerUnreceivedRanking || [])
    .filter(r => Number(r.unpaid) > 0)
    .slice(0, 5)
)

const loadOverview = async () => {
  overviewLoading.value = true
  const now = new Date()
  try {
    const res = await getMonthlyOverview({ year: now.getFullYear(), month: now.getMonth() + 1 })
    if (res.data) overview.value = res.data
  } catch { /* silent */ } finally { overviewLoading.value = false }
}

const loadExtra = async () => {
  try {
    const [inv, pp, sp, purchase, sales] = await Promise.all([
      getInventoryOverview(),
      getPurchasePendingStats(),
      getSalesPendingStats(),
      getPurchaseOrders({ status: 'PENDING', page: 0, size: 5 }),
      getSalesOrders({ status: 'PENDING', page: 0, size: 5 })
    ])
    inventoryValue.value = inv.data?.totalValue || 0
    purchasePending.value = {
      count: Number(pp.data?.count || 0),
      totalAmount: Number(pp.data?.totalAmount || 0)
    }
    salesPending.value = {
      count: Number(sp.data?.count || 0),
      totalAmount: Number(sp.data?.totalAmount || 0)
    }
    pendingPurchases.value = purchase.data?.content || []
    pendingSales.value = sales.data?.content || []
  } catch { /* silent */ }
}

onMounted(() => {
  loadOverview()
  loadExtra()
})
</script>

<style scoped lang="scss">
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hero-card,
.stat-card,
.todo-card {
  :deep(.el-card__body) { padding: 0; }
  :deep(.el-card__header) {
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;
  }
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.hero-month-bar {
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  padding: 12px 16px 0;
}

.hero-dual {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1px;
  background: #f0f0f0;
}

/* 应该赚了 / 实际赚了 */
.hero-profit {
  color: #fff;
  padding: 16px 12px 14px;
  text-align: center;

  &.book {
    background: linear-gradient(145deg, #5daf34 0%, #67c23a 50%, #85ce61 100%);
    &.negative {
      background: linear-gradient(145deg, #d9534f 0%, #f56c6c 50%, #f78989 100%);
    }
  }

  &.cash {
    background: linear-gradient(145deg, #337ecc 0%, #409eff 50%, #66b1ff 100%);
    &.negative {
      background: linear-gradient(145deg, #c45656 0%, #e06c75 50%, #f78989 100%);
    }
  }
}

.hero-sub {
  font-size: 11px;
  opacity: 0.8;
  margin-top: 2px;
}

.hero-label {
  font-size: 13px;
  opacity: 0.9;
}

.hero-value {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.5px;
  margin: 8px 0 12px;
  line-height: 1.2;
}

.hero-breakdown {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex-wrap: wrap;
}

.breakdown-item {
  background: rgba(255, 255, 255, 0.18);
  border-radius: 8px;
  padding: 6px 10px;
  min-width: 72px;

  span {
    display: block;
    font-size: 11px;
    opacity: 0.85;
  }
  b {
    display: block;
    font-size: 13px;
    font-weight: 700;
    margin-top: 2px;
  }
}

.breakdown-sep {
  font-size: 14px;
  opacity: 0.6;
  font-weight: 300;
}

.pending-row {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 10px;
  flex-wrap: wrap;
}

.pending-chip {
  font-size: 11px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  padding: 3px 10px;
  opacity: 0.95;
}

/* 往来四格 */
.stat-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1px;
  background: #f0f0f0;
}

.stat-cell {
  background: #fff;
  padding: 14px 10px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 4px;

  &.clickable {
    cursor: pointer;
    &:active { background: #faf5fc; }
  }
}

.stat-label {
  font-size: 12px;
  color: #909399;
}

.stat-value {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  word-break: break-all;
}

/* 待办 */
.todo-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 1px;
  background: #f0f0f0;
}

.todo-block {
  background: #fff;
  padding: 12px 14px;
  min-width: 0;
}

.todo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}

.todo-head-left {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.todo-title {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.todo-badge {
  font-size: 11px;
  color: #909399;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.todo-row {
  display: grid;
  grid-template-columns: 1fr auto;
  grid-template-rows: auto auto;
  gap: 0 8px;
  padding: 8px 0;
  border-bottom: 1px solid #f5f5f5;
  font-size: 13px;

  &:last-child { border-bottom: none; padding-bottom: 0; }
}

.todo-name {
  grid-column: 1;
  grid-row: 1;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.todo-date {
  grid-column: 1;
  grid-row: 2;
  font-size: 11px;
  color: #c0c4cc;
}

.todo-amount {
  grid-column: 2;
  grid-row: 1 / 3;
  align-self: center;
  font-weight: 600;
  color: #303133;
  white-space: nowrap;
}

.todo-empty {
  font-size: 12px;
  color: #c0c4cc;
  text-align: center;
  padding: 12px 0 4px;
}

.text-danger { color: #f56c6c !important; }
.text-profit { color: #67c23a !important; }
.text-purple { color: #9a60b4 !important; }

@media (min-width: 768px) {
  .stat-grid {
    grid-template-columns: repeat(4, 1fr);
  }

  .stat-value { font-size: 17px; }

  .hero-value { font-size: 30px; }

  .hero-dual { grid-template-columns: 1fr 1fr; }

  .todo-row {
    grid-template-columns: 1fr auto auto;
    grid-template-rows: auto;
    align-items: center;
  }

  .todo-date {
    grid-column: 2;
    grid-row: 1;
    font-size: 12px;
  }

  .todo-amount {
    grid-column: 3;
    grid-row: 1;
  }
}

@media (max-width: 600px) {
  .hero-dual {
    grid-template-columns: 1fr;
  }

  .hero-value { font-size: 24px; }

  .breakdown-item {
    min-width: calc(33% - 16px);
    flex: 1;
  }

  .breakdown-sep { display: none; }

  .hero-breakdown {
    gap: 8px;
  }

  .todo-grid {
    grid-template-columns: 1fr;
  }

  .stat-value { font-size: 14px; }
}
</style>
