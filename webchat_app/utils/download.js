import config from '~/config';
import { getToken } from '~/api/request';

const { baseUrl } = config;

function buildQuery(params) {
  return Object.entries(params || {})
    .filter(([, v]) => v !== null && v !== undefined && v !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`)
    .join('&');
}

function sanitizeFilename(name) {
  const raw = String(name || '导出.xlsx').trim();
  const withExt = raw.endsWith('.xlsx') ? raw : `${raw}.xlsx`;
  return withExt.replace(/[/\\:*?"<>|]/g, '_') || '导出.xlsx';
}

export function downloadExport(path, params = {}, filename = '导出.xlsx') {
  const token = getToken();
  const query = buildQuery(params);
  const url = `${baseUrl}${path}${query ? `?${query}` : ''}`;
  const safeName = sanitizeFilename(filename);
  const filePath = `${wx.env.USER_DATA_PATH}/${safeName}`;

  return new Promise((resolve, reject) => {
    wx.downloadFile({
      url,
      filePath,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success(res) {
        if (res.statusCode !== 200) {
          reject(new Error('导出失败'));
          return;
        }
        wx.openDocument({
          filePath: res.filePath || filePath,
          fileType: 'xlsx',
          showMenu: true,
          success: resolve,
          fail: reject,
        });
      },
      fail: reject,
    });
  });
}
