import request, { saveTokens, clearTokens } from './request';

export function wxLogin(data) {
  return request('/auth/wx/login', 'POST', data).then((res) => {
    if (res.data && !res.data.needRegister) {
      saveTokens(res.data);
    }
    return res;
  });
}

export function wxRegister(data) {
  return request('/auth/wx/register', 'POST', data).then((res) => {
    if (res.data) saveTokens(res.data);
    return res;
  });
}

export function getCurrentUser() {
  return request('/auth/me', 'GET');
}

export function updateProfile(data) {
  return request('/users/me', 'PUT', data);
}

export function getSiteSettings(tenantCode) {
  const query = tenantCode ? `?tenantCode=${encodeURIComponent(tenantCode)}` : '';
  return request(`/site-settings${query}`, 'GET');
}

export function logoutLocal() {
  clearTokens();
  wx.removeStorageSync('user_info');
  wx.removeStorageSync('tenant_info');
}
