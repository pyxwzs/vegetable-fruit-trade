import request from './request'

export function getSalesOrders(params) {
  return request({ url: '/sales', method: 'get', params })
}

export function getSalesOrder(id) {
  return request({ url: `/sales/${id}`, method: 'get' })
}

export function createSalesOrder(data) {
  return request({ url: '/sales', method: 'post', data })
}

export function completeSalesOrder(id) {
  return request({ url: `/sales/${id}/complete`, method: 'post' })
}

export function cancelSalesOrder(id) {
  return request({ url: `/sales/${id}/cancel`, method: 'post' })
}

export function collectSalesOrder(id, data) {
  return request({ url: `/sales/${id}/collect`, method: 'post', data })
}

export function getSalePayments(id) {
  return request({ url: `/sales/${id}/payments`, method: 'get' })
}

export function getSalesPendingStats() {
  return request({ url: '/sales/pending-stats', method: 'get' })
}
