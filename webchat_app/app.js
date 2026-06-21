const config = require('./config');

App({
  globalData: {
    appName: config.appName,
    siteName: config.appName,
    userInfo: null,
    tenantInfo: null,
  },

  onLaunch() {
    const updateManager = wx.getUpdateManager();
    updateManager.onUpdateReady(() => {
      wx.showModal({
        title: '更新提示',
        content: '新版本已经准备好，是否重启应用？',
        success(res) {
          if (res.confirm) updateManager.applyUpdate();
        },
      });
    });

    if (!config.isMock) {
      const tenant = wx.getStorageSync('tenant_info');
      const user = wx.getStorageSync('user_info');
      if (tenant) {
        this.globalData.tenantInfo = tenant;
      }
      if (user) this.globalData.userInfo = user;
    }
  },
});
