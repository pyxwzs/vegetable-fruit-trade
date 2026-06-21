<template>
  <div class="login-hint">
    <div class="card">
      <h1>租户业务请使用微信小程序</h1>
      <p>本系统租户端已改为<strong>微信授权登录</strong>，不再支持 Web 用户名密码登录。</p>
      <p>请打开「果蔬配送经营管理系统」小程序，使用微信一键登录或邀请码注册。</p>
      <el-divider />
      <p class="sub">平台管理员请使用独立入口：</p>
      <el-button type="primary" @click="goPlatform">平台管理登录</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goPlatform() {
  router.push('/platform/login')
}
</script>

<style scoped>
.login-hint {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  padding: 24px;
  box-sizing: border-box;
}

.card {
  max-width: 480px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 32px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}

h1 {
  margin: 0 0 16px;
  font-size: 22px;
  color: #303133;
}

p {
  margin: 0 0 12px;
  line-height: 1.7;
  color: #606266;
  font-size: 15px;
}

.sub {
  margin-top: 8px;
  font-size: 14px;
  color: #909399;
}
</style>
