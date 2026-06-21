Page({
  data: {
    version: '1.0.0',
  },

  goPassword() {
    wx.navigateTo({ url: '/pages/account/password/index' });
  },
});
