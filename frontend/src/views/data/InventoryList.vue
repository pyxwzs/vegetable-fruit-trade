<template>
  <div class="inventory-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>库存查看</span>
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
        <el-table-column prop="quantity" label="当前库存" width="110">
          <template #default="{ row }">
            <span :class="{ 'low-stock': isLowStock(row) }">{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="product.unit" label="单位" width="80" />
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getInventories, getLowStockProducts } from '@/api/inventory'
import { getActiveWarehouses } from '@/api/warehouse'

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

const handleSearch = () => { page.value = 1; loadData() }
const resetSearch = () => { searchKeyword.value = ''; warehouseId.value = null; page.value = 1; loadData() }

const isLowStock = (row) => { const q = Number(row.quantity); return q > 0 && q < LOW_STOCK_THRESHOLD }

onMounted(() => {
  loadWarehouses().then(() => loadData())
  loadWarnings()
})
</script>

<style scoped lang="scss">
.inventory-list {
  .card-header { display: flex; justify-content: space-between; align-items: center; }
  .search-bar { margin-bottom: 16px; display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
  .warning-block { margin-bottom: 16px; }
  .pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
  .low-stock { color: #f56c6c; font-weight: bold; }
}
</style>
