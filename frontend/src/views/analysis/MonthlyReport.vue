<template>
  <div class="monthly-report">
    <el-tabs v-model="activeTab" type="border-card">

      <!-- ========== 农户采购月报 ========== -->
      <el-tab-pane label="农户采购月报" name="purchase">
        <div class="toolbar">
          <el-select
              v-model="purchaseSupplierId"
              placeholder="全部农户"
              clearable filterable
              style="width: 200px"
              @change="loadPurchase"
          >
            <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
          <el-select v-model="purchaseYear" style="width: 110px" @change="loadPurchase">
            <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
          </el-select>
          <el-button type="primary" @click="loadPurchase">查询</el-button>
          <el-button type="success" @click="exportPurchase">导出 Excel</el-button>
        </div>

        <div v-if="purchaseReport" class="report-wrap">
          <div class="report-title">
            {{ purchaseReport.entityName }} · {{ purchaseReport.year }}年 采购汇总
          </div>
          <el-table :data="purchaseReport.rows" border show-summary :summary-method="purchaseSummary">
            <el-table-column prop="month" label="月份" width="80">
              <template #default="{ row }">{{ row.month }}月</template>
            </el-table-column>
            <el-table-column prop="orderCount" label="订单数" width="90" />
            <el-table-column label="采购总额" width="130">
              <template #default="{ row }">
                <span :class="{ 'zero': !row.totalAmount || row.totalAmount == 0 }">
                  ¥{{ Number(row.totalAmount).toFixed(2) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="已付金额" width="130">
              <template #default="{ row }">¥{{ Number(row.settledAmount).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="未付金额" width="130">
              <template #default="{ row }">
                <span :class="{ 'unpaid': row.pendingAmount > 0 }">
                  ¥{{ Number(row.pendingAmount).toFixed(2) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-else description="请选择农户和年份查询" />
      </el-tab-pane>

      <!-- ========== 客户销售月报 ========== -->
      <el-tab-pane label="客户销售月报" name="sales">
        <div class="toolbar">
          <el-select
              v-model="salesCustomerId"
              placeholder="全部客户"
              clearable filterable
              style="width: 200px"
              @change="loadSales"
          >
            <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
          <el-select v-model="salesYear" style="width: 110px" @change="loadSales">
            <el-option v-for="y in yearOptions" :key="y" :label="`${y}年`" :value="y" />
          </el-select>
          <el-button type="primary" @click="loadSales">查询</el-button>
          <el-button type="success" @click="exportSales">导出 Excel</el-button>
        </div>

        <div v-if="salesReport" class="report-wrap">
          <div class="report-title">
            {{ salesReport.entityName }} · {{ salesReport.year }}年 销售汇总
          </div>
          <el-table :data="salesReport.rows" border show-summary :summary-method="salesSummary">
            <el-table-column prop="month" label="月份" width="80">
              <template #default="{ row }">{{ row.month }}月</template>
            </el-table-column>
            <el-table-column prop="orderCount" label="订单数" width="90" />
            <el-table-column label="销售总额" width="130">
              <template #default="{ row }">
                <span :class="{ 'zero': !row.totalAmount || row.totalAmount == 0 }">
                  ¥{{ Number(row.totalAmount).toFixed(2) }}
                </span>
              </template>
            </el-table-column>
            <el-table-column label="已收金额" width="130">
              <template #default="{ row }">¥{{ Number(row.settledAmount).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="未收金额" width="130">
              <template #default="{ row }">
                <span :class="{ 'unpaid': row.pendingAmount > 0 }">
                  ¥{{ Number(row.pendingAmount).toFixed(2) }}
                </span>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-else description="请选择客户和年份查询" />
      </el-tab-pane>

    </el-tabs>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import * as XLSX from 'xlsx'
import { getMonthlyPurchase, getMonthlySales } from '@/api/analytics'
import { getSuppliers } from '@/api/supplier'
import { getCustomers } from '@/api/customer'

const activeTab = ref('purchase')
const currentYear = new Date().getFullYear()
const yearOptions = Array.from({ length: 5 }, (_, i) => currentYear - i)

// 农户采购
const suppliers = ref([])
const purchaseSupplierId = ref(null)
const purchaseYear = ref(currentYear)
const purchaseReport = ref(null)
const purchaseLoading = ref(false)

// 客户销售
const customers = ref([])
const salesCustomerId = ref(null)
const salesYear = ref(currentYear)
const salesReport = ref(null)
const salesLoading = ref(false)

const loadSuppliers = async () => {
  try {
    const res = await getSuppliers({ page: 0, size: 200 })
    suppliers.value = res.data.content || []
  } catch { suppliers.value = [] }
}

const loadCustomers = async () => {
  try {
    const res = await getCustomers({ page: 0, size: 200 })
    customers.value = res.data.content || []
  } catch { customers.value = [] }
}

const loadPurchase = async () => {
  purchaseLoading.value = true
  try {
    const params = { year: purchaseYear.value }
    if (purchaseSupplierId.value) params.supplierId = purchaseSupplierId.value
    purchaseReport.value = (await getMonthlyPurchase(params)).data
  } catch { ElMessage.error('查询失败') } finally { purchaseLoading.value = false }
}

const loadSales = async () => {
  salesLoading.value = true
  try {
    const params = { year: salesYear.value }
    if (salesCustomerId.value) params.customerId = salesCustomerId.value
    salesReport.value = (await getMonthlySales(params)).data
  } catch { ElMessage.error('查询失败') } finally { salesLoading.value = false }
}

const exportToExcel = (report, type) => {
  if (!report) { ElMessage.warning('请先查询数据'); return }
  const label = type === 'purchase' ? '采购' : '销售'
  const settledLabel = type === 'purchase' ? '已付金额' : '已收金额'
  const pendingLabel = type === 'purchase' ? '未付金额' : '未收金额'

  const headers = ['月份', '订单数', `${label}总额`, settledLabel, pendingLabel]
  const dataRows = report.rows.map((r) => [
    `${r.month}月`,
    r.orderCount,
    Number(r.totalAmount).toFixed(2),
    Number(r.settledAmount).toFixed(2),
    Number(r.pendingAmount).toFixed(2)
  ])
  dataRows.push([
    '全年合计',
    report.rows.reduce((s, r) => s + Number(r.orderCount), 0),
    Number(report.yearTotalAmount).toFixed(2),
    Number(report.yearSettledAmount).toFixed(2),
    Number(report.yearPendingAmount).toFixed(2)
  ])

  const ws = XLSX.utils.aoa_to_sheet([headers, ...dataRows])
  ws['!cols'] = [{ wch: 8 }, { wch: 8 }, { wch: 14 }, { wch: 14 }, { wch: 14 }]
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, `${report.year}年${label}月报`)
  XLSX.writeFile(wb, `${report.entityName}_${report.year}年${label}月报.xlsx`)
  ElMessage.success('导出成功')
}

const exportPurchase = () => exportToExcel(purchaseReport.value, 'purchase')
const exportSales = () => exportToExcel(salesReport.value, 'sales')

const purchaseSummary = ({ data }) => {
  const total = (key) => data.reduce((s, r) => s + Number(r[key] || 0), 0)
  return ['合计', total('orderCount'), `¥${total('totalAmount').toFixed(2)}`, `¥${total('settledAmount').toFixed(2)}`, `¥${total('pendingAmount').toFixed(2)}`]
}

const salesSummary = ({ data }) => {
  const total = (key) => data.reduce((s, r) => s + Number(r[key] || 0), 0)
  return ['合计', total('orderCount'), `¥${total('totalAmount').toFixed(2)}`, `¥${total('settledAmount').toFixed(2)}`, `¥${total('pendingAmount').toFixed(2)}`]
}

onMounted(async () => {
  await Promise.all([loadSuppliers(), loadCustomers()])
  loadPurchase()
  loadSales()
})
</script>

<style scoped lang="scss">
.monthly-report {
  .toolbar {
    display: flex;
    gap: 10px;
    align-items: center;
    margin-bottom: 20px;
    flex-wrap: wrap;
  }
  .report-wrap {
    .report-title {
      font-size: 15px;
      font-weight: 600;
      color: #303133;
      margin-bottom: 12px;
    }
  }
  .zero { color: #c0c4cc; }
  .unpaid { color: #f56c6c; font-weight: 500; }
}
</style>
