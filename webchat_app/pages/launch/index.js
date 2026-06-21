Page({
  onLoad() {
    const token = wx.getStorageSync('access_token');
    if (token) {
      wx.switchTab({ url: '/pages/home/index' });
    } else {
      wx.reLaunch({ url: '/pages/login/login' });
    }
  },
});
