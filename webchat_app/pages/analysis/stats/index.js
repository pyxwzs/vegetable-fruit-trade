import { getSupplierProductStats, getCustomerProductStats } from '~/api/analytics';
import { getSuppliers, getCustomers } from '~/api/partner';
import { requireLogin } from '~/utils/auth';
import { calcTotalPages, slicePage, PAGE_SIZES } from '~/utils/reconciliation';

const TABS = [
  { key: 'purchase', label: '供应商供货' },
  { key: 'sales', label: '客户销售' },
];

const MONTH_OPTIONS = [{ label: '全年', value: '' }].concat(
  Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 }))
);

function buildYearOptions() {
  const current = new Date().getFullYear();
  return Array.from({ length: 5 }, (_, i) => current - i);
}

function mapReport(report) {
  const groups = (report?.groups || []).map((g) => ({
    entityId: g.entityId,
    entityName: g.entityName || '—',
    productCount: (g.products || []).length,
    totalAmountText: Number(g.totalAmount || 0).toFixed(2),
    totalQuantityText: Number(g.totalQuantity || 0).toFixed(1),
    expanded: true,
    products: (g.products || []).map((p) => ({
      productName: p.productName || '—',
      quantityText: p.unit
        ? `${Number(p.quantity || 0).toFixed(1)} ${p.unit}`
        : Number(p.quantity || 0).toFixed(1),
      amountText: Number(p.totalAmount || 0).toFixed(2),
    })),
  }));
  const productTypeCount = groups.reduce((sum, g) => sum + g.productCount, 0);
  return {
    grandTotalAmountText: Number(report?.grandTotalAmount || 0).toFixed(2),
    grandTotalQuantityText: Number(report?.grandTotalQuantity || 0).toFixed(1),
    groupCount: groups.length,
    productTypeCount,
    groups,
  };
}

function paginateGroups(groups, page, pageSize) {
  const total = (groups || []).length;
  const totalPages = calcTotalPages(total, pageSize);
  const safePage = Math.min(Math.max(page, 1), totalPages);
  return {
    displayGroups: slicePage(groups, safePage, pageSize),
    page: safePage,
    totalPages,
    groupTotal: total,
  };
}

Page({
  data: {
    tabs: TABS,
    activeTab: 'purchase',
    yearOptions: buildYearOptions(),
    yearIndex: 0,
    monthOptions: MONTH_OPTIONS,
    monthIndex: 0,
    entityOptions: [{ id: '', name: '全部' }],
    entityIndex: 0,
    reportView: {
      grandTotalAmountText: '0.00',
      grandTotalQuantityText: '0.0',
      groupCount: 0,
      productTypeCount: 0,
    },
    allGroups: [],
    displayGroups: [],
    page: 1,
    pageSize: 5,
    pageSizeOptions: PAGE_SIZES,
    pageSizeIndex: 0,
    totalPages: 1,
    groupTotal: 0,
    loading: false,
  },

  onShow() {
    if (!requireLogin()) return;
    this.loadEntities().then(() => this.loadReport());
  },

  onPullDownRefresh() {
    Promise.all([this.loadEntities(), this.loadReport()]).finally(() => wx.stopPullDownRefresh());
  },

  async loadEntities() {
    try {
      const isPurchase = this.data.activeTab === 'purchase';
      const res = isPurchase
        ? await getSuppliers({ page: 0, size: 500 })
        : await getCustomers({ page: 0, size: 500 });
      const list = (res.data?.content || []).map((item) => ({
        id: item.id,
        name: item.name || '—',
      }));
      const entityIndex = Math.min(this.data.entityIndex, list.length);
      this.setData({
        entityOptions: [{ id: '', name: isPurchase ? '全部供应商' : '全部客户' }, ...list],
        entityIndex,
      });
    } catch {
      this.setData({
        entityOptions: [{ id: '', name: '全部' }],
        entityIndex: 0,
      });
    }
  },

  async loadReport() {
    this.setData({ loading: true });
    try {
      const { activeTab, yearOptions, yearIndex, monthOptions, monthIndex, entityOptions, entityIndex } =
        this.data;
      const params = { year: yearOptions[yearIndex] };
      const month = monthOptions[monthIndex]?.value;
      if (month) params.month = month;
      const entityId = entityOptions[entityIndex]?.id;
      if (entityId) {
        if (activeTab === 'purchase') params.supplierId = entityId;
        else params.customerId = entityId;
      }

      const res =
        activeTab === 'purchase'
          ? await getSupplierProductStats(params)
          : await getCustomerProductStats(params);
      const mapped = mapReport(res.data);
      const pagination = paginateGroups(mapped.groups, 1, this.data.pageSize);
      this.setData({
        reportView: {
          grandTotalAmountText: mapped.grandTotalAmountText,
          grandTotalQuantityText: mapped.grandTotalQuantityText,
          groupCount: mapped.groupCount,
          productTypeCount: mapped.productTypeCount,
        },
        allGroups: mapped.groups,
        ...pagination,
        loading: false,
      });
    } catch (e) {
      this.setData({
        reportView: {
          grandTotalAmountText: '0.00',
          grandTotalQuantityText: '0.0',
          groupCount: 0,
          productTypeCount: 0,
        },
        allGroups: [],
        displayGroups: [],
        page: 1,
        totalPages: 1,
        groupTotal: 0,
        loading: false,
      });
      wx.showToast({ title: e.message || '加载失败', icon: 'none' });
    }
  },

  onTabChange(e) {
    const key = e.currentTarget.dataset.key;
    if (!key || key === this.data.activeTab) return;
    this.setData({ activeTab: key, entityIndex: 0, page: 1 }, () => {
      this.loadEntities().then(() => this.loadReport());
    });
  },

  onYearChange(e) {
    this.setData({ yearIndex: Number(e.detail.value), page: 1 }, () => this.loadReport());
  },

  onMonthChange(e) {
    this.setData({ monthIndex: Number(e.detail.value), page: 1 }, () => this.loadReport());
  },

  onEntityChange(e) {
    this.setData({ entityIndex: Number(e.detail.value), page: 1 }, () => this.loadReport());
  },

  onPageSizeChange(e) {
    const pageSizeIndex = Number(e.detail.value);
    const pageSize = PAGE_SIZES[pageSizeIndex] || 5;
    this.setData({
      pageSizeIndex,
      pageSize,
      ...paginateGroups(this.data.allGroups, 1, pageSize),
    });
  },

  onPrevPage() {
    if (this.data.page <= 1) return;
    this.setData(paginateGroups(this.data.allGroups, this.data.page - 1, this.data.pageSize));
  },

  onNextPage() {
    if (this.data.page >= this.data.totalPages) return;
    this.setData(paginateGroups(this.data.allGroups, this.data.page + 1, this.data.pageSize));
  },

  toggleGroup(e) {
    const entityId = e.currentTarget.dataset.id;
    const allGroups = this.data.allGroups.map((g) =>
      g.entityId === entityId ? { ...g, expanded: !g.expanded } : g
    );
    this.setData({
      allGroups,
      ...paginateGroups(allGroups, this.data.page, this.data.pageSize),
    });
  },
});
