import request from './request'

export function listCategories() {
  return request({
    url: '/categories',
    method: 'get'
  })
}
