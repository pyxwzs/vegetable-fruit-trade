import request from './request';
import { downloadExport } from '~/utils/download';

export function getHomeSummary() {
  return request('/analytics/home-summary', 'GET');
}

export function getMonthlyOverview(params) {
  return request('/analytics/monthly-overview', 'GET', params);
}

export function getYearlyBalance(params) {
  return request('/analytics/yearly-balance', 'GET', params);
}

export function getMonthlyPurchase(params) {
  return request('/analytics/monthly-purchase', 'GET', params);
}

export function getMonthlySales(params) {
  return request('/analytics/monthly-sales', 'GET', params);
}

export function getDailyPurchaseDetail(params) {
  return request('/analytics/daily-purchase', 'GET', params);
}

export function getDailySalesDetail(params) {
  return request('/analytics/daily-sales', 'GET', params);
}

export function getPurchaseItems(params) {
  return request('/analytics/purchase-items', 'GET', params);
}

export function getSalesItems(params) {
  return request('/analytics/sales-items', 'GET', params);
}

export function getPurchasePartners(params) {
  return request('/analytics/purchase-partners', 'GET', params);
}

export function getSalesPartners(params) {
  return request('/analytics/sales-partners', 'GET', params);
}

export function getSupplierProductStats(params) {
  return request('/analytics/supplier-product-stats', 'GET', params);
}

export function getCustomerProductStats(params) {
  return request('/analytics/customer-product-stats', 'GET', params);
}

export function exportPurchasePartners(params, filename) {
  return downloadExport('/analytics/export/purchase-partners', params, filename);
}

export function exportSalesPartners(params, filename) {
  return downloadExport('/analytics/export/sales-partners', params, filename);
}

export function exportPurchaseMonth(params, filename) {
  return downloadExport('/analytics/export/purchase-month', params, filename);
}

export function exportSalesMonth(params, filename) {
  return downloadExport('/analytics/export/sales-month', params, filename);
}

export function exportPurchaseYear(params, filename) {
  return downloadExport('/analytics/export/purchase-year', params, filename);
}

export function exportSalesYear(params, filename) {
  return downloadExport('/analytics/export/sales-year', params, filename);
}
