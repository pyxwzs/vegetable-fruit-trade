import {
    getInventories,
    inbound as inboundRequest,
    outbound as outboundRequest
} from '@/api/inventory'

export default {
    namespaced: true,

    state: {
        inventories: []
    },

    mutations: {
        SET_INVENTORIES(state, inventories) { state.inventories = inventories }
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
        }
    }
}
