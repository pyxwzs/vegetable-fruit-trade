import { requireLogin, isMenuEnabled, getUserInfoCache } from '~/utils/auth';

const ALL_MENUS = [
  {
    group: '进销管理',
    accent: '#0052d9',
    items: [
      {
        key: 'purchase',
        title: '采购管理',
        desc: '下单入库',
        icon: 'cart',
        iconColor: '#0052d9',
        iconBg: '#ecf2fe',
        url: '/pages/trade/purchase/index',
      },
      {
        key: 'sales',
        title: '销售管理',
        desc: '出库收款',
        icon: 'shop',
        iconColor: '#2ba471',
        iconBg: '#e3f9e9',
        url: '/pages/trade/sales/index',
      },
    ],
  },
  {
    group: '商品库存',
    accent: '#7b61ff',
    items: [
      {
        key: 'products',
        title: '商品管理',
        desc: '品类价格',
        icon: 'catalog',
        iconColor: '#7b61ff',
        iconBg: '#f2f0ff',
        url: '/pages/trade/products/index',
      },
      {
        key: 'inventory',
        title: '库存管理',
        desc: '盘点调拨',
        icon: 'layers',
        iconColor: '#ed7b2f',
        iconBg: '#fff1e9',
        url: '/pages/trade/inventory/index',
      },
    ],
  },
  {
    group: '往来管理',
    accent: '#2ba471',
    items: [
      {
        key: 'suppliers',
        title: '供应商',
        desc: '档案维护',
        icon: 'usergroup',
        iconColor: '#0052d9',
        iconBg: '#ecf2fe',
        url: '/pages/partner/suppliers/index',
      },
      {
        key: 'supplier-product-metrics',
        title: '供货订量',
        desc: '指标完成',
        icon: 'chart',
        iconColor: '#2ba471',
        iconBg: '#e3f9e9',
        url: '/pages/partner/metrics/index',
      },
      {
        key: 'customers',
        title: '客户管理',
        desc: '档案维护',
        icon: 'user',
        iconColor: '#7b61ff',
        iconBg: '#f2f0ff',
        url: '/pages/partner/customers/index',
      },
      {
        key: 'expenses',
        title: '支出记录',
        desc: '费用登记',
        icon: 'money',
        iconColor: '#e34d59',
        iconBg: '#fff0ed',
        url: '/pages/partner/expenses/index',
      },
    ],
  },
  {
    group: '对账统计',
    accent: '#ed7b2f',
    items: [
      {
        key: 'monthly-report',
        title: '月度对账',
        desc: '收支汇总',
        icon: 'calendar',
        iconColor: '#0052d9',
        iconBg: '#ecf2fe',
        url: '/pages/analysis/monthly/index',
      },
      {
        key: 'partner-product-stats',
        title: '购销统计',
        desc: '往来分析',
        icon: 'chart-bar',
        iconColor: '#ed7b2f',
        iconBg: '#fff1e9',
        url: '/pages/analysis/stats/index',
      },
    ],
  },
];

Page({
  data: {
    menuGroups: [],
    menuCount: 0,
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ value: 'business' });
    }
    if (!requireLogin()) return;
    this.buildMenus();
  },

  buildMenus() {
    const user = getUserInfoCache();
    const menuKeys = user?.menuKeys;
    const menuGroups = ALL_MENUS.map((group) => {
      const items = group.items.filter((item) => isMenuEnabled(menuKeys, item.key));
      if (!items.length) return null;
      return { ...group, items };
    }).filter(Boolean);
    const menuCount = menuGroups.reduce((sum, g) => sum + g.items.length, 0);
    this.setData({ menuGroups, menuCount });
  },

  onMenuTap(e) {
    const { url } = e.currentTarget.dataset;
    if (url) wx.navigateTo({ url });
  },
});
