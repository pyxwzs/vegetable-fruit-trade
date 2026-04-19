import request from './request'

export function getSuppliers(params) {
  return request({
    url: '/suppliers',
    method: 'get',
    params
  })
}

export function getActiveSuppliers() {
  return request({
    url: '/suppliers/active',
    method: 'get'
  })
}

export function getSupplier(id) {
  return request({
    url: `/suppliers/${id}`,
    method: 'get'
  })
}

export function createSupplier(data) {
  return request({
    url: '/suppliers',
    method: 'post',
    data
  })
}

export function updateSupplier(id, data) {
  return request({
    url: `/suppliers/${id}`,
    method: 'put',
    data
  })
}

export function deleteSupplier(id) {
  return request({
    url: `/suppliers/${id}`,
    method: 'delete'
  })
}
