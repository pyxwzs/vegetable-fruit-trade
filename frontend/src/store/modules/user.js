import { postPlatformLogin, getCurrentUser } from '@/api/user'

export default {
    namespaced: true,
    state: {
        token: localStorage.getItem('token') || '',
        userInfo: null
    },

    mutations: {
        SET_TOKEN(state, token) {
            state.token = token
            if (token) {
                localStorage.setItem('token', token)
            } else {
                localStorage.removeItem('token')
            }
        },
        SET_USER_INFO(state, userInfo) {
            state.userInfo = userInfo
        },
        CLEAR_USER(state) {
            state.token = ''
            state.userInfo = null
            localStorage.removeItem('token')
            localStorage.removeItem('refreshToken')
        }
    },

    actions: {
        async loginPlatform({ commit, dispatch }, { account, password, rememberMe }) {
            const response = await postPlatformLogin({ account, password, rememberMe })
            const d = response.data
            commit('SET_TOKEN', d.token)
            if (d.refreshToken) {
                localStorage.setItem('refreshToken', d.refreshToken)
            } else {
                localStorage.removeItem('refreshToken')
            }
            await dispatch('tenant/setTenant', {
                tenantId: d.tenantId,
                tenantCode: d.tenantCode,
                tenantName: d.tenantName
            }, { root: true })
            await dispatch('site/load', d.tenantCode, { root: true })
            await dispatch('getUserInfo')
        },

        async getUserInfo({ commit }) {
            const response = await getCurrentUser()
            const userInfo = response.data
            commit('SET_USER_INFO', userInfo)
            return userInfo
        },

        logout({ commit, dispatch }) {
            commit('CLEAR_USER')
            dispatch('tenant/clearTenant', null, { root: true })
            dispatch('site/reset', null, { root: true })
        }
    },

    getters: {
        isLoggedIn: state => !!state.token,
        menuKeys: state => state.userInfo?.menuKeys || null,
        isMenuEnabled: state => (key) => {
            const keys = state.userInfo?.menuKeys
            if (!keys || keys.length === 0) return true
            return keys.includes(key)
        },
        isPlatformAdmin: state => state.userInfo?.role === 'PLATFORM_ADMIN'
    }
}
