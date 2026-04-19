<template>
  <div class="file-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>文件管理</span>
          <div class="header-actions">
            <el-select v-model="uploadBizType" placeholder="上传类型" style="width: 140px; margin-right: 8px">
              <el-option label="商品图片" value="product" />
              <el-option label="合同" value="contract" />
              <el-option label="资质文件" value="certificate" />
              <el-option label="其他" value="other" />
            </el-select>
            <el-upload
                class="upload-btn"
                :show-file-list="false"
                :http-request="handleCustomUpload"
                :before-upload="handleBeforeUpload"
            >
              <el-button type="primary" :loading="uploading">上传文件</el-button>
            </el-upload>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-input
            v-model="searchKeyword"
            placeholder="文件名"
            style="width: 260px"
            clearable
            @keyup.enter="handleSearch"
        />
        <el-select v-model="businessType" placeholder="业务类型" clearable style="width: 150px">
          <el-option label="商品图片" value="product" />
          <el-option label="合同" value="contract" />
          <el-option label="资质文件" value="certificate" />
          <el-option label="其他" value="other" />
        </el-select>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="files" style="width: 100%" v-loading="loading" border>
        <el-table-column label="预览" width="80" align="center">
          <template #default="{ row }">
            <el-image
                v-if="isImage(row)"
                :src="row.url"
                :preview-src-list="[row.url]"
                fit="cover"
                style="width: 40px; height: 40px; border-radius: 4px;"
            />
            <el-icon v-else :size="24">
              <Document />
            </el-icon>
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" min-width="250" show-overflow-tooltip />
        <el-table-column prop="fileSizeDisplay" label="大小" width="120" />
        <el-table-column prop="fileType" label="类型" width="100" />
        <el-table-column label="业务类型" width="120">
          <template #default="{ row }">{{ bizLabel(row.businessType) }}</template>
        </el-table-column>
        <el-table-column prop="uploaderName" label="上传人" width="120" />
        <el-table-column prop="createTime" label="上传时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="80" align="center" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handlePreview(row)">预览</el-button>
            <el-button type="success" link @click="handleDownload(row)">下载</el-button>
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
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { formatDateTime } from '@/utils/date'
import { getFiles, uploadFile, deleteFile, downloadFile, getAccessUrl } from '@/api/file'

const loading = ref(false)
const uploading = ref(false)
const files = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const businessType = ref('')
const uploadBizType = ref('product')

const bizLabel = (t) =>
  ({ product: '商品图片', contract: '合同', certificate: '资质文件', other: '其他' }[t] || t || '—')

const loadData = async () => {
  loading.value = true
  try {
    const res = await getFiles({
      page: page.value - 1,
      size: size.value,
      keyword: searchKeyword.value || undefined,
      businessType: businessType.value || undefined
    })
    const p = res.data
    files.value = p.content || []
    total.value = p.totalElements ?? 0
  } catch {
    ElMessage.error('加载文件列表失败')
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
  businessType.value = ''
  page.value = 1
  loadData()
}

const handleBeforeUpload = (file) => {
  const isLt100M = file.size / 1024 / 1024 < 100
  if (!isLt100M) {
    ElMessage.error('文件大小不能超过 100MB')
    return false
  }
  return true
}

const handleCustomUpload = async (options) => {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    fd.append('businessType', uploadBizType.value || 'other')
    await uploadFile(fd)
    ElMessage.success('上传成功')
    loadData()
    options.onSuccess()
  } catch (e) {
    options.onError(e)
  } finally {
    uploading.value = false
  }
}

const handlePreview = async (row) => {
  try {
    if (row.url) {
      window.open(row.url, '_blank')
      return
    }
    const res = await getAccessUrl(row.fileId)
    const u = res.data
    if (u) window.open(u, '_blank')
    else ElMessage.info('无法获取访问地址')
  } catch {
    ElMessage.error('预览失败')
  }
}

const handleDownload = async (row) => {
  try {
    const blobRes = await downloadFile(row.fileId)
    const blob = blobRes instanceof Blob ? blobRes : new Blob([blobRes])
    const url = window.URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = row.fileName || 'download'
    a.click()
    window.URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载失败')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除「${row.fileName}」？`, '提示', { type: 'warning' })
    .then(async () => {
      await deleteFile(row.fileId)
      ElMessage.success('已删除')
      loadData()
    })
    .catch(() => {})
}

const isImage = (file) => {
  const imageTypes = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp']
  return imageTypes.includes(String(file.fileType || '').toLowerCase())
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.file-list {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }
  .header-actions {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
  }
  .search-bar {
    margin-bottom: 20px;
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
  .upload-btn {
    display: inline-block;
  }
}
</style>
