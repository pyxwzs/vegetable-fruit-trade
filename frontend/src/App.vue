<template>
  <el-config-provider :locale="zhCn">
    <router-view />
  </el-config-provider>
</template>

<script setup>
import { onMounted } from 'vue'
import { useStore } from 'vuex'
import { ElConfigProvider } from 'element-plus'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

const store = useStore()

onMounted(() => {
  const code = store.state.tenant.tenantCode || 'default'
  store.dispatch('site/load', code).catch(() => {})
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  width: 100%;
}
</style>