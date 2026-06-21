/** 开发时在开发者工具勾选「不校验合法域名」；上线前改为 HTTPS 域名 */

/**
 * 真机预览/调试时填写你电脑的局域网 IP（与手机同一 WiFi）。
 * macOS 终端执行: ipconfig getifaddr en0
 * 留空则真机仍尝试 127.0.0.1（仅模拟器可用）。
 */
const LAN_HOST = '192.168.31.186';

function resolveHost() {
  try {
    const sys = wx.getSystemInfoSync();
    if (sys.platform === 'devtools') {
      return '127.0.0.1';
    }
    if (LAN_HOST) {
      return LAN_HOST.replace(/^https?:\/\//, '').replace(/\/.*$/, '');
    }
  } catch (e) {
    // 非小程序环境（构建脚本等）
  }
  return '127.0.0.1';
}

module.exports = {
  isMock: false,
  /** 小程序产品名（登录页等未登录场景展示，勿与租户名混淆） */
  appName: '果蔬配送经营管理系统',
  /** 真机调试用的局域网 IP，见上方 LAN_HOST */
  lanHost: LAN_HOST,
  get baseUrl() {
    return `http://${resolveHost()}:8080/api`;
  },
};
