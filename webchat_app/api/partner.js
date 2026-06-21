import request from './request';

export function getSuppliers(params) {
  return request('/suppliers', 'GET', params);
}

export function getActiveSuppliers() {
  return request('/suppliers/active', 'GET');
}

export function getSupplier(id) {
  return request(`/suppliers/${id}`, 'GET');
}

export function createSupplier(data) {
  return request('/suppliers', 'POST', data);
}

export function updateSupplier(id, data) {
  return request(`/suppliers/${id}`, 'PUT', data);
}

export function deleteSupplier(id) {
  return request(`/suppliers/${id}`, 'DELETE');
}

export function getCustomers(params) {
  return request('/customers', 'GET', params);
}

export function getActiveCustomers() {
  return request('/customers/active', 'GET');
}

export function getCustomer(id) {
  return request(`/customers/${id}`, 'GET');
}

export function createCustomer(data) {
  return request('/customers', 'POST', data);
}

export function updateCustomer(id, data) {
  return request(`/customers/${id}`, 'PUT', data);
}

export function deleteCustomer(id) {
  return request(`/customers/${id}`, 'DELETE');
}

export function getExpenses(params) {
  return request('/expenses', 'GET', params);
}

export function getExpenseYearTotal(params) {
  return request('/expenses/year-total', 'GET', params);
}

export function createExpense(data) {
  return request('/expenses', 'POST', data);
}

export function updateExpense(id, data) {
  return request(`/expenses/${id}`, 'PUT', data);
}

export function deleteExpense(id) {
  return request(`/expenses/${id}`, 'DELETE');
}

export function getExpenseCategories() {
  return request('/expenses/categories', 'GET');
}

export function getSupplierProductMetrics(params) {
  return request('/supplier-product-metrics', 'GET', params);
}

export function getSupplierMetricCompletion(params) {
  return request('/supplier-product-metrics/completion', 'GET', params);
}

export function createSupplierProductMetric(data) {
  return request('/supplier-product-metrics', 'POST', data);
}

export function updateSupplierProductMetric(id, data) {
  return request(`/supplier-product-metrics/${id}`, 'PUT', data);
}

export function deleteSupplierProductMetric(id) {
  return request(`/supplier-product-metrics/${id}`, 'DELETE');
}
