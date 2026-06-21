import request from './request'

export function getTenantPublic(code) {
    return request({
        url: '/tenants/public',
        method: 'get',
        params: { code }
    })
}
