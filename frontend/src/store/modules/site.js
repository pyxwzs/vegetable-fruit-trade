import { getSiteSettings, updateSiteSettings, uploadSiteLogo, updateSiteLogoUrl } from '@/api/siteSettings'
import defaultLogo from '@/assets/logo.png'

const baseURL = process.env.VUE_APP_BASE_API || '/api'

function resolveLogoUrl(logoPath, logoVersion) {
    if (!logoPath) {
        return defaultLogo
    }
    if (/^https?:\/\//i.test(logoPath)) {
        return logoPath
    }
    return `${baseURL}${logoPath}?v=${logoVersion}`
}

function isExternalLogo(logoPath) {
    return logoPath && /^https?:\/\//i.test(logoPath)
}

export default {
    namespaced: true,
    state: {
        siteName: '果蔬批发',
        logoPath: null,
        logoVersion: 0,
        loaded: false
    },

    getters: {
        logoUrl(state) {
            return resolveLogoUrl(state.logoPath, state.logoVersion)
        },
        isExternalLogo: (state) => isExternalLogo(state.logoPath)
    },

    mutations: {
        SET_SETTINGS(state, payload) {
            if (payload.siteName) {
                state.siteName = payload.siteName
            }
            state.logoPath = payload.logoPath || null
            state.loaded = true
        },
        RESET(state) {
            state.siteName = '果蔬批发'
            state.logoPath = null
            state.logoVersion = 0
            state.loaded = false
        },
        BUMP_LOGO(state) {
            state.logoVersion += 1
        }
    },

    actions: {
        async load({ commit }, tenantCode) {
            const res = await getSiteSettings(tenantCode)
            commit('SET_SETTINGS', res.data)
            return res.data
        },

        async updateName({ commit }, siteName) {
            const res = await updateSiteSettings({ siteName })
            commit('SET_SETTINGS', res.data)
            return res.data
        },

        async uploadLogo({ commit }, file) {
            const res = await uploadSiteLogo(file)
            commit('SET_SETTINGS', res.data)
            commit('BUMP_LOGO')
            return res.data
        },

        async updateLogoUrl({ commit }, logoUrl) {
            const res = await updateSiteLogoUrl(logoUrl)
            commit('SET_SETTINGS', res.data)
            commit('BUMP_LOGO')
            return res.data
        },

        async clearLogo({ dispatch }) {
            return dispatch('updateLogoUrl', '')
        },

        reset({ commit }) {
            commit('RESET')
        }
    }
}
