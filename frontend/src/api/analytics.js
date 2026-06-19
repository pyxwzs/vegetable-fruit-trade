import request from './request'

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
