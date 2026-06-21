import request from './request'

export function getSiteSettings(tenantCode) {
    return request({
        url: '/site-settings',
        method: 'get',
        params: tenantCode ? { tenantCode } : undefined
    })
}

export function updateSiteSettings(data) {
    return request({
        url: '/site-settings',
        method: 'put',
        data
    })
}

export function uploadSiteLogo(file) {
    const form = new FormData()
    form.append('file', file)
    return request({
        url: '/site-settings/logo',
        method: 'post',
        data: form,
        headers: { 'Content-Type': 'multipart/form-data' }
    })
}

export function updateSiteLogoUrl(logoUrl) {
    return request({
        url: '/site-settings/logo-url',
        method: 'put',
        data: { logoUrl }
    })
}
