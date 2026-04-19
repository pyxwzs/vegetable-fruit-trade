<template>
  <div class="log-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>日志审计</span>
          <div>
            <el-button @click="handleExport">导出</el-button>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-input
            v-model="searchForm.username"
            placeholder="用户名"
            style="width: 150px"
            clearable
        />
        <el-input
            v-model="searchForm.module"
            placeholder="模块"
            style="width: 150px"
            clearable
        />
        <el-select v-model="searchForm.success" placeholder="状态" clearable style="width: 120px">
          <el-option label="成功" :value="true" />
          <el-option label="失败" :value="false" />
        </el-select>
        <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 300px"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="logs" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="ip" label="IP地址" width="150" />
        <el-table-column prop="module" label="模块" width="120" />
        <el-table-column prop="action" label="操作" width="120" />
        <el-table-column prop="executionTime" label="耗时(ms)" width="100" align="center">
          <template #default="{ row }">
            <span :class="{'slow-query': row.executionTime > 2000}">
              {{ row.executionTime }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="success" label="状态" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.success ? 'success' : 'danger'" size="small">
              {{ row.success ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestUrl" label="请求URL" min-width="250" show-overflow-tooltip />
        <el-table-column prop="method" label="请求方法" width="100" />
        <el-table-column prop="createTime" label="操作时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          :page-sizes="[10, 20, 30, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
          class="pagination"
      />
    </el-card>

    <!-- 日志详情对话框 -->
    <el-dialog v-model="detailVisible" title="日志详情" width="800px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ currentLog.username }}</el-descriptions-item>
        <el-descriptions-item label="IP地址">{{ currentLog.ip }}</el-descriptions-item>
        <el-descriptions-item label="模块">{{ currentLog.module }}</el-descriptions-item>
        <el-descriptions-item label="操作">{{ currentLog.action }}</el-descriptions-item>
        <el-descriptions-item label="请求URL" :span="2">{{ currentLog.requestUrl }}</el-descriptions-item>
        <el-descriptions-item label="请求方法">{{ currentLog.method }}</el-descriptions-item>
        <el-descriptions-item label="耗时">{{ currentLog.executionTime }}ms</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="currentLog.success ? 'success' : 'danger'">
            {{ currentLog.success ? '成功' : '失败' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="User-Agent" :span="2">{{ currentLog.userAgent }}</el-descriptions-item>
        <el-descriptions-item label="请求参数" :span="2">
          <pre class="log-pre">{{ formatJson(currentLog.parameters) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="返回结果" :span="2">
          <pre class="log-pre">{{ formatJson(currentLog.result) }}</pre>
        </el-descriptions-item>
        <el-descriptions-item v-if="!currentLog.success" label="错误信息" :span="2">
          <pre class="log-pre error">{{ currentLog.errorMessage }}</pre>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { formatDateTime, formatDate } from '@/utils/date'
import { getOperationLogs } from '@/api/operationLog'

const loading = ref(false)
const logs = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const detailVisible = ref(false)
const currentLog = ref({})

const searchForm = reactive({
  username: '',
  module: '',
  success: null,
  dateRange: []
})

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value - 1,
      size: size.value,
      sort: 'createTime,desc'
    }
    if (searchForm.username) params.username = searchForm.username.trim()
    if (searchForm.module) params.module = searchForm.module.trim()
    if (searchForm.success !== null && searchForm.success !== undefined && searchForm.success !== '') {
      params.success = searchForm.success
    }
    if (searchForm.dateRange && searchForm.dateRange.length === 2) {
      params.startDate = formatDate(searchForm.dateRange[0])
      params.endDate = formatDate(searchForm.dateRange[1])
    }
    const res = await getOperationLogs(params)
    const pageData = res.data
    logs.value = pageData.content || []
    total.value = pageData.totalElements ?? 0
  } catch (error) {
    console.error('加载日志失败:', error)
    ElMessage.error(error.response?.data?.message || '加载日志失败')
    logs.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const resetSearch = () => {
  searchForm.username = ''
  searchForm.module = ''
  searchForm.success = null
  searchForm.dateRange = []
  page.value = 1
  loadData()
}

const viewDetail = (row) => {
  currentLog.value = row
  detailVisible.value = true
}

const formatJson = (str) => {
  if (!str) return ''
  try {
    return JSON.stringify(JSON.parse(str), null, 2)
  } catch {
    return str
  }
}

const handleExport = () => {
  ElMessage.info('导出功能开发中')
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.log-list {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .search-bar {
    margin-bottom: 20px;
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .slow-query {
    color: #f56c6c;
    font-weight: bold;
  }

  .log-pre {
    margin: 0;
    padding: 10px;
    background-color: #f5f7fa;
    border-radius: 4px;
    max-height: 300px;
    overflow: auto;
    font-family: 'Courier New', monospace;
    font-size: 12px;

    &.error {
      color: #f56c6c;
    }
  }
}
</style>