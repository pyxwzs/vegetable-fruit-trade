import { getAnnouncements, getTopAnnouncements, createAnnouncement, markAsRead } from '@/api/announcement'

export default {
    namespaced: true,

    state: {
        announcements: [],
        topAnnouncements: [],
        unreadCount: 0
    },

    mutations: {
        SET_ANNOUNCEMENTS(state, announcements) {
            state.announcements = announcements
        },
        SET_TOP_ANNOUNCEMENTS(state, announcements) {
            state.topAnnouncements = announcements
        },
        SET_UNREAD_COUNT(state, count) {
            state.unreadCount = count
        }
    },

    actions: {
        async fetchAnnouncements({ commit }, params) {
            const response = await getAnnouncements(params)
            commit('SET_ANNOUNCEMENTS', response.data.content)
            return response.data
        },

        async fetchTopAnnouncements({ commit }) {
            const response = await getTopAnnouncements()
            commit('SET_TOP_ANNOUNCEMENTS', response.data)
            return response.data
        },

        async createAnnouncement(_, data) {
            const response = await createAnnouncement(data)
            return response.data
        },

        async markNotificationAsRead(_, id) {
            await markAsRead(id)
        }
    }
}