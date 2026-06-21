import { getCurrentUser, logoutLocal } from '~/api/auth';
import { isLoggedIn, getUserInfoCache, getTenantInfoCache, saveSession } from '~/utils/auth';
import { resolveAssetUrl } from '~/utils/asset';

const ROLE_LABELS = {
  TENANT_ADMIN: '管理员',
  PLATFORM_ADMIN: '平台管理员',
};

const STATUS_LABELS = {
  ENABLED: '正常',
  DISABLED: '已禁用',
};

Page({
  data: {
    isLoad: false,
    loading: false,
    version: '1.0.0',
    personalInfo: {},
    tenantName: '',
    roleLabel: '管理员',
    statusLabel: '正常',
    statusClass: '',
    menuList: [
      {
        name: '个人资料',
        icon: 'user',
        url: '/pages/account/profile/index',
        iconBg: '#ecf2fe',
        iconColor: '#0052d9',
      },
    ],
    guestFeatures: [
      { icon: 'chart-bar', text: '经营看板' },
      { icon: 'shop', text: '进销存' },
      { icon: 'money-circle', text: '对账统计' },
    ],
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ value: 'my' });
    }
    this.refresh();
  },

  onPullDownRefresh() {
    this.refresh().finally(() => wx.stopPullDownRefresh());
  },

  async refresh() {
    if (!isLoggedIn()) {
      this.setData({
        isLoad: false,
        personalInfo: {},
        tenantName: '',
        roleLabel: '管理员',
        statusLabel: '正常',
        statusClass: '',
      });
      return;
    }
    await this.loadProfile();
  },

  async loadProfile() {
    if (this.data.loading) return;
    this.setData({ loading: true });
    try {
      const res = await getCurrentUser();
      const info = res.data || {};
      saveSession(null, info);
      this.applyProfile(info);
    } catch {
      const cached = getUserInfoCache();
      if (cached) {
        this.applyProfile(cached);
      } else {
        this.setData({ isLoad: false });
      }
    } finally {
      this.setData({ loading: false });
    }
  },

  applyProfile(info) {
    const tenant = getTenantInfoCache();
    const status = info.status || 'ENABLED';
    const menuList = [
      {
        name: '个人资料',
        icon: 'user',
        url: '/pages/account/profile/index',
        iconBg: '#ecf2fe',
        iconColor: '#0052d9',
      },
    ];
    this.setData({
      isLoad: true,
      menuList,
      personalInfo: {
        name: info.realName || info.wxNickname || info.username || '用户',
        wxNickname: info.wxNickname || '',
        phone: info.phone || '',
        avatarUrl: resolveAssetUrl(info.avatarUrl) || '',
      },
      tenantName: tenant?.tenantName || '',
      roleLabel: ROLE_LABELS[info.role] || '管理员',
      statusLabel: STATUS_LABELS[status] || '正常',
      statusClass: status === 'DISABLED' ? 'danger' : '',
    });
  },

  onLogin() {
    wx.reLaunch({ url: '/pages/login/login' });
  },

  onRegister() {
    wx.navigateTo({ url: '/pages/register/register' });
  },

  onMenuClick(e) {
    const { url } = e.currentTarget.dataset;
    if (url) wx.navigateTo({ url });
  },

  onLogout() {
    wx.showModal({
      title: '提示',
      content: '确定退出登录？',
      success: (res) => {
        if (!res.confirm) return;
        logoutLocal();
        this.setData({
          isLoad: false,
          personalInfo: {},
          tenantName: '',
        });
        wx.reLaunch({ url: '/pages/login/login' });
      },
    });
  },
});
