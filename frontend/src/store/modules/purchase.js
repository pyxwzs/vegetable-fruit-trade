import { getPurchaseOrders, getPurchaseOrder, createPurchaseOrder, approvePurchaseOrder, receivePurchaseOrder } from '@/api/purchase'

export default {
    namespaced: true,

    state: {
        orders: [],
        currentOrder: null
    },

    mutations: {
        SET_ORDERS(state, orders) {
            state.orders = orders
        },
        SET_CURRENT_ORDER(state, order) {
            state.currentOrder = order
        }
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

        async approveOrder(_, id) {
            const response = await approvePurchaseOrder(id)
            return response.data
        },

        async receiveOrder(_, id) {
            const response = await receivePurchaseOrder(id)
            return response.data
        }
    }
}