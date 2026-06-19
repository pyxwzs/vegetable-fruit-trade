<template>
  <div class="inventory-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>库存管理</span>
          <div class="header-actions">
            <el-button type="success" @click="openInbound">入库</el-button>
            <el-button type="warning" @click="openOutbound()">出库</el-button>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-input
            v-model="searchKeyword"
            placeholder="商品名称/编码"
            style="width: 260px"
            clearable
            @keyup.enter="handleSearch"
        />
        <el-select v-model="warehouseId" placeholder="仓库" clearable style="width: 160px">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <div v-if="lowStockList.length" class="warning-block">
        <el-alert type="warning" show-icon :closable="false">
          <template #title>预警：低库存（&lt;10）{{ lowStockList.length }} 条</template>
        </el-alert>
      </div>

      <el-table :data="inventories" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="product.productCode" label="商品编码" width="120" />
        <el-table-column prop="product.name" label="商品名称" min-width="160" />
        <el-table-column prop="warehouse.name" label="仓库" width="120" />
        <el-table-column prop="quantity" label="库存数量" width="110">
          <template #default="{ row }">
            <span :class="{ 'low-stock': isLowStock(row) }">{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="success" link @click="openInboundForRow(row)">入库</el-button>
            <el-button type="warning" link @click="openOutbound(row)">出库</el-button>
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
    <el-dialog v-model="inboundVisible" title="入库" width="480px" destroy-on-close>
      <el-form :model="inboundForm" label-width="80px">
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
        <el-form-item label="数量" required>
          <el-input-number v-model="inboundForm.quantity" :min="0.001" :precision="3" style="width: 100%" />
        </el-form-item>
        <el-form-item label="单价">
          <el-input-number v-model="inboundForm.price" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inboundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitInbound">确定</el-button>
      </template>
    </el-dialog>

    <!-- 出库 -->
    <el-dialog v-model="outboundVisible" title="出库" width="420px" destroy-on-close>
      <el-form :model="outboundForm" label-width="80px">
        <el-form-item label="商品">
          <el-input :model-value="outboundForm.productName" disabled />
        </el-form-item>
        <el-form-item label="仓库">
          <el-input :model-value="outboundForm.warehouseName" disabled />
        </el-form-item>
        <el-form-item label="当前库存">
          <span>{{ outboundForm.currentQuantity }}</span>
        </el-form-item>
        <el-form-item label="出库数量" required>
          <el-input-number
              v-model="outboundForm.quantity"
              :min="0.001"
              :max="Number(outboundForm.currentQuantity) || undefined"
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getInventories, inbound, outbound, getLowStockProducts } from '@/api/inventory'
import { getActiveWarehouses } from '@/api/warehouse'
import { getAllEnabledProducts } from '@/api/product'

const LOW_STOCK_THRESHOLD = 10

const loading = ref(false)
const inventories = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const warehouseId = ref(null)
const warehouses = ref([])
const lowStockList = ref([])
const productOptions = ref([])

const inboundVisible = ref(false)
const inboundForm = ref({ productId: null, warehouseId: null, quantity: 1, price: 0 })

const outboundVisible = ref(false)
const outboundForm = ref({
  productId: null, warehouseId: null,
  productName: '', warehouseName: '', currentQuantity: 0, quantity: 1
})

const loadWarehouses = async () => {
  try { warehouses.value = (await getActiveWarehouses()).data || [] } catch { warehouses.value = [] }
}

const loadWarnings = async () => {
  try { lowStockList.value = (await getLowStockProducts()).data || [] } catch { lowStockList.value = [] }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getInventories({
      page: page.value - 1, size: size.value,
      keyword: searchKeyword.value || undefined, warehouseId: warehouseId.value || undefined
    })
    inventories.value = res.data.content
    total.value = res.data.totalElements
  } catch { ElMessage.error('加载库存失败') } finally { loading.value = false }
}

const loadProducts = async () => {
  try { productOptions.value = (await getAllEnabledProducts()).data || [] } catch { /* */ }
}

const handleSearch = () => { page.value = 1; loadData() }
const resetSearch = () => { searchKeyword.value = ''; warehouseId.value = null; page.value = 1; loadData() }

const openInbound = () => {
  inboundForm.value = { productId: null, warehouseId: warehouses.value[0]?.id ?? null, quantity: 1, price: 0 }
  inboundVisible.value = true
}

const openInboundForRow = (row) => {
  inboundForm.value = {
    productId: row.product?.id, warehouseId: row.warehouse?.id, quantity: 1, price: 0
  }
  inboundVisible.value = true
}

const submitInbound = async () => {
  const f = inboundForm.value
  if (!f.productId || !f.warehouseId || !f.quantity) { ElMessage.warning('请填写商品、仓库与数量'); return }
  try {
    await inbound({ productId: f.productId, warehouseId: f.warehouseId, quantity: f.quantity, price: f.price || undefined })
    ElMessage.success('入库成功')
    inboundVisible.value = false
    loadData(); loadWarnings()
  } catch { /* 拦截器 */ }
}

const openOutbound = (row) => {
  if (!row) { ElMessage.info('请从表格行点击「出库」'); return }
  outboundForm.value = {
    productId: row.product?.id, warehouseId: row.warehouse?.id,
    productName: row.product?.name || '', warehouseName: row.warehouse?.name || '',
    currentQuantity: row.quantity, quantity: Math.min(1, Number(row.quantity) || 1)
  }
  outboundVisible.value = true
}

const submitOutbound = async () => {
  const f = outboundForm.value
  if (!f.productId || !f.warehouseId || !f.quantity) { ElMessage.warning('数据不完整'); return }
  try {
    await outbound({ productId: f.productId, warehouseId: f.warehouseId, quantity: f.quantity })
    ElMessage.success('出库成功')
    outboundVisible.value = false
    loadData(); loadWarnings()
  } catch { /* 拦截器 */ }
}

const isLowStock = (row) => { const q = Number(row.quantity); return q > 0 && q < LOW_STOCK_THRESHOLD }

onMounted(() => {
  loadWarehouses().then(() => loadData())
  loadProducts()
  loadWarnings()
})
</script>

<style scoped lang="scss">
.inventory-list {
  .card-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
  .header-actions { display: flex; flex-wrap: wrap; gap: 8px; }
  .search-bar { margin-bottom: 16px; display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
  .warning-block { margin-bottom: 16px; }
  .pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
  .low-stock { color: #f56c6c; font-weight: bold; }
}
</style>
