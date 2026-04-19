import { loginMfaVerify, postLogin, getCurrentUser } from '@/api/user'

export default {
    namespaced: true,
    state: {
        token: localStorage.getItem('token') || '',
        userInfo: null,
        permissions: []
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
        SET_PERMISSIONS(state, permissions) {
            state.permissions = permissions
        },
        CLEAR_USER(state) {
            state.token = ''
            state.userInfo = null
            state.permissions = []
            localStorage.removeItem('token')
            localStorage.removeItem('refreshToken')
        }
    },

    actions: {
        /**
         * 登录第一步：提交账号密码。若需 MFA 则返回 needMfa；否则已写入 token 并拉取用户信息。
         */
        async loginStep1({ commit, dispatch }, { username, password, rememberMe }) {
            const response = await postLogin({ username, password, rememberMe })
            const d = response.data
            if (d.mfaRequired) {
                return {
                    needMfa: true,
                    sessionId: d.sessionId,
                    maskedEmail: d.maskedEmail || ''
                }
            }
            commit('SET_TOKEN', d.token)
            if (d.refreshToken) {
                localStorage.setItem('refreshToken', d.refreshToken)
            } else {
                localStorage.removeItem('refreshToken')
            }
            await dispatch('getUserInfo')
            return { needMfa: false }
        },

        /**
         * 登录第二步：提交邮箱验证码后换取 token
         */
        async login({ commit, dispatch }, { sessionId, code }) {
            const response = await loginMfaVerify({ sessionId, code })
            const payload = response.data
            const token = payload.token
            commit('SET_TOKEN', token)
            if (payload.refreshToken) {
                localStorage.setItem('refreshToken', payload.refreshToken)
            } else {
                localStorage.removeItem('refreshToken')
            }
            await dispatch('getUserInfo')
            return response
        },

        async getUserInfo({ commit }) {
            const response = await getCurrentUser()
            const userInfo = response.data
            commit('SET_USER_INFO', userInfo)
            if (userInfo.roles) {
                const permissions = userInfo.roles.flatMap(role =>
                    role.permissions ? role.permissions.map(p => p.code) : []
                )
                commit('SET_PERMISSIONS', [...new Set(permissions)])
            }
            return userInfo
        },

        logout({ commit }) {
            commit('CLEAR_USER')
        }
    },

    getters: {
        isLoggedIn: state => !!state.token,
        /** 含 ALL（仅系统管理员脚本/bootstrap 角色）时视为拥有全部权限码 */
        hasPermission: state => permission =>
            state.permissions.includes('ALL') || state.permissions.includes(permission),
        userRoles: state => state.userInfo?.roles?.map(r => r.name) || []
    }
}
