<template>
  <div class="product-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>商品管理</span>
          <div class="header-actions">
            <el-button type="primary" @click="handleAdd">新增商品</el-button>
            <el-button @click="downloadTemplate">下载导入模板</el-button>
            <el-upload
                :show-file-list="false"
                accept=".xlsx,.xls"
                :http-request="handleImportRequest"
            >
              <el-button type="success">批量导入</el-button>
            </el-upload>
            <el-button @click="handleExport">导出</el-button>
          </div>
        </div>
      </template>

      <div class="search-bar">
        <el-input
            v-model="searchKeyword"
            placeholder="商品名称/编码/条码"
            style="width: 220px"
            clearable
            @keyup.enter="handleSearch"
        />
        <div class="scan-row">
          <el-input
              v-model="scanCode"
              placeholder="扫码枪/手输条码或编码后回车"
              style="width: 260px"
              clearable
              @keyup.enter="handleScanLookup"
          />
          <el-button :loading="cameraStarting" @click="openCameraScanDialog">摄像头扫码</el-button>
        </div>
        <el-button type="primary" @click="handleSearch">搜索</el-button>
        <el-button @click="resetSearch">重置</el-button>
      </div>

      <el-table :data="products" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="productCode" label="商品编码" width="130" />
        <el-table-column prop="barcode" label="条码" width="140" show-overflow-tooltip />
        <el-table-column prop="name" label="商品名称" min-width="160" />
        <el-table-column prop="category.name" label="分类" width="100" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column prop="specification" label="规格" width="100" />
        <el-table-column prop="purchasePrice" label="采购价" width="100">
          <template #default="{ row }">
            ¥{{ row.purchasePrice?.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="salePrice" label="销售价" width="100">
          <template #default="{ row }">
            ¥{{ row.salePrice?.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleEdit(row)">编辑</el-button>
            <el-button type="info" link @click="openPriceHistory(row)">价格历史</el-button>
            <el-button
                v-if="row.barcode"
                type="success"
                link
                @click="exportBarcodePng(row.barcode, row.productCode || row.name)"
            >
              条码图
            </el-button>
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

    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="620px"
        @close="handleDialogClose"
    >
      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
      >
        <el-form-item label="商品编码" prop="productCode">
          <el-input v-model="form.productCode" placeholder="请输入商品编码" :disabled="!!form.id" />
        </el-form-item>

        <el-form-item v-if="form.id" label="条码">
          <el-input :model-value="form.barcode" readonly placeholder="无" />
          <div v-show="form.barcode" class="barcode-preview-wrap">
            <svg ref="barcodeSvgRef" class="barcode-svg" />
            <div class="barcode-export-actions">
              <el-button size="small" @click="exportBarcodePng(form.barcode, form.productCode || form.name)">
                导出 PNG
              </el-button>
              <el-button size="small" @click="exportBarcodeSvg(form.barcode, form.productCode || form.name)">
                导出 SVG
              </el-button>
            </div>
            <span class="barcode-tip">预览；USB 扫码枪对准下方「扫码或输入条码」框扫入可快速打开本商品</span>
          </div>
        </el-form-item>
        <el-form-item v-else label="条码">
          <span class="barcode-hint">保存后由系统自动生成（无需填写）</span>
        </el-form-item>

        <el-form-item label="商品名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>

        <el-form-item label="商品分类" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择分类" clearable style="width: 100%">
            <el-option
                v-for="c in categories"
                :key="c.id"
                :label="c.name"
                :value="c.id"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="单位" prop="unit">
          <el-input v-model="form.unit" placeholder="请输入单位" />
        </el-form-item>

        <el-form-item label="规格" prop="specification">
          <el-input v-model="form.specification" placeholder="请输入规格" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="采购价" prop="purchasePrice">
              <el-input v-model="form.purchasePrice" placeholder="采购价" type="number">
                <template #prefix>¥</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="销售价" prop="salePrice">
              <el-input v-model="form.salePrice" placeholder="销售价" type="number">
                <template #prefix>¥</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="保质期" prop="shelfLife">
          <el-input v-model="form.shelfLife" placeholder="保质期（天）" type="number">
            <template #append>天</template>
          </el-input>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="ENABLED">启用</el-radio>
            <el-radio label="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="商品图片" prop="imageUrl">
          <el-upload
              class="avatar-uploader"
              :show-file-list="false"
              :http-request="uploadProductImage"
              :before-upload="beforeAvatarUpload"
          >
            <img v-if="form.imageUrl" :src="form.imageUrl" class="avatar" alt="" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input v-model="form.description" type="textarea" rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitting">确定</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog v-model="historyVisible" title="价格变动历史" width="720px" destroy-on-close>
      <el-table :data="priceHistoryRows" v-loading="historyLoading" border max-height="400">
        <el-table-column prop="createdAt" label="时间" width="170">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="变更前进价" width="110">
          <template #default="{ row }">¥{{ row.prevPurchasePrice != null ? Number(row.prevPurchasePrice).toFixed(2) : '-' }}</template>
        </el-table-column>
        <el-table-column label="变更前售价" width="110">
          <template #default="{ row }">¥{{ row.prevSalePrice != null ? Number(row.prevSalePrice).toFixed(2) : '-' }}</template>
        </el-table-column>
        <el-table-column label="变更后进价" width="110">
          <template #default="{ row }">¥{{ row.newPurchasePrice != null ? Number(row.newPurchasePrice).toFixed(2) : '-' }}</template>
        </el-table-column>
        <el-table-column label="变更后售价" width="110">
          <template #default="{ row }">¥{{ row.newSalePrice != null ? Number(row.newSalePrice).toFixed(2) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="source" label="来源" width="90">
          <template #default="{ row }">
            {{ row.source === 'IMPORT' ? '导入' : '手工' }}
          </template>
        </el-table-column>
        <el-table-column prop="operatorUsername" label="操作人" width="100" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <el-dialog
        v-model="cameraScanVisible"
        title="摄像头扫码"
        width="420px"
        append-to-body
        align-center
        @opened="onCameraScanDialogOpened"
        @closed="onCameraScanDialogClosed"
    >
      <p class="camera-scan-hint">
        将一维条码置于画面中央，保持稳定与光线充足；识别成功后将自动按条码/编码查找并打开商品（需授权摄像头，建议使用 HTTPS 或 localhost）。
      </p>
      <div id="product-scan-camera-region" class="camera-scan-region" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import JsBarcode from 'jsbarcode'
import {
  getProducts,
  createProduct,
  updateProduct,
  deleteProduct,
  lookupProduct,
  downloadProductImportTemplate,
  importProducts,
  getProductPriceHistory
} from '@/api/product'
import { listCategories } from '@/api/category'
import { uploadFile } from '@/api/file'
import { formatDateTime } from '@/utils/date'
import { Html5Qrcode, Html5QrcodeSupportedFormats } from 'html5-qrcode'

const CAMERA_SCAN_ELEMENT_ID = 'product-scan-camera-region'

const loading = ref(false)
const products = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const searchKeyword = ref('')
const scanCode = ref('')
const categories = ref([])

const cameraScanVisible = ref(false)
const cameraStarting = ref(false)
let html5QrCodeInstance = null
let cameraDecodeHandled = false

const dialogVisible = ref(false)
const dialogTitle = ref('新增商品')
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  productCode: '',
  barcode: '',
  name: '',
  categoryId: null,
  unit: '',
  specification: '',
  purchasePrice: null,
  salePrice: null,
  shelfLife: null,
  imageUrl: '',
  description: '',
  status: 'ENABLED'
})

const rules = {
  productCode: [
    { required: true, message: '请输入商品编码', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  name: [
    { required: true, message: '请输入商品名称', trigger: 'blur' },
    { min: 2, max: 100, message: '长度在 2 到 100 个字符', trigger: 'blur' }
  ],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }]
}

const historyVisible = ref(false)
const historyLoading = ref(false)
const priceHistoryRows = ref([])

const barcodeSvgRef = ref(null)

function sanitizeFilenamePart(s) {
  if (!s || typeof s !== 'string') return 'barcode'
  return s.trim().replace(/[/\\?%*:|"<>]/g, '_').slice(0, 80) || 'barcode'
}

function downloadBlob(blob, filename) {
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

/** 与预览一致的条码绘制参数（canvas / svg 通用） */
function getBarcodeJsOptions(value) {
  const base = {
    width: 2,
    height: 56,
    displayValue: true,
    margin: 10,
    background: '#ffffff'
  }
  if (/^\d{13}$/.test(value)) {
    return { ...base, format: 'EAN13' }
  }
  return { ...base, format: 'CODE128' }
}

/** 导出条码为 PNG（离屏 canvas，不依赖预览） */
function exportBarcodePng(value, filenamePrefix) {
  const v = value?.trim()
  if (!v) {
    ElMessage.warning('无条码可导出')
    return
  }
  const canvas = document.createElement('canvas')
  try {
    JsBarcode(canvas, v, getBarcodeJsOptions(v))
    canvas.toBlob(
      (blob) => {
        if (!blob) {
          ElMessage.error('生成图片失败')
          return
        }
        const name = `${sanitizeFilenamePart(filenamePrefix)}_${v}.png`
        downloadBlob(blob, name)
        ElMessage.success('已下载 PNG')
      },
      'image/png',
      1
    )
  } catch {
    ElMessage.error('该条码无法导出为图片')
  }
}

/** 导出条码为 SVG 矢量文件 */
function exportBarcodeSvg(value, filenamePrefix) {
  const v = value?.trim()
  if (!v) {
    ElMessage.warning('无条码可导出')
    return
  }
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg')
  try {
    JsBarcode(svg, v, getBarcodeJsOptions(v))
    const xml = new XMLSerializer().serializeToString(svg)
    const blob = new Blob([xml], { type: 'image/svg+xml;charset=utf-8' })
    downloadBlob(blob, `${sanitizeFilenamePart(filenamePrefix)}_${v}.svg`)
    ElMessage.success('已下载 SVG')
  } catch {
    ElMessage.error('该条码无法导出为 SVG')
  }
}

const renderBarcodePreview = async () => {
  await nextTick()
  const el = barcodeSvgRef.value
  const v = form.barcode?.trim()
  if (!el || !v) return
  try {
    while (el.firstChild) el.removeChild(el.firstChild)
    JsBarcode(el, v, { ...getBarcodeJsOptions(v), margin: 6 })
  } catch {
    while (el.firstChild) el.removeChild(el.firstChild)
  }
}

watch(
  () => form.barcode,
  () => {
    renderBarcodePreview()
  }
)

const loadCategories = async () => {
  try {
    const res = await listCategories()
    categories.value = res.data || []
  } catch {
    categories.value = []
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      page: page.value - 1,
      size: size.value,
      keyword: searchKeyword.value || undefined
    }
    const response = await getProducts(params)
    products.value = response.data.content
    total.value = response.data.totalElements
  } catch (error) {
    console.error('加载商品失败:', error)
    ElMessage.error('加载商品失败')
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
  scanCode.value = ''
  page.value = 1
  loadData()
}

const stopCameraScanner = async () => {
  const inst = html5QrCodeInstance
  html5QrCodeInstance = null
  if (!inst) return
  try {
    if (inst.isScanning) {
      await inst.stop()
    }
  } catch {
    try {
      await inst.stop()
    } catch {
      /* ignore */
    }
  }
  try {
    inst.clear()
  } catch {
    /* ignore */
  }
}

const openCameraScanDialog = () => {
  cameraDecodeHandled = false
  cameraScanVisible.value = true
}

const onCameraScanDialogOpened = () => {
  startCameraScanner()
}

const onCameraScanDialogClosed = async () => {
  await stopCameraScanner()
}

const startCameraScanner = async () => {
  await stopCameraScanner()
  await nextTick()
  cameraStarting.value = true
  cameraDecodeHandled = false
  const formats = [
    Html5QrcodeSupportedFormats.EAN_13,
    Html5QrcodeSupportedFormats.EAN_8,
    Html5QrcodeSupportedFormats.CODE_128,
    Html5QrcodeSupportedFormats.CODE_39,
    Html5QrcodeSupportedFormats.UPC_A,
    Html5QrcodeSupportedFormats.UPC_E
  ]
  const scanConfig = {
    fps: 8,
    qrbox: { width: 300, height: 120 },
    aspectRatio: 1.777778
  }
  const onDecoded = async (text) => {
    if (cameraDecodeHandled) return
    const code = text?.trim()
    if (!code) return
    cameraDecodeHandled = true
    scanCode.value = code
    await stopCameraScanner()
    cameraScanVisible.value = false
    await handleScanLookup()
  }
  const tryOpen = async (constraints) => {
    const el = document.getElementById(CAMERA_SCAN_ELEMENT_ID)
    if (!el) {
      throw new Error('扫码区域未就绪')
    }
    html5QrCodeInstance = new Html5Qrcode(CAMERA_SCAN_ELEMENT_ID, {
      formatsToSupport: formats,
      verbose: false
    })
    await html5QrCodeInstance.start(constraints, scanConfig, onDecoded, () => {})
  }
  try {
    try {
      await tryOpen({ facingMode: 'environment' })
    } catch {
      await stopCameraScanner()
      await nextTick()
      await tryOpen({ facingMode: 'user' })
    }
  } catch (e) {
    console.error(e)
    ElMessage.error(
      e?.message?.includes?.('Permission') || e?.name === 'NotAllowedError'
        ? '摄像头权限被拒绝'
        : '无法打开摄像头，请使用 HTTPS、localhost，并检查浏览器权限'
    )
    await stopCameraScanner()
    cameraScanVisible.value = false
  } finally {
    cameraStarting.value = false
  }
}

const handleScanLookup = async () => {
  const q = scanCode.value?.trim()
  if (!q) return
  try {
    const res = await lookupProduct(q)
    const p = res.data
    ElMessage.success(`已定位：${p.name}`)
    handleEdit(p)
    scanCode.value = ''
  } catch {
    // 拦截器已提示
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增商品'
  Object.assign(form, {
    id: null,
    productCode: '',
    barcode: '',
    name: '',
    categoryId: null,
    unit: '',
    specification: '',
    purchasePrice: null,
    salePrice: null,
    shelfLife: null,
    imageUrl: '',
    description: '',
    status: 'ENABLED'
  })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑商品'
  Object.assign(form, {
    id: row.id,
    productCode: row.productCode,
    barcode: row.barcode || '',
    name: row.name,
    categoryId: row.category?.id ?? null,
    unit: row.unit,
    specification: row.specification,
    purchasePrice: row.purchasePrice,
    salePrice: row.salePrice,
    shelfLife: row.shelfLife,
    imageUrl: row.imageUrl,
    description: row.description,
    status: row.status || 'ENABLED'
  })
  dialogVisible.value = true
}

const openPriceHistory = async (row) => {
  historyVisible.value = true
  priceHistoryRows.value = []
  historyLoading.value = true
  try {
    const res = await getProductPriceHistory(row.id)
    priceHistoryRows.value = res.data || []
  } catch {
    priceHistoryRows.value = []
  } finally {
    historyLoading.value = false
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除商品"${row.name}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteProduct(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch {
      // 拦截器
    }
  }).catch(() => {})
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const payload = {
          productCode: form.productCode,
          name: form.name,
          categoryId: form.categoryId,
          unit: form.unit,
          specification: form.specification,
          purchasePrice: form.purchasePrice != null && form.purchasePrice !== ''
            ? Number(form.purchasePrice)
            : null,
          salePrice: form.salePrice != null && form.salePrice !== ''
            ? Number(form.salePrice)
            : null,
          shelfLife: form.shelfLife != null && form.shelfLife !== ''
            ? parseInt(form.shelfLife, 10)
            : null,
          imageUrl: form.imageUrl,
          description: form.description,
          status: form.status
        }
        if (form.id) {
          payload.barcode = form.barcode?.trim() || undefined
          await updateProduct(form.id, payload)
          ElMessage.success('更新成功')
        } else {
          const res = await createProduct(payload)
          const code = res.data?.barcode
          ElMessage.success(code ? `创建成功，条码：${code}` : '创建成功')
        }
        dialogVisible.value = false
        loadData()
      } catch {
        // 拦截器
      } finally {
        submitting.value = false
      }
    }
  })
}

const uploadProductImage = async (options) => {
  try {
    const fd = new FormData()
    fd.append('file', options.file)
    fd.append('businessType', 'product')
    if (form.id) {
      fd.append('businessId', String(form.id))
    }
    const res = await uploadFile(fd)
    const meta = res.data
    if (meta?.url) {
      form.imageUrl = meta.url
      ElMessage.success('图片已上传')
    }
    options.onSuccess(res)
  } catch (e) {
    options.onError(e)
  }
}

const beforeAvatarUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件!')
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB!')
  }
  return isImage && isLt2M
}

const downloadTemplate = async () => {
  try {
    const blob = await downloadProductImportTemplate()
    const b = blob instanceof Blob ? blob : new Blob([blob])
    const url = window.URL.createObjectURL(b)
    const a = document.createElement('a')
    a.href = url
    a.download = '商品导入模板.xlsx'
    a.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('模板已下载')
  } catch {
    ElMessage.error('下载失败')
  }
}

const handleImportRequest = async (options) => {
  try {
    const res = await importProducts(options.file)
    const d = res.data
    ElMessage.success(`导入完成：成功 ${d.successCount} 条，失败 ${d.failCount} 条`)
    if (d.errors?.length) {
      ElMessageBox.alert(d.errors.join('\n'), '失败明细（前若干条）', { type: 'warning' })
    }
    loadData()
    options.onSuccess?.()
  } catch {
    options.onError?.(new Error('fail'))
  }
}

const handleExport = () => {
  ElMessage.info('导出功能开发中')
}

onMounted(() => {
  loadCategories()
  loadData()
})

onUnmounted(() => {
  stopCameraScanner()
})
</script>

<style scoped lang="scss">
.product-list {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    flex-wrap: wrap;
    gap: 8px;
  }

  .header-actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
  }

  .search-bar {
    margin-bottom: 20px;
    display: flex;
    gap: 10px;
    flex-wrap: wrap;
    align-items: center;
  }

  .scan-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
  }

  .camera-scan-hint {
    margin: 0 0 12px;
    font-size: 13px;
    line-height: 1.5;
    color: var(--el-text-color-secondary);
  }

  .camera-scan-region {
    min-height: 220px;
    width: 100%;
    border-radius: 8px;
    overflow: hidden;
    background: var(--el-fill-color-light);
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }

  .avatar-uploader {
    :deep(.el-upload) {
      border: 1px dashed var(--el-border-color);
      border-radius: 6px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: var(--el-transition-duration-fast);

      &:hover {
        border-color: var(--el-color-primary);
      }
    }
  }

  .avatar {
    width: 100px;
    height: 100px;
    display: block;
    object-fit: cover;
  }

  .avatar-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 100px;
    height: 100px;
    text-align: center;
    line-height: 100px;
  }

  .barcode-hint {
    font-size: 13px;
    color: var(--el-text-color-secondary);
  }

  .barcode-preview-wrap {
    margin-top: 8px;
    padding: 8px 0;

    .barcode-export-actions {
      margin-top: 10px;
      display: flex;
      flex-wrap: wrap;
      gap: 8px;
    }

    .barcode-svg {
      display: block;
      max-width: 100%;
      height: auto;
    }

    .barcode-tip {
      display: block;
      margin-top: 6px;
      font-size: 12px;
      color: var(--el-text-color-secondary);
    }
  }
}
</style>
