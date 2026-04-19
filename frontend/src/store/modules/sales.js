import { getSalesOrders, getSalesOrder, createSalesOrder, approveSalesOrder, shipSalesOrder } from '@/api/sales'

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
            const response = await getSalesOrders(params)
            commit('SET_ORDERS', response.data.content)
            return response.data
        },

        async fetchOrder({ commit }, id) {
            const response = await getSalesOrder(id)
            commit('SET_CURRENT_ORDER', response.data)
            return response.data
        },

        async createOrder(_, data) {
            const response = await createSalesOrder(data)
            return response.data
        },

        async approveOrder(_, id) {
            const response = await approveSalesOrder(id)
            return response.data
        },

        async shipOrder(_, { id, warehouseId }) {
            const response = await shipSalesOrder(id, warehouseId ?? 1)
            return response.data
        }
    }
}