import request from './request'

// 获取销售订单列表
export function getSalesOrders(params) {
    return request({
        url: '/sales',
        method: 'get',
        params
    })
}

// 获取单个销售订单
export function getSalesOrder(id) {
    return request({
        url: `/sales/${id}`,
        method: 'get'
    })
}

// 创建销售订单
export function createSalesOrder(data) {
    return request({
        url: '/sales',
        method: 'post',
        data
    })
}

// 审核销售订单
export function approveSalesOrder(id) {
    return request({
        url: `/sales/${id}/approve`,
        method: 'post'
    })
}

// 发货
export function shipSalesOrder(id, warehouseId) {
    return request({
        url: `/sales/${id}/ship`,
        method: 'post',
        params: { warehouseId }
    })
}

// 取消销售订单
export function cancelSalesOrder(id) {
    return request({
        url: `/sales/${id}/cancel`,
        method: 'post'
    })
}

// 销售退货（回补库存）
export function salesReturn(id, data) {
    return request({
        url: `/sales/${id}/return`,
        method: 'post',
        data
    })
}

/** 登记销售收款（财务） */
export function collectSalesOrder(id, data) {
    return request({
        url: `/sales/${id}/collect`,
        method: 'post',
        data
    })
}