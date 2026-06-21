import config from '~/config';

function getToken() {
  return wx.getStorageSync('access_token') || '';
}

function getRefreshToken() {
  return wx.getStorageSync('refresh_token') || '';
}

function saveTokens(payload) {
  if (payload?.token) wx.setStorageSync('access_token', payload.token);
  if (payload?.refreshToken) wx.setStorageSync('refresh_token', payload.refreshToken);
}

function clearTokens() {
  wx.removeStorageSync('access_token');
  wx.removeStorageSync('refresh_token');
}

function isPublicUrl(url) {
  return (
    url.includes('/auth/wx/login') ||
    url.includes('/auth/wx/register') ||
    url.includes('/auth/refresh') ||
    (url.includes('/site-settings') && !getToken())
  );
}

function requestOnce(url, method = 'GET', data = {}, extraHeader = {}) {
  const header = {
    'content-type': 'application/json',
    ...extraHeader,
  };
  const token = getToken();
  if (token && !isPublicUrl(url)) {
    header.Authorization = `Bearer ${token}`;
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: config.baseUrl + url,
      method,
      data,
      header,
      timeout: 15000,
      success(res) {
        const body = res.data || {};
        if (res.statusCode === 401 && !extraHeader._retry) {
          const refresh = getRefreshToken();
          if (refresh) {
            wx.request({
              url: `${config.baseUrl}/auth/refresh`,
              method: 'POST',
              data: { refreshToken: refresh },
              header: { 'content-type': 'application/json' },
              timeout: 15000,
              success(refreshRes) {
                const refreshBody = refreshRes.data || {};
                if (refreshRes.statusCode === 200 && refreshBody.code === 200) {
                  saveTokens(refreshBody.data);
                  requestOnce(url, method, data, { _retry: true })
                    .then(resolve)
                    .catch(reject);
                } else {
                  clearTokens();
                  reject(refreshBody);
                }
              },
              fail: reject,
            });
            return;
          }
          clearTokens();
          reject(body);
          return;
        }

        if (res.statusCode >= 200 && res.statusCode < 300 && body.code === 200) {
          resolve(body);
        } else {
          reject(body);
        }
      },
      fail(err) {
        const errMsg = err?.errMsg || '';
        if (errMsg.includes('fail')) {
          reject({
            message:
              '无法连接服务器。模拟器请确认后端已启动；真机请将 webchat_app/config.js 的 LAN_HOST 改为你电脑的局域网 IP（与手机同一 WiFi）',
            errMsg,
          });
          return;
        }
        reject(err);
      },
    });
  });
}

export { getToken, getRefreshToken, saveTokens, clearTokens, isPublicUrl };
export default requestOnce;
