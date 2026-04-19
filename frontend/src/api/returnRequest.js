import request from './request'

export function getReturnRequests(params) {
    return request({ url: '/return-requests', method: 'get', params })
}

export function warehouseApprove(id) {
    return request({ url: `/return-requests/${id}/warehouse-approve`, method: 'post' })
}

export function warehouseReject(id, data) {
    return request({ url: `/return-requests/${id}/warehouse-reject`, method: 'post', data })
}

export function financeApprove(id) {
    return request({ url: `/return-requests/${id}/finance-approve`, method: 'post' })
}

export function financeReject(id, data) {
    return request({ url: `/return-requests/${id}/finance-reject`, method: 'post', data })
}
