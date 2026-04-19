<template>
  <div class="purchase-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>采购订单</span>
          <el-button v-if="canCreatePurchase" type="primary" @click="openCreate">新建采购单</el-button>
        </div>
      </template>

      <el-alert type="info" show-icon :closable="false" class="fund-hint" title="资金说明" description="已付金额与支付状态仅通过「付款」登记变更（需财务权限）；审核、发货、收货不改变资金。" />

      <div class="search-bar">
        <el-input
            v-model="searchKeyword"
            placeholder="订单号/供应商"
            style="width: 240px"
            clearable
            @keyup.enter="handleSearch"
        />
        <el-select v-model="status" placeholder="状态" clearable style="width: 150px">
          <el-option label="待审核" value="PENDING" />
          <el-option label="已审核" value="APPROVED" />
          <el-option label="已发货" value="SHIPPED" />
          <el-option label="已收货" value="RECEIVED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="orders" v-loading="loading" border style="width: 100%">
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="supplier.name" label="供应商" min-width="140" />
        <el-table-column prop="orderDate" label="下单日期" width="110">
          <template #default="{ row }">{{ formatDate(row.orderDate) }}</template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" width="110">
          <template #default="{ row }">¥{{ Number(row.totalAmount).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="paidAmount" label="已付" width="100">
          <template #default="{ row }">¥{{ Number(row.paidAmount || 0).toFixed(2) }}</template>
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
        <el-table-column prop="purchaser.realName" label="采购员" width="90" />
        <el-table-column label="操作" width="340" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button
                v-if="canPayPurchase && row.status !== 'CANCELLED' && row.paymentStatus !== 'PAID'"
                type="warning"
                link
                @click="openPay(row)"
            >付款</el-button>
            <el-button v-if="row.status === 'PENDING' && canApprovePurchase" type="success" link @click="doApprove(row)">审核</el-button>
            <el-button v-if="row.status === 'PENDING' && canApprovePurchase" type="danger" link @click="doCancel(row)">取消</el-button>
            <el-button v-if="row.status === 'APPROVED' && canApprovePurchase" type="warning" link @click="doShip(row)">发货</el-button>
            <el-button v-if="row.status === 'SHIPPED' && canReceivePurchase" type="success" link @click="doReceive(row)">确认入库</el-button>
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

    <!-- 新建 -->
    <el-dialog v-model="createVisible" title="新建采购单" width="640px" destroy-on-close>
      <el-form label-width="100px">
        <el-form-item label="供应商" required>
          <el-select v-model="createForm.supplierId" filterable placeholder="选择" style="width: 100%">
            <el-option v-for="s in supplierOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="预计交货">
          <el-date-picker v-model="createForm.expectedDeliveryDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
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

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" title="采购单详情" width="720px" destroy-on-close>
      <div v-if="detailOrder" class="detail-body">
        <p><strong>订单号</strong> {{ detailOrder.orderNo }}</p>
        <p><strong>供应商</strong> {{ detailOrder.supplier?.name }}</p>
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
        <el-button
            v-if="detailOrder && canPurchaseReturn && (detailOrder.status === 'RECEIVED' || detailOrder.status === 'COMPLETED')"
            type="warning"
            @click="openReturn"
        >
          提交退货申请
        </el-button>
      </template>
    </el-dialog>

    <!-- 退货申请 -->
    <el-dialog v-model="returnVisible" title="提交采购退货申请" width="520px" destroy-on-close>
      <el-alert type="info" show-icon :closable="false" style="margin-bottom:12px"
        title="申请提交后流程"
        description="① 仓管员审批通过 → 库存扣减  ② 财务审批通过 → 已付金额减少。可在「退货申请」页跟踪进度。"
      />
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

    <el-dialog v-model="payVisible" title="登记采购付款" width="420px" destroy-on-close @closed="payRow = null">
      <p v-if="payRow" class="hint">订单 {{ payRow.orderNo }}，待付 ¥{{ unpaidPurchase(payRow).toFixed(2) }}</p>
      <el-form label-width="100px">
        <el-form-item label="本次付款">
          <el-input-number
              v-model="payAmount"
              :min="0.01"
              :precision="2"
              :max="payRow ? unpaidPurchase(payRow) : undefined"
              style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPay">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getPurchaseOrders,
  getPurchaseOrder,
  createPurchaseOrder,
  approvePurchaseOrder,
  shipPurchaseOrder,
  receivePurchaseOrder,
  cancelPurchaseOrder,
  purchaseReturn,
  payPurchaseOrder
} from '@/api/purchase'
import { getActiveSuppliers } from '@/api/supplier'
import { getAllEnabledProducts } from '@/api/product'
import { formatDate } from '@/utils/date'

const store = useStore()
const canPayPurchase = computed(() => store.getters['user/hasPermission']('purchase:pay'))
const canApprovePurchase = computed(() => store.getters['user/hasPermission']('purchase:approve'))
const canReceivePurchase = computed(() => store.getters['user/hasPermission']('inventory:inbound'))
const canCreatePurchase = computed(() => store.getters['user/hasPermission']('purchase:create'))
/** 与后端 purchase:create | purchase:approve 一致，采购业务角色可提交采购退货 */
const canPurchaseReturn = computed(() => {
  const has = store.getters['user/hasPermission']
  return has('purchase:create') || has('purchase:approve')
})

const loading = ref(false)
const orders = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const status = ref('')

const createVisible = ref(false)
const createLoading = ref(false)
const createForm = ref({
  supplierId: null,
  expectedDeliveryDate: null,
  paymentMethod: '',
  remark: '',
  items: [{ productId: null, quantity: 1, price: 1 }]
})
const supplierOptions = ref([])
const productOptions = ref([])

const detailVisible = ref(false)
const detailOrder = ref(null)

const returnVisible = ref(false)
const returnRows = ref([])

const payVisible = ref(false)
const payRow = ref(null)
const payAmount = ref(0)

const unpaidPurchase = (row) => {
  const total = Number(row?.totalAmount || 0)
  const paid = Number(row?.paidAmount || 0)
  return Math.max(0, Number((total - paid).toFixed(2)))
}

const openPay = (row) => {
  const left = unpaidPurchase(row)
  if (left <= 0) {
    ElMessage.info('该单已付清')
    return
  }
  payRow.value = row
  payAmount.value = left
  payVisible.value = true
}

const submitPay = async () => {
  if (!payRow.value || payAmount.value <= 0) {
    ElMessage.warning('请输入有效金额')
    return
  }
  if (payAmount.value > unpaidPurchase(payRow.value) + 0.0001) {
    ElMessage.warning('金额不能超过待付金额')
    return
  }
  try {
    await payPurchaseOrder(payRow.value.id, { amount: payAmount.value })
    ElMessage.success('付款已登记')
    payVisible.value = false
    loadData()
  } catch {
    /*  */
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getPurchaseOrders({
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
    const [s, p] = await Promise.all([getActiveSuppliers(), getAllEnabledProducts()])
    supplierOptions.value = s.data || []
    productOptions.value = p.data || []
  } catch {
    supplierOptions.value = []
    productOptions.value = []
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
    supplierId: supplierOptions.value[0]?.id ?? null,
    expectedDeliveryDate: null,
    paymentMethod: '',
    remark: '',
    items: [{ productId: null, quantity: 1, price: 1 }]
  }
  createVisible.value = true
}

const addLine = () => {
  createForm.value.items.push({ productId: null, quantity: 1, price: 1 })
}

/** 提交前合并相同商品的行，避免重复添加同一商品 */
const mergeItems = (items) => {
  const map = new Map()
  for (const item of items) {
    if (!item.productId) continue
    if (map.has(item.productId)) {
      const existing = map.get(item.productId)
      existing.quantity = Number(existing.quantity) + Number(item.quantity)
    } else {
      map.set(item.productId, { ...item })
    }
  }
  return Array.from(map.values())
}

const removeLine = (idx) => {
  createForm.value.items.splice(idx, 1)
}

const submitCreate = async () => {
  if (createLoading.value) return
  const f = createForm.value
  if (!f.supplierId || !f.items?.length) {
    ElMessage.warning('请选择供应商并添加明细')
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
    const mergedItems = mergeItems(f.items)
    await createPurchaseOrder({
      supplierId: f.supplierId,
      expectedDeliveryDate: f.expectedDeliveryDate || undefined,
      paymentMethod: f.paymentMethod || undefined,
      remark: f.remark || undefined,
      items: mergedItems.map((x) => ({
        productId: x.productId,
        quantity: x.quantity,
        price: x.price
      }))
    })
    ElMessage.success('创建成功')
    createVisible.value = false
    loadData()
  } catch {
    /*  */
  } finally {
    createLoading.value = false
  }
}

const openDetail = async (row) => {
  try {
    const res = await getPurchaseOrder(row.id)
    detailOrder.value = res.data
    detailVisible.value = true
  } catch {
    ElMessage.error('加载详情失败')
  }
}

const openReturn = () => {
  const o = detailOrder.value
  if (!o?.items) return
  returnRows.value = o.items.map((it) => {
    const ret = it.returnedQuantity != null ? Number(it.returnedQuantity) : 0
    const can = Number(it.quantity) - ret
    return {
      productId: it.product?.id,
      productName: it.product?.name || '',
      canReturn: can > 0 ? can : 0,
      qty: 0
    }
  }).filter((x) => x.canReturn > 0)
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
    await purchaseReturn(detailOrder.value.id, { warehouseId: 1, lines })
    ElMessage.success('退货申请已提交，等待仓管员审批')
    returnVisible.value = false
    detailVisible.value = false
    loadData()
  } catch {
    /*  */
  }
}

const doApprove = (row) => {
  ElMessageBox.confirm(`审核通过「${row.orderNo}」？`, '提示', { type: 'warning' })
    .then(async () => {
      await approvePurchaseOrder(row.id)
      ElMessage.success('已审核')
      loadData()
    })
    .catch(() => {})
}

const doShip = (row) => {
  ElMessageBox.confirm(`确认发货「${row.orderNo}」？`, '提示')
    .then(async () => {
      await shipPurchaseOrder(row.id)
      ElMessage.success('已发货')
      loadData()
    })
    .catch(() => {})
}

const doReceive = (row) => {
  ElMessageBox.confirm(`确认收货并入库「${row.orderNo}」？将按订单明细增加库存。`, '收货', { type: 'warning' })
    .then(async () => {
      await receivePurchaseOrder(row.id)
      ElMessage.success('已收货入库')
      loadData()
    })
    .catch(() => {})
}

const doCancel = (row) => {
  ElMessageBox.confirm(`取消订单「${row.orderNo}」？`, '提示', { type: 'warning' })
    .then(async () => {
      await cancelPurchaseOrder(row.id)
      ElMessage.success('已取消')
      loadData()
    })
    .catch(() => {})
}

const payType = (s) => ({ UNPAID: 'danger', PARTIAL: 'warning', PAID: 'success' }[s] || 'info')
const payText = (s) => ({ UNPAID: '未付', PARTIAL: '部分', PAID: '已付' }[s] || s)

const statusType = (s) =>
  ({
    PENDING: 'info',
    APPROVED: 'success',
    SHIPPED: 'warning',
    RECEIVED: 'primary',
    COMPLETED: 'success',
    CANCELLED: 'danger'
  }[s] || 'info')

const statusText = (s) =>
  ({
    PENDING: '待审核',
    APPROVED: '已审核',
    SHIPPED: '已发货',
    RECEIVED: '已收货',
    COMPLETED: '已完成',
    CANCELLED: '已取消'
  }[s] || s)

onMounted(() => {
  if (canCreatePurchase.value) {
    loadOptions().then(() => loadData())
  } else {
    loadData()
  }
})
</script>

<style scoped lang="scss">
.purchase-list {
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
}
</style>
