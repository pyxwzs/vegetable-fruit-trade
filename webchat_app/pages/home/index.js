import { getMonthlyOverview } from '~/api/analytics';
import { getInventoryOverview } from '~/api/inventory';
import { getPurchaseOrders, getPurchasePendingStats } from '~/api/purchase';
import { getSalesOrders, getSalesPendingStats } from '~/api/sales';
import { requireLogin } from '~/utils/auth';
import { currentMonthLabel, fmtDate, fmtMoney } from '~/utils/format';

const SHORTCUTS = [
  { key: 'purchase', title: '采购', icon: 'cart', color: '#0052d9', bg: '#ecf2fe', url: '/pages/trade/purchase/index' },
  { key: 'sales', title: '销售', icon: 'shop', color: '#2ba471', bg: '#e3f9e9', url: '/pages/trade/sales/index' },
  { key: 'monthly', title: '对账', icon: 'calendar', color: '#ed7b2f', bg: '#fff1e9', url: '/pages/analysis/monthly/index' },
  { key: 'expenses', title: '支出', icon: 'money', color: '#e34d59', bg: '#fff0ed', url: '/pages/partner/expenses/index' },
];

function mapPendingPurchase(row) {
  return {
    id: row.id,
    name: row.supplier?.name || '—',
    date: fmtDate(row.orderDate),
    amountText: fmtMoney(row.totalAmount),
  };
}

function mapPendingSales(row) {
  return {
    id: row.id,
    name: row.customer?.name || '—',
    date: fmtDate(row.orderDate),
    amountText: fmtMoney(row.totalAmount),
  };
}

function mapRanking(list, limit = 3) {
  return (list || [])
    .filter((r) => Number(r.unpaid) > 0)
    .slice(0, limit)
    .map((r) => ({
      name: r.name || '—',
      amountText: fmtMoney(r.unpaid),
    }));
}

Page({
  data: {
    loading: true,
    monthLabel: currentMonthLabel(),
    overview: {},
    bookProfit: 0,
    cashProfit: 0,
    profitGap: 0,
    inventoryValue: 0,
    purchasePending: { count: 0, totalAmount: 0 },
    salesPending: { count: 0, totalAmount: 0 },
    pendingPurchases: [],
    pendingSales: [],
    supplierRanking: [],
    customerRanking: [],
    shortcuts: SHORTCUTS,
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ value: 'home' });
    }
    if (!requireLogin()) return;
    this.loadData();
  },

  onPullDownRefresh() {
    this.loadData().finally(() => wx.stopPullDownRefresh());
  },

  async loadData() {
    this.setData({ loading: true });
    const now = new Date();
    const ym = { year: now.getFullYear(), month: now.getMonth() + 1 };
    try {
      const [overviewRes, invRes, ppStats, spStats, purchaseRes, salesRes] = await Promise.all([
        getMonthlyOverview(ym),
        getInventoryOverview(),
        getPurchasePendingStats(),
        getSalesPendingStats(),
        getPurchaseOrders({ status: 'PENDING', page: 0, size: 3 }),
        getSalesOrders({ status: 'PENDING', page: 0, size: 3 }),
      ]);

      const o = overviewRes.data || {};
      const bookProfit =
        o.netProfit != null
          ? Number(o.netProfit)
          : Number(o.salesTotal || 0) - Number(o.purchaseTotal || 0) - Number(o.otherExpenses || 0);
      const cashProfit =
        Number(o.collectedFromCustomers || 0) -
        Number(o.paidToFarmers || 0) -
        Number(o.otherExpenses || 0);

      const purchaseContent = purchaseRes.data?.content || purchaseRes.data?.records || [];
      const salesContent = salesRes.data?.content || salesRes.data?.records || [];

      this.setData({
        overview: o,
        bookProfit,
        cashProfit,
        profitGap: bookProfit - cashProfit,
        inventoryValue: invRes.data?.totalValue || 0,
        purchasePending: ppStats.data || { count: 0, totalAmount: 0 },
        salesPending: spStats.data || { count: 0, totalAmount: 0 },
        pendingPurchases: purchaseContent.map(mapPendingPurchase),
        pendingSales: salesContent.map(mapPendingSales),
        supplierRanking: mapRanking(o.farmerUnpaidRanking),
        customerRanking: mapRanking(o.customerUnreceivedRanking),
        loading: false,
      });
    } catch (e) {
      this.setData({ loading: false });
      wx.showToast({ title: e.message || '加载失败', icon: 'none' });
    }
  },

  goUrl(e) {
    const { url } = e.currentTarget.dataset;
    if (url) wx.navigateTo({ url });
  },

  goPurchaseUnpaid() {
    wx.navigateTo({ url: '/pages/trade/purchase/index?tab=UNPAID' });
  },

  goPurchasePending() {
    wx.navigateTo({ url: '/pages/trade/purchase/index?tab=PENDING' });
  },

  goSalesUnpaid() {
    wx.navigateTo({ url: '/pages/trade/sales/index?tab=UNPAID' });
  },

  goSalesPending() {
    wx.navigateTo({ url: '/pages/trade/sales/index?tab=PENDING' });
  },

  goInventory() {
    wx.navigateTo({ url: '/pages/trade/inventory/index' });
  },
});
