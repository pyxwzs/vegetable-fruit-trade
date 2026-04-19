import request from './request'

// 获取采购订单列表
export function getPurchaseOrders(params) {
    return request({
        url: '/purchase',
        method: 'get',
        params
    })
}

// 获取单个采购订单
export function getPurchaseOrder(id) {
    return request({
        url: `/purchase/${id}`,
        method: 'get'
    })
}

// 创建采购订单
export function createPurchaseOrder(data) {
    return request({
        url: '/purchase',
        method: 'post',
        data
    })
}

// 审核采购订单
export function approvePurchaseOrder(id) {
    return request({
        url: `/purchase/${id}/approve`,
        method: 'post'
    })
}

// 收货
export function receivePurchaseOrder(id) {
    return request({
        url: `/purchase/${id}/receive`,
        method: 'post'
    })
}

// 取消采购订单
export function cancelPurchaseOrder(id) {
    return request({
        url: `/purchase/${id}/cancel`,
        method: 'post'
    })
}

// 发货（已审核 -> 已发货）
export function shipPurchaseOrder(id) {
    return request({
        url: `/purchase/${id}/ship`,
        method: 'post'
    })
}

// 采购退货（扣减库存）
export function purchaseReturn(id, data) {
    return request({
        url: `/purchase/${id}/return`,
        method: 'post',
        data
    })
}

/** 登记采购付款（财务） */
export function payPurchaseOrder(id, data) {
    return request({
        url: `/purchase/${id}/pay`,
        method: 'post',
        data
    })
}