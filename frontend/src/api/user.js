import request from './request'

export function postLogin(data) {
    return request({
        url: '/auth/login',
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

export function changePassword(data) {
    return request({
        url: '/auth/me/password',
        method: 'put',
        data
    })
}

export function updateMe(data) {
    return request({
        url: '/users/me',
        method: 'put',
        data
    })
}
