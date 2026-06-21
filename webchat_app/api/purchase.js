import request from './request';

export function getPurchaseOrders(params) {
  return request('/purchase', 'GET', params);
}

export function getPurchaseOrder(id) {
  return request(`/purchase/${id}`, 'GET');
}

export function getPurchasePendingStats() {
  return request('/purchase/pending-stats', 'GET');
}

export function completePurchaseOrder(id) {
  return request(`/purchase/${id}/complete`, 'POST');
}

export function payPurchaseOrder(id, data) {
  return request(`/purchase/${id}/pay`, 'POST', data);
}

export function getPurchasePayments(id) {
  return request(`/purchase/${id}/payments`, 'GET');
}

export function cancelPurchaseOrder(id) {
  return request(`/purchase/${id}/cancel`, 'POST');
}
