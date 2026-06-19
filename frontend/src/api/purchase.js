import request from './request'

export function getPurchaseOrders(params) {
  return request({ url: '/purchase', method: 'get', params })
}

export function getPurchaseOrder(id) {
  return request({ url: `/purchase/${id}`, method: 'get' })
}

export function createPurchaseOrder(data) {
  return request({ url: '/purchase', method: 'post', data })
}

export function completePurchaseOrder(id) {
  return request({ url: `/purchase/${id}/complete`, method: 'post' })
}

export function cancelPurchaseOrder(id) {
  return request({ url: `/purchase/${id}/cancel`, method: 'post' })
}
