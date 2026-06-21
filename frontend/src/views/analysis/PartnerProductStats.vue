<template>
  <div class="partner-product-stats">
    <el-tabs v-model="activeTab" type="border-card" @tab-change="onTabChange">
      <el-tab-pane label="供应商供货统计" name="purchase">
        <StatsPanel
          type="purchase"
          :year="year"
          :month="month"
          :entity-id="supplierId"
          :entities="suppliers"
          entity-label="供应商"
          :report="purchaseReport"
          :loading="loading"
          @update:year="v => { year = v; load() }"
          @update:month="v => { month = v; load() }"
          @update:entity-id="v => { supplierId = v; load() }"
          @query="load"
        />
      </el-tab-pane>
      <el-tab-pane label="客户销售统计" name="sales">
        <StatsPanel
          type="sales"
          :year="year"
          :month="month"
          :entity-id="customerId"
          :entities="customers"
          entity-label="客户"
          :report="salesReport"
          :loading="loading"
          @update:year="v => { year = v; load() }"
          @update:month="v => { month = v; load() }"
          @update:entity-id="v => { customerId = v; load() }"
          @query="load"
        />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import StatsPanel from './PartnerProductStatsPanel.vue'
import { getSupplierProductStats, getCustomerProductStats } from '@/api/analytics'
import { getSuppliers } from '@/api/supplier'
import { getCustomers } from '@/api/customer'
import { usePageRefresh } from '@/composables/usePageRefresh'

const activeTab = ref('purchase')
const currentYear = new Date().getFullYear()
const year = ref(currentYear)
const month = ref(null)
const supplierId = ref(null)
const customerId = ref(null)
const suppliers = ref([])
const customers = ref([])
const purchaseReport = ref(null)
const salesReport = ref(null)
const loading = ref(false)

const load = async () => {
  loading.value = true
  try {
    const params = { year: year.value }
    if (month.value) params.month = month.value
    if (activeTab.value === 'purchase') {
      if (supplierId.value) params.supplierId = supplierId.value
      purchaseReport.value = (await getSupplierProductStats(params)).data
    } else {
      if (customerId.value) params.customerId = customerId.value
      salesReport.value = (await getCustomerProductStats(params)).data
    }
  } catch {
    if (activeTab.value === 'purchase') purchaseReport.value = null
    else salesReport.value = null
  } finally {
    loading.value = false
  }
}

const onTabChange = () => load()

usePageRefresh(load)

onMounted(async () => {
  try {
    const [s, c] = await Promise.all([
      getSuppliers({ page: 0, size: 500 }),
      getCustomers({ page: 0, size: 500 })
    ])
    suppliers.value = s.data?.content || []
    customers.value = c.data?.content || []
  } catch { /* silent */ }
  load()
})
</script>

<style scoped lang="scss">
.partner-product-stats {
  :deep(.el-tabs__content) { padding: 12px; }
}
</style>
