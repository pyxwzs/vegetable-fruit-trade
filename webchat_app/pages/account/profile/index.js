import { getCurrentUser, updateProfile } from '~/api/auth';
import { requireLogin, saveSession } from '~/utils/auth';
import { resolveAssetUrl } from '~/utils/asset';

Page({
  data: {
    form: { realName: '', phone: '', wxNickname: '' },
    avatarUrl: '',
    loading: false,
  },

  onShow() {
    if (!requireLogin()) return;
    this.loadProfile();
  },

  async loadProfile() {
    try {
      const res = await getCurrentUser();
      const u = res.data || {};
      this.setData({
        form: {
          realName: u.realName || '',
          phone: u.phone || '',
          wxNickname: u.wxNickname || '',
        },
        avatarUrl: resolveAssetUrl(u.avatarUrl) || '',
      });
      saveSession(null, u);
    } catch (e) {
      wx.showToast({ title: e.message || '加载失败', icon: 'none' });
    }
  },

  onFieldChange(e) {
    const { field } = e.currentTarget.dataset;
    this.setData({ [`form.${field}`]: e.detail.value || '' });
  },

  async onSave() {
    const { realName, phone } = this.data.form;
    this.setData({ loading: true });
    try {
      const res = await updateProfile({
        realName: realName.trim() || undefined,
        phone: phone.trim() || undefined,
      });
      saveSession(null, res.data);
      wx.showToast({ title: '已保存', icon: 'success' });
      setTimeout(() => wx.navigateBack(), 500);
    } catch (e) {
      wx.showToast({ title: e.message || '保存失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },
});
