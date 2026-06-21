export default {
    namespaced: true,
    state: {
        tenantId: localStorage.getItem('tenantId') ? Number(localStorage.getItem('tenantId')) : null,
        tenantCode: localStorage.getItem('tenantCode') || 'default',
        tenantName: localStorage.getItem('tenantName') || ''
    },

    mutations: {
        SET_TENANT(state, payload) {
            if (payload.tenantId != null) {
                state.tenantId = payload.tenantId
                localStorage.setItem('tenantId', String(payload.tenantId))
            }
            if (payload.tenantCode) {
                state.tenantCode = payload.tenantCode
                localStorage.setItem('tenantCode', payload.tenantCode)
            }
            if (payload.tenantName != null) {
                state.tenantName = payload.tenantName
                localStorage.setItem('tenantName', payload.tenantName)
            }
        },
        CLEAR_TENANT(state) {
            state.tenantId = null
            state.tenantName = ''
            localStorage.removeItem('tenantId')
            localStorage.removeItem('tenantName')
        }
    },

    actions: {
        setTenant({ commit }, payload) {
            commit('SET_TENANT', payload)
        },
        clearTenant({ commit }) {
            commit('CLEAR_TENANT')
        }
    }
}
