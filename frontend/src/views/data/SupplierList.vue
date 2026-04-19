<template>
  <div class="supplier-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>供应商管理</span>
          <div>
            <el-button type="primary" @click="openForm()">新增供应商</el-button>
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
        <el-select v-model="status" placeholder="状态" clearable style="width: 140px">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
          <el-option label="黑名单" value="BLACKLISTED" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="suppliers" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="supplierCode" label="编码" width="120" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="contact" label="联系人" width="100" />
        <el-table-column prop="phone" label="电话" width="120" />
        <el-table-column label="信用评级" width="130">
          <template #default="{ row }">
            <el-rate :model-value="Number(row.creditRating) || 0" disabled allow-half />
          </template>
        </el-table-column>
        <el-table-column prop="deliveryOnTimeRate" label="准时率%" width="90" />
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
        <el-form-item label="供应商编码" prop="supplierCode">
          <el-input v-model="form.supplierCode" :disabled="!!form.id" placeholder="唯一编码" />
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
        <el-form-item label="信用评级" prop="creditRating">
          <el-input-number v-model="form.creditRating" :min="0" :max="5" :step="0.1" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="准时交货率%" prop="deliveryOnTimeRate">
          <el-input-number v-model="form.deliveryOnTimeRate" :min="0" :max="100" style="width: 100%" />
        </el-form-item>
        <el-form-item label="质量合格率%" prop="qualityPassRate">
          <el-input-number v-model="form.qualityPassRate" :min="0" :max="100" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
            <el-option label="黑名单" value="BLACKLISTED" />
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
import { getSuppliers, createSupplier, updateSupplier, deleteSupplier } from '@/api/supplier'

const loading = ref(false)
const suppliers = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const status = ref('')

const formVisible = ref(false)
const formTitle = ref('新增供应商')
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  supplierCode: '',
  name: '',
  contact: '',
  phone: '',
  email: '',
  address: '',
  creditRating: 4,
  deliveryOnTimeRate: 90,
  qualityPassRate: 95,
  status: 'ACTIVE',
  remark: ''
})

const rules = {
  supplierCode: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const statusTag = (s) => {
  if (s === 'ACTIVE') return 'success'
  if (s === 'BLACKLISTED') return 'danger'
  return 'info'
}

const statusText = (s) => {
  const m = { ACTIVE: '启用', INACTIVE: '停用', BLACKLISTED: '黑名单' }
  return m[s] || s
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getSuppliers({
      page: page.value - 1,
      size: size.value,
      keyword: searchKeyword.value || undefined,
      status: status.value || undefined
    })
    suppliers.value = res.data.content
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
  status.value = ''
  page.value = 1
  loadData()
}

const resetForm = () => {
  formRef.value?.resetFields?.()
}

const openForm = (row) => {
  formTitle.value = row ? '编辑供应商' : '新增供应商'
  if (row) {
    Object.assign(form, {
      id: row.id,
      supplierCode: row.supplierCode,
      name: row.name,
      contact: row.contact || '',
      phone: row.phone || '',
      email: row.email || '',
      address: row.address || '',
      creditRating: row.creditRating != null ? Number(row.creditRating) : 4,
      deliveryOnTimeRate: row.deliveryOnTimeRate ?? 90,
      qualityPassRate: row.qualityPassRate != null ? Number(row.qualityPassRate) : 95,
      status: row.status || 'ACTIVE',
      remark: row.remark || ''
    })
  } else {
    Object.assign(form, {
      id: null,
      supplierCode: '',
      name: '',
      contact: '',
      phone: '',
      email: '',
      address: '',
      creditRating: 4,
      deliveryOnTimeRate: 90,
      qualityPassRate: 95,
      status: 'ACTIVE',
      remark: ''
    })
  }
  formVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (ok) => {
    if (!ok) return
    submitting.value = true
    try {
      const payload = {
        supplierCode: form.supplierCode,
        name: form.name,
        contact: form.contact || undefined,
        phone: form.phone || undefined,
        email: form.email || undefined,
        address: form.address || undefined,
        creditRating: form.creditRating,
        deliveryOnTimeRate: form.deliveryOnTimeRate,
        qualityPassRate: form.qualityPassRate,
        status: form.status,
        remark: form.remark || undefined
      }
      if (form.id) {
        await updateSupplier(form.id, payload)
        ElMessage.success('已保存')
      } else {
        await createSupplier(payload)
        ElMessage.success('已创建')
      }
      formVisible.value = false
      loadData()
    } catch {
      /* 拦截器 */
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除供应商「${row.name}」？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await deleteSupplier(row.id)
        ElMessage.success('已删除')
        loadData()
      } catch {
        /*  */
      }
    })
    .catch(() => {})
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.supplier-list {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
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
}
</style>
