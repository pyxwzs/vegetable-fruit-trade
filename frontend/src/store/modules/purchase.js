import { getPurchaseOrders, getPurchaseOrder, createPurchaseOrder, completePurchaseOrder, cancelPurchaseOrder, payPurchaseOrder } from '@/api/purchase'

export default {
    namespaced: true,

    state: {
        orders: [],
        currentOrder: null
    },

    mutations: {
        SET_ORDERS(state, orders) { state.orders = orders },
        SET_CURRENT_ORDER(state, order) { state.currentOrder = order }
    },

    actions: {
        async fetchOrders({ commit }, params) {
            const response = await getPurchaseOrders(params)
            commit('SET_ORDERS', response.data.content)
            return response.data
        },

        async fetchOrder({ commit }, id) {
            const response = await getPurchaseOrder(id)
            commit('SET_CURRENT_ORDER', response.data)
            return response.data
        },

        async createOrder(_, data) {
            const response = await createPurchaseOrder(data)
            return response.data
        },

        async completeOrder(_, id) {
            const response = await completePurchaseOrder(id)
            return response.data
        },

        async cancelOrder(_, id) {
            const response = await cancelPurchaseOrder(id)
            return response.data
        },

        async payOrder(_, { id, data }) {
            const response = await payPurchaseOrder(id, data)
            return response.data
        }
    }
}
