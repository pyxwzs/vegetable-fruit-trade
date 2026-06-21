import { onMounted, onActivated } from 'vue'

/** 进入页面或从缓存激活时自动刷新（配合 keep-alive 时 onActivated 生效） */
export function usePageRefresh(refreshFn) {
  onMounted(refreshFn)
  onActivated(refreshFn)
}
