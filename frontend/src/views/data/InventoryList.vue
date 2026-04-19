<template>
  <div class="inventory-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>库存管理</span>
          <div class="header-actions">
            <el-button type="success" @click="openInbound">入库</el-button>
            <el-button type="warning" @click="openOutbound()">出库</el-button>
            <el-button @click="handleExport">导出</el-button>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-input
            v-model="searchKeyword"
            placeholder="商品名称/编码/批次号"
            style="width: 260px"
            clearable
            @keyup.enter="handleSearch"
        />
        <el-select v-model="warehouseId" placeholder="仓库" clearable style="width: 160px">
          <el-option
              v-for="w in warehouses"
              :key="w.id"
              :label="w.name"
              :value="w.id"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <div v-if="expiringList.length || lowStockList.length" class="warning-block">
        <el-alert type="warning" show-icon :closable="false">
          <template #title>
            预警：临期 {{ expiringList.length }} 条，低库存（&lt;10）{{ lowStockList.length }} 条
          </template>
        </el-alert>
      </div>

      <el-table :data="inventories" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="product.productCode" label="商品编码" width="120" />
        <el-table-column prop="product.name" label="商品名称" min-width="140" />
        <el-table-column prop="warehouse.name" label="仓库" width="100" />
        <el-table-column prop="batchNo" label="批次号" width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.batchNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="quantity" label="库存数量" width="100">
          <template #default="{ row }">
            <span :class="{ 'low-stock': isLowStock(row) }">{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="availableQuantity" label="可用" width="90" />
        <el-table-column prop="frozenQuantity" label="冻结" width="90" />
        <el-table-column prop="productionDate" label="生产日期" width="110">
          <template #default="{ row }">{{ formatDate(row.productionDate) }}</template>
        </el-table-column>
        <el-table-column prop="expiryDate" label="过期日期" width="110">
          <template #default="{ row }">
            <span :class="{ expiring: isExpiring(row), expired: isExpired(row) }">
              {{ formatDate(row.expiryDate) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="location" label="库位" width="90" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="warning" link @click="openOutbound(row)">出库</el-button>
            <el-button type="primary" link @click="openStocktake(row)">盘点</el-button>
            <el-button type="success" link @click="openTransfer(row)">调拨</el-button>
            <el-button type="info" link @click="openFreeze(row)">冻结</el-button>
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

    <!-- 入库 -->
    <el-dialog v-model="inboundVisible" title="入库" width="520px" destroy-on-close>
      <el-form :model="inboundForm" label-width="100px">
        <el-form-item label="商品" required>
          <el-select v-model="inboundForm.productId" filterable placeholder="请选择商品" style="width: 100%">
            <el-option v-for="item in productOptions" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="仓库" required>
          <el-select v-model="inboundForm.warehouseId" placeholder="请选择仓库" style="width: 100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="批次号">
          <el-input v-model="inboundForm.batchNo" placeholder="可选" />
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number v-model="inboundForm.quantity" :min="0.001" :precision="3" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单价">
          <el-input-number v-model="inboundForm.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="生产日期">
          <el-date-picker
              v-model="inboundForm.productionDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="过期日期">
          <el-date-picker
              v-model="inboundForm.expiryDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="不填则按商品保质期推算"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="库位">
          <el-input v-model="inboundForm.location" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inboundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitInbound">确定</el-button>
      </template>
    </el-dialog>

    <!-- 出库 -->
    <el-dialog v-model="outboundVisible" title="出库" width="480px" destroy-on-close>
      <el-form :model="outboundForm" label-width="100px">
        <el-form-item label="商品">
          <el-input :model-value="outboundForm.productName" disabled />
        </el-form-item>
        <el-form-item label="仓库">
          <el-input :model-value="outboundForm.warehouseName" disabled />
        </el-form-item>
        <el-form-item label="批次号">
          <el-input :model-value="outboundForm.batchNo || '-'" disabled />
        </el-form-item>
        <el-form-item label="可出数量">
          <span>{{ outboundForm.availableQuantity }}</span>
        </el-form-item>
        <el-form-item label="出库数量" required>
          <el-input-number
              v-model="outboundForm.quantity"
              :min="0.001"
              :max="Number(outboundForm.availableQuantity) || undefined"
              :precision="3"
              style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="outboundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitOutbound">确定</el-button>
      </template>
    </el-dialog>

    <!-- 盘点 -->
    <el-dialog v-model="stocktakeVisible" title="库存盘点" width="460px" destroy-on-close>
      <el-form :model="stocktakeForm" label-width="110px">
        <el-form-item label="当前账面数量">
          <span>{{ stocktakeForm.bookQuantity }}</span>
        </el-form-item>
        <el-form-item label="冻结数量">
          <span>{{ stocktakeForm.frozenQuantity }}</span>
        </el-form-item>
        <el-form-item label="实盘数量" required>
          <el-input-number v-model="stocktakeForm.actualQuantity" :min="0" :precision="3" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="stocktakeForm.remark" type="textarea" rows="2" placeholder="盘盈/盘亏说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="stocktakeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStocktake">保存盘点</el-button>
      </template>
    </el-dialog>

    <!-- 调拨 -->
    <el-dialog v-model="transferVisible" title="仓库间调拨" width="480px" destroy-on-close>
      <el-form :model="transferForm" label-width="100px">
        <el-form-item label="从仓库">
          <el-input :model-value="transferForm.fromWarehouseName" disabled />
        </el-form-item>
        <el-form-item label="目标仓库" required>
          <el-select v-model="transferForm.toWarehouseId" placeholder="选择目标仓库" style="width: 100%">
            <el-option
                v-for="w in transferWarehouseOptions"
                :key="w.id"
                :label="w.name"
                :value="w.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="可调数量">
          <span>{{ transferForm.availableQuantity }}</span>
        </el-form-item>
        <el-form-item label="调拨数量" required>
          <el-input-number
              v-model="transferForm.quantity"
              :min="0.001"
              :max="Number(transferForm.availableQuantity) || undefined"
              :precision="3"
              style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="transferForm.remark" type="textarea" rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" @click="submitTransfer">确认调拨</el-button>
      </template>
    </el-dialog>

    <!-- 冻结 -->
    <el-dialog v-model="freezeVisible" title="冻结库存" width="400px" destroy-on-close>
      <el-form :model="freezeForm" label-width="100px">
        <el-form-item label="可用数量">
          <span>{{ freezeForm.availableQuantity }}</span>
        </el-form-item>
        <el-form-item label="冻结数量" required>
          <el-input-number
              v-model="freezeForm.quantity"
              :min="0.001"
              :max="Number(freezeForm.availableQuantity) || undefined"
              :precision="3"
              style="width: 100%"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="freezeVisible = false">取消</el-button>
        <el-button type="primary" @click="submitFreeze">确认冻结</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getInventories,
  inbound,
  outbound,
  stocktake,
  transferInventory,
  freezeInventory,
  getExpiringProducts,
  getLowStockProducts
} from '@/api/inventory'
import { getActiveWarehouses } from '@/api/warehouse'
import { getAllEnabledProducts } from '@/api/product'
import { formatDate } from '@/utils/date'

const LOW_STOCK_THRESHOLD = 10

const loading = ref(false)
const inventories = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const warehouseId = ref(null)
const warehouses = ref([])
const expiringList = ref([])
const lowStockList = ref([])
const productOptions = ref([])

const inboundVisible = ref(false)
const inboundForm = ref({
  productId: null,
  warehouseId: null,
  batchNo: '',
  quantity: 1,
  price: 0,
  productionDate: null,
  expiryDate: null,
  location: ''
})

const outboundVisible = ref(false)
const outboundForm = ref({
  productId: null,
  warehouseId: null,
  batchNo: '',
  productName: '',
  warehouseName: '',
  availableQuantity: 0,
  quantity: 1
})

const stocktakeVisible = ref(false)
const stocktakeForm = ref({
  inventoryId: null,
  bookQuantity: 0,
  frozenQuantity: 0,
  actualQuantity: 0,
  remark: ''
})

const transferVisible = ref(false)
const transferForm = ref({
  sourceInventoryId: null,
  fromWarehouseId: null,
  fromWarehouseName: '',
  availableQuantity: 0,
  toWarehouseId: null,
  quantity: 1,
  remark: ''
})

const freezeVisible = ref(false)
const freezeForm = ref({
  inventoryId: null,
  availableQuantity: 0,
  quantity: 1
})

const transferWarehouseOptions = computed(() => {
  const cur = transferForm.value.fromWarehouseId
  return warehouses.value.filter((w) => w.id !== cur)
})

const loadWarehouses = async () => {
  try {
    const res = await getActiveWarehouses()
    warehouses.value = res.data || []
  } catch {
    warehouses.value = []
  }
}

const loadWarnings = async () => {
  try {
    const [e, l] = await Promise.all([getExpiringProducts(), getLowStockProducts()])
    expiringList.value = e.data || []
    lowStockList.value = l.data || []
  } catch {
    expiringList.value = []
    lowStockList.value = []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value - 1,
      size: size.value,
      keyword: searchKeyword.value || undefined,
      warehouseId: warehouseId.value || undefined
    }
    const response = await getInventories(params)
    inventories.value = response.data.content
    total.value = response.data.totalElements
  } catch (error) {
    console.error('加载库存失败:', error)
    ElMessage.error('加载库存失败')
  } finally {
    loading.value = false
  }
}

const loadProducts = async () => {
  try {
    const response = await getAllEnabledProducts()
    productOptions.value = response.data || []
  } catch (error) {
    console.error('加载商品失败:', error)
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const resetSearch = () => {
  searchKeyword.value = ''
  warehouseId.value = null
  page.value = 1
  loadData()
}

const openInbound = () => {
  inboundForm.value = {
    productId: null,
    warehouseId: warehouses.value[0]?.id ?? null,
    batchNo: '',
    quantity: 1,
    price: 0,
    productionDate: null,
    expiryDate: null,
    location: ''
  }
  inboundVisible.value = true
}

const submitInbound = async () => {
  const f = inboundForm.value
  if (!f.productId || !f.warehouseId || !f.quantity) {
    ElMessage.warning('请填写商品、仓库与数量')
    return
  }
  try {
    const payload = {
      productId: f.productId,
      warehouseId: f.warehouseId,
      batchNo: f.batchNo || undefined,
      quantity: f.quantity,
      price: f.price || undefined,
      productionDate: f.productionDate || undefined,
      expiryDate: f.expiryDate || undefined,
      location: f.location || undefined
    }
    await inbound(payload)
    ElMessage.success('入库成功')
    inboundVisible.value = false
    loadData()
    loadWarnings()
  } catch {
    /* 拦截器 */
  }
}

const openOutbound = (row) => {
  if (!row) {
    ElMessage.info('请从表格行点击「出库」，或先搜索到具体批次')
    return
  }
  outboundForm.value = {
    productId: row.product?.id,
    warehouseId: row.warehouse?.id,
    batchNo: row.batchNo,
    productName: row.product?.name || '',
    warehouseName: row.warehouse?.name || '',
    availableQuantity: row.availableQuantity,
    quantity: Math.min(1, Number(row.availableQuantity) || 1)
  }
  outboundVisible.value = true
}

const submitOutbound = async () => {
  const f = outboundForm.value
  if (!f.productId || !f.warehouseId || !f.quantity) {
    ElMessage.warning('数据不完整')
    return
  }
  try {
    await outbound({
      productId: f.productId,
      warehouseId: f.warehouseId,
      batchNo: f.batchNo || undefined,
      quantity: f.quantity
    })
    ElMessage.success('出库成功')
    outboundVisible.value = false
    loadData()
    loadWarnings()
  } catch {
    /* 拦截器 */
  }
}

const openStocktake = (row) => {
  stocktakeForm.value = {
    inventoryId: row.id,
    bookQuantity: row.quantity,
    frozenQuantity: row.frozenQuantity ?? 0,
    actualQuantity: row.quantity,
    remark: ''
  }
  stocktakeVisible.value = true
}

const submitStocktake = async () => {
  const f = stocktakeForm.value
  try {
    await stocktake({
      inventoryId: f.inventoryId,
      actualQuantity: f.actualQuantity,
      remark: f.remark || undefined
    })
    ElMessage.success('盘点已保存')
    stocktakeVisible.value = false
    loadData()
    loadWarnings()
  } catch {
    /* 拦截器 */
  }
}

const openTransfer = (row) => {
  transferForm.value = {
    sourceInventoryId: row.id,
    fromWarehouseId: row.warehouse?.id,
    fromWarehouseName: row.warehouse?.name || '',
    availableQuantity: row.availableQuantity,
    toWarehouseId: null,
    quantity: Math.min(1, Number(row.availableQuantity) || 1),
    remark: ''
  }
  transferVisible.value = true
}

const submitTransfer = async () => {
  const f = transferForm.value
  if (!f.toWarehouseId || !f.quantity) {
    ElMessage.warning('请选择目标仓库并填写数量')
    return
  }
  try {
    await transferInventory({
      sourceInventoryId: f.sourceInventoryId,
      toWarehouseId: f.toWarehouseId,
      quantity: f.quantity,
      remark: f.remark || undefined
    })
    ElMessage.success('调拨成功')
    transferVisible.value = false
    loadData()
    loadWarnings()
  } catch {
    /* 拦截器 */
  }
}

const openFreeze = (row) => {
  freezeForm.value = {
    inventoryId: row.id,
    availableQuantity: row.availableQuantity,
    quantity: Math.min(1, Number(row.availableQuantity) || 1)
  }
  freezeVisible.value = true
}

const submitFreeze = async () => {
  const f = freezeForm.value
  try {
    await freezeInventory({
      inventoryId: f.inventoryId,
      quantity: f.quantity
    })
    ElMessage.success('已冻结')
    freezeVisible.value = false
    loadData()
    loadWarnings()
  } catch {
    /* 拦截器 */
  }
}

const isLowStock = (row) => {
  const q = Number(row.quantity)
  return q > 0 && q < LOW_STOCK_THRESHOLD
}

const isExpiring = (item) => {
  if (!item.expiryDate) return false
  const days = (new Date(item.expiryDate) - new Date()) / (1000 * 60 * 60 * 24)
  return days <= 7 && days > 0
}

const isExpired = (item) => {
  if (!item.expiryDate) return false
  return new Date(item.expiryDate) < new Date()
}

const getStatusType = (status) => {
  const types = {
    NORMAL: 'success',
    EXPIRING: 'warning',
    EXPIRED: 'danger',
    FROZEN: 'info'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    NORMAL: '正常',
    EXPIRING: '临期',
    EXPIRED: '过期',
    FROZEN: '冻结'
  }
  return texts[status] || status
}

const handleExport = () => {
  ElMessage.info('导出功能开发中')
}

onMounted(() => {
  loadWarehouses().then(() => {
    loadData()
  })
  loadProducts()
  loadWarnings()
})
</script>

<style scoped lang="scss">
.inventory-list {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  .header-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .search-bar {
    margin-bottom: 16px;
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    align-items: center;
  }

  .warning-block {
    margin-bottom: 16px;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .low-stock {
    color: #f56c6c;
    font-weight: bold;
  }

  .expiring {
    color: #e6a23c;
    font-weight: bold;
  }

  .expired {
    color: #f56c6c;
    text-decoration: line-through;
  }
}
</style>
