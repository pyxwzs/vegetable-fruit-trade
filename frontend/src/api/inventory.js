import request from './request'

export function getInventories(params) {
  return request({ url: '/inventory', method: 'get', params })
}

export function inbound(data) {
  return request({ url: '/inventory/inbound', method: 'post', data })
}

export function outbound(data) {
  return request({ url: '/inventory/outbound', method: 'post', data })
}

export function getLowStockProducts() {
  return request({ url: '/inventory/low-stock', method: 'get' })
}
