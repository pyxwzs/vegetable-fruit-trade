<template>
  <div class="sales-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>销售订单管理</span>
          <el-button v-if="canCreateSales" type="primary" @click="openCreate">新建销售单</el-button>
        </div>
      </template>

      <el-alert type="info" show-icon :closable="false" class="fund-hint" title="资金说明" description="已收金额与支付状态仅通过「收款」登记变更（需财务权限）；审核、发货不改变资金。" />

      <el-alert
          v-if="creditWarnings.length"
          type="warning"
          show-icon
          class="credit-alert"
          :closable="false"
          title="授信预警（未结清占用 ≥ 额度 80%）"
      >
        <template #default>
          <span v-for="(w, i) in creditWarnings" :key="w.customerId" class="warn-line">
            {{ w.customerName }}：占用 ¥{{ Number(w.usedAmount).toFixed(2) }} / 额度 ¥{{ Number(w.creditLimit).toFixed(2) }}
            <el-tag v-if="w.overLimit" type="danger" size="small" style="margin-left: 6px">超额</el-tag>
            <span v-if="i < creditWarnings.length - 1">；</span>
          </span>
        </template>
      </el-alert>

      <div class="search-bar">
        <el-input
            v-model="searchKeyword"
            placeholder="订单号/客户"
            style="width: 240px"
            clearable
            @keyup.enter="handleSearch"
        />
        <el-select v-model="status" placeholder="订单状态" clearable style="width: 150px">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已审核" value="APPROVED" />
          <el-option label="已发货" value="SHIPPED" />
          <el-option label="已送达" value="DELIVERED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="orders" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="customer.name" label="客户" min-width="140" />
        <el-table-column prop="orderDate" label="下单日期" width="110">
          <template #default="{ row }">{{ formatDate(row.orderDate) }}</template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" width="110">
          <template #default="{ row }">¥{{ Number(row.totalAmount).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="receivedAmount" label="已收" width="100">
          <template #default="{ row }">¥{{ Number(row.receivedAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="paymentStatus" label="支付" width="100">
          <template #default="{ row }">
            <el-tag :type="payType(row.paymentStatus)">{{ payText(row.paymentStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="salesman.realName" label="销售员" width="90" />
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button
                v-if="canCollectSales && row.status !== 'CANCELLED' && row.paymentStatus !== 'PAID'"
                type="warning"
                link
                @click="openCollect(row)"
            >收款</el-button>
            <el-button v-if="row.status === 'PENDING' && canApproveSales" type="success" link @click="doApprove(row)">审核</el-button>
            <el-button v-if="row.status === 'PENDING' && canApproveSales" type="danger" link @click="doCancel(row)">取消</el-button>
            <el-button v-if="row.status === 'APPROVED' && canShipSales" type="warning" link @click="openShip(row)">确认出库</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 30, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
          class="pagination"
      />
    </el-card>

    <el-dialog v-model="createVisible" title="新建销售单" width="640px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="客户" required>
          <el-select v-model="createForm.customerId" filterable placeholder="选择客户" style="width: 100%">
            <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="交货日期">
          <el-date-picker v-model="createForm.deliveryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="付款方式">
          <el-input v-model="createForm.paymentMethod" placeholder="如：转账、月结" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="明细" required>
          <el-button size="small" @click="addLine">添加一行</el-button>
          <el-table :data="createForm.items" border size="small" class="items-table">
            <el-table-column label="商品" min-width="180">
              <template #default="{ row }">
                <el-select v-model="row.productId" filterable placeholder="商品" style="width: 100%">
                  <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="数量" width="120">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="0.001" :precision="3" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column label="单价" width="120">
              <template #default="{ row }">
                <el-input-number v-model="row.price" :min="0.01" :precision="2" style="width: 100%" />
              </template>
            </el-table-column>
            <el-table-column width="70" align="center">
              <template #default="{ $index }">
                <el-button type="danger" link @click="removeLine($index)">删</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="submitCreate">提交</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="shipVisible" title="发货出库" width="420px" destroy-on-close>
      <p class="hint">从所选仓库按先入先出扣减可用库存。</p>
      <el-form label-width="90px">
        <el-form-item label="出库仓库">
          <el-select v-model="shipWarehouseId" placeholder="仓库" style="width: 100%">
            <el-option v-for="w in warehouseOptions" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipLoading" @click="confirmShip">确认发货</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="销售单详情" width="720px" destroy-on-close>
      <div v-if="detailOrder" class="detail-body">
        <p><strong>订单号</strong> {{ detailOrder.orderNo }}</p>
        <p><strong>客户</strong> {{ detailOrder.customer?.name }}</p>
        <p><strong>状态</strong> {{ statusText(detailOrder.status) }}</p>
        <p><strong>总金额</strong> ¥{{ Number(detailOrder.totalAmount).toFixed(2) }}</p>
        <el-table :data="detailOrder.items" border size="small" class="detail-table">
          <el-table-column prop="product.name" label="商品" />
          <el-table-column prop="quantity" label="数量" width="100" />
          <el-table-column prop="returnedQuantity" label="已退货" width="90">
            <template #default="{ row }">{{ row.returnedQuantity ?? 0 }}</template>
          </el-table-column>
          <el-table-column prop="price" label="单价" width="100">
            <template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="100">
            <template #default="{ row }">¥{{ Number(row.amount).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button v-if="detailOrder && canPrint(detailOrder)" @click="printOrder">打印送货单</el-button>
        <el-button
            v-if="detailOrder && canSalesReturn && canReturn(detailOrder)"
            type="warning"
            @click="openReturn"
        >
          销售退货申请
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="returnVisible" title="提交销售退货申请" width="520px" destroy-on-close>
      <el-alert type="info" show-icon :closable="false" style="margin-bottom:12px"
        title="申请提交后流程"
        description="① 仓管员审批通过 → 货物入库  ② 财务审批通过 → 已收金额减少。可在「退货申请」页跟踪进度。"
      />
      <el-form label-width="90px" class="ret-form">
        <el-form-item label="入库仓库">
          <el-select v-model="returnWarehouseId" style="width: 100%">
            <el-option v-for="w in warehouseOptions" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-table v-if="returnRows.length" :data="returnRows" border size="small">
        <el-table-column prop="productName" label="商品" />
        <el-table-column prop="canReturn" label="可退" width="90" />
        <el-table-column label="本次退货量" width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.qty" :min="0" :max="row.canReturn" :precision="3" style="width: 100%" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="returnVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReturn">提交退货申请</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="collectVisible" title="登记销售收款" width="420px" destroy-on-close @closed="collectRow = null">
      <p v-if="collectRow" class="hint">订单 {{ collectRow.orderNo }}，待收 ¥{{ unpaidSales(collectRow).toFixed(2) }}</p>
      <el-form label-width="100px">
        <el-form-item label="本次收款">
          <el-input-number
              v-model="collectAmount"
              :min="0.01"
              :precision="2"
              :max="collectRow ? unpaidSales(collectRow) : undefined"
              style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="collectVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCollect">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSalesOrders,
  getSalesOrder,
  createSalesOrder,
  approveSalesOrder,
  shipSalesOrder,
  cancelSalesOrder,
  salesReturn,
  collectSalesOrder
} from '@/api/sales'
import { getActiveCustomers, getCreditWarnings } from '@/api/customer'
import { getAllEnabledProducts } from '@/api/product'
import { getActiveWarehouses } from '@/api/warehouse'
import { formatDate } from '@/utils/date'

const store = useStore()
const canCollectSales = computed(() => store.getters['user/hasPermission']('sales:collect'))
const canApproveSales = computed(() => store.getters['user/hasPermission']('sales:approve'))
const canShipSales = computed(() => store.getters['user/hasPermission']('inventory:outbound'))
const canCreateSales = computed(() => store.getters['user/hasPermission']('sales:create'))
/** 与后端 sales:create | sales:approve 一致，销售业务角色可提交销售退货 */
const canSalesReturn = computed(() => {
  const has = store.getters['user/hasPermission']
  return has('sales:create') || has('sales:approve')
})

const loading = ref(false)
const orders = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const status = ref('')
const creditWarnings = ref([])

const createVisible = ref(false)
const createLoading = ref(false)
const createForm = ref({
  customerId: null,
  deliveryDate: null,
  paymentMethod: '',
  remark: '',
  items: [{ productId: null, quantity: 1, price: 1 }]
})
const customerOptions = ref([])
const productOptions = ref([])
const warehouseOptions = ref([])

const shipVisible = ref(false)
const shipLoading = ref(false)
const shipWarehouseId = ref(1)
const shipTargetId = ref(null)

const detailVisible = ref(false)
const detailOrder = ref(null)

const returnVisible = ref(false)
const returnRows = ref([])
const returnWarehouseId = ref(1)

const collectVisible = ref(false)
const collectRow = ref(null)
const collectAmount = ref(0)

const unpaidSales = (row) => {
  const total = Number(row?.totalAmount || 0)
  const rec = Number(row?.receivedAmount || 0)
  return Math.max(0, Number((total - rec).toFixed(2)))
}

const openCollect = (row) => {
  const left = unpaidSales(row)
  if (left <= 0) {
    ElMessage.info('该单已收清')
    return
  }
  collectRow.value = row
  collectAmount.value = left
  collectVisible.value = true
}

const submitCollect = async () => {
  if (!collectRow.value || collectAmount.value <= 0) {
    ElMessage.warning('请输入有效金额')
    return
  }
  if (collectAmount.value > unpaidSales(collectRow.value) + 0.0001) {
    ElMessage.warning('金额不能超过待收金额')
    return
  }
  try {
    await collectSalesOrder(collectRow.value.id, { amount: collectAmount.value })
    ElMessage.success('收款已登记')
    collectVisible.value = false
    loadData()
    loadCreditWarnings()
  } catch {
    /*  */
  }
}

const loadCreditWarnings = async () => {
  try {
    const res = await getCreditWarnings()
    creditWarnings.value = res.data || []
  } catch {
    creditWarnings.value = []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSalesOrders({
      page: page.value - 1,
      size: size.value,
      keyword: searchKeyword.value || undefined,
      status: status.value || undefined
    })
    orders.value = res.data.content
    total.value = res.data.totalElements
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const loadOptions = async () => {
  try {
    const [c, p, w] = await Promise.all([
      getActiveCustomers(),
      getAllEnabledProducts(),
      getActiveWarehouses()
    ])
    customerOptions.value = c.data || []
    productOptions.value = p.data || []
    warehouseOptions.value = w.data || []
    if (w.data?.length) {
      shipWarehouseId.value = w.data[0].id
      returnWarehouseId.value = w.data[0].id
    }
  } catch {
    customerOptions.value = []
    productOptions.value = []
    warehouseOptions.value = []
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const resetSearch = () => {
  searchKeyword.value = ''
  status.value = ''
  page.value = 1
  loadData()
}

const openCreate = () => {
  createForm.value = {
    customerId: customerOptions.value[0]?.id ?? null,
    deliveryDate: null,
    paymentMethod: '',
    remark: '',
    items: [{ productId: null, quantity: 1, price: 1 }]
  }
  createVisible.value = true
}

const addLine = () => {
  createForm.value.items.push({ productId: null, quantity: 1, price: 1 })
}

const removeLine = (idx) => {
  createForm.value.items.splice(idx, 1)
}

const submitCreate = async () => {
  if (createLoading.value) return
  const f = createForm.value
  if (!f.customerId || !f.items?.length) {
    ElMessage.warning('请选择客户并添加明细')
    return
  }
  for (const line of f.items) {
    if (!line.productId || !line.quantity || !line.price) {
      ElMessage.warning('请完善每行商品、数量、单价')
      return
    }
  }
  createLoading.value = true
  try {
    await createSalesOrder({
      customerId: f.customerId,
      deliveryDate: f.deliveryDate || undefined,
      paymentMethod: f.paymentMethod || undefined,
      remark: f.remark || undefined,
      items: f.items.map((x) => ({
        productId: x.productId,
        quantity: x.quantity,
        price: x.price
      }))
    })
    ElMessage.success('创建成功')
    createVisible.value = false
    loadData()
    loadCreditWarnings()
  } catch {
    /*  */
  } finally {
    createLoading.value = false
  }
}

const openDetail = async (row) => {
  try {
    const res = await getSalesOrder(row.id)
    detailOrder.value = res.data
    detailVisible.value = true
  } catch {
    ElMessage.error('加载详情失败')
  }
}

const canPrint = (o) => o && ['APPROVED', 'SHIPPED', 'DELIVERED', 'COMPLETED'].includes(o.status)

const printOrder = () => {
  const o = detailOrder.value
  if (!o) return
  const rows = (o.items || [])
    .map(
      (it) =>
        `<tr><td>${it.product?.name || ''}</td><td>${it.quantity}</td><td>${Number(it.price).toFixed(2)}</td><td>${Number(it.amount).toFixed(2)}</td></tr>`
    )
    .join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>送货单 ${o.orderNo}</title>
    <style>body{font-family:sans-serif;padding:20px;}table{border-collapse:collapse;width:100%;}td,th{border:1px solid #333;padding:6px;}</style></head><body>
    <h2>送货单</h2><p>单号：${o.orderNo}</p><p>客户：${o.customer?.name || ''}</p><p>收货地址：${o.customer?.address || '—'}</p>
    <table><thead><tr><th>商品</th><th>数量</th><th>单价</th><th>金额</th></tr></thead><tbody>${rows}</tbody></table>
    <p>合计：¥${Number(o.totalAmount).toFixed(2)}</p></body></html>`
  const w = window.open('', '_blank')
  if (!w) {
    ElMessage.warning('请允许弹出窗口以打印')
    return
  }
  w.document.write(html)
  w.document.close()
  w.focus()
  w.print()
  w.close()
}

const canReturn = (o) => o && ['SHIPPED', 'DELIVERED', 'COMPLETED'].includes(o.status)

const openReturn = () => {
  const o = detailOrder.value
  if (!o?.items) return
  returnRows.value = o.items
    .map((it) => {
      const ret = it.returnedQuantity != null ? Number(it.returnedQuantity) : 0
      const can = Number(it.quantity) - ret
      return {
        productId: it.product?.id,
        productName: it.product?.name || '',
        canReturn: can > 0 ? can : 0,
        qty: 0
      }
    })
    .filter((x) => x.canReturn > 0)
  if (!returnRows.value.length) {
    ElMessage.info('没有可退数量')
    return
  }
  returnVisible.value = true
}

const submitReturn = async () => {
  const lines = returnRows.value
    .filter((r) => r.qty > 0)
    .map((r) => ({ productId: r.productId, quantity: r.qty }))
  if (!lines.length) {
    ElMessage.warning('请填写退货数量')
    return
  }
  try {
    await salesReturn(detailOrder.value.id, {
      warehouseId: returnWarehouseId.value,
      lines
    })
    ElMessage.success('退货申请已提交，等待仓管员审批')
    returnVisible.value = false
    detailVisible.value = false
    loadData()
    loadCreditWarnings()
  } catch {
    /*  */
  }
}

const doApprove = (row) => {
  ElMessageBox.confirm(`审核通过「${row.orderNo}」？`, '提示', { type: 'warning' })
    .then(async () => {
      await approveSalesOrder(row.id)
      ElMessage.success('已审核')
      loadData()
    })
    .catch(() => {})
}

const openShip = (row) => {
  shipTargetId.value = row.id
  shipWarehouseId.value = warehouseOptions.value[0]?.id ?? 1
  shipVisible.value = true
}

const confirmShip = async () => {
  if (!shipTargetId.value) return
  shipLoading.value = true
  try {
    await shipSalesOrder(shipTargetId.value, shipWarehouseId.value)
    ElMessage.success('已发货')
    shipVisible.value = false
    loadData()
  } catch {
    /*  */
  } finally {
    shipLoading.value = false
  }
}

const doCancel = (row) => {
  ElMessageBox.confirm(`取消订单「${row.orderNo}」？`, '提示', { type: 'warning' })
    .then(async () => {
      await cancelSalesOrder(row.id)
      ElMessage.success('已取消')
      loadData()
    })
    .catch(() => {})
}

const payType = (s) => ({ UNPAID: 'danger', PARTIAL: 'warning', PAID: 'success' }[s] || 'info')
const payText = (s) => ({ UNPAID: '未支付', PARTIAL: '部分支付', PAID: '已支付' }[s] || s)

const statusType = (s) =>
  ({
    PENDING: 'info',
    APPROVED: 'success',
    SHIPPED: 'warning',
    DELIVERED: 'primary',
    COMPLETED: 'success',
    CANCELLED: 'danger'
  }[s] || 'info')

const statusText = (s) =>
  ({
    PENDING: '待审核',
    APPROVED: '已审核',
    SHIPPED: '已发货',
    DELIVERED: '已送达',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }[s] || s)

onMounted(() => {
  if (canCreateSales.value) {
    loadOptions().then(() => {
      loadData()
      loadCreditWarnings()
    })
  } else {
    loadData()
    loadCreditWarnings()
  }
})
</script>

<style scoped lang="scss">
.sales-list {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }
  .fund-hint {
    margin-bottom: 12px;
  }
  .credit-alert {
    margin-bottom: 16px;
  }
  .warn-line {
    display: inline;
  }
  .search-bar {
    margin-bottom: 16px;
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    align-items: center;
  }
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
  .items-table {
    margin-top: 8px;
    width: 100%;
  }
  .detail-body p {
    margin: 6px 0;
  }
  .detail-table {
    margin-top: 12px;
  }
  .hint {
    font-size: 13px;
    color: var(--el-text-color-secondary);
    margin-bottom: 12px;
  }
  .ret-form {
    margin-bottom: 8px;
  }
}
</style>
