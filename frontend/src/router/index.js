import { createRouter, createWebHistory } from 'vue-router'
import store from '../api'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/',
        component: () => import('../layouts/MainLayout.vue'),
        meta: { requiresAuth: true },
        children: [
            {
                path: '',
                name: 'Dashboard',
                component: () => import('../views/Dashboard.vue'),
                meta: { title: '仪表盘' }
            },
            {
                path: 'data-management',
                name: 'DataManagement',
                component: () => import('../views/data/DataManagement.vue'),
                meta: { title: '数据管理' },
                children: [
                    {
                        path: 'products',
                        name: 'Products',
                        component: () => import('../views/data/ProductList.vue'),
                        meta: { title: '商品管理' }
                    },
                    {
                        path: 'inventory',
                        name: 'Inventory',
                        component: () => import('../views/data/InventoryList.vue'),
                        meta: { title: '库存管理' }
                    },
                    {
                        path: 'purchase',
                        name: 'Purchase',
                        component: () => import('../views/data/PurchaseOrderList.vue'),
                        meta: { title: '采购管理' }
                    },
                    {
                        path: 'sales',
                        name: 'Sales',
                        component: () => import('../views/data/SalesOrderList.vue'),
                        meta: { title: '销售管理' }
                    },
                    {
                        path: 'suppliers',
                        name: 'Suppliers',
                        component: () => import('../views/data/SupplierList.vue'),
                        meta: { title: '供应商管理' }
                    },
                    {
                        path: 'customers',
                        name: 'Customers',
                        component: () => import('../views/data/CustomerList.vue'),
                        meta: { title: '客户管理' }
                    }
                ]
            },
            {
                path: 'analysis',
                name: 'Analysis',
                component: () => import('../views/analysis/AnalysisDashboard.vue'),
                meta: { title: '经营分析' }
            },
            {
                path: 'profile',
                name: 'Profile',
                component: () => import('../views/Profile.vue'),
                meta: { title: '个人资料' }
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach(async (to, from, next) => {
    const token = localStorage.getItem('token')
    const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)

    if (requiresAuth && !token) {
        next('/login')
        return
    }
    if (to.path === '/login' && token) {
        next('/')
        return
    }
    if (token && requiresAuth && !store.state.user.userInfo) {
        try {
            await store.dispatch('user/getUserInfo')
        } catch {
            /* token 失效由请求拦截器处理 */
        }
    }
    document.title = to.meta.title ? `果蔬批发 - ${to.meta.title}` : '果蔬批发管理系统'
    next()
})

export default router
