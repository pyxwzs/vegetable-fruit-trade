<template>
  <div class="announcement-list">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>公告列表</span>
          <el-button
            v-if="hasPerm('announcement:manage')"
            type="primary"
            @click="$router.push('/announcements/create')"
          >
            发布公告
          </el-button>
        </div>
      </template>

      <el-table :data="announcements" style="width: 100%" v-loading="loading">
        <el-table-column prop="title" label="标题" min-width="200">
          <template #default="{ row }">
            <div class="title-cell">
              <el-tag v-if="row.isTop" size="small" type="danger" effect="dark">置顶</el-tag>
              <el-tag v-if="row.priority === 'URGENT'" size="small" type="warning">紧急</el-tag>
              <span class="title-text clickable" @click="viewDetail(row)">{{ row.title }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="publisher.realName" label="发布人" width="120" />
        <el-table-column prop="publishTime" label="发布时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.publishTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="readCount" label="阅读量" width="100" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row)">查看</el-button>
            <el-button
              v-if="hasPerm('announcement:manage')"
              type="danger"
              link
              @click="deleteAnnouncementHandler(row)"
            >删除</el-button>
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

    <!-- 公告详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="currentAnnouncement?.title"
      width="680px"
      top="8vh"
      destroy-on-close
    >
      <div v-if="currentAnnouncement" class="detail-body">
        <div class="detail-meta">
          <el-tag v-if="currentAnnouncement.isTop" size="small" type="danger" effect="dark">置顶</el-tag>
          <el-tag v-if="currentAnnouncement.priority === 'URGENT'" size="small" type="warning">紧急</el-tag>
          <span class="meta-text">
            发布人：{{ currentAnnouncement.publisher?.realName || currentAnnouncement.publisher?.username }}
          </span>
          <span class="meta-text">发布时间：{{ formatDate(currentAnnouncement.publishTime) }}</span>
          <span class="meta-text">阅读量：{{ currentAnnouncement.readCount }}</span>
        </div>
        <el-divider />
        <div class="detail-content" v-html="renderContent(currentAnnouncement.content)" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useStore } from 'vuex'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAnnouncements, deleteAnnouncement } from '@/api/announcement'
import { formatDate } from '@/utils/date'

const store = useStore()
const hasPerm = (p) => store.getters['user/hasPermission'](p)

const loading = ref(false)
const announcements = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const detailVisible = ref(false)
const currentAnnouncement = ref(null)

const loadData = async () => {
  loading.value = true
  try {
    const params = { page: page.value - 1, size: size.value }
    const response = await getAnnouncements(params)
    announcements.value = response.data.content
    total.value = response.data.totalElements
  } catch (error) {
    console.error('加载公告失败:', error)
    ElMessage.error('加载公告失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = async (row) => {
  currentAnnouncement.value = row
  detailVisible.value = true
  // 更新本地阅读量（展示用）
  const item = announcements.value.find(a => a.id === row.id)
  if (item) item.readCount = (item.readCount || 0) + 1
}

/** 将换行符转换为 <br>，防止 XSS 但保留基本格式 */
const renderContent = (content) => {
  if (!content) return ''
  return content
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/\n/g, '<br>')
}

const deleteAnnouncementHandler = (row) => {
  ElMessageBox.confirm(`确定要删除公告"${row.title}"吗？`, '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await deleteAnnouncement(row.id)
      ElMessage.success('删除成功')
      loadData()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.announcement-list {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }

  .title-cell {
    display: flex;
    align-items: center;
    gap: 8px;

    .title-text {
      font-weight: 500;

      &.clickable {
        cursor: pointer;
        color: #409eff;
        &:hover { text-decoration: underline; }
      }
    }
  }

  .pagination {
    margin-top: 20px;
    display: flex;
    justify-content: flex-end;
  }
}

.detail-body {
  .detail-meta {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 12px;
    color: #606266;
    font-size: 13px;

    .meta-text {
      color: #909399;
    }
  }

  .detail-content {
    font-size: 15px;
    line-height: 1.8;
    color: #303133;
    min-height: 80px;
    white-space: pre-wrap;
    word-break: break-word;
  }
}
</style>
