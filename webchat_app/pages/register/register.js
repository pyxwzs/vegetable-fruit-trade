import { wxRegister, getCurrentUser, logoutLocal } from '~/api/auth';
import { saveSession } from '~/utils/auth';
import { getWxLoginCode, readFileBase64, showAlert } from '~/utils/wx';

Page({
  data: {
    form: {
      inviteCode: '',
      tenantName: '',
      realName: '',
      phone: '',
      wxNickname: '',
    },
    avatarUrl: '',
    avatarPath: '',
    loading: false,
  },

  onFieldChange(e) {
    const { field } = e.currentTarget.dataset;
    let value = e.detail.value || '';
    if (field === 'inviteCode') value = value.trim().toUpperCase();
    this.setData({ [`form.${field}`]: value });
  },

  onChooseAvatar(e) {
    const avatarUrl = (e.detail && e.detail.avatarUrl) || '';
    this.setData({
      avatarUrl,
      avatarPath: avatarUrl,
    });
  },

  async handleRegister() {
    const f = this.data.form;
    if (!f.inviteCode) {
      showAlert('请填写邀请码');
      return;
    }
    if (!f.tenantName.trim()) {
      showAlert('请填写名称');
      return;
    }
    if (!f.realName.trim()) {
      showAlert('请填写真实姓名');
      return;
    }
    if (!/^1[3-9]\d{9}$/.test(String(f.phone || '').trim())) {
      showAlert('请填写正确的手机号');
      return;
    }
    if (!f.wxNickname.trim()) {
      showAlert('请点击输入框授权或填写微信昵称');
      return;
    }
    if (!this.data.avatarPath) {
      showAlert('请点击头像授权微信头像');
      return;
    }

    this.setData({ loading: true });
    try {
      logoutLocal();
      const wxCode = await getWxLoginCode();
      let avatarBase64 = '';
      if (this.data.avatarPath) {
        avatarBase64 = await readFileBase64(this.data.avatarPath);
      }
      const res = await wxRegister({
        inviteCode: f.inviteCode.trim(),
        wxCode,
        tenantName: f.tenantName.trim(),
        realName: f.realName.trim(),
        phone: f.phone.trim(),
        wxNickname: f.wxNickname.trim(),
        avatarBase64: avatarBase64 || undefined,
      });
      const d = res.data || {};
      saveSession(d, null);
      const userRes = await getCurrentUser();
      saveSession(d, userRes.data);
      wx.showModal({
        title: '注册成功',
        content: `租户编号：${d.tenantCode || '-'}\n即将进入系统`,
        showCancel: false,
        success: () => {
          wx.switchTab({ url: '/pages/home/index' });
        },
      });
    } catch (e) {
      showAlert((e && e.message) || '注册失败');
    } finally {
      this.setData({ loading: false });
    }
  },
});
