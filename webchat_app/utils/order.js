export const PURCHASE_STATUS = {
  PENDING: '未入库',
  COMPLETED: '已入库',
  CANCELLED: '已取消',
};

export const SALES_STATUS = {
  PENDING: '未出库',
  COMPLETED: '已出库',
  CANCELLED: '已取消',
};

export const PAYMENT_STATUS = {
  UNPAID: '未结清',
  PARTIAL: '部分结清',
  PAID: '已结清',
};

export function paymentStatusLabel(status) {
  return PAYMENT_STATUS[status] || status || '-';
}

export function purchaseStatusTheme(status) {
  const map = { PENDING: 'warning', COMPLETED: 'success', CANCELLED: 'default' };
  return map[status] || 'default';
}

export function salesStatusTheme(status) {
  const map = { PENDING: 'warning', COMPLETED: 'success', CANCELLED: 'default' };
  return map[status] || 'default';
}

export function paymentStatusTheme(status) {
  const map = { UNPAID: 'danger', PARTIAL: 'warning', PAID: 'success' };
  return map[status] || 'default';
}

export function calcUnpaid(total, paid) {
  return Math.max(0, Number(total || 0) - Number(paid || 0));
}
