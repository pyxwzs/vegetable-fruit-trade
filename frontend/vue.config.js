const { defineConfig } = require('@vue/cli-service')

module.exports = defineConfig({
  transpileDependencies: true,
  devServer: {
    port: 8081,
    // 热模块替换（.vue/.js/.css 等保存后局部更新，无需整页刷新）
    hot: true,
    liveReload: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    },
    client: {
      overlay: {
        // 忽略 Chrome 下 Element Plus / ECharts 等触发的良性 ResizeObserver 提示，避免全屏报错遮罩
        runtimeErrors: (error) => {
          const msg = error && error.message ? error.message : ''
          if (/ResizeObserver loop/i.test(msg)) {
            return false
          }
          return true
        }
      }
    }
  }
})
