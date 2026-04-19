import {
    getInventories,
    inbound as inboundRequest,
    outbound as outboundRequest,
    getExpiringProducts,
    getLowStockProducts
} from '@/api/inventory'

export default {
    namespaced: true,

    state: {
        inventories: [],
        expiringProducts: [],
        lowStockProducts: []
    },

    mutations: {
        SET_INVENTORIES(state, inventories) {
            state.inventories = inventories
        },
        SET_EXPIRING_PRODUCTS(state, products) {
            state.expiringProducts = products
        },
        SET_LOW_STOCK_PRODUCTS(state, products) {
            state.lowStockProducts = products
        }
    },

    actions: {
        async fetchInventories({ commit }, params) {
            const response = await getInventories(params)
            commit('SET_INVENTORIES', response.data.content)
            return response.data
        },

        async inbound(context, data) {
            void context
            const response = await inboundRequest(data)
            return response.data
        },

        async outbound(context, data) {
            void context
            const response = await outboundRequest(data)
            return response.data
        },

        async fetchExpiringProducts({ commit }) {
            const response = await getExpiringProducts()
            commit('SET_EXPIRING_PRODUCTS', response.data)
            return response.data
        },

        async fetchLowStockProducts({ commit }) {
            const response = await getLowStockProducts()
            commit('SET_LOW_STOCK_PRODUCTS', response.data)
            return response.data
        }
    }
}