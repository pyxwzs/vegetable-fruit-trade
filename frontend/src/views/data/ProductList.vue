<template>
  <div class="product-list">
    <el-card>
      <div class="page-actions">
        <el-button type="primary" @click="handleAdd">新增商品</el-button>
      </div>

      <div class="search-bar">
        <el-input v-model="searchKeyword" placeholder="商品名称" style="width: 220px"
            clearable @keyup.enter="handleSearch" />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <div v-if="isMobile" v-loading="loading" class="mobile-card-list">
        <MobileListCard v-for="row in products" :key="row.id" :title="row.name">
          <template #tag>
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
          <div class="card-row"><span class="label">分类</span><span class="value">{{ row.category || '—' }}</span></div>
          <div class="card-row"><span class="label">规格/单位</span><span class="value">{{ row.specification || '—' }} / {{ row.unit }}</span></div>
          <template #actions>
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" plain @click="handleDelete(row)">删除</el-button>
          </template>
        </MobileListCard>
        <el-empty v-if="!loading && !products.length" description="暂无数据" />
      </div>
      <div v-else class="table-wrap">
        <el-table :data="products" style="width: 100%" v-loading="loading" border>
          <el-table-column prop="name" label="商品名称" min-width="160" />
          <el-table-column prop="category" label="分类" width="100" />
          <el-table-column prop="unit" label="单位" width="80" />
          <el-table-column prop="specification" label="规格" width="120" />
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
                {{ row.status === 'ENABLED' ? '启用' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
              <el-button type="danger" link @click="handleDelete(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <el-pagination v-model:current-page="page" v-model:page-size="size"
          :total="total" :page-sizes="[5, 10, 20]"
          layout="total, prev, pager, next"
          @size-change="loadData" @current-change="loadData"
          class="pagination" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="dialogTitle"
        :width="isMobile ? '94%' : '460px'" @close="handleDialogClose">
      <el-form ref="formRef" :model="form" :rules="rules"
          :label-width="isMobile ? '70px' : '80px'">
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" allow-create filterable clearable placeholder="选择或输入分类" style="width: 100%">
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="如：斤、箱、个" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="form.specification" placeholder="如：50g/袋（可不填）" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio label="ENABLED">启用</el-radio>
            <el-radio label="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useIsMobile } from '@/composables/useIsMobile'
import MobileListCard from '@/components/MobileListCard.vue'
import { getProducts, getProductCategories, createProduct, updateProduct, deleteProduct } from '@/api/product'

const isMobile = useIsMobile()

const categoryOptions = ref([])

const loading = ref(false)
const products = ref([])
const page = ref(1)
const size = ref(5)
const total = ref(0)
const searchKeyword = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('新增商品')
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, name: '', category: '', unit: '', specification: '', status: 'ENABLED' })

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
}

const loadCategories = async () => {
  try {
    const res = await getProductCategories()
    categoryOptions.value = res.data || []
  } catch {
    categoryOptions.value = []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getProducts({ page: page.value - 1, size: size.value, keyword: searchKeyword.value || undefined })
    products.value = res.data?.content || []
    total.value = res.data?.totalElements || 0
  } catch { ElMessage.error('加载商品失败') } finally { loading.value = false }
}

const handleSearch = () => { page.value = 1; loadData() }
const resetSearch = () => { searchKeyword.value = ''; page.value = 1; loadData() }
const handleDialogClose = () => { formRef.value?.resetFields() }

const handleAdd = () => {
  dialogTitle.value = '新增商品'
  Object.assign(form, { id: null, name: '', category: '', unit: '', specification: '', status: 'ENABLED' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑商品'
  Object.assign(form, {
    id: row.id, name: row.name, category: row.category || '',
    unit: row.unit || '', specification: row.specification || '', status: row.status || 'ENABLED'
  })
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除商品"${row.name}"吗？`, '提示', { type: 'warning' })
    .then(async () => {
      try { await deleteProduct(row.id); ElMessage.success('删除成功'); loadData() } catch { /* 拦截器 */ }
    }).catch(() => {})
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = { name: form.name, category: form.category || undefined, unit: form.unit, specification: form.specification || undefined, status: form.status }
      if (form.id) { await updateProduct(form.id, payload); ElMessage.success('更新成功') }
      else { await createProduct(payload); ElMessage.success('创建成功') }
      dialogVisible.value = false
      loadData()
      loadCategories()
    } catch { /* 拦截器 */ } finally { submitting.value = false }
  })
}

onMounted(() => { loadCategories(); loadData() })
</script>

<style scoped lang="scss">
.product-list {
  .page-actions { display: flex; justify-content: flex-end; margin-bottom: 12px; }
  .search-bar { margin-bottom: 12px; display: flex; gap: 8px; flex-wrap: wrap; align-items: center; }
  .table-wrap { overflow-x: auto; -webkit-overflow-scrolling: touch; }
  .pagination { margin-top: 16px; display: flex; justify-content: center; }
}
</style>
