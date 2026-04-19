import request from './request'

export function getDailyReport(date) {
  return request({
    url: '/analytics/daily-report',
    method: 'get',
    params: date ? { date } : {}
  })
}

export function getHomeSummary() {
  return request({
    url: '/analytics/home-summary',
    method: 'get'
  })
}

export function getOverviewKpi(range) {
  return request({
    url: '/analytics/overview-kpi',
    method: 'get',
    params: { range }
  })
}

export function getSalesTrend(range) {
  return request({
    url: '/analytics/sales-trend',
    method: 'get',
    params: { range }
  })
}

export function getProductRanking(range, limit = 10) {
  return request({
    url: '/analytics/product-ranking',
    method: 'get',
    params: { range, limit }
  })
}

export function getProfitSummary(range) {
  return request({
    url: '/analytics/profit-summary',
    method: 'get',
    params: { range }
  })
}

export function getReplenishment() {
  return request({
    url: '/analytics/replenishment',
    method: 'get'
  })
}

export function getCustomerRanking(range, limit = 20) {
  return request({
    url: '/analytics/customer-ranking',
    method: 'get',
    params: { range, limit }
  })
}
