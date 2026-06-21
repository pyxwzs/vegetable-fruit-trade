import request from './request';

export function getInventoryList(params) {
  return request('/inventory', 'GET', params);
}

export function getInventoryOverview() {
  return request('/inventory/overview', 'GET');
}
