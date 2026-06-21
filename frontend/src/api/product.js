import request from './request'

export function getProducts(params) {
  return request({ url: '/products', method: 'get', params })
}

export function getAllEnabledProducts() {
  return request({ url: '/products/all-enabled', method: 'get' })
}

export function getProduct(id) {
  return request({ url: `/products/${id}`, method: 'get' })
}

export function createProduct(data) {
  return request({ url: '/products', method: 'post', data })
}

export function updateProduct(id, data) {
  return request({ url: `/products/${id}`, method: 'put', data })
}

export function deleteProduct(id) {
  return request({ url: `/products/${id}`, method: 'delete' })
}

export function getProductCategories() {
  return request({ url: '/products/categories', method: 'get' })
}
