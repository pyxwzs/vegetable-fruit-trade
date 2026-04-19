import request from './request'

export function getOperationLogs(params) {
  return request({
    url: '/operation-logs',
    method: 'get',
    params
  })
}
