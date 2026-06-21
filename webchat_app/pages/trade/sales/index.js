import {
  getSalesOrders,
  getSalesPendingStats,
  completeSalesOrder,
  collectSalesOrder,
  cancelSalesOrder,
} from '~/api/sales';
import { getYearlyBalance } from '~/api/analytics';
import { requireLogin } from '~/utils/auth';
import { fmtMoney, fmtDate } from '~/utils/format';
import {
  defaultDateState,
  applyDateQuery,
  filterStatePatch,
  periodPrefix,
  createDateQueryHandlers,
} from '~/utils/dateQuery';
import {
  SALES_STATUS,
  paymentStatusLabel,
  salesStatusTheme,
  paymentStatusTheme,
  calcUnpaid,
} from '~/utils/order';

const FILTERS = [
  { label: '全部', value: 'ALL' },
  { label: '结清', value: 'PAID' },
  { label: '未结清', value: 'UNPAID' },
  { label: '出库', value: 'COMPLETED' },
  { label: '未出库', value: 'PENDING' },
];

const PAGE_SIZES = [5, 10, 20];

function calcTotalPages(total, pageSize) {
  return Math.max(1, Math.ceil(Number(total || 0) / pageSize));
}

function applyFilter(params, filterKey) {
  if (filterKey === 'UNPAID') {
    params.paymentStatus = 'UNPAID,PARTIAL';
  } else if (filterKey === 'PAID') {
    params.paymentStatus = 'PAID';
  } else if (filterKey === 'COMPLETED' || filterKey === 'PENDING') {
    params.status = filterKey;
  }
}

function mapOrder(row) {
  const unpaid = calcUnpaid(row.totalAmount, row.receivedAmount);
  return {
    id: row.id,
    orderNo: row.orderNo || `#${row.id}`,
    customerName: row.customer?.name || '—',
    orderDate: fmtDate(row.orderDate),
    totalAmount: row.totalAmount,
    totalAmountText: fmtMoney(row.totalAmount),
    receivedAmountText: fmtMoney(row.receivedAmount),
    unpaid,
    unpaidText: fmtMoney(unpaid),
    status: row.status,
    statusText: SALES_STATUS[row.status] || row.status,
    statusTheme: salesStatusTheme(row.status),
    paymentStatus: row.paymentStatus,
    paymentText: paymentStatusLabel(row.paymentStatus),
    paymentTheme: paymentStatusTheme(row.paymentStatus),
    canComplete: row.status === 'PENDING',
    canCollect: row.status !== 'CANCELLED' && row.paymentStatus !== 'PAID',
    canCancel: row.status === 'PENDING',
  };
}

Page({
  data: {
    list: [],
    loading: false,
    filters: FILTERS,
    filterKey: 'ALL',
    filterIndex: 0,
    keyword: '',
    pendingStats: { count: 0, totalAmount: 0 },
    pendingAmountText: '0.00',
    periodAmountText: '0.00',
    periodCount: 0,
    periodPrefix: '当日',
    unpaidTotalText: '0.00',
    unpaidTotal: 0,
    ...defaultDateState(),
    page: 1,
    pageSize: 5,
    pageSizeOptions: PAGE_SIZES,
    pageSizeIndex: 0,
    total: 0,
    totalPages: 1,
  },

  onLoad(options) {
    if (options.tab) {
      const idx = FILTERS.findIndex((f) => f.value === options.tab);
      if (idx >= 0) {
        this.setData({ ...filterStatePatch(options.tab, idx) });
      }
    }
  },

  onShow() {
    if (!requireLogin()) return;
    this.loadGlobalBalance();
    this.loadSummary();
    this.loadList();
  },

  onPullDownRefresh() {
    Promise.all([this.loadGlobalBalance(), this.loadSummary(), this.loadList()]).finally(() => wx.stopPullDownRefresh());
  },

  ...createDateQueryHandlers(function reloadDateQuery() {
    this.loadSummary();
    this.loadList();
  }),

  onFilterChange(e) {
    const index = Number(e.detail.value);
    const item = FILTERS[index] || FILTERS[0];
    this.setData({ ...filterStatePatch(item.value, index), page: 1 }, () => this.loadList());
  },

  onSearchInput(e) {
    this.setData({ keyword: (e.detail.value || '').trim() });
  },

  onSearchSubmit() {
    this.setData({ page: 1 }, () => this.loadList());
  },

  onSearchClear() {
    this.setData({ keyword: '', page: 1 }, () => this.loadList());
  },

  onPendingBannerTap() {
    const idx = FILTERS.findIndex((f) => f.value === 'PENDING');
    this.setData({ ...filterStatePatch('PENDING', idx), page: 1 }, () => this.loadList());
  },

  onUnpaidBannerTap() {
    const idx = FILTERS.findIndex((f) => f.value === 'UNPAID');
    this.setData({ ...filterStatePatch('UNPAID', idx), page: 1 }, () => this.loadList());
  },

  onPageSizeChange(e) {
    const index = Number(e.detail.value);
    const size = PAGE_SIZES[index] || 5;
    this.setData({ pageSizeIndex: index, pageSize: size, page: 1 }, () => this.loadList());
  },

  onPrevPage() {
    if (this.data.page <= 1 || this.data.loading) return;
    this.setData({ page: this.data.page - 1 }, () => this.loadList(true));
  },

  onNextPage() {
    if (this.data.page >= this.data.totalPages || this.data.loading) return;
    this.setData({ page: this.data.page + 1 }, () => this.loadList(true));
  },

  async loadGlobalBalance() {
    try {
      const res = await getYearlyBalance({ year: new Date().getFullYear() });
      const data = res.data || {};
      this.setData({
        unpaidTotal: Number(data.uncollectedFromCustomers || 0),
        unpaidTotalText: fmtMoney(data.uncollectedFromCustomers),
      });
    } catch {
      this.setData({ unpaidTotal: 0, unpaidTotalText: '0.00' });
    }
  },

  async loadSummary() {
    const { dateMode, selectedDate, selectedMonth, selectedYear } = this.data;
    try {
      const params = { page: 0, size: 500 };
      applyDateQuery(params, dateMode, selectedDate, selectedMonth, selectedYear);
      const ordersRes = await getSalesOrders(params);
      const rows = ordersRes.data?.content || [];
      const total = rows.reduce((s, r) => s + Number(r.totalAmount || 0), 0);
      this.setData({
        periodAmountText: fmtMoney(total),
        periodCount: rows.length,
        periodPrefix: periodPrefix(dateMode),
      });
    } catch {
      this.setData({
        periodAmountText: '0.00',
        periodCount: 0,
        periodPrefix: periodPrefix(dateMode),
      });
    }
  },

  async loadList(scrollTop) {
    this.setData({ loading: true });
    try {
      const { page, pageSize, dateMode, selectedDate, selectedMonth, selectedYear } = this.data;
      const params = { page: page - 1, size: pageSize };
      applyDateQuery(params, dateMode, selectedDate, selectedMonth, selectedYear);
      applyFilter(params, this.data.filterKey);
      if (this.data.keyword) params.keyword = this.data.keyword;

      const [listRes, statsRes] = await Promise.all([
        getSalesOrders(params),
        getSalesPendingStats(),
      ]);

      const pageData = listRes.data || {};
      const total = Number(pageData.totalElements || 0);
      const stats = statsRes.data || { count: 0, totalAmount: 0 };
      this.setData({
        list: (pageData.content || []).map(mapOrder),
        total,
        totalPages: calcTotalPages(total, pageSize),
        pendingStats: stats,
        pendingAmountText: fmtMoney(stats.totalAmount),
        loading: false,
      });
      if (scrollTop) {
        wx.pageScrollTo({ scrollTop: 0, duration: 200 });
      }
    } catch (e) {
      this.setData({ loading: false });
      wx.showToast({ title: e.message || '加载失败', icon: 'none' });
    }
  },

  onCardTap(e) {
    const { id } = e.currentTarget.dataset;
    if (id) wx.navigateTo({ url: `/pages/trade/sales/detail?id=${id}` });
  },

  stopBubble() {},

  onComplete(e) {
    const { id } = e.currentTarget.dataset;
    wx.showModal({
      title: '确认出库',
      content: '确认该销售单货物已出库？',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await completeSalesOrder(id);
          wx.showToast({ title: '已出库', icon: 'success' });
          this.loadGlobalBalance();
          this.loadSummary();
          this.loadList();
        } catch (err) {
          wx.showToast({ title: err.message || '操作失败', icon: 'none' });
        }
      },
    });
  },

  onCollect(e) {
    const { id, unpaid } = e.currentTarget.dataset;
    wx.showModal({
      title: '登记收款',
      editable: true,
      placeholderText: '请输入收款金额',
      content: unpaid > 0 ? String(unpaid.toFixed(2)) : '',
      success: async (res) => {
        if (!res.confirm) return;
        const amount = Number(res.content);
        if (!amount || amount <= 0) {
          wx.showToast({ title: '请输入有效金额', icon: 'none' });
          return;
        }
        try {
          await collectSalesOrder(id, {
            amount,
            paymentDate: new Date().toISOString().slice(0, 10),
            paymentMethod: '转账',
          });
          wx.showToast({ title: '收款成功', icon: 'success' });
          this.loadGlobalBalance();
          this.loadSummary();
          this.loadList();
        } catch (err) {
          wx.showToast({ title: err.message || '收款失败', icon: 'none' });
        }
      },
    });
  },

  onCancel(e) {
    const { id } = e.currentTarget.dataset;
    wx.showModal({
      title: '取消订单',
      content: '确定取消该销售单？',
      confirmColor: '#e34d59',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await cancelSalesOrder(id);
          wx.showToast({ title: '已取消', icon: 'success' });
          this.loadGlobalBalance();
          this.loadSummary();
          this.loadList();
        } catch (err) {
          wx.showToast({ title: err.message || '操作失败', icon: 'none' });
        }
      },
    });
  },
});
