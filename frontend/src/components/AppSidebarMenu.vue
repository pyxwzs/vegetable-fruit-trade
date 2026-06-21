<template>
  <div class="sidebar-menu-wrap">
    <el-menu
        :default-active="activeMenu"
        :collapse="collapse"
        :collapse-transition="false"
        router
        class="menu menu-main"
        @select="onSelect"
    >
      <template v-for="group in visibleGroups" :key="group.key">
        <el-menu-item v-if="group.type === 'item'" :index="group.path">
          <el-icon><component :is="group.icon" /></el-icon>
          <span>{{ group.title }}</span>
        </el-menu-item>

        <el-sub-menu v-else :index="group.key">
          <template #title>
            <el-icon><component :is="group.icon" /></el-icon>
            <span>{{ group.title }}</span>
          </template>
          <el-menu-item
              v-for="item in group.children"
              :key="item.menuKey"
              :index="item.path"
          >
            {{ item.title }}
          </el-menu-item>
        </el-sub-menu>
      </template>
    </el-menu>

    <el-menu
        :default-active="activeMenu"
        :collapse="collapse"
        :collapse-transition="false"
        router
        class="menu menu-footer"
        @select="onSelect"
    >
      <el-menu-item index="/menu-settings">
        <el-icon><Setting /></el-icon>
        <span>菜单设置</span>
      </el-menu-item>
      <el-menu-item index="/site-settings">
        <el-icon><Picture /></el-icon>
        <span>站点设置</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useStore } from 'vuex'
import { Setting, Picture } from '@element-plus/icons-vue'
import { filterMenuGroups } from '@/config/menu'

defineProps({
  collapse: { type: Boolean, default: false }
})

const emit = defineEmits(['select'])

const route = useRoute()
const store = useStore()

const activeMenu = computed(() => route.path)
const visibleGroups = computed(() => {
  const menuKeys = store.state.user.userInfo?.menuKeys
  return filterMenuGroups(menuKeys)
})

const onSelect = () => emit('select')
</script>

<style scoped lang="scss">
.sidebar-menu-wrap {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 56px);
}

.menu-main {
  flex: 1;
  overflow-y: auto;
  border-right: none;
}

.menu-footer {
  flex-shrink: 0;
  border-right: none;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}
</style>
