<template>
  <div class="supplier-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>供应商管理</span>
          <el-button type="primary" @click="openForm()">新增供应商</el-button>
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
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="suppliers" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="supplierCode" label="编码" width="120" />
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="contact" label="联系人" width="100" />
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="address" label="地址" min-width="160" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
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

    <el-dialog v-model="formVisible" :title="formTitle" width="500px" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="供应商编码" prop="supplierCode">
          <el-input v-model="form.supplierCode" :disabled="!!form.id" placeholder="唯一编码" />
        </el-form-item>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contact" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" type="textarea" rows="2" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width: 100%">
            <el-option label="启用" value="ACTIVE" />
            <el-option label="停用" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
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
  address: '',
  status: 'ACTIVE',
  remark: ''
})

const rules = {
  supplierCode: [{ required: true, message: '请输入编码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
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

const handleSearch = () => { page.value = 1; loadData() }
const resetSearch = () => { searchKeyword.value = ''; status.value = ''; page.value = 1; loadData() }
const resetForm = () => { formRef.value?.resetFields?.() }

const openForm = (row) => {
  formTitle.value = row ? '编辑供应商' : '新增供应商'
  Object.assign(form, row ? {
    id: row.id, supplierCode: row.supplierCode, name: row.name,
    contact: row.contact || '', phone: row.phone || '',
    address: row.address || '', status: row.status || 'ACTIVE', remark: row.remark || ''
  } : {
    id: null, supplierCode: '', name: '', contact: '', phone: '',
    address: '', status: 'ACTIVE', remark: ''
  })
  formVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (ok) => {
    if (!ok) return
    submitting.value = true
    try {
      const payload = {
        supplierCode: form.supplierCode, name: form.name,
        contact: form.contact || undefined, phone: form.phone || undefined,
        address: form.address || undefined, status: form.status, remark: form.remark || undefined
      }
      if (form.id) { await updateSupplier(form.id, payload); ElMessage.success('已保存') }
      else { await createSupplier(payload); ElMessage.success('已创建') }
      formVisible.value = false
      loadData()
    } catch { /* 拦截器 */ } finally { submitting.value = false }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除供应商「${row.name}」？`, '提示', { type: 'warning' })
    .then(async () => { await deleteSupplier(row.id); ElMessage.success('已删除'); loadData() })
    .catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.supplier-list {
  .card-header { display: flex; justify-content: space-between; align-items: center; }
  .search-bar { margin-bottom: 16px; display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
  .pagination { margin-top: 20px; display: flex; justify-content: flex-end; }
}
</style>
