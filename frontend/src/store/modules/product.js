import { getProducts, getAllEnabledProducts, getProduct, createProduct, updateProduct, deleteProduct } from '@/api/product'

export default {
    namespaced: true,

    state: {
        products: [],
        currentProduct: null,
        enabledProducts: []
    },

    mutations: {
        SET_PRODUCTS(state, products) {
            state.products = products
        },
        SET_CURRENT_PRODUCT(state, product) {
            state.currentProduct = product
        },
        SET_ENABLED_PRODUCTS(state, products) {
            state.enabledProducts = products
        }
    },

    actions: {
        async fetchProducts({ commit }, params) {
            const response = await getProducts(params)
            commit('SET_PRODUCTS', response.data.content)
            return response.data
        },

        async fetchEnabledProducts({ commit }) {
            const response = await getAllEnabledProducts()
            commit('SET_ENABLED_PRODUCTS', response.data)
            return response.data
        },

        async fetchProduct({ commit }, id) {
            const response = await getProduct(id)
            commit('SET_CURRENT_PRODUCT', response.data)
            return response.data
        },

        async createProduct(_, data) {
            const response = await createProduct(data)
            return response.data
        },

        async updateProduct(_, { id, data }) {
            const response = await updateProduct(id, data)
            return response.data
        },

        async deleteProduct(_, id) {
            await deleteProduct(id)
        }
    }
}