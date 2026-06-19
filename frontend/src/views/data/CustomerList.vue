<template>
  <div class="customer-list">
    <el-card>
      <div class="page-actions">
        <el-button type="primary" @click="openForm()">新增客户</el-button>
      </div>

      <div class="search-bar">
        <el-input v-model="searchKeyword" placeholder="名称/联系人" style="width: 240px"
            clearable @keyup.enter="handleSearch" />
        <el-select v-model="statusFilter" placeholder="状态" clearable style="width: 120px">
          <el-option label="启用" value="ACTIVE" />
          <el-option label="停用" value="INACTIVE" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <div v-if="isMobile" v-loading="loading" class="mobile-card-list">
        <MobileListCard v-for="row in customers" :key="row.id" :title="row.name">
          <template #tag>
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '启用' : '停用' }}
            </el-tag>
          </template>
          <div class="card-row"><span class="label">联系人</span><span class="value">{{ row.contact || '—' }}</span></div>
          <div class="card-row"><span class="label">电话</span><span class="value">{{ row.phone || '—' }}</span></div>
          <div v-if="row.address" class="card-row"><span class="label">地址</span><span class="value">{{ row.address }}</span></div>
          <template #actions>
            <el-button type="primary" size="small" @click="openForm(row)">编辑</el-button>
            <el-button type="danger" size="small" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </MobileListCard>
        <el-empty v-if="!loading && !customers.length" description="暂无数据" />
      </div>
      <div v-else class="table-wrap">
        <el-table :data="customers" style="width: 100%" v-loading="loading" border>
          <el-table-column prop="name" label="客户名称" min-width="160" />
          <el-table-column prop="contact" label="联系人" width="100" />
          <el-table-column prop="phone" label="联系电话" width="130" />
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
      </div>

      <el-pagination v-model:current-page="page" v-model:page-size="size"
          :total="total" :page-sizes="[5, 10, 20]"
          layout="total, sizes, prev, pager, next"
          @size-change="loadData" @current-change="loadData"
          class="pagination" />
    </el-card>

    <el-dialog v-model="formVisible" :title="formTitle" :width="isMobile ? '92%' : '480px'" destroy-on-close @close="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
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
import { useIsMobile } from '@/composables/useIsMobile'
import MobileListCard from '@/components/MobileListCard.vue'

const isMobile = useIsMobile()

const loading = ref(false)
const customers = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const statusFilter = ref('')

const formVisible = ref(false)
const formTitle = ref('新增客户')
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, name: '', contact: '', phone: '', address: '', status: 'ACTIVE', })

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getCustomers({ page: page.value - 1, size: size.value, keyword: searchKeyword.value || undefined, status: statusFilter.value || undefined })
    customers.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } catch { ElMessage.error('加载失败') } finally { loading.value = false }
}

const handleSearch = () => { page.value = 1; loadData() }
const resetSearch = () => { searchKeyword.value = ''; statusFilter.value = ''; page.value = 1; loadData() }
const resetForm = () => { formRef.value?.resetFields?.() }

const openForm = (row) => {
  formTitle.value = row ? '编辑客户' : '新增客户'
  Object.assign(form, row
    ? { id: row.id, name: row.name, contact: row.contact || '', phone: row.phone || '', address: row.address || '', status: row.status || 'ACTIVE' }
    : { id: null, name: '', contact: '', phone: '', address: '', status: 'ACTIVE', })
  formVisible.value = true
}

const submitForm = async () => {
  await formRef.value?.validate(async (ok) => {
    if (!ok) return
    submitting.value = true
    try {
      const payload = { name: form.name, contact: form.contact || undefined, phone: form.phone || undefined, address: form.address || undefined, status: form.status }
      if (form.id) { await updateCustomer(form.id, payload) }
      else { await createCustomer(payload) }
      ElMessage.success('保存成功')
      formVisible.value = false
      loadData()
    } catch { /* */ } finally { submitting.value = false }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除客户「${row.name}」？`, '提示', { type: 'warning' })
    .then(async () => { await deleteCustomer(row.id); ElMessage.success('已删除'); loadData() })
    .catch(() => {})
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.customer-list {
  .page-actions { display: flex; justify-content: flex-end; margin-bottom: 12px; }
  .search-bar { margin-bottom: 12px; display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
  .table-wrap { overflow-x: auto; -webkit-overflow-scrolling: touch; }
  .pagination { margin-top: 16px; display: flex; justify-content: center; }
}
</style>
