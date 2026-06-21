import config from '~/config';

/** 将后端相对路径转为可访问 URL */
export function resolveAssetUrl(path) {
  if (!path) return '';
  if (path.startsWith('http://') || path.startsWith('https://') || path.startsWith('wxfile://')) {
    return path;
  }
  const base = config.baseUrl.replace(/\/$/, '');
  const p = path.startsWith('/') ? path : `/${path}`;
  return `${base}${p}`;
}
