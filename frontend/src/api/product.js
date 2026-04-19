import request from './request'

/** 按商品编码或条码精确查询（扫码） */
export function lookupProduct(code) {
  return request({
    url: '/products/lookup',
    method: 'get',
    params: { code }
  })
}

export function downloadProductImportTemplate() {
  return request({
    url: '/products/import/template',
    method: 'get',
    responseType: 'blob'
  })
}

export function importProducts(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request({
    url: '/products/import',
    method: 'post',
    data: fd
  })
}

export function getProductPriceHistory(productId) {
  return request({
    url: `/products/${productId}/price-history`,
    method: 'get'
  })
}

export function getProducts(params) {
    return request({
        url: '/products',
        method: 'get',
        params
    })
}

export function getAllEnabledProducts() {
    return request({
        url: '/products/all-enabled',
        method: 'get'
    })
}

export function getProduct(id) {
    return request({
        url: `/products/${id}`,
        method: 'get'
    })
}

export function createProduct(data) {
    return request({
        url: '/products',
        method: 'post',
        data
    })
}

export function updateProduct(id, data) {
    return request({
        url: `/products/${id}`,
        method: 'put',
        data
    })
}

export function deleteProduct(id) {
    return request({
        url: `/products/${id}`,
        method: 'delete'
    })
}