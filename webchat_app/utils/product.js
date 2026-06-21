export const PRODUCT_STATUS = {
  ENABLED: '启用',
  DISABLED: '停用',
};

export function productStatusTheme(status) {
  return status === 'ENABLED' ? 'success' : 'default';
}
