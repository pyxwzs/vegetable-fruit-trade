import request from './request';

export function getProducts(params) {
  return request('/products', 'GET', params);
}

export function getAllEnabledProducts() {
  return request('/products/all-enabled', 'GET');
}

export function getProduct(id) {
  return request(`/products/${id}`, 'GET');
}

export function createProduct(data) {
  return request('/products', 'POST', data);
}

export function updateProduct(id, data) {
  return request(`/products/${id}`, 'PUT', data);
}

export function deleteProduct(id) {
  return request(`/products/${id}`, 'DELETE');
}

export function getProductCategories() {
  return request('/products/categories', 'GET');
}
