import request from './request'

// 获取公告列表
export function getAnnouncements(params) {
    return request({
        url: '/announcements',
        method: 'get',
        params
    })
}

// 获取置顶公告
export function getTopAnnouncements() {
    return request({
        url: '/announcements/top',
        method: 'get'
    })
}

// 创建公告
export function createAnnouncement(data) {
    return request({
        url: '/announcements',
        method: 'post',
        data
    })
}

// 删除公告
export function deleteAnnouncement(id) {
    return request({
        url: `/announcements/${id}`,
        method: 'delete'
    })
}

// 标记通知为已读
export function markAsRead(id) {
    return request({
        url: `/announcements/notifications/${id}/read`,
        method: 'post'
    })
}

// 获取公告详情
export function getAnnouncement(id) {
    return request({
        url: `/announcements/${id}`,
        method: 'get'
    })
}

// 更新公告
export function updateAnnouncement(id, data) {
    return request({
        url: `/announcements/${id}`,
        method: 'put',
        data
    })
}