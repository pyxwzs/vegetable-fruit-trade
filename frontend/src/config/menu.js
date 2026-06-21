import {
  Odometer, ShoppingCart, Box, UserFilled, Calendar
} from '@element-plus/icons-vue'

/** 全部可配置菜单 key（与后端 MenuKeyConstants 保持一致） */
export const ALL_MENU_KEYS = [
  'dashboard',
  'purchase',
  'sales',
  'products',
  'inventory',
  'suppliers',
  'supplier-product-metrics',
  'customers',
  'expenses',
  'monthly-report',
  'partner-product-stats'
]

export const MENU_GROUPS = [
  {
    key: 'dashboard',
    type: 'item',
    menuKey: 'dashboard',
    path: '/',
    title: '仪表盘',
    icon: Odometer
  },
  {
    key: 'trade',
    type: 'group',
    title: '进销管理',
    icon: ShoppingCart,
    children: [
      { menuKey: 'purchase', path: '/purchase', title: '采购管理' },
      { menuKey: 'sales', path: '/sales', title: '销售管理' }
    ]
  },
  {
    key: 'stock',
    type: 'group',
    title: '商品库存',
    icon: Box,
    children: [
      { menuKey: 'products', path: '/products', title: '商品管理' },
      { menuKey: 'inventory', path: '/inventory', title: '库存查看' }
    ]
  },
  {
    key: 'partner',
    type: 'group',
    title: '往来管理',
    icon: UserFilled,
    children: [
      { menuKey: 'suppliers', path: '/suppliers', title: '供应商管理' },
      { menuKey: 'supplier-product-metrics', path: '/supplier-product-metrics', title: '供货订量' },
      { menuKey: 'customers', path: '/customers', title: '客户管理' },
      { menuKey: 'expenses', path: '/expenses', title: '支出记录' }
    ]
  },
  {
    key: 'reconcile',
    type: 'group',
    title: '月度对账',
    icon: Calendar,
    children: [
      { menuKey: 'monthly-report', path: '/monthly-report', title: '供应商/客户对账' },
      { menuKey: 'partner-product-stats', path: '/partner-product-stats', title: '购销统计' }
    ]
  }
]

/** 菜单设置树（父级可整组勾选/禁用，子级可单独勾选/禁用） */
export const MENU_SETTING_TREE = MENU_GROUPS.map(group => {
  if (group.type === 'item') {
    return { id: group.menuKey, label: group.title }
  }
  return {
    id: `group:${group.key}`,
    label: group.title,
    children: group.children.map(c => ({
      id: c.menuKey,
      label: c.title
    }))
  }
})

export function isMenuEnabled(menuKeys, key) {
  if (!menuKeys || menuKeys.length === 0) return true
  return menuKeys.includes(key)
}

export function filterMenuGroups(menuKeys) {
  return MENU_GROUPS.map(group => {
    if (group.type === 'item') {
      return isMenuEnabled(menuKeys, group.menuKey) ? group : null
    }
    const children = group.children.filter(c => isMenuEnabled(menuKeys, c.menuKey))
    if (children.length === 0) return null
    return { ...group, children }
  }).filter(Boolean)
}

export function firstEnabledPath(menuKeys) {
  for (const group of filterMenuGroups(menuKeys)) {
    if (group.type === 'item') return group.path
    if (group.children?.length) return group.children[0].path
  }
  return '/'
}

export function pathToMenuKey(path) {
  if (path === '/' || path === '') return 'dashboard'
  const segment = path.replace(/^\//, '')
  return ALL_MENU_KEYS.includes(segment) ? segment : null
}

/** 平台管理员 Web 管理后台首页 */
export const PLATFORM_ADMIN_HOME = '/platform/tenants'

export function defaultHomePath(userInfo) {
  return userInfo?.role === 'PLATFORM_ADMIN' ? PLATFORM_ADMIN_HOME : '/'
}

export function isPlatformAdminRole(userInfo) {
  return userInfo?.role === 'PLATFORM_ADMIN'
}
