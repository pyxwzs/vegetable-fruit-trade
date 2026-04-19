<template>
  <div class="customer-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>客户管理</span>
          <div>
            <el-button type="primary" @click="openForm()">新增客户</el-button>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-input
            v-model="searchKeyword"
            placeholder="名称/编码/联系人"
            style="width: 260px"
            clearable
            @keyup.enter="handleSearch"
        />
        <el-select v-model="creditLevelFilter" placeholder="信用等级" clearable style="width: 120px">
          <el-option label="A级" value="A" />
          <el-option label="B级" value="B" />
          <el-option label="C级" value="C" />
          <el-option label="D级" value="D" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="customers" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="customerCode" label="客户编码" width="120" />
        <el-table-column prop="name" label="客户名称" min-width="160" />
        <el-table-column prop="contact" label="联系人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="120" />
        <el-table-column prop="type" label="客户类型" width="100">
          <template #default="{ row }">{{ typeText(row.type) }}</template>
        </el-table-column>
        <el-table-column prop="creditLevel" label="信用等级" width="90">
          <template #default="{ row }">
            <el-tag :type="creditTag(row.creditLevel)">{{ row.creditLevel }}级</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="creditLimit" label="信用额度" width="120">
          <template #default="{ row }">¥{{ Number(row.creditLimit || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="totalPurchaseAmount" label="累计采购额" width="120">
          <template #default="{ row }">¥{{ Number(row.totalPurchaseAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="openForm(row)">编辑</el-button>
            <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
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

    <el-dialog v-model="formVisible" :title="formTitle" width="560px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="客户编码" prop="customerCode">
          <el-input v-model="form.customerCode" :disabled="!!form.id" placeholder="唯一编码" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="联系人" prop="contact">
          <el-input v-model="form.contact" />
        </el-form-item>
        <el-form-item label="电话" prop="phone">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="地址" prop="address">
          <el-input v-model="form.address" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="税号" prop="taxNumber">
          <el-input v-model="form.taxNumber" />
        </el-form-item>
        <el-form-item label="客户类型" prop="type">
          <el-select v-model="form.type" style="width: 100%">
            <el-option label="批发" value="WHOLESALE" />
            <el-option label="零售" value="RETAIL" />
            <el-option label="连锁" value="CHAIN" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="信用等级" prop="creditLevel">
          <el-select v-model="form.creditLevel" style="width: 100%">
            <el-option label="A" value="A" />
            <el-option label="B" value="B" />
            <el-option label="C" value="C" />
            <el-option label="D" value="D" />
          </el-select>
        </el-form-item>
        <el-form-item label="授信额度" prop="creditLimit">
          <el-input-number v-model="form.creditLimit" :min="0" :precision="2" style="width: 100%" />
          <span class="hint-inline">0 表示不启用额度校验（先款后货等）</span>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
            <el-option label="冻结" value="FROZEN" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" rows="2" />
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
import { getCustomers, createCustomer, updateCustomer, deleteCustomer } from '@/api/customer'

const loading = ref(false)
const customers = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const creditLevelFilter = ref('')

const formVisible = ref(false)
const formTitle = ref('新增客户')
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  customerCode: '',
  name: '',
  contact: '',
  phone: '',
  email: '',
  address: '',
  taxNumber: '',
  type: 'RETAIL',
  creditLevel: 'B',
  creditLimit: 0,
  status: 'ACTIVE',
  remark: ''
})

const rules = {
  customerCode: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const typeText = (t) =>
  ({ WHOLESALE: '批发', RETAIL: '零售', CHAIN: '连锁', OTHER: '其他' }[t] || t)

const creditTag = (lv) =>
  ({ A: 'success', B: 'primary', C: 'warning', D: 'danger' }[lv] || 'info')

const statusTag = (s) => {
  if (s === 'ACTIVE') return 'success'
  if (s === 'FROZEN') return 'danger'
  return 'info'
}

const statusText = (s) =>
  ({ ACTIVE: '启用', INACTIVE: '停用', FROZEN: '冻结' }[s] || s)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCustomers({
      page: page.value - 1,
      size: size.value,
      keyword: searchKeyword.value || undefined,
      creditLevel: creditLevelFilter.value || undefined
    })
    customers.value = res.data.content
    total.value = res.data.totalElements
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadData()
}

const resetSearch = () => {
  searchKeyword.value = ''
  creditLevelFilter.value = ''
  page.value = 1
  loadData()
}

const resetForm = () => {
  formRef.value?.resetFields?.()
}

const openForm = (row) => {
  formTitle.value = row ? '编辑客户' : '新增客户'
  if (row) {
    Object.assign(form, {
      id: row.id,
      customerCode: row.customerCode,
      name: row.name,
      contact: row.contact || '',
      phone: row.phone || '',
      email: row.email || '',
      address: row.address || '',
      taxNumber: row.taxNumber || '',
      type: row.type || 'RETAIL',
      creditLevel: row.creditLevel || 'B',
      creditLimit: row.creditLimit != null ? Number(row.creditLimit) : 0,
      status: row.status || 'ACTIVE',
      remark: row.remark || ''
    })
  } else {
    Object.assign(form, {
      id: null,
      customerCode: '',
      name: '',
      contact: '',
      phone: '',
      email: '',
      address: '',
      taxNumber: '',
      type: 'RETAIL',
      creditLevel: 'B',
      creditLimit: 0,
      status: 'ACTIVE',
      remark: ''
    })
  }
  formVisible.value = true
}

const submitForm = async () => {
  await formRef.value?.validate?.().catch(() => Promise.reject())
  submitting.value = true
  try {
    const payload = {
      customerCode: form.customerCode,
      name: form.name,
      contact: form.contact || undefined,
      phone: form.phone || undefined,
      email: form.email || undefined,
      address: form.address || undefined,
      taxNumber: form.taxNumber || undefined,
      type: form.type,
      creditLevel: form.creditLevel,
      creditLimit: form.creditLimit,
      status: form.status,
      remark: form.remark || undefined
    }
    if (form.id) {
      await updateCustomer(form.id, payload)
    } else {
      await createCustomer(payload)
    }
    ElMessage.success('保存成功')
    formVisible.value = false
    loadData()
  } catch {
    /*  */
  } finally {
    submitting.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除客户「${row.name}」？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteCustomer(row.id)
      ElMessage.success('已删除')
      loadData()
    })
    .catch(() => {})
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.customer-list {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
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
  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
  .hint-inline {
    margin-left: 8px;
    font-size: 12px;
    color: var(--el-text-color-secondary);
  }
}
</style>
