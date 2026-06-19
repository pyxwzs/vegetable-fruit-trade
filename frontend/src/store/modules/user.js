import { postLogin, getCurrentUser } from '@/api/user'

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
        async loginStep1({ commit, dispatch }, { username, password, rememberMe }) {
            const response = await postLogin({ username, password, rememberMe })
            const d = response.data
            commit('SET_TOKEN', d.token)
            if (d.refreshToken) {
                localStorage.setItem('refreshToken', d.refreshToken)
            } else {
                localStorage.removeItem('refreshToken')
            }
            await dispatch('getUserInfo')
            return { needMfa: false }
        },

        async getUserInfo({ commit }) {
            const response = await getCurrentUser()
            const userInfo = response.data
            commit('SET_USER_INFO', userInfo)
            return userInfo
        },

        logout({ commit }) {
            commit('CLEAR_USER')
        }
    },

    getters: {
        isLoggedIn: state => !!state.token,
        hasPermission: () => () => true
    }
}
