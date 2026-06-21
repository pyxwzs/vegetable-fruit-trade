import request from './request';

export function getSalesOrders(params) {
  return request('/sales', 'GET', params);
}

export function getSalesOrder(id) {
  return request(`/sales/${id}`, 'GET');
}

export function getSalesPendingStats() {
  return request('/sales/pending-stats', 'GET');
}

export function completeSalesOrder(id) {
  return request(`/sales/${id}/complete`, 'POST');
}

export function collectSalesOrder(id, data) {
  return request(`/sales/${id}/collect`, 'POST', data);
}

export function getSalesPayments(id) {
  return request(`/sales/${id}/payments`, 'GET');
}

export function cancelSalesOrder(id) {
  return request(`/sales/${id}/cancel`, 'POST');
}
