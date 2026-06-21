import { getInventoryOverview } from '~/api/inventory';
import { getPurchasePendingStats } from '~/api/purchase';
import { getSalesPendingStats } from '~/api/sales';
import { requireLogin } from '~/utils/auth';
import { fmtMoney } from '~/utils/format';
import {
  mapInventoryItem,
  buildCategoryFilters,
  filterInventoryItems,
  paginateItems,
} from '~/utils/inventory';

const PAGE_SIZES = [5, 10, 20];

Page({
  data: {
    allItems: [],
    list: [],
    loading: false,
    skuCount: 0,
    totalValue: 0,
    totalValueText: '0.00',
    purchasePending: { count: 0, totalAmount: 0 },
    salesPending: { count: 0, totalAmount: 0 },
    purchasePendingText: '0.00',
    salesPendingText: '0.00',
    filters: [{ label: '全部分类', value: 'ALL' }],
    filterKey: 'ALL',
    filterIndex: 0,
    keyword: '',
    page: 1,
    pageSize: 5,
    pageSizeOptions: PAGE_SIZES,
    pageSizeIndex: 0,
    total: 0,
    totalPages: 1,
  },

  onShow() {
    if (!requireLogin()) return;
    this.loadData();
  },

  onPullDownRefresh() {
    this.loadData().finally(() => wx.stopPullDownRefresh());
  },

  onFilterChange(e) {
    const index = Number(e.detail.value);
    const item = this.data.filters[index] || this.data.filters[0];
    this.setData({ filterIndex: index, filterKey: item.value, page: 1 }, () => this.applyList());
  },

  onSearchInput(e) {
    this.setData({ keyword: (e.detail.value || '').trim() });
  },

  onSearchSubmit() {
    this.setData({ page: 1 }, () => this.applyList());
  },

  onSearchClear() {
    this.setData({ keyword: '', page: 1 }, () => this.applyList());
  },

  onPageSizeChange(e) {
    const index = Number(e.detail.value);
    const size = PAGE_SIZES[index] || 5;
    this.setData({ pageSizeIndex: index, pageSize: size, page: 1 }, () => this.applyList());
  },

  onPrevPage() {
    if (this.data.page <= 1 || this.data.loading) return;
    this.setData({ page: this.data.page - 1 }, () => this.applyList(true));
  },

  onNextPage() {
    if (this.data.page >= this.data.totalPages || this.data.loading) return;
    this.setData({ page: this.data.page + 1 }, () => this.applyList(true));
  },

  goPurchasePending() {
    wx.navigateTo({ url: '/pages/trade/purchase/index?tab=PENDING' });
  },

  goSalesPending() {
    wx.navigateTo({ url: '/pages/trade/sales/index?tab=PENDING' });
  },

  async loadData() {
    this.setData({ loading: true });
    try {
      const [overviewRes, ppRes, spRes] = await Promise.all([
        getInventoryOverview(),
        getPurchasePendingStats(),
        getSalesPendingStats(),
      ]);

      const overview = overviewRes.data || {};
      const allItems = (overview.items || []).map(mapInventoryItem);
      const pp = ppRes.data || { count: 0, totalAmount: 0 };
      const sp = spRes.data || { count: 0, totalAmount: 0 };
      const totalValue = Number(overview.totalValue || 0);

      this.setData(
        {
          allItems,
          skuCount: Number(overview.skuCount || allItems.length),
          totalValue,
          totalValueText: fmtMoney(totalValue),
          purchasePending: pp,
          salesPending: sp,
          purchasePendingText: fmtMoney(pp.totalAmount),
          salesPendingText: fmtMoney(sp.totalAmount),
          filters: buildCategoryFilters(allItems),
          filterIndex: 0,
          filterKey: 'ALL',
          page: 1,
          loading: false,
        },
        () => this.applyList(),
      );
    } catch (e) {
      this.setData({ loading: false });
      wx.showToast({ title: e.message || '加载失败', icon: 'none' });
    }
  },

  applyList(scrollTop) {
    const { allItems, keyword, filterKey, page, pageSize } = this.data;
    const filtered = filterInventoryItems(allItems, keyword, filterKey);
    const { list, total, totalPages } = paginateItems(filtered, page, pageSize);
    this.setData({ list, total, totalPages });
    if (scrollTop) {
      wx.pageScrollTo({ scrollTop: 0, duration: 200 });
    }
  },
});
