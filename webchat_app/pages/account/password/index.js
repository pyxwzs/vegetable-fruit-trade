import { changePassword } from '~/api/auth';
import { requireLogin } from '~/utils/auth';

Page({
  data: {
    oldPassword: '',
    newPassword: '',
    confirmPassword: '',
    loading: false,
  },

  onShow() {
    requireLogin();
  },

  onFieldChange(e) {
    const { field } = e.currentTarget.dataset;
    this.setData({ [field]: e.detail.value || '' });
  },

  async onSubmit() {
    const { oldPassword, newPassword, confirmPassword } = this.data;
    if (!oldPassword || !newPassword) {
      wx.showToast({ title: '请填写完整', icon: 'none' });
      return;
    }
    if (newPassword.length < 6) {
      wx.showToast({ title: '新密码至少6位', icon: 'none' });
      return;
    }
    if (newPassword !== confirmPassword) {
      wx.showToast({ title: '两次密码不一致', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    try {
      await changePassword({ oldPassword, newPassword, confirmPassword });
      wx.showToast({ title: '密码已修改', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 500);
    } catch (e) {
      wx.showToast({ title: e.message || '修改失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },
});
