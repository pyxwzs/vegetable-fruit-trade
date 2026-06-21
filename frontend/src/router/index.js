import { createRouter, createWebHistory } from 'vue-router'
import store from '../api'
import {
    firstEnabledPath,
    isMenuEnabled,
    isPlatformAdminRole,
    pathToMenuKey,
    PLATFORM_ADMIN_HOME
} from '@/config/menu'

const routes = [
    {
        path: '/login',
        name: 'Login',
        component: () => import('../views/Login.vue'),
        meta: { requiresAuth: false }
    },
    {
        path: '/platform/login',
        name: 'PlatformLogin',
        component: () => import('../views/PlatformLogin.vue'),
        meta: { requiresAuth: false, platformPortal: true }
    },
    {
        path: '/platform',
        component: () => import('../layouts/PlatformLayout.vue'),
        meta: { requiresAuth: true, platformPortal: true },
        children: [
            {
                path: '',
                redirect: '/platform/tenants'
            },
            {
                path: 'tenants',
                name: 'PlatformTenants',
                component: () => import('../views/TenantAdmin.vue'),
                meta: { title: '租户管理', platformAdmin: true }
            }
        ]
    },
    {
        path: '/',
        component: () => import('../layouts/MainLayout.vue'),
        meta: { requiresAuth: true, tenantPortal: true },
        children: [
            {
                path: '',
                name: 'Dashboard',
                component: () => import('../views/Dashboard.vue'),
                meta: { title: '仪表盘', menuKey: 'dashboard' }
            },
            {
                path: 'products',
                name: 'Products',
                component: () => import('../views/data/ProductList.vue'),
                meta: { title: '商品管理', menuKey: 'products' }
            },
            {
                path: 'inventory',
                name: 'Inventory',
                component: () => import('../views/data/InventoryList.vue'),
                meta: { title: '库存管理', menuKey: 'inventory' }
            },
            {
                path: 'purchase',
                name: 'Purchase',
                component: () => import('../views/data/PurchaseOrderList.vue'),
                meta: { title: '采购管理', menuKey: 'purchase' }
            },
            {
                path: 'sales',
                name: 'Sales',
                component: () => import('../views/data/SalesOrderList.vue'),
                meta: { title: '销售管理', menuKey: 'sales' }
            },
            {
                path: 'suppliers',
                name: 'Suppliers',
                component: () => import('../views/data/SupplierList.vue'),
                meta: { title: '供应商管理', menuKey: 'suppliers' }
            },
            {
                path: 'supplier-product-metrics',
                name: 'SupplierProductMetrics',
                component: () => import('../views/data/SupplierProductMetricList.vue'),
                meta: { title: '供货订量', menuKey: 'supplier-product-metrics' }
            },
            {
                path: 'customers',
                name: 'Customers',
                component: () => import('../views/data/CustomerList.vue'),
                meta: { title: '客户管理', menuKey: 'customers' }
            },
            {
                path: 'expenses',
                name: 'Expenses',
                component: () => import('../views/data/ExpenseList.vue'),
                meta: { title: '支出记录', menuKey: 'expenses' }
            },
            {
                path: 'monthly-report',
                name: 'MonthlyReport',
                component: () => import('../views/analysis/MonthlyReport.vue'),
                meta: { title: '对账', menuKey: 'monthly-report' }
            },
            {
                path: 'partner-product-stats',
                name: 'PartnerProductStats',
                component: () => import('../views/analysis/PartnerProductStats.vue'),
                meta: { title: '购销统计', menuKey: 'partner-product-stats' }
            },
            {
                path: 'menu-settings',
                name: 'MenuSettings',
                component: () => import('../views/MenuSettings.vue'),
                meta: { title: '菜单设置' }
            },
            {
                path: 'site-settings',
                name: 'SiteSettings',
                component: () => import('../views/SiteSettings.vue'),
                meta: { title: '站点设置' }
            }
        ]
    },
    {
        path: '/tenant-admin',
        redirect: PLATFORM_ADMIN_HOME
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

router.beforeEach(async (to, from, next) => {
    const token = localStorage.getItem('token')
    const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
    const isPlatformPortal = to.matched.some((record) => record.meta.platformPortal)

    if (requiresAuth && !token) {
        next(isPlatformPortal ? '/platform/login' : '/login')
        return
    }

    if (token && requiresAuth && !store.state.user.userInfo) {
        try {
            await store.dispatch('user/getUserInfo')
        } catch {
            /* token 失效由请求拦截器处理 */
        }
    }

    const userInfo = store.state.user.userInfo

    if (token && to.path === '/platform/login' && isPlatformAdminRole(userInfo)) {
        next(PLATFORM_ADMIN_HOME)
        return
    }

    if (token && to.path === '/login') {
        if (isPlatformAdminRole(userInfo)) {
            next(PLATFORM_ADMIN_HOME)
        } else {
            next('/login')
        }
        return
    }

    if (requiresAuth && isPlatformAdminRole(userInfo)) {
        if (!isPlatformPortal) {
            next(PLATFORM_ADMIN_HOME)
            return
        }
    }

    if (requiresAuth && !isPlatformAdminRole(userInfo) && isPlatformPortal) {
        next('/')
        return
    }

    const menuKey = to.meta.menuKey || pathToMenuKey(to.path)
    const menuKeys = userInfo?.menuKeys
    if (requiresAuth && menuKey && menuKeys && !isMenuEnabled(menuKeys, menuKey)) {
        next(firstEnabledPath(menuKeys))
        return
    }

    const siteName = store.state.site.siteName || '果蔬批发'
    const titlePrefix = isPlatformPortal ? '平台管理' : siteName
    document.title = to.meta.title ? `${titlePrefix} - ${to.meta.title}` : `${titlePrefix}管理系统`
    next()
})

export default router
