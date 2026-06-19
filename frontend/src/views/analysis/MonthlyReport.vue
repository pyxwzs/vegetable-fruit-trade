<template>
  <div class="reconciliation">
    <el-tabs v-model="activeTab" type="border-card" @tab-change="onTabChange">

      <el-tab-pane label="农户对账" name="purchase">
        <ReconciliationPanel
          type="purchase"
          :entities="suppliers"
          entity-label="农户"
          :entity-name="entityName('purchase')"
          v-model:entity-id="purchaseEntityId"
          v-model:year="purchaseYear"
          v-model:month="purchaseMonth"
          v-model:view-mode="purchaseViewMode"
          v-model:selected-day="purchaseSelectedDay"
          :selected-month="purchaseSelectedMonth"
          :summary-rows="purchaseSummaryRows"
          :daily-rows="purchaseDailyRows"
          :partner-report="purchasePartnerReport"
          :items="purchaseItems"
          :loading="purchaseLoading"
          @query="loadPurchase"
          @select-month="selectPurchaseMonth"
          @select-partner="id => selectPartner('purchase', id)"
          @export="exportPurchase"
        />
      </el-tab-pane>

      <el-tab-pane label="客户对账" name="sales">
        <ReconciliationPanel
          type="sales"
          :entities="customers"
          entity-label="客户"
          :entity-name="entityName('sales')"
          v-model:entity-id="salesEntityId"
          v-model:year="salesYear"
          v-model:month="salesMonth"
          v-model:view-mode="salesViewMode"
          v-model:selected-day="salesSelectedDay"
          :selected-month="salesSelectedMonth"
          :summary-rows="salesSummaryRows"
          :daily-rows="salesDailyRows"
          :partner-report="salesPartnerReport"
          :items="salesItems"
          :loading="salesLoading"
          @query="loadSales"
          @select-month="selectSalesMonth"
          @select-partner="id => selectPartner('sales', id)"
          @export="exportSales"
        />
      </el-tab-pane>

    </el-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ReconciliationPanel from './ReconciliationPanel.vue'
import {
  getMonthlyPurchase, getMonthlySales,
  getPurchaseItems, getSalesItems,
  getDailyPurchaseDetail, getDailySalesDetail,
  getPurchasePartners, getSalesPartners
} from '@/api/analytics'
import { getSuppliers } from '@/api/supplier'
import { getCustomers } from '@/api/customer'
import { downloadMonthExcel, downloadYearExcel, downloadPartnerListExcel } from '@/utils/reconciliationExport'

const activeTab = ref('purchase')
const currentYear = new Date().getFullYear()
const currentMonth = new Date().getMonth() + 1

const suppliers = ref([])
const customers = ref([])

const purchaseEntityId = ref(null)
const purchaseYear = ref(currentYear)
const purchaseMonth = ref(currentMonth)
const purchaseViewMode = ref('year')
const purchaseSelectedDay = ref('')
const purchaseSummary = ref(null)
const purchaseDaily = ref(null)
const purchasePartnerReport = ref(null)
const purchaseItems = ref([])
const purchaseLoading = ref(false)
const purchaseSelectedMonth = ref(currentMonth)

const salesEntityId = ref(null)
const salesYear = ref(currentYear)
const salesMonth = ref(currentMonth)
const salesViewMode = ref('year')
const salesSelectedDay = ref('')
const salesSummary = ref(null)
const salesDaily = ref(null)
const salesPartnerReport = ref(null)
const salesItems = ref([])
const salesLoading = ref(false)
const salesSelectedMonth = ref(currentMonth)

const purchaseSummaryRows = computed(() => purchaseSummary.value?.rows || [])
const purchaseDailyRows = computed(() => purchaseDaily.value?.rows || [])
const salesSummaryRows = computed(() => salesSummary.value?.rows || [])
const salesDailyRows = computed(() => salesDaily.value?.rows || [])

const entityName = (type) => {
  const id = type === 'purchase' ? purchaseEntityId.value : salesEntityId.value
  const list = type === 'purchase' ? suppliers.value : customers.value
  return list.find(e => e.id === id)?.name || ''
}

const loadMonthItems = async (type, year, month, entityId) => {
  const params = { year, month }
  if (type === 'purchase') {
    if (entityId) params.supplierId = entityId
    return (await getPurchaseItems(params)).data?.items || []
  }
  if (entityId) params.customerId = entityId
  return (await getSalesItems(params)).data?.items || []
}

const loadPurchase = async () => {
  purchaseLoading.value = true
  purchaseSelectedDay.value = ''
  purchaseItems.value = []
  try {
    if (!purchaseEntityId.value) {
      const params = { year: purchaseYear.value }
      if (purchaseViewMode.value === 'month') params.month = purchaseMonth.value
      purchasePartnerReport.value = (await getPurchasePartners(params)).data
      purchaseSummary.value = null
      purchaseDaily.value = null
      return
    }

    purchasePartnerReport.value = null
    if (purchaseViewMode.value === 'year') {
      purchaseSummary.value = (await getMonthlyPurchase({
        year: purchaseYear.value,
        supplierId: purchaseEntityId.value
      })).data
      purchaseDaily.value = null
      purchaseItems.value = await loadMonthItems(
        'purchase', purchaseYear.value, purchaseSelectedMonth.value, purchaseEntityId.value
      )
    } else {
      purchaseSummary.value = null
      purchaseDaily.value = (await getDailyPurchaseDetail({
        supplierId: purchaseEntityId.value,
        year: purchaseYear.value,
        month: purchaseMonth.value
      })).data
      purchaseItems.value = await loadMonthItems(
        'purchase', purchaseYear.value, purchaseMonth.value, purchaseEntityId.value
      )
    }
  } catch {
    ElMessage.error('查询失败')
  } finally {
    purchaseLoading.value = false
  }
}

const loadSales = async () => {
  salesLoading.value = true
  salesSelectedDay.value = ''
  salesItems.value = []
  try {
    if (!salesEntityId.value) {
      const params = { year: salesYear.value }
      if (salesViewMode.value === 'month') params.month = salesMonth.value
      salesPartnerReport.value = (await getSalesPartners(params)).data
      salesSummary.value = null
      salesDaily.value = null
      return
    }

    salesPartnerReport.value = null
    if (salesViewMode.value === 'year') {
      salesSummary.value = (await getMonthlySales({
        year: salesYear.value,
        customerId: salesEntityId.value
      })).data
      salesDaily.value = null
      salesItems.value = await loadMonthItems(
        'sales', salesYear.value, salesSelectedMonth.value, salesEntityId.value
      )
    } else {
      salesSummary.value = null
      salesDaily.value = (await getDailySalesDetail({
        customerId: salesEntityId.value,
        year: salesYear.value,
        month: salesMonth.value
      })).data
      salesItems.value = await loadMonthItems(
        'sales', salesYear.value, salesMonth.value, salesEntityId.value
      )
    }
  } catch {
    ElMessage.error('查询失败')
  } finally {
    salesLoading.value = false
  }
}

const selectPartner = (type, id) => {
  if (type === 'purchase') {
    purchaseEntityId.value = id
    loadPurchase()
  } else {
    salesEntityId.value = id
    loadSales()
  }
}

const selectPurchaseMonth = async (month) => {
  purchaseSelectedMonth.value = month
  purchaseLoading.value = true
  try {
    purchaseItems.value = await loadMonthItems(
      'purchase', purchaseYear.value, month, purchaseEntityId.value
    )
  } catch {
    ElMessage.error('加载明细失败')
  } finally {
    purchaseLoading.value = false
  }
}

const selectSalesMonth = async (month) => {
  salesSelectedMonth.value = month
  salesLoading.value = true
  try {
    salesItems.value = await loadMonthItems(
      'sales', salesYear.value, month, salesEntityId.value
    )
  } catch {
    ElMessage.error('加载明细失败')
  } finally {
    salesLoading.value = false
  }
}

const exportPurchase = async () => {
  if (!purchaseEntityId.value) {
    if (!purchasePartnerReport.value?.rows?.length) {
      ElMessage.warning('暂无数据')
      return
    }
    const label = purchaseViewMode.value === 'year'
      ? `${purchaseYear.value}年`
      : `${purchaseYear.value}年${purchaseMonth.value}月`
    downloadPartnerListExcel(purchasePartnerReport.value, { type: 'purchase', periodLabel: label })
    ElMessage.success('导出成功')
    return
  }

  if (purchaseViewMode.value === 'year') {
    purchaseLoading.value = true
    try {
      await downloadYearExcel(
        (year, month) => loadMonthItems('purchase', year, month, purchaseEntityId.value),
        { entityName: entityName('purchase'), year: purchaseYear.value, type: 'purchase' }
      )
      ElMessage.success('导出成功')
    } catch {
      ElMessage.error('导出失败')
    } finally {
      purchaseLoading.value = false
    }
  } else {
    if (!purchaseItems.value.length) { ElMessage.warning('暂无数据'); return }
    downloadMonthExcel(purchaseItems.value, {
      entityName: entityName('purchase'),
      year: purchaseYear.value,
      month: purchaseMonth.value,
      type: 'purchase'
    })
    ElMessage.success('导出成功')
  }
}

const exportSales = async () => {
  if (!salesEntityId.value) {
    if (!salesPartnerReport.value?.rows?.length) {
      ElMessage.warning('暂无数据')
      return
    }
    const label = salesViewMode.value === 'year'
      ? `${salesYear.value}年`
      : `${salesYear.value}年${salesMonth.value}月`
    downloadPartnerListExcel(salesPartnerReport.value, { type: 'sales', periodLabel: label })
    ElMessage.success('导出成功')
    return
  }

  if (salesViewMode.value === 'year') {
    salesLoading.value = true
    try {
      await downloadYearExcel(
        (year, month) => loadMonthItems('sales', year, month, salesEntityId.value),
        { entityName: entityName('sales'), year: salesYear.value, type: 'sales' }
      )
      ElMessage.success('导出成功')
    } catch {
      ElMessage.error('导出失败')
    } finally {
      salesLoading.value = false
    }
  } else {
    if (!salesItems.value.length) { ElMessage.warning('暂无数据'); return }
    downloadMonthExcel(salesItems.value, {
      entityName: entityName('sales'),
      year: salesYear.value,
      month: salesMonth.value,
      type: 'sales'
    })
    ElMessage.success('导出成功')
  }
}

const onTabChange = () => {
  purchaseSelectedDay.value = ''
  salesSelectedDay.value = ''
}

onMounted(async () => {
  try {
    suppliers.value = (await getSuppliers({ page: 0, size: 500 })).data?.content || []
    customers.value = (await getCustomers({ page: 0, size: 500 })).data?.content || []
  } catch { /* silent */ }
  loadPurchase()
  loadSales()
})
</script>

<style scoped lang="scss">
.reconciliation {
  :deep(.el-tabs__content) { padding: 12px; }
}
</style>
