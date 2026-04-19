import request from './request'

export function getInventories(params) {
  return request({
    url: '/inventory',
    method: 'get',
    params
  })
}

export function inbound(data) {
  return request({
    url: '/inventory/inbound',
    method: 'post',
    data
  })
}

export function outbound(data) {
  return request({
    url: '/inventory/outbound',
    method: 'post',
    data
  })
}

export function stocktake(data) {
  return request({
    url: '/inventory/stocktake',
    method: 'post',
    data
  })
}

export function transferInventory(data) {
  return request({
    url: '/inventory/transfer',
    method: 'post',
    data
  })
}

export function freezeInventory(data) {
  return request({
    url: '/inventory/freeze',
    method: 'post',
    data
  })
}

export function unfreezeInventory(data) {
  return request({
    url: '/inventory/unfreeze',
    method: 'post',
    data
  })
}

export function getExpiringProducts() {
  return request({
    url: '/inventory/expiring',
    method: 'get'
  })
}

export function getLowStockProducts() {
  return request({
    url: '/inventory/low-stock',
    method: 'get'
  })
}
