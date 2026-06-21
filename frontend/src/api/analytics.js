import request from './request'
import axios from 'axios'

const baseURL = process.env.VUE_APP_BASE_API || '/api'

export function getHomeSummary() {
  return request({ url: '/analytics/home-summary', method: 'get' })
}

export function getMonthlyPurchase(params) {
  return request({ url: '/analytics/monthly-purchase', method: 'get', params })
}

export function getMonthlySales(params) {
  return request({ url: '/analytics/monthly-sales', method: 'get', params })
}

export function getDailyPurchaseDetail(params) {
  return request({ url: '/analytics/daily-purchase', method: 'get', params })
}

export function getDailySalesDetail(params) {
  return request({ url: '/analytics/daily-sales', method: 'get', params })
}

export function getPurchaseItems(params) {
  return request({ url: '/analytics/purchase-items', method: 'get', params })
}

export function getSalesItems(params) {
  return request({ url: '/analytics/sales-items', method: 'get', params })
}

export function getMonthlyOverview(params) {
  return request({ url: '/analytics/monthly-overview', method: 'get', params })
}

export function getYearlyBalance(params) {
  return request({ url: '/analytics/yearly-balance', method: 'get', params })
}

export function getPurchasePartners(params) {
  return request({ url: '/analytics/purchase-partners', method: 'get', params })
}

export function getSalesPartners(params) {
  return request({ url: '/analytics/sales-partners', method: 'get', params })
}

export function getSupplierProductStats(params) {
  return request({ url: '/analytics/supplier-product-stats', method: 'get', params })
}

export function getCustomerProductStats(params) {
  return request({ url: '/analytics/customer-product-stats', method: 'get', params })
}

function downloadBlob(path, params) {
  const token = localStorage.getItem('token')
  return axios({
    baseURL,
    url: path,
    params,
    responseType: 'blob',
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  }).then(res => {
    const disposition = res.headers['content-disposition'] || ''
    let filename = 'export.xlsx'
    const match = disposition.match(/filename\*=UTF-8''(.+)/i)
    if (match) filename = decodeURIComponent(match[1])
    const url = window.URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    window.URL.revokeObjectURL(url)
  })
}

export const exportPurchasePartners = (params) => downloadBlob('/analytics/export/purchase-partners', params)
export const exportSalesPartners = (params) => downloadBlob('/analytics/export/sales-partners', params)
export const exportPurchaseMonth = (params) => downloadBlob('/analytics/export/purchase-month', params)
export const exportSalesMonth = (params) => downloadBlob('/analytics/export/sales-month', params)
export const exportPurchaseYear = (params) => downloadBlob('/analytics/export/purchase-year', params)
export const exportSalesYear = (params) => downloadBlob('/analytics/export/sales-year', params)
