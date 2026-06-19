<template>
  <div class="sales-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>销售订单管理</span>
          <el-button type="primary" @click="openCreate">新建销售单</el-button>
        </div>
      </template>

      <div class="search-bar">
        <el-input
            v-model="searchKeyword"
            placeholder="订单号/客户"
            style="width: 240px"
            clearable
            @keyup.enter="handleSearch"
        />
        <el-select v-model="status" placeholder="货物状态" clearable style="width: 130px">
          <el-option label="待处理" value="PENDING" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="orders" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="customer.name" label="客户" min-width="130" />
        <el-table-column prop="orderDate" label="下单日期" width="110">
          <template #default="{ row }">{{ formatDate(row.orderDate) }}</template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="总金额" width="110">
          <template #default="{ row }">¥{{ Number(row.totalAmount).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="receivedAmount" label="已收" width="100">
          <template #default="{ row }">¥{{ Number(row.receivedAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="收款状态" width="100">
          <template #default="{ row }">
            <el-tag :type="payTag(row.paymentStatus)">{{ payText(row.paymentStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="货物状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openDetail(row)">详情</el-button>
            <el-button
                v-if="row.status === 'PENDING'"
                type="success" link @click="doComplete(row)"
            >完成出库</el-button>
            <el-button
                v-if="row.status !== 'CANCELLED' && row.paymentStatus !== 'PAID'"
                type="warning" link @click="openCollect(row)"
            >登记收款</el-button>
            <el-button
                v-if="row.status === 'PENDING'"
                type="danger" link @click="doCancel(row)"
            >取消</el-button>
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
    <el-dialog v-model="createVisible" title="新建销售单" width="620px" destroy-on-close>
      <el-form label-width="80px">
        <el-form-item label="客户" required>
          <el-select v-model="createForm.customerId" filterable placeholder="选择客户" style="width: 100%">
            <el-option v-for="c in customerOptions" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
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
            <el-table-column width="60" align="center">
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
    <el-dialog v-model="detailVisible" title="销售单详情" width="680px" destroy-on-close>
      <div v-if="detailOrder" class="detail-body">
        <p><strong>订单号：</strong>{{ detailOrder.orderNo }}</p>
        <p><strong>客户：</strong>{{ detailOrder.customer?.name }}</p>
        <p><strong>货物状态：</strong>{{ statusText(detailOrder.status) }}</p>
        <p><strong>总金额：</strong>¥{{ Number(detailOrder.totalAmount).toFixed(2) }}</p>
        <p><strong>已收金额：</strong>¥{{ Number(detailOrder.receivedAmount || 0).toFixed(2) }}</p>
        <p><strong>收款状态：</strong>{{ payText(detailOrder.paymentStatus) }}</p>
        <p><strong>备注：</strong>{{ detailOrder.remark || '-' }}</p>
        <el-table :data="detailOrder.items" border size="small" class="detail-table">
          <el-table-column prop="product.name" label="商品" />
          <el-table-column prop="quantity" label="数量" width="100" />
          <el-table-column prop="price" label="单价" width="100">
            <template #default="{ row }">¥{{ Number(row.price).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="110">
            <template #default="{ row }">¥{{ Number(row.amount).toFixed(2) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 登记收款 -->
    <el-dialog v-model="collectVisible" title="登记收款" width="400px" destroy-on-close @closed="collectRow = null">
      <div v-if="collectRow" class="pay-hint">
        <p>订单：{{ collectRow.orderNo }}</p>
        <p>总金额：¥{{ Number(collectRow.totalAmount).toFixed(2) }}</p>
        <p>已收：¥{{ Number(collectRow.receivedAmount || 0).toFixed(2) }}</p>
        <p>待收：¥{{ unreceived(collectRow).toFixed(2) }}</p>
      </div>
      <el-form label-width="90px" style="margin-top: 16px">
        <el-form-item label="本次收款">
          <el-input-number
              v-model="collectAmount"
              :min="0.01"
              :precision="2"
              :max="collectRow ? unreceived(collectRow) : undefined"
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSalesOrders, getSalesOrder,
  createSalesOrder, completeSalesOrder, cancelSalesOrder, collectSalesOrder
} from '@/api/sales'
import { getActiveCustomers } from '@/api/customer'
import { getAllEnabledProducts } from '@/api/product'
import { formatDate } from '@/utils/date'

const loading = ref(false)
const orders = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const status = ref('')

const createVisible = ref(false)
const createLoading = ref(false)
const createForm = ref({ customerId: null, remark: '', items: [{ productId: null, quantity: 1, price: 1 }] })
const customerOptions = ref([])
const productOptions = ref([])

const detailVisible = ref(false)
const detailOrder = ref(null)

const collectVisible = ref(false)
const collectRow = ref(null)
const collectAmount = ref(0)

const unreceived = (row) => Math.max(0, Number((Number(row.totalAmount) - Number(row.receivedAmount || 0)).toFixed(2)))

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSalesOrders({
      page: page.value - 1, size: size.value,
      keyword: searchKeyword.value || undefined, status: status.value || undefined
    })
    orders.value = res.data.content
    total.value = res.data.totalElements
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
const resetSearch = () => { searchKeyword.value = ''; status.value = ''; page.value = 1; loadData() }

const openCreate = () => {
  createForm.value = { customerId: customerOptions.value[0]?.id ?? null, remark: '', items: [{ productId: null, quantity: 1, price: 1 }] }
  createVisible.value = true
}

const addLine = () => createForm.value.items.push({ productId: null, quantity: 1, price: 1 })
const removeLine = (idx) => createForm.value.items.splice(idx, 1)

const submitCreate = async () => {
  if (createLoading.value) return
  const f = createForm.value
  if (!f.customerId || !f.items?.length) { ElMessage.warning('请选择客户并添加明细'); return }
  for (const line of f.items) {
    if (!line.productId || !line.quantity || !line.price) { ElMessage.warning('请完善每行商品、数量、单价'); return }
  }
  createLoading.value = true
  try {
    await createSalesOrder({
      customerId: f.customerId, remark: f.remark || undefined,
      items: f.items.map((x) => ({ productId: x.productId, quantity: x.quantity, price: x.price }))
    })
    ElMessage.success('创建成功')
    createVisible.value = false
    loadData()
  } catch { /* */ } finally { createLoading.value = false }
}

const openDetail = async (row) => {
  try {
    detailOrder.value = (await getSalesOrder(row.id)).data
    detailVisible.value = true
  } catch { ElMessage.error('加载详情失败') }
}

const doComplete = (row) => {
  ElMessageBox.confirm(`确认完成销售单「${row.orderNo}」？将自动扣减库存。`, '完成出库', { type: 'warning' })
    .then(async () => { await completeSalesOrder(row.id); ElMessage.success('货物已出库'); loadData() })
    .catch(() => {})
}

const doCancel = (row) => {
  ElMessageBox.confirm(`取消订单「${row.orderNo}」？`, '提示', { type: 'warning' })
    .then(async () => { await cancelSalesOrder(row.id); ElMessage.success('已取消'); loadData() })
    .catch(() => {})
}

const openCollect = (row) => {
  const left = unreceived(row)
  if (left <= 0) { ElMessage.info('该单已收清'); return }
  collectRow.value = row
  collectAmount.value = left
  collectVisible.value = true
}

const submitCollect = async () => {
  if (!collectRow.value || collectAmount.value <= 0) { ElMessage.warning('请输入有效金额'); return }
  try {
    await collectSalesOrder(collectRow.value.id, { amount: collectAmount.value })
    ElMessage.success('收款已登记')
    collectVisible.value = false
    loadData()
  } catch { /* */ }
}

const payTag = (s) => ({ UNPAID: 'danger', PARTIAL: 'warning', PAID: 'success' }[s] || 'info')
const payText = (s) => ({ UNPAID: '未收款', PARTIAL: '部分收款', PAID: '已收清' }[s] || s)
const statusType = (s) => ({ PENDING: 'info', COMPLETED: 'success', CANCELLED: 'danger' }[s] || 'info')
const statusText = (s) => ({ PENDING: '待出库', COMPLETED: '已出库', CANCELLED: '已取消' }[s] || s)

onMounted(() => { loadOptions().then(() => loadData()) })
</script>

<style scoped lang="scss">
.sales-list {
  .card-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
  .search-bar { margin-bottom: 16px; display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
  .pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
  .items-table { margin-top: 8px; width: 100%; }
  .detail-body p { margin: 6px 0; }
  .detail-table { margin-top: 12px; }
  .pay-hint p { margin: 4px 0; font-size: 13px; color: var(--el-text-color-regular); }
}
</style>
