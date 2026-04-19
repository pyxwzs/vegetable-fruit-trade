<template>
  <div class="announcement-form">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑公告' : '发布公告' }}</span>
        </div>
      </template>

      <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-width="100px"
          class="form"
      >
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" maxlength="200" show-word-limit />
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <!-- 使用简单的textarea代替富文本编辑器，避免依赖问题 -->
          <el-input
              v-model="form.content"
              type="textarea"
              :rows="10"
              placeholder="请输入公告内容"
          />
        </el-form-item>

        <el-form-item label="优先级" prop="priority">
          <el-radio-group v-model="form.priority">
            <el-radio label="LOW">低</el-radio>
            <el-radio label="NORMAL">普通</el-radio>
            <el-radio label="HIGH">高</el-radio>
            <el-radio label="URGENT">紧急</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="置顶" prop="isTop">
          <el-switch v-model="form.isTop" />
        </el-form-item>

        <el-form-item label="目标角色" prop="targetRoles">
          <el-select v-model="form.targetRoles" multiple placeholder="请选择目标角色" style="width: 100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="采购员" value="PURCHASER" />
            <el-option label="仓管员" value="WAREHOUSE_KEEPER" />
            <el-option label="销售员" value="SALESMAN" />
            <el-option label="财务员" value="FINANCE" />
          </el-select>
        </el-form-item>

        <el-form-item label="定时发布" prop="isTimed">
          <el-switch v-model="form.isTimed" />
        </el-form-item>

        <el-form-item v-if="form.isTimed" label="发布时间" prop="publishTime">
          <el-date-picker
              v-model="form.publishTime"
              type="datetime"
              placeholder="选择发布时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="过期时间" prop="expireTime">
          <el-date-picker
              v-model="form.expireTime"
              type="datetime"
              placeholder="选择过期时间"
              format="YYYY-MM-DD HH:mm:ss"
              value-format="YYYY-MM-DD HH:mm:ss"
              style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="附件" prop="attachments">
          <el-upload
              class="upload-demo"
              action="#"
              :on-preview="handlePreview"
              :on-remove="handleRemove"
              :before-remove="beforeRemove"
              multiple
              :limit="3"
              :on-exceed="handleExceed"
              :file-list="fileList"
          >
            <el-button type="primary">点击上传</el-button>
            <template #tip>
              <div class="el-upload__tip">
                只能上传图片/文档文件，且不超过10MB
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitForm" :loading="submitting">发布</el-button>
          <el-button @click="$router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createAnnouncement } from '@/api/announcement'
const router = useRouter()
const formRef = ref(null)
const submitting = ref(false)
const isEdit = ref(false)

const fileList = ref([])

const form = reactive({
  title: '',
  content: '',
  priority: 'NORMAL',
  isTop: false,
  targetRoles: [],
  isTimed: false,
  publishTime: null,
  expireTime: null,
  attachments: ''
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 2, max: 200, message: '长度在 2 到 200 个字符', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' }
  ],
  targetRoles: [
    { required: true, message: '请选择目标角色', trigger: 'change' }
  ]
}

const handlePreview = (file) => {
  console.log('预览文件:', file)
}

const handleRemove = (file, fileList) => {
  console.log('移除文件:', file, fileList)
}

const beforeRemove = (uploadFile) => {
  return ElMessageBox.confirm(`确定移除 ${uploadFile.name}？`).then(
      () => true,
      () => false
  )
}

const handleExceed = () => {
  ElMessage.warning('最多只能上传3个文件')
}

const submitForm = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        const toIso = (s) => {
          if (!s || typeof s !== 'string') return undefined
          return s.includes('T') ? s : s.replace(' ', 'T')
        }
        const payload = {
          title: form.title.trim(),
          content: form.content,
          priority: form.priority,
          isTop: Boolean(form.isTop),
          targetRoles: form.targetRoles,
          isTimed: Boolean(form.isTimed),
          publishTime: form.isTimed ? toIso(form.publishTime) : undefined,
          expireTime: form.expireTime ? toIso(form.expireTime) : undefined,
          attachments: form.attachments || ''
        }
        await createAnnouncement(payload)
        ElMessage.success('发布成功')
        router.push('/announcements')
      } catch (error) {
        console.error('发布失败:', error)
        ElMessage.error(error.response?.data?.message || '发布失败')
      } finally {
        submitting.value = false
      }
    }
  })
}
</script>

<style scoped lang="scss">
.announcement-form {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 18px;
    font-weight: bold;
  }

  .form {
    max-width: 800px;
    margin: 0 auto;
  }
}
</style>