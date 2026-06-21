import config from '~/config';
import { wxLogin, getCurrentUser } from '~/api/auth';
import { saveSession } from '~/utils/auth';
import { getWxLoginCode, showAlert } from '~/utils/wx';

const APP_NAME = config.appName;

Page({
  data: {
    isCheck: false,
    radioValue: '',
    loading: false,
    siteName: APP_NAME,
    features: [
      { icon: 'cart', text: '进销管理' },
      { icon: 'chart', text: '经营统计' },
      { icon: 'calendar', text: '月度对账' },
    ],
  },

  onLoad() {
    wx.setNavigationBarTitle({ title: APP_NAME });
  },

  onCheckChange(e) {
    const value = e.detail.value;
    this.setData({ radioValue: value, isCheck: value === 'agree' });
  },

  async finishSession(loginData) {
    saveSession(loginData, null);
    const userRes = await getCurrentUser();
    saveSession(loginData, userRes.data);
    wx.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => {
      wx.switchTab({ url: '/pages/home/index' });
    }, 400);
  },

  async handleWxLogin() {
    if (!this.data.isCheck) {
      showAlert('请先同意使用本系统管理租户业务数据');
      return;
    }
    if (this.data.loading) return;
    this.setData({ loading: true });
    try {
      const wxCode = await getWxLoginCode();
      const res = await wxLogin({ wxCode });
      const data = res.data || {};
      if (data.needRegister) {
        wx.showModal({
          title: '尚未注册',
          content: '该微信尚未注册租户，请使用邀请码完成注册',
          confirmText: '去注册',
          success(modal) {
            if (modal.confirm) {
              wx.navigateTo({ url: '/pages/register/register' });
            }
          },
        });
        return;
      }
      await this.finishSession(data);
    } catch (e) {
      showAlert((e && e.message) || (e && e.errMsg) || '登录失败');
    } finally {
      this.setData({ loading: false });
    }
  },

  goRegister() {
    wx.navigateTo({ url: '/pages/register/register' });
  },
});
