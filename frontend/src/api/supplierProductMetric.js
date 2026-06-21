import request from './request'

export function getSupplierProductMetrics(params) {
  return request({ url: '/supplier-product-metrics', method: 'get', params })
}

export function getSupplierMetricCompletion(params) {
  return request({ url: '/supplier-product-metrics/completion', method: 'get', params })
}

export function getSupplierProductMetric(id) {
  return request({ url: `/supplier-product-metrics/${id}`, method: 'get' })
}

export function createSupplierProductMetric(data) {
  return request({ url: '/supplier-product-metrics', method: 'post', data })
}

export function updateSupplierProductMetric(id, data) {
  return request({ url: `/supplier-product-metrics/${id}`, method: 'put', data })
}

export function deleteSupplierProductMetric(id) {
  return request({ url: `/supplier-product-metrics/${id}`, method: 'delete' })
}
