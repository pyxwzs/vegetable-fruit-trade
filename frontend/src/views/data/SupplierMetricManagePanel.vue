<template>
  <div class="manage-panel">
    <el-card shadow="never">
      <div class="page-tip">
        可为每个供应商设定<strong>年指标</strong>或<strong>月指标</strong>；实际采购量自动汇总，超量可用于后续奖励。
      </div>

      <div class="page-actions">
        <el-button type="primary" @click="openForm()">新增订量</el-button>
      </div>

      <div class="search-bar">
        <el-radio-group v-model="filterPeriodType" @change="handleSearch">
          <el-radio-button label="MONTH">月指标</el-radio-button>
          <el-radio-button label="YEAR">年指标</el-radio-button>
        </el-radio-group>
        <el-date-picker
            v-if="filterPeriodType === 'MONTH'"
            v-model="filterMonthPeriod"
            type="month"
            value-format="YYYY-M"
            style="width: 140px"
            @change="handleSearch"
        />
        <el-date-picker
            v-else
            v-model="filterYearPeriod"
            type="year"
            value-format="YYYY"
            style="width: 120px"
            @change="handleSearch"
        />
        <el-select v-model="supplierId" placeholder="供应商" clearable filterable style="width: 160px">
          <el-option v-for="s in supplierOptions" :key="s.id" :label="s.name" :value="s.id" />
        </el-select>
        <el-select v-model="productId" placeholder="商品" clearable filterable style="width: 160px">
          <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
        <el-input v-model="keyword" placeholder="供应商/商品" style="width: 160px" clearable @keyup.enter="handleSearch" />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <div v-if="isMobile" v-loading="loading" class="mobile-card-list">
        <MobileListCard v-for="row in rows" :key="row.id" :title="`${row.supplierName} · ${row.productName}`">
          <template #tag>
            <el-tag size="small">{{ row.periodLabel }}</el-tag>
          </template>
          <div class="card-row"><span class="label">订量</span><span class="value">{{ qtyFmt(row.targetQty, row) }}</span></div>
          <div class="card-row"><span class="label">实际</span><span class="value">{{ qtyFmt(row.actualQty, row) }}</span></div>
          <div class="card-row"><span class="label">差额</span><span class="value" :class="gapClass(row.gapQty)">{{ gapFmt(row.gapQty, row) }}</span></div>
          <template #actions>
            <el-button type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button type="danger" size="small" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </MobileListCard>
        <el-empty v-if="!loading && !rows.length" description="该周期暂无订量指标" />
      </div>
      <div v-else class="table-wrap">
        <el-table :data="rows" v-loading="loading" border>
          <el-table-column prop="supplierName" label="供应商" min-width="110" />
          <el-table-column prop="productName" label="商品" min-width="90" />
          <el-table-column prop="periodLabel" label="周期" width="100" />
          <el-table-column label="订量" width="100">
            <template #default="{ row }">{{ qtyFmt(row.targetQty, row) }}</template>
          </el-table-column>
          <el-table-column label="实际" width="100">
            <template #default="{ row }">{{ qtyFmt(row.actualQty, row) }}</template>
          </el-table-column>
          <el-table-column label="差额" width="100">
            <template #default="{ row }">
              <span :class="gapClass(row.gapQty)">{{ gapFmt(row.gapQty, row) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="完成度" width="120">
            <template #default="{ row }">
              <el-progress :percentage="Math.min(100, row.completionPercent || 0)" :stroke-width="8" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="130" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="openForm(row)">编辑</el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-pagination v-model:current-page="page" v-model:page-size="size"
        :total="total" :page-sizes="[5, 10, 20]"
        layout="total, sizes, prev, pager, next"
        @size-change="loadData" @current-change="loadData"
        class="pagination" />
    </el-card>

    <el-dialog v-model="formVisible" :title="formTitle" :width="isMobile ? '92%' : '520px'" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="指标类型" prop="periodType">
          <el-radio-group v-model="form.periodType">
            <el-radio label="MONTH">月指标</el-radio>
            <el-radio label="YEAR">年指标</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="供应商" prop="supplierId">
          <el-select v-model="form.supplierId" filterable placeholder="选择供应商" style="width: 100%">
            <el-option v-for="s in supplierOptions" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品" prop="productId">
          <el-select v-model="form.productId" filterable placeholder="选择商品" style="width: 100%">
            <el-option v-for="p in productOptions" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.periodType === 'MONTH'" label="所属月份" prop="monthPeriod">
          <el-date-picker v-model="form.monthPeriod" type="month" value-format="YYYY-M" style="width: 100%" />
        </el-form-item>
        <el-form-item v-else label="所属年份" prop="yearPeriod">
          <el-date-picker v-model="form.yearPeriod" type="year" value-format="YYYY" style="width: 100%" />
        </el-form-item>
        <el-form-item label="订量指标" prop="targetQty">
          <el-input-number v-model="form.targetQty" :min="0.1" :precision="1" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getSupplierProductMetrics, createSupplierProductMetric,
  updateSupplierProductMetric, deleteSupplierProductMetric
} from '@/api/supplierProductMetric'
import { getActiveSuppliers } from '@/api/supplier'
import { getAllEnabledProducts } from '@/api/product'
import { useIsMobile } from '@/composables/useIsMobile'
import { usePageRefresh } from '@/composables/usePageRefresh'
import MobileListCard from '@/components/MobileListCard.vue'

const emit = defineEmits(['changed'])

const isMobile = useIsMobile()
const now = new Date()
const defaultMonth = `${now.getFullYear()}-${now.getMonth() + 1}`
const defaultYear = String(now.getFullYear())

const loading = ref(false)
const rows = ref([])
const page = ref(1)
const size = ref(5)
const total = ref(0)
const keyword = ref('')
const supplierId = ref(null)
const productId = ref(null)
const filterPeriodType = ref('MONTH')
const filterMonthPeriod = ref(defaultMonth)
const filterYearPeriod = ref(defaultYear)

const supplierOptions = ref([])
const productOptions = ref([])
const formVisible = ref(false)
const formTitle = ref('新增订量')
const submitting = ref(false)
const formRef = ref(null)

const form = reactive({
  id: null,
  periodType: 'MONTH',
  supplierId: null,
  productId: null,
  monthPeriod: defaultMonth,
  yearPeriod: defaultYear,
  year: now.getFullYear(),
  month: now.getMonth() + 1,
  targetQty: null,
  status: 'ACTIVE'
})

const rules = {
  periodType: [{ required: true, message: '请选择指标类型', trigger: 'change' }],
  supplierId: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  productId: [{ required: true, message: '请选择商品', trigger: 'change' }],
  monthPeriod: [{ required: true, message: '请选择月份', trigger: 'change' }],
  yearPeriod: [{ required: true, message: '请选择年份', trigger: 'change' }],
  targetQty: [{ required: true, message: '请填写订量指标', trigger: 'blur' }]
}

const qtyFmt = (v, row) => {
  const n = Number(v || 0).toFixed(1)
  return row?.productUnit ? `${n} ${row.productUnit}` : n
}

const gapFmt = (v, row) => {
  const text = Math.abs(Number(v || 0)).toFixed(1)
  return row?.productUnit ? `${text} ${row.productUnit}` : text
}

const gapClass = (v) => {
  const n = Number(v || 0)
  if (n > 0) return 'text-excess'
  if (n < 0) return 'text-shortfall'
  return ''
}

const buildListParams = () => {
  const params = {
    page: page.value - 1,
    size: size.value,
    periodType: filterPeriodType.value,
    keyword: keyword.value || undefined,
    supplierId: supplierId.value || undefined,
    productId: productId.value || undefined
  }
  if (filterPeriodType.value === 'MONTH') {
    const [y, m] = (filterMonthPeriod.value || defaultMonth).split('-').map(Number)
    params.year = y
    params.month = m
  } else {
    params.year = Number(filterYearPeriod.value || defaultYear)
  }
  return params
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

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSupplierProductMetrics(buildListParams())
    rows.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } catch {
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => { page.value = 1; loadData() }
const resetSearch = () => {
  keyword.value = ''
  supplierId.value = null
  productId.value = null
  filterPeriodType.value = 'MONTH'
  filterMonthPeriod.value = defaultMonth
  filterYearPeriod.value = defaultYear
  handleSearch()
}

const resetForm = () => {
  Object.assign(form, {
    id: null, periodType: 'MONTH',
    supplierId: null, productId: null,
    monthPeriod: defaultMonth, yearPeriod: defaultYear,
    year: now.getFullYear(), month: now.getMonth() + 1,
    targetQty: null, status: 'ACTIVE'
  })
}

const openForm = (row) => {
  resetForm()
  formTitle.value = row ? '编辑订量' : '新增订量'
  if (row) {
    Object.assign(form, {
      id: row.id,
      periodType: row.periodType || 'MONTH',
      supplierId: row.supplierId,
      productId: row.productId,
      monthPeriod: row.periodType === 'YEAR' ? defaultMonth : `${row.year}-${row.month}`,
      yearPeriod: String(row.year),
      year: row.year,
      month: row.month || 0,
      targetQty: row.targetQty,
      status: row.status || 'ACTIVE'
    })
  }
  formVisible.value = true
}

const buildPayload = () => {
  const payload = {
    id: form.id,
    periodType: form.periodType,
    supplierId: form.supplierId,
    productId: form.productId,
    targetQty: form.targetQty,
    status: form.status
  }
  if (form.periodType === 'MONTH') {
    const [y, m] = (form.monthPeriod || defaultMonth).split('-').map(Number)
    payload.year = y
    payload.month = m
  } else {
    payload.year = Number(form.yearPeriod || defaultYear)
    payload.month = 0
  }
  return payload
}

const submitForm = async () => {
  await formRef.value.validate()
  submitting.value = true
  try {
    const payload = buildPayload()
    if (form.id) {
      await updateSupplierProductMetric(form.id, payload)
    } else {
      await createSupplierProductMetric(payload)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    filterPeriodType.value = payload.periodType
    if (payload.periodType === 'MONTH') {
      filterMonthPeriod.value = `${payload.year}-${payload.month}`
    } else {
      filterYearPeriod.value = String(payload.year)
    }
    loadData()
    emit('changed')
  } catch { /* 拦截器 */ } finally {
    submitting.value = false
  }
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm(
      `确定删除「${row.supplierName} · ${row.productName}」${row.periodLabel}的订量？`,
      '提示',
      { type: 'warning' }
  )
  await deleteSupplierProductMetric(row.id)
  ElMessage.success('已删除')
  loadData()
  emit('changed')
}

usePageRefresh(loadData)

defineExpose({ reload: loadData })

onMounted(loadOptions)
</script>

<style scoped lang="scss">
.manage-panel {
  :deep(.el-card__body) {
    padding: 16px 18px;
  }
}

.page-tip {
  font-size: 13px;
  color: #909399;
  margin-bottom: 14px;
  line-height: 1.55;
  padding: 0 2px;
}
.search-bar {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 14px;
  align-items: center;
}
.page-actions { margin-bottom: 14px; }
.pagination { margin-top: 14px; justify-content: flex-end; }
.table-wrap {
  overflow-x: auto;

  :deep(.el-table__cell) {
    padding: 10px 12px;
  }

  :deep(.cell) {
    padding: 0 4px;
    line-height: 1.45;
  }
}
.text-excess { color: #e6a23c; font-weight: 600; }
.text-shortfall { color: #f56c6c; font-weight: 600; }

@media (max-width: 767px) {
  .manage-panel {
    :deep(.el-card__body) {
      padding: 14px 12px;
    }
  }

  .search-bar {
    :deep(.el-radio-group),
    :deep(.el-date-editor),
    :deep(.el-select),
    :deep(.el-input) {
      width: 100% !important;
    }
  }
}
</style>
