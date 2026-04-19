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
        path: '/forgot-password',
        name: 'ForgotPassword',
        component: () => import('../views/ForgotPassword.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/register',
        name: 'Register',
        component: () => import('../views/Register.vue'),
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
                path: 'announcements',
                name: 'Announcements',
                component: () => import('../views/announcement/AnnouncementList.vue'),
                meta: { title: '公告通知' }
            },
            {
                path: 'announcements/create',
                name: 'CreateAnnouncement',
                component: () => import('../views/announcement/AnnouncementForm.vue'),
                meta: { title: '发布公告' }
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
                    },
                    {
                        path: 'return-requests',
                        name: 'ReturnRequests',
                        component: () => import('../views/data/ReturnRequestList.vue'),
                        meta: { title: '退货申请' }
                    },
                    {
                        path: 'finance-summary',
                        name: 'FinanceSummary',
                        component: () => import('../views/finance/FinanceSummary.vue'),
                        meta: { title: '资金汇总' }
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
            },
            {
                path: 'system',
                name: 'System',
                component: () => import('../views/system/SystemManagement.vue'),
                meta: { title: '系统管理' },
                children: [
                    {
                        path: 'users',
                        name: 'Users',
                        component: () => import('../views/system/UserList.vue'),
                        meta: { title: '用户管理' }
                    },
                    {
                        path: 'logs',
                        name: 'Logs',
                        component: () => import('../views/system/LogList.vue'),
                        meta: { title: '日志审计' }
                    },
                    {
                        path: 'files',
                        name: 'Files',
                        component: () => import('../views/system/FileList.vue'),
                        meta: { title: '文件管理' }
                    }
                ]
            }
        ]
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫：有 token 时拉取当前用户与权限（多角色权限在 Vuex 中合并为并集）
router.beforeEach(async (to, from, next) => {
    const token = localStorage.getItem('token')
    const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)

    if (requiresAuth && !token) {
        next('/login')
        return
    }
    if ((to.path === '/login' || to.path === '/forgot-password' || to.path === '/register') && token) {
        next('/')
        return
    }
    if (token && requiresAuth && !store.state.user.userInfo) {
        try {
            await store.dispatch('user/getUserInfo')
        } catch {
            /* token 失效等由请求拦截器处理 */
        }
    }
    document.title = to.meta.title ? `果蔬批发 - ${to.meta.title}` : '果蔬批发管理系统'
    next()
})

export default router