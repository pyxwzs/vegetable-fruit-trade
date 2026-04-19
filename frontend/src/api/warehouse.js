import request from './request'

export function getActiveWarehouses() {
  return request({
    url: '/warehouses/active',
    method: 'get'
  })
}
