<template>
  <div class="expense-list">

    <!-- 支出统计仪表盘 -->
    <el-card class="summary-card" shadow="never" v-loading="chartLoading">
      <template #header>
        <div class="chart-header">
          <span class="chart-title">支出统计</span>
          <div class="chart-controls">
            <el-radio-group v-model="dateMode" size="small" @change="loadDashboard">
              <el-radio-button label="day">按日</el-radio-button>
              <el-radio-button label="month">按月</el-radio-button>
            </el-radio-group>
            <el-date-picker
              v-if="dateMode === 'day'"
              v-model="selectedDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="选择日期"
              size="small"
              style="width: 140px"
              @change="loadDashboard"
            />
            <el-date-picker
              v-else
              v-model="selectedMonth"
              type="month"
              value-format="YYYY-MM"
              placeholder="选择月份"
              size="small"
              style="width: 120px"
              @change="loadDashboard"
            />
          </div>
        </div>
        <div class="kpi-row">
          <div class="kpi-item kpi-clickable period" @click="filterPeriod">
            <span class="kpi-label">{{ dateMode === 'day' ? '今日支出' : '本月支出' }}</span>
            <span class="kpi-value orange">¥{{ periodSummary.totalAmount.toFixed(2) }}</span>
            <span class="kpi-sub">点击查看{{ dateMode === 'day' ? '当日' : '当月' }}记录 ›</span>
          </div>
          <div class="kpi-divider"></div>
          <div class="kpi-item">
            <span class="kpi-label">本年支出</span>
            <span class="kpi-value red">¥{{ Number(yearTotal).toFixed(2) }}</span>
            <span class="kpi-sub">{{ periodSummary.count }} 笔（当前范围）</span>
          </div>
        </div>
      </template>

      <div class="charts-row" v-show="periodSummary.items.length">
        <div class="chart-wrap">
          <div class="chart-label">分类占比</div>
          <v-chart :option="pieOption" :init-options="{ renderer: 'canvas' }" autoresize style="height:200px" />
        </div>
        <div class="chart-wrap">
          <div class="chart-label">分类金额</div>
          <v-chart :option="barOption" :init-options="{ renderer: 'canvas' }" autoresize style="height:200px" />
        </div>
      </div>
      <el-empty
        v-show="!chartLoading && !periodSummary.items.length"
        :description="dateMode === 'day' ? '当日暂无支出' : '当月暂无支出'"
        :image-size="50"
      />
    </el-card>

    <!-- 明细列表 -->
    <el-card>
      <div class="page-actions">
        <el-button type="primary" @click="openForm()">新增支出</el-button>
      </div>

      <div class="search-bar">
        <el-input v-model="keyword" placeholder="分类" clearable style="width: 200px"
            @keyup.enter="handleSearch" />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-tag v-if="periodLabel" type="primary" closable @close="clearPeriodFilter" style="margin-bottom:8px">
        日期：{{ periodLabel }}
      </el-tag>

      <div v-if="isMobile" v-loading="loading" class="mobile-card-list">
        <MobileListCard v-for="row in list" :key="row.id" :title="row.category">
          <template #tag>
            <el-tag type="warning" size="small">¥{{ Number(row.amount).toFixed(2) }}</el-tag>
          </template>
          <div class="card-row"><span class="label">日期</span><span class="value">{{ row.expenseDate }}</span></div>
          <template #actions>
            <el-button type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button type="danger" size="small" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </MobileListCard>
        <el-empty v-if="!loading && !list.length" description="暂无支出记录" />
      </div>
      <div v-else class="table-wrap">
        <el-table :data="list" v-loading="loading" border style="width: 100%">
          <el-table-column prop="expenseDate" label="日期" min-width="120" />
          <el-table-column prop="category" label="分类" min-width="140" />
          <el-table-column label="金额" min-width="120" align="right">
            <template #default="{ row }">
              <b class="text-danger">¥{{ Number(row.amount).toFixed(2) }}</b>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
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

    <el-dialog v-model="formVisible" :title="formTitle" :width="isMobile ? '92%' : '440px'" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="日期" prop="expenseDate">
          <el-date-picker v-model="form.expenseDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" allow-create filterable placeholder="选择或输入分类" style="width: 100%">
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额(元)" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" style="width: 100%" />
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
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { use } from 'echarts/core'
import { PieChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import VChart from 'vue-echarts'
import { getExpenses, getExpenseYearTotal, getExpenseCategories, createExpense, updateExpense, deleteExpense } from '@/api/expense'
import { useIsMobile } from '@/composables/useIsMobile'
import MobileListCard from '@/components/MobileListCard.vue'

use([PieChart, BarChart, GridComponent, TooltipComponent, CanvasRenderer])

const isMobile = useIsMobile()
const PIE_COLORS = ['#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#5470c6']

const now = new Date()
const todayStr = now.toISOString().slice(0, 10)
const thisMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`

const dateMode = ref('day')
const selectedDate = ref(todayStr)
const selectedMonth = ref(thisMonth)
const chartLoading = ref(false)
const yearTotal = ref(0)
const periodSummary = ref({ count: 0, totalAmount: 0, items: [] })

const monthRange = (ym) => {
  const [y, m] = ym.split('-').map(Number)
  const mm = String(m).padStart(2, '0')
  const last = new Date(y, m, 0).getDate()
  return { startDate: `${y}-${mm}-01`, endDate: `${y}-${mm}-${String(last).padStart(2, '0')}`, year: y, month: m }
}

const aggregate = (rows) => {
  const map = {}
  rows.forEach(r => {
    const key = r.category || '其他'
    if (!map[key]) map[key] = { category: key, amount: 0, count: 0 }
    map[key].amount += Number(r.amount)
    map[key].count += 1
  })
  const items = Object.values(map).sort((a, b) => b.amount - a.amount)
  const totalAmount = items.reduce((s, r) => s + r.amount, 0)
  return { count: rows.length, totalAmount, items }
}

const loadDashboard = async () => {
  chartLoading.value = true
  try {
    let params
    if (dateMode.value === 'day') {
      const d = new Date(selectedDate.value)
      params = { year: d.getFullYear(), month: d.getMonth() + 1, page: 0, size: 200 }
    } else {
      const { year, month } = monthRange(selectedMonth.value)
      params = { year, month, page: 0, size: 200 }
    }
    const [expRes, yearRes] = await Promise.all([
      getExpenses(params),
      getExpenseYearTotal({ year: now.getFullYear() })
    ])
    let rows = expRes.data?.content || []
    if (dateMode.value === 'day') {
      rows = rows.filter(r => r.expenseDate === selectedDate.value)
    }
    periodSummary.value = aggregate(rows)
    yearTotal.value = Number(yearRes.data || 0)
  } catch {
    periodSummary.value = { count: 0, totalAmount: 0, items: [] }
    yearTotal.value = 0
  } finally {
    chartLoading.value = false
  }
}

const pieOption = computed(() => ({
  tooltip: { trigger: 'item', formatter: p => `${p.name}<br/>¥${Number(p.value).toFixed(2)} (${p.percent}%)` },
  legend: { show: false },
  color: PIE_COLORS,
  series: [{
    type: 'pie',
    radius: ['38%', '68%'],
    center: ['50%', '46%'],
    label: { show: true, fontSize: 11, formatter: p => p.name.length > 4 ? p.name.slice(0, 4) + '…' : p.name },
    data: periodSummary.value.items.map(i => ({ name: i.category, value: i.amount }))
  }]
}))

const barOption = computed(() => {
  const items = [...periodSummary.value.items].reverse()
  return {
    tooltip: { trigger: 'axis', formatter: p => `${p[0].name}<br/>¥${Number(p[0].value).toFixed(2)}` },
    grid: { left: 70, right: 30, top: 10, bottom: 10 },
    xAxis: { type: 'value', axisLabel: { fontSize: 10 } },
    yAxis: {
      type: 'category',
      data: items.map(i => i.category.length > 6 ? i.category.slice(0, 6) + '…' : i.category),
      axisLabel: { fontSize: 11 }
    },
    series: [{
      type: 'bar',
      data: items.map(i => i.amount),
      itemStyle: { color: '#e6a23c', borderRadius: [0, 4, 4, 0] },
      label: { show: true, position: 'right', formatter: p => `¥${Number(p.value).toFixed(0)}`, fontSize: 10 }
    }]
  }
})

const loading = ref(false)
const list = ref([])
const page = ref(1)
const size = ref(5)
const total = ref(0)
const keyword = ref('')
const startDate = ref('')
const endDate = ref('')
const periodLabel = ref('')

const categoryOptions = ref([])

const loadCategories = async () => {
  try {
    const res = await getExpenseCategories()
    categoryOptions.value = res.data || []
  } catch {
    categoryOptions.value = []
  }
}

const formVisible = ref(false)
const formTitle = ref('新增支出')
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, expenseDate: '', category: '', amount: 0 })

const rules = {
  expenseDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  category: [{ required: true, message: '请填写分类', trigger: 'blur' }],
  amount: [{ required: true, type: 'number', min: 0.01, message: '金额须大于0', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getExpenses({
      keyword: keyword.value || undefined,
      startDate: startDate.value || undefined,
      endDate: endDate.value || undefined,
      page: page.value - 1,
      size: size.value
    })
    list.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const refreshAll = () => { loadDashboard(); loadData() }

const handleSearch = () => { page.value = 1; loadData() }
const resetSearch = () => {
  keyword.value = ''; startDate.value = ''; endDate.value = ''; periodLabel.value = ''
  page.value = 1; loadData()
}
const clearPeriodFilter = () => {
  startDate.value = ''; endDate.value = ''; periodLabel.value = ''
  page.value = 1; loadData()
}
const filterPeriod = () => {
  if (dateMode.value === 'day') {
    startDate.value = selectedDate.value
    endDate.value = selectedDate.value
    periodLabel.value = selectedDate.value
  } else {
    const { startDate: from, endDate: to } = monthRange(selectedMonth.value)
    startDate.value = from
    endDate.value = to
    periodLabel.value = `${selectedMonth.value}（本月）`
  }
  page.value = 1
  loadData()
}

const openForm = (row) => {
  formTitle.value = row ? '编辑支出' : '新增支出'
  Object.assign(form, row ? {
    id: row.id, expenseDate: row.expenseDate, category: row.category, amount: Number(row.amount)
  } : {
    id: null, expenseDate: todayStr, category: '', amount: 0
  })
  formVisible.value = true
}

const submitForm = async () => {
  await formRef.value?.validate(async (ok) => {
    if (!ok) return
    submitting.value = true
    try {
      const payload = { expenseDate: form.expenseDate, category: form.category, amount: form.amount }
      if (form.id) { await updateExpense(form.id, payload); ElMessage.success('已保存') }
      else { await createExpense(payload); ElMessage.success('已添加') }
      formVisible.value = false
      loadCategories()
      refreshAll()
    } catch { /* 拦截器 */ } finally { submitting.value = false }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除「${row.category} ¥${Number(row.amount).toFixed(2)}」？`, '提示', { type: 'warning' })
    .then(async () => { await deleteExpense(row.id); ElMessage.success('已删除'); refreshAll() })
    .catch(() => {})
}

onMounted(() => { loadCategories(); loadDashboard(); loadData() })
</script>

<style scoped lang="scss">
.text-danger { color: #f56c6c; }

.summary-card { margin-bottom: 12px; }

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}
.chart-title { font-size: 15px; font-weight: 600; color: #303133; }
.chart-controls { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }

.kpi-row {
  display: flex;
  align-items: stretch;
  margin-top: 12px;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}
.kpi-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
  padding: 4px 8px;
  border-radius: 6px;
}
.kpi-divider { width: 1px; background: #f0f0f0; margin: 0 4px; }
.kpi-label { font-size: 12px; color: #909399; }
.kpi-value { font-size: 18px; font-weight: 700; }
.kpi-value.orange { color: #e6a23c; }
.kpi-value.red { color: #f56c6c; }
.kpi-sub { font-size: 11px; color: #c0c4cc; }
.kpi-clickable { cursor: pointer; transition: background 0.2s; }
.kpi-clickable.period:hover { background: #fdf6ec; }

.charts-row { display: flex; gap: 8px; }
.chart-wrap {
  flex: 1;
  min-width: 0;
  .chart-label { font-size: 12px; color: #909399; text-align: center; margin-bottom: 4px; }
}

.expense-list {
  .page-actions { display: flex; justify-content: flex-end; margin-bottom: 12px; }
  .search-bar { margin-bottom: 12px; display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
  .table-wrap {
    width: 100%;
    overflow-x: auto;
    -webkit-overflow-scrolling: touch;
    :deep(.el-table) { width: 100% !important; }
  }
  .pagination { margin-top: 16px; display: flex; justify-content: center; }
}

@media (max-width: 600px) {
  .charts-row { flex-direction: column; }
  .kpi-value { font-size: 16px; }
}
</style>
