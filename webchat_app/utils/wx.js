/** 获取 wx.login 临时 code */
export function getWxLoginCode() {
  return new Promise((resolve, reject) => {
    wx.login({
      success(res) {
        if (res.code) resolve(res.code);
        else reject(new Error('微信登录失败'));
      },
      fail(err) {
        reject(err || new Error('微信登录失败'));
      },
    });
  });
}

/** 读取本地临时文件为 base64 */
export function readFileBase64(filePath) {
  return new Promise((resolve, reject) => {
    if (!filePath) {
      resolve('');
      return;
    }
    wx.getFileSystemManager().readFile({
      filePath,
      encoding: 'base64',
      success(res) {
        resolve(res.data || '');
      },
      fail(err) {
        reject(err || new Error('读取头像失败'));
      },
    });
  });
}

export function showAlert(content, title = '提示') {
  wx.showModal({
    title,
    content: content || '操作失败',
    showCancel: false,
  });
}
