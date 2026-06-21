import request from './request'

export function postPlatformLogin(data) {
    return request({
        url: '/auth/platform/login',
        method: 'post',
        data
    })
}

export function refreshToken(refreshToken) {
    return request({
        url: '/auth/refresh',
        method: 'post',
        data: { refreshToken }
    })
}

export function getCurrentUser() {
    return request({
        url: '/auth/me',
        method: 'get'
    })
}

export function updateMe(data) {
    return request({
        url: '/users/me',
        method: 'put',
        data
    })
}

export function updateMenuKeys(data) {
    return request({
        url: '/auth/me/menus',
        method: 'put',
        data
    })
}
