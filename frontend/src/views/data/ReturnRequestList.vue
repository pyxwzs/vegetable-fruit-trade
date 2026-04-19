<template>
  <div class="return-request-list">
    <div class="page-header">
      <h2>退货申请管理</h2>
    </div>

    <!-- 过滤栏 -->
    <el-card class="filter-card">
      <el-form :model="query" inline>
        <el-form-item label="类型">
          <el-select v-model="query.kind" clearable placeholder="全部" style="width:120px">
            <el-option label="采购退货" value="PURCHASE" />
            <el-option label="销售退货" value="SALES" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width:150px">
            <el-option label="待仓库审批" value="PENDING" />
            <el-option label="待财务审批" value="WH_APPROVED" />
            <el-option label="仓库已拒绝" value="WH_REJECTED" />
            <el-option label="财务已审批" value="FIN_APPROVED" />
            <el-option label="财务已拒绝" value="FIN_REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 快捷 Tab -->
    <el-tabs v-model="activeTab" class="status-tabs" @tab-click="onTabChange">
      <el-tab-pane label="全部" name="" />
      <el-tab-pane v-if="canWarehouseApprove" label="待仓库审批" name="PENDING" />
      <el-tab-pane v-if="canFinanceApprove" label="待财务审批" name="WH_APPROVED" />
      <el-tab-pane label="已完成" name="FIN_APPROVED" />
      <el-tab-pane label="已拒绝" name="WH_REJECTED" />
    </el-tabs>

    <!-- 列表 -->
    <el-card>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column label="ID" prop="id" width="80" />
        <el-table-column label="类型" prop="kind" width="100">
          <template #default="{ row }">
            <el-tag :type="row.kind === 'PURCHASE' ? 'primary' : 'success'" size="small">
              {{ row.kind === 'PURCHASE' ? '采购退货' : '销售退货' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="关联单号" prop="orderNo" min-width="160" />
        <el-table-column label="退货金额" width="120">
          <template #default="{ row }">
            <span class="amount">¥{{ (row.returnAmount || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交人" prop="submitUsername" width="110" />
        <el-table-column label="提交时间" width="160">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openDetail(row)">详情</el-button>
            <template v-if="row.status === 'PENDING' && canWarehouseApprove">
              <el-button size="small" type="success" @click="doWarehouseApprove(row)">仓库通过</el-button>
              <el-button size="small" type="danger" @click="openRejectDialog(row, 'warehouse')">仓库拒绝</el-button>
            </template>
            <template v-if="row.status === 'WH_APPROVED' && canFinanceApprove">
              <el-button size="small" type="primary" @click="doFinanceApprove(row)">财务通过</el-button>
              <el-button size="small" type="danger" @click="openRejectDialog(row, 'finance')">财务拒绝</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="退货申请详情" size="480px">
      <div v-if="detail" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="申请ID">{{ detail.id }}</el-descriptions-item>
          <el-descriptions-item label="类型">
            <el-tag :type="detail.kind === 'PURCHASE' ? 'primary' : 'success'" size="small">
              {{ detail.kind === 'PURCHASE' ? '采购退货' : '销售退货' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="关联单号">{{ detail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detail.status)" size="small">{{ statusLabel(detail.status) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="退货金额" :span="2">
            <span class="amount-lg">¥{{ (detail.returnAmount || 0).toFixed(2) }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="提交人">{{ detail.submitUsername }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ formatDate(detail.createdAt) }}</el-descriptions-item>
        </el-descriptions>

        <div class="section-title">退货明细</div>
        <el-table :data="detail.lines || []" border size="small">
          <el-table-column label="商品名称" prop="productName" />
          <el-table-column label="退货数量" prop="quantity" width="100" />
          <el-table-column label="单价" width="100">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column label="小计" width="100">
            <template #default="{ row }">¥{{ (row.quantity * row.price).toFixed(2) }}</template>
          </el-table-column>
        </el-table>

        <el-descriptions :column="1" border class="mt-12" v-if="detail.whApproveUsername || detail.whRejectReason">
          <el-descriptions-item label="仓库审批人">{{ detail.whApproveUsername || '-' }}</el-descriptions-item>
          <el-descriptions-item label="仓库审批时间">{{ formatDate(detail.whApprovedAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.whRejectReason" label="仓库拒绝理由">{{ detail.whRejectReason }}</el-descriptions-item>
        </el-descriptions>

        <el-descriptions :column="1" border class="mt-12" v-if="detail.finApproveUsername || detail.finRejectReason">
          <el-descriptions-item label="财务审批人">{{ detail.finApproveUsername || '-' }}</el-descriptions-item>
          <el-descriptions-item label="财务审批时间">{{ formatDate(detail.finApprovedAt) }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.finRejectReason" label="财务拒绝理由">{{ detail.finRejectReason }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-drawer>

    <!-- 拒绝理由弹窗 -->
    <el-dialog v-model="rejectVisible" :title="rejectType === 'warehouse' ? '仓库拒绝理由' : '财务拒绝理由'" width="400px">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="3"
        placeholder="请输入拒绝理由（选填）"
      />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="danger" :loading="actionLoading" @click="submitReject">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getReturnRequests,
  warehouseApprove,
  warehouseReject,
  financeApprove,
  financeReject
} from '@/api/returnRequest'

const store = useStore()
const hasPerm = (code) => store.getters['user/hasPermission'](code)

const canWarehouseApprove = computed(
  () => hasPerm('inventory:inbound') || hasPerm('inventory:outbound')
)
const canFinanceApprove = computed(
  () => hasPerm('purchase:pay') || hasPerm('sales:collect')
)

const loading = ref(false)
const actionLoading = ref(false)
const list = ref([])
const total = ref(0)
const activeTab = ref('')

const query = ref({
  kind: '',
  status: '',
  page: 1,
  size: 10
})

const detailVisible = ref(false)
const detail = ref(null)
const rejectVisible = ref(false)
const rejectReason = ref('')
const rejectType = ref('')
const rejectTarget = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      kind: query.value.kind || undefined,
      status: query.value.status || undefined,
      page: query.value.page - 1,
      size: query.value.size,
      sort: 'createdAt,desc'
    }
    const res = await getReturnRequests(params)
    list.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  query.value.kind = ''
  query.value.status = ''
  query.value.page = 1
  activeTab.value = ''
  loadData()
}

const onTabChange = (tab) => {
  query.value.status = tab.paneName
  query.value.page = 1
  loadData()
}

const openDetail = (row) => {
  detail.value = row
  detailVisible.value = true
}

const doWarehouseApprove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认仓库审批通过？将立即执行库存变动（退货金额 ¥${(row.returnAmount || 0).toFixed(2)}）`,
      '仓库审批确认',
      { type: 'warning', confirmButtonText: '确认通过', cancelButtonText: '取消' }
    )
    actionLoading.value = true
    await warehouseApprove(row.id)
    ElMessage.success('仓库审批通过，库存已变动')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    actionLoading.value = false
  }
}

const doFinanceApprove = async (row) => {
  try {
    await ElMessageBox.confirm(
      `确认财务审批通过？将减少订单已付/已收金额 ¥${(row.returnAmount || 0).toFixed(2)}`,
      '财务审批确认',
      { type: 'warning', confirmButtonText: '确认通过', cancelButtonText: '取消' }
    )
    actionLoading.value = true
    await financeApprove(row.id)
    ElMessage.success('财务审批通过，金额已调整')
    loadData()
  } catch (e) {
    if (e !== 'cancel') ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    actionLoading.value = false
  }
}

const openRejectDialog = (row, type) => {
  rejectTarget.value = row
  rejectType.value = type
  rejectReason.value = ''
  rejectVisible.value = true
}

const submitReject = async () => {
  actionLoading.value = true
  try {
    const data = { reason: rejectReason.value }
    if (rejectType.value === 'warehouse') {
      await warehouseReject(rejectTarget.value.id, data)
      ElMessage.success('仓库已拒绝，库存不变动')
    } else {
      await financeReject(rejectTarget.value.id, data)
      ElMessage.success('财务已拒绝，金额不调整')
    }
    rejectVisible.value = false
    loadData()
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    actionLoading.value = false
  }
}

const statusLabel = (status) => {
  const map = {
    PENDING: '待仓库审批',
    WH_APPROVED: '待财务审批',
    WH_REJECTED: '仓库已拒绝',
    FIN_APPROVED: '财务已审批',
    FIN_REJECTED: '财务已拒绝',
    APPROVED: '已通过',
    REJECTED: '已拒绝'
  }
  return map[status] || status
}

const statusType = (status) => {
  const map = {
    PENDING: 'warning',
    WH_APPROVED: 'primary',
    WH_REJECTED: 'danger',
    FIN_APPROVED: 'success',
    FIN_REJECTED: 'danger',
    APPROVED: 'success',
    REJECTED: 'danger'
  }
  return map[status] || 'info'
}

const formatDate = (val) => {
  if (!val) return '-'
  return new Date(val).toLocaleString('zh-CN', { hour12: false })
}

onMounted(loadData)
</script>

<style scoped lang="scss">
.return-request-list {
  .page-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 16px;
    h2 { margin: 0; font-size: 18px; }
  }
  .filter-card { margin-bottom: 12px; }
  .status-tabs { margin-bottom: 4px; }
  .pagination { margin-top: 16px; text-align: right; }
  .amount { font-weight: 600; color: #e6a23c; }
  .amount-lg { font-size: 18px; font-weight: 700; color: #e6a23c; }
  .section-title {
    font-weight: 600;
    font-size: 14px;
    margin: 16px 0 8px;
    padding-left: 8px;
    border-left: 3px solid #409eff;
  }
  .mt-12 { margin-top: 12px; }
  .detail-content { padding: 0 4px; }
}
</style>
