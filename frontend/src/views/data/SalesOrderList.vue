<template>
  <div class="sales-list">

    <!-- 待出库统计条 -->
    <div class="pending-bar" v-if="pendingStats.count > 0" @click="filterPending">
      <span class="pending-icon">⏳</span>
      <span>待出库订单：<b>{{ pendingStats.count }} 笔</b></span>
      <span class="pending-sep">·</span>
      <span>合计金额：<b class="pending-amount">¥{{ pendingStats.totalAmount.toFixed(2) }}</b></span>
      <span class="pending-action">点击查看 ›</span>
    </div>

    <!-- 销售统计图表 -->
    <SummaryChart ref="summaryChartRef" type="sales" accent="#67c23a"
        @filter-unpaid="filterUnpaid" @filter-period="filterPeriod" />

    <el-card>
      <div class="page-actions">
        <el-button type="primary" @click="openCreate">新建销售单</el-button>
      </div>

      <div class="search-bar">
        <el-input v-model="searchKeyword" placeholder="订单号/客户" style="width: 200px"
            clearable @keyup.enter="handleSearch" />
        <el-select v-model="status" placeholder="货物状态" clearable style="width: 110px">
          <el-option label="待出库" value="PENDING" />
          <el-option label="已出库" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-select v-model="paymentStatusFilter" placeholder="收款状态" clearable style="width: 120px">
          <el-option label="未收＋部分收" value="UNPAID,PARTIAL" />
          <el-option label="未收款" value="UNPAID" />
          <el-option label="部分收款" value="PARTIAL" />
          <el-option label="已收清" value="PAID" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>
      <el-tag v-if="periodLabel" type="primary" closable @close="clearPeriodFilter" style="margin-bottom:8px;margin-right:8px">
        日期：{{ periodLabel }}
      </el-tag>
      <el-tag v-if="paymentStatusFilter" type="warning" closable @close="clearPaymentFilter" style="margin-bottom:8px">
        仅显示：{{ {'UNPAID,PARTIAL':'未收款+部分收款',UNPAID:'未收款',PARTIAL:'部分收款',PAID:'已收清'}[paymentStatusFilter] }}
      </el-tag>

      <div v-if="isMobile" v-loading="loading" class="mobile-card-list">
        <MobileListCard v-for="row in orders" :key="row.id" :title="row.customer?.name || '—'">
          <template #tag>
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
            <el-tag :type="payTag(row.paymentStatus)" size="small">{{ payText(row.paymentStatus) }}</el-tag>
          </template>
          <div class="card-row"><span class="label">单号</span><span class="value">{{ row.orderNo }}</span></div>
          <div class="card-row"><span class="label">日期</span><span class="value">{{ formatDate(row.orderDate) }}</span></div>
          <div class="card-row"><span class="label">总金额</span><span class="value">¥{{ Number(row.totalAmount).toFixed(2) }}</span></div>
          <div class="card-row"><span class="label">未收</span><span class="value" :class="{ 'text-danger': unreceived(row) > 0 }">¥{{ unreceived(row).toFixed(2) }}</span></div>
          <template #actions>
            <el-button type="primary" size="small" @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING'" type="success" size="small" @click="doComplete(row)">出库</el-button>
            <el-button v-if="row.status !== 'CANCELLED' && row.paymentStatus !== 'PAID'" type="warning" size="small" @click="openCollect(row)">收款</el-button>
            <el-button v-if="row.status === 'PENDING'" type="danger" size="small" plain @click="doCancel(row)">取消</el-button>
          </template>
        </MobileListCard>
        <el-empty v-if="!loading && !orders.length" description="暂无数据" />
      </div>
      <div v-else class="table-wrap">
      <el-table :data="orders" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="customer.name" label="客户" min-width="120" />
        <el-table-column prop="orderDate" label="日期" width="110">
          <template #default="{ row }">{{ formatDate(row.orderDate) }}</template>
        </el-table-column>
        <el-table-column label="总金额" width="110">
          <template #default="{ row }">¥{{ Number(row.totalAmount).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="已收" width="100">
          <template #default="{ row }">¥{{ Number(row.receivedAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="未收" width="100">
          <template #default="{ row }">
            <span :class="{ 'text-danger': unreceived(row) > 0 }">¥{{ unreceived(row).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="收款状态" width="100">
          <template #default="{ row }">
            <el-tag :type="payTag(row.paymentStatus)" size="small">{{ payText(row.paymentStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="货物状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button v-if="row.status === 'PENDING'" type="success" link @click="doComplete(row)">完成出库</el-button>
            <el-button v-if="row.status !== 'CANCELLED' && row.paymentStatus !== 'PAID'"
                type="warning" link @click="openCollect(row)">登记收款</el-button>
            <el-button v-if="row.status === 'PENDING'" type="danger" link @click="doCancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      </div>
      <el-pagination v-model:current-page="page" v-model:page-size="size"
          :total="total" :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData" @current-change="loadData" class="pagination" />
    </el-card>

    <!-- 新建 -->
    <el-dialog v-model="createVisible" title="新建销售单" :width="isMobile ? '94%' : '660px'" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="客户" required>
          <el-select v-model="createForm.customerId" filterable placeholder="选择客户" style="width: 100%">
            <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" required>
          <el-date-picker v-model="createForm.orderDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="明细" required>
          <div style="width:100%">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
              <span style="font-size:13px;color:#606266">共 {{ createForm.items.length }} 行</span>
              <el-button size="small" type="primary" plain @click="addLine">+ 添加商品</el-button>
            </div>

            <!-- 移动端：卡片式 -->
            <div v-if="isMobile" class="mobile-items">
              <div v-for="(row, idx) in createForm.items" :key="idx" class="item-card">
                <div class="item-card-header">
                  <span class="item-no">第 {{ idx+1 }} 行</span>
                  <el-button type="danger" link size="small" @click="removeLine(idx)">删除</el-button>
                </div>
                <div class="item-field">
                  <span class="item-label">商品</span>
                  <el-select v-model="row.productId" filterable placeholder="选择商品" style="width:100%">
                    <el-option v-for="p in productOptions" :key="p.id"
                        :label="`${p.name}${p.specification ? ' ['+p.specification+']' : ''}`" :value="p.id" />
                  </el-select>
                </div>
                <div class="item-row2">
                  <div class="item-field half">
                    <span class="item-label">数量</span>
                    <el-input v-model.number="row.quantity" type="number" placeholder="0.000" style="width:100%" />
                  </div>
                  <div class="item-field half">
                    <span class="item-label">单价(元)</span>
                    <el-input v-model.number="row.price" type="number" placeholder="0.00" style="width:100%" />
                  </div>
                </div>
                <div class="item-subtotal">小计：¥{{ ((Number(row.quantity)||0) * (Number(row.price)||0)).toFixed(2) }}</div>
              </div>
            </div>

            <!-- 桌面：表格 -->
            <div v-else class="table-wrap">
              <el-table :data="createForm.items" border size="small" class="items-table">
                <el-table-column label="商品" min-width="160">
                  <template #default="{ row }">
                    <el-select v-model="row.productId" filterable placeholder="商品" style="width: 100%">
                      <el-option v-for="p in productOptions" :key="p.id"
                          :label="`${p.name}${p.specification ? ' ['+p.specification+']' : ''}`" :value="p.id" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="数量" width="120">
                  <template #default="{ row }">
                    <el-input-number v-model="row.quantity" :min="0.001" :precision="3" style="width: 100%" />
                  </template>
                </el-table-column>
                <el-table-column label="单价(元)" width="130">
                  <template #default="{ row }">
                    <el-input-number v-model="row.price" :min="0.01" :precision="2" style="width: 100%" />
                  </template>
                </el-table-column>
                <el-table-column label="小计" width="90" align="right">
                  <template #default="{ row }">¥{{ (row.quantity * row.price).toFixed(2) }}</template>
                </el-table-column>
                <el-table-column width="50" align="center">
                  <template #default="{ $index }">
                    <el-button type="danger" link @click="removeLine($index)">删</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div class="items-total">合计：<b>¥{{ createTotal.toFixed(2) }}</b></div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="销售单详情" :width="isMobile ? '94%' : '720px'" destroy-on-close>
      <div v-if="detailOrder" class="detail-body">
        <el-descriptions :column="isMobile ? 1 : 2" border size="small">
          <el-descriptions-item label="订单号">{{ detailOrder.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="客户">{{ detailOrder.customer?.name }}</el-descriptions-item>
          <el-descriptions-item label="下单日期">{{ formatDate(detailOrder.orderDate) }}</el-descriptions-item>
          <el-descriptions-item label="货物状态">{{ statusText(detailOrder.status) }}</el-descriptions-item>
          <el-descriptions-item label="总金额">¥{{ Number(detailOrder.totalAmount).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="收款状态">
            <el-tag :type="payTag(detailOrder.paymentStatus)" size="small">{{ payText(detailOrder.paymentStatus) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="已收金额">¥{{ Number(detailOrder.receivedAmount || 0).toFixed(2) }}</el-descriptions-item>
          <el-descriptions-item label="未收金额">
            <span :class="{ 'text-danger': unreceived(detailOrder) > 0 }">¥{{ unreceived(detailOrder).toFixed(2) }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <div class="section-title">商品明细</div>
        <div class="table-wrap">
          <el-table :data="detailOrder.items" border size="small">
            <el-table-column prop="product.name" label="商品" min-width="100" />
            <el-table-column label="规格" width="80">
              <template #default="{ row }">{{ row.product?.specification || '-' }}</template>
            </el-table-column>
            <el-table-column label="数量" width="90" align="right">
              <template #default="{ row }">{{ Number(row.quantity).toFixed(3) }} {{ row.product?.unit }}</template>
            </el-table-column>
            <el-table-column label="单价" width="80" align="right">
              <template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="小计" width="90" align="right">
              <template #default="{ row }"><b>¥{{ Number(row.amount).toFixed(2) }}</b></template>
            </el-table-column>
          </el-table>
        </div>

        <div class="section-title">
          <span>收款记录</span>
          <el-button type="primary" size="small" @click="openCollect(detailOrder)">登记收款</el-button>
        </div>
        <div class="table-wrap">
          <el-table :data="payHistory" border size="small" v-loading="payHistoryLoading">
            <el-table-column prop="paymentDate" label="收款日期" width="110" />
            <el-table-column label="金额" width="100" align="right">
              <template #default="{ row }">¥{{ Number(row.amount).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="paymentMethod" label="收款方式" width="100" />
          </el-table>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 登记收款 -->
    <el-dialog v-model="collectVisible" title="登记收款" :width="isMobile ? '92%' : '440px'" destroy-on-close @closed="collectRow = null">
      <div v-if="collectRow" class="pay-hint">
        <p>订单：<b>{{ collectRow.orderNo }}</b></p>
        <p>总金额：¥{{ Number(collectRow.totalAmount).toFixed(2) }} &nbsp;·&nbsp; 已收：¥{{ Number(collectRow.receivedAmount || 0).toFixed(2) }} &nbsp;·&nbsp; 待收：<b class="text-danger">¥{{ unreceived(collectRow).toFixed(2) }}</b></p>
      </div>
      <el-form :model="collectForm" label-width="90px" style="margin-top: 12px">
        <el-form-item label="收款日期" required>
          <el-date-picker v-model="collectForm.paymentDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="收款金额" required>
          <el-input-number v-model="collectForm.amount" :min="0.01" :precision="2"
              :max="collectRow ? unreceived(collectRow) : undefined" style="width: 100%" />
        </el-form-item>
        <el-form-item label="收款方式">
          <el-select v-model="collectForm.paymentMethod" placeholder="选择或输入" allow-create filterable style="width: 100%">
            <el-option label="现金" value="现金" />
            <el-option label="微信" value="微信" />
            <el-option label="支付宝" value="支付宝" />
            <el-option label="银行转账" value="银行转账" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="collectVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCollect">确认收款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
const summaryChartRef = ref(null)
const refreshChart = () => { summaryChartRef.value?.refresh(); loadPendingStats() }

const pendingStats = ref({ count: 0, totalAmount: 0 })
const loadPendingStats = async () => {
  try {
    const res = await getSalesPendingStats()
    pendingStats.value = { count: Number(res.data?.count || 0), totalAmount: Number(res.data?.totalAmount || 0) }
  } catch { pendingStats.value = { count: 0, totalAmount: 0 } }
}
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSalesOrders, getSalesOrder,
  createSalesOrder, completeSalesOrder, cancelSalesOrder,
  collectSalesOrder, getSalePayments, getSalesPendingStats
} from '@/api/sales'
import { getActiveCustomers } from '@/api/customer'
import SummaryChart from '@/components/SummaryChart.vue'
import { getAllEnabledProducts } from '@/api/product'
import { formatDate } from '@/utils/date'
import { useIsMobile } from '@/composables/useIsMobile'
import MobileListCard from '@/components/MobileListCard.vue'

const isMobile = useIsMobile()


const loading = ref(false)
const orders = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const status = ref('')
const paymentStatusFilter = ref('')
const startDate = ref('')
const endDate = ref('')
const periodLabel = ref('')

const createVisible = ref(false)
const createLoading = ref(false)
const today = new Date().toISOString().slice(0, 10)
const createForm = ref({ customerId: null, orderDate: today, items: [{ productId: null, quantity: 1, price: 1 }] })
const customerOptions = ref([])
const productOptions = ref([])

const createTotal = computed(() =>
  createForm.value.items.reduce((s, r) => s + (Number(r.quantity) || 0) * (Number(r.price) || 0), 0)
)

const detailVisible = ref(false)
const detailOrder = ref(null)
const payHistory = ref([])
const payHistoryLoading = ref(false)

const collectVisible = ref(false)
const collectRow = ref(null)
const collectForm = ref({ paymentDate: today, amount: 0, paymentMethod: '现金' })

const unreceived = (row) => Math.max(0, Number((Number(row.totalAmount) - Number(row.receivedAmount || 0)).toFixed(2)))

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSalesOrders({
      page: page.value - 1, size: size.value,
      keyword: searchKeyword.value || undefined,
      status: status.value || undefined,
      paymentStatus: paymentStatusFilter.value || undefined,
      startDate: startDate.value || undefined,
      endDate: endDate.value || undefined
    })
    orders.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } catch { ElMessage.error('加载失败') } finally { loading.value = false }
}

const loadOptions = async () => {
  try {
    const [c, p] = await Promise.all([getActiveCustomers(), getAllEnabledProducts()])
    customerOptions.value = c.data || []
    productOptions.value = p.data || []
  } catch { customerOptions.value = []; productOptions.value = [] }
}

const handleSearch = () => { page.value = 1; loadData() }
const resetSearch = () => {
  searchKeyword.value = ''; status.value = ''; paymentStatusFilter.value = ''
  startDate.value = ''; endDate.value = ''; periodLabel.value = ''
  page.value = 1; loadData()
}
const clearPaymentFilter = () => { paymentStatusFilter.value = ''; page.value = 1; loadData() }
const clearPeriodFilter = () => { startDate.value = ''; endDate.value = ''; periodLabel.value = ''; page.value = 1; loadData() }
const filterUnpaid = () => {
  paymentStatusFilter.value = 'UNPAID,PARTIAL'
  startDate.value = ''; endDate.value = ''; periodLabel.value = ''
  page.value = 1; loadData()
}
const filterPending = () => {
  status.value = 'PENDING'; paymentStatusFilter.value = ''
  startDate.value = ''; endDate.value = ''; periodLabel.value = ''
  page.value = 1; loadData()
}
const filterPeriod = ({ mode, startDate: from, endDate: to, label }) => {
  startDate.value = from
  endDate.value = to
  periodLabel.value = mode === 'day' ? label : `${label}（本月）`
  status.value = ''; paymentStatusFilter.value = ''
  page.value = 1; loadData()
}

const openCreate = () => {
  createForm.value = { customerId: null, orderDate: today, items: [{ productId: null, quantity: 1, price: 1 }] }
  createVisible.value = true
}

const addLine = () => createForm.value.items.push({ productId: null, quantity: 1, price: 1 })
const removeLine = (idx) => createForm.value.items.splice(idx, 1)

const submitCreate = async () => {
  if (createLoading.value) return
  const f = createForm.value
  if (!f.customerId || !f.orderDate || !f.items?.length) { ElMessage.warning('请填写客户、日期并添加明细'); return }
  for (const line of f.items) {
    if (!line.productId || !line.quantity || !line.price) { ElMessage.warning('请完善每行商品、数量、单价'); return }
  }
  createLoading.value = true
  try {
    await createSalesOrder({
      customerId: f.customerId, orderDate: f.orderDate,
      items: f.items.map((x) => ({ productId: x.productId, quantity: x.quantity, price: x.price }))
    })
    ElMessage.success('创建成功')
    createVisible.value = false
    loadData(); refreshChart()
  } catch { /* */ } finally { createLoading.value = false }
}

const openDetail = async (row) => {
  try {
    detailOrder.value = (await getSalesOrder(row.id)).data
    detailVisible.value = true
    payHistoryLoading.value = true
    payHistory.value = (await getSalePayments(row.id)).data || []
  } catch { ElMessage.error('加载详情失败') } finally { payHistoryLoading.value = false }
}

const doComplete = (row) => {
  ElMessageBox.confirm(`确认完成销售单「${row.orderNo}」？将自动扣减库存。`, '完成出库', { type: 'warning' })
    .then(async () => { await completeSalesOrder(row.id); ElMessage.success('货物已出库'); loadData(); refreshChart() })
    .catch(() => {})
}

const doCancel = (row) => {
  ElMessageBox.confirm(`取消订单「${row.orderNo}」？`, '提示', { type: 'warning' })
    .then(async () => { await cancelSalesOrder(row.id); ElMessage.success('已取消'); loadData(); refreshChart() })
    .catch(() => {})
}

const openCollect = (row) => {
  const left = unreceived(row)
  if (left <= 0) { ElMessage.info('该单已收清'); return }
  collectRow.value = row
  collectForm.value = { paymentDate: today, amount: left, paymentMethod: '现金' }
  collectVisible.value = true
}

const submitCollect = async () => {
  if (!collectRow.value || !collectForm.value.amount) { ElMessage.warning('请输入有效金额'); return }
  if (!collectForm.value.paymentDate) { ElMessage.warning('请选择收款日期'); return }
  try {
    await collectSalesOrder(collectRow.value.id, collectForm.value)
    ElMessage.success('收款已登记')
    collectVisible.value = false
    loadData(); refreshChart()
    if (detailVisible.value && detailOrder.value?.id === collectRow.value.id) {
      payHistory.value = (await getSalePayments(collectRow.value.id)).data || []
      detailOrder.value = (await getSalesOrder(collectRow.value.id)).data
    }
  } catch { /* */ }
}

const payTag = (s) => ({ UNPAID: 'danger', PARTIAL: 'warning', PAID: 'success' }[s] || 'info')
const payText = (s) => ({ UNPAID: '未收款', PARTIAL: '部分收款', PAID: '已收清' }[s] || s)
const statusType = (s) => ({ PENDING: 'info', COMPLETED: 'success', CANCELLED: 'danger' }[s] || 'info')
const statusText = (s) => ({ PENDING: '待出库', COMPLETED: '已出库', CANCELLED: '已取消' }[s] || s)

onMounted(() => { loadOptions().then(() => loadData()); loadPendingStats() })
</script>

<style scoped lang="scss">
.sales-list {
  .page-actions { display: flex; justify-content: flex-end; margin-bottom: 12px; }
  .search-bar { margin-bottom: 12px; display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
  .table-wrap { overflow-x: auto; -webkit-overflow-scrolling: touch; }
  .pagination { margin-top: 16px; display: flex; justify-content: center; }
  .items-table { margin-top: 8px; width: 100%; }
  .items-total { text-align: right; padding: 8px 0 0; font-size: 14px; font-weight: 600; color: #303133; }
  .detail-body {
    .section-title { margin: 14px 0 8px; font-weight: 600; font-size: 14px; color: #303133; display: flex; justify-content: space-between; align-items: center; }
  }
  .pay-hint { background: #f5f7fa; padding: 10px 14px; border-radius: 6px; margin-bottom: 8px; p { margin: 4px 0; font-size: 13px; } }
  .text-danger { color: #f56c6c; font-weight: 500; }

  .mobile-items {
    display: flex;
    flex-direction: column;
    gap: 10px;
    width: 100%;
  }
  .item-card {
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    padding: 10px 12px;
    background: #fafafa;
    .item-card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 8px;
      .item-no { font-size: 12px; color: #909399; }
    }
    .item-field {
      margin-bottom: 8px;
      &.half { flex: 1; }
      .item-label { display: block; font-size: 12px; color: #606266; margin-bottom: 4px; }
    }
    .item-row2 {
      display: flex;
      gap: 10px;
    }
    .item-subtotal {
      text-align: right;
      font-size: 13px;
      font-weight: 600;
      color: #409eff;
      margin-top: 4px;
    }
  }
}

.text-danger { color: #f56c6c; font-weight: 600; }

.pending-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  background: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 6px;
  padding: 10px 16px;
  margin-bottom: 10px;
  cursor: pointer;
  font-size: 14px;
  color: #614700;
  transition: background 0.2s;
  flex-wrap: wrap;
  &:hover { background: #ffe7ba; }
  .pending-icon { font-size: 16px; }
  .pending-sep { color: #aaa; }
  .pending-amount { color: #d46b08; font-size: 15px; }
  .pending-action { margin-left: auto; color: #fa8c16; font-size: 13px; }
}
</style>
