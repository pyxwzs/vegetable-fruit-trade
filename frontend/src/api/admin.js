import request from './request'

export function listTenants() {
    return request({ url: '/admin/tenants', method: 'get' })
}

export function updateTenantStatus(id, status) {
    return request({
        url: `/admin/tenants/${id}/status`,
        method: 'put',
        data: { status }
    })
}

export function listInviteCodes() {
    return request({ url: '/admin/invite-codes', method: 'get' })
}

export function generateInviteCode(remark) {
    return request({
        url: '/admin/invite-codes',
        method: 'post',
        data: remark ? { remark } : {}
    })
}
