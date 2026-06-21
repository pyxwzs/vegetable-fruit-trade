import { getToken } from '~/api/request';

export function isLoggedIn() {
  return !!getToken();
}

export function requireLogin() {
  if (!isLoggedIn()) {
    wx.reLaunch({ url: '/pages/login/login' });
    return false;
  }
  return true;
}

export function saveSession(loginData, userInfo) {
  if (loginData) {
    wx.setStorageSync('tenant_info', {
      tenantId: loginData.tenantId,
      tenantCode: loginData.tenantCode,
      tenantName: loginData.tenantName,
    });
  }
  if (userInfo) {
    wx.setStorageSync('user_info', userInfo);
  }
}

export function getUserInfoCache() {
  return wx.getStorageSync('user_info') || null;
}

export function getTenantInfoCache() {
  return wx.getStorageSync('tenant_info') || null;
}

export function isMenuEnabled(menuKeys, key) {
  if (!menuKeys || menuKeys.length === 0) return true;
  return menuKeys.includes(key);
}
