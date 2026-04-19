import request from './request'

export function getCustomers(params) {
  return request({
    url: '/customers',
    method: 'get',
    params
  })
}

export function getCustomer(id) {
  return request({
    url: `/customers/${id}`,
    method: 'get'
  })
}

export function getActiveCustomers() {
  return request({
    url: '/customers/active',
    method: 'get'
  })
}

export function getCreditWarnings() {
  return request({
    url: '/customers/credit-warnings',
    method: 'get'
  })
}

export function createCustomer(data) {
  return request({
    url: '/customers',
    method: 'post',
    data
  })
}

export function updateCustomer(id, data) {
  return request({
    url: `/customers/${id}`,
    method: 'put',
    data
  })
}

export function deleteCustomer(id) {
  return request({
    url: `/customers/${id}`,
    method: 'delete'
  })
}
