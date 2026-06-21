import {
  getMonthlyPurchase,
  getMonthlySales,
  getDailyPurchaseDetail,
  getDailySalesDetail,
  getPurchaseItems,
  getSalesItems,
  getPurchasePartners,
  getSalesPartners,
  exportPurchasePartners,
  exportSalesPartners,
  exportPurchaseMonth,
  exportSalesMonth,
  exportPurchaseYear,
  exportSalesYear,
} from '~/api/analytics';
import { getSuppliers, getCustomers } from '~/api/partner';
import { requireLogin } from '~/utils/auth';
import {
  buildYearOptions,
  buildMonthOptions,
  fmtAmount,
  mapPartnerRows,
  mapSummaryRows,
  mapDailyRows,
  buildDetailView,
  buildReconciliationExportFilename,
  createPanelState,
  paginatePanel,
  PAGE_SIZES,
} from '~/utils/reconciliation';

const TABS = [
  { key: 'purchase', label: '供应商对账' },
  { key: 'sales', label: '客户对账' },
];

function panelKey(tab) {
  return tab === 'purchase' ? 'purchasePanel' : 'salesPanel';
}

function entityField(tab) {
  return tab === 'purchase' ? 'supplierId' : 'customerId';
}

Page({
  data: {
    tabs: TABS,
    activeTab: 'purchase',
    yearOptions: buildYearOptions(),
    monthOptions: buildMonthOptions(),
    pageSizeOptions: PAGE_SIZES,
    purchasePanel: createPanelState('供应商'),
    salesPanel: createPanelState('客户'),
    currentPanel: createPanelState('供应商'),
    loading: false,
  },

  onShow() {
    if (!requireLogin()) return;
    this.syncView();
    this.initEntities('purchase').then(() => this.loadActive());
  },

  onPullDownRefresh() {
    Promise.all([this.initEntities(this.data.activeTab), this.loadActive()]).finally(() =>
      wx.stopPullDownRefresh()
    );
  },

  getPanel(tab) {
    return this.data[panelKey(tab || this.data.activeTab)];
  },

  syncView() {
    const tab = this.data.activeTab;
    const panel = this.getPanel(tab);
    const year = this.data.yearOptions[panel.yearIndex];
    const month = this.data.monthOptions[panel.monthIndex]?.value;
    const periodTitle = panel.viewMode === 'year' ? `${year}年` : `${year}年${month}月`;
    this.setData({
      currentPanel: {
        ...panel,
        year,
        month,
        periodTitle,
        pendingLabel: tab === 'purchase' ? '未付' : '未收',
        amountLabel: tab === 'purchase' ? '采购额' : '销售额',
        settledLabel: tab === 'purchase' ? '已付' : '已收',
        isPurchase: tab === 'purchase',
        exportLabel: !panel.entityId
          ? '导出汇总'
          : panel.viewMode === 'year'
            ? '导出年报'
            : '导出月报',
      },
    });
  },

  setPanel(tab, patch) {
    const key = panelKey(tab);
    const next = { ...this.data[key], ...patch };
    this.setData({ [key]: next }, () => this.syncView());
  },

  async initEntities(tab) {
    try {
      const isPurchase = tab === 'purchase';
      const res = isPurchase
        ? await getSuppliers({ page: 0, size: 500 })
        : await getCustomers({ page: 0, size: 500 });
      const list = (res.data?.content || []).map((e) => ({ id: e.id, name: e.name || '—' }));
      const panel = this.getPanel(tab);
      const entityOptions = [{ id: '', name: `全部${panel.entityLabel}` }, ...list];
      const entityIndex = Math.min(panel.entityIndex, entityOptions.length - 1);
      const picked = entityOptions[entityIndex];
      this.setPanel(tab, {
        entityOptions,
        entityIndex,
        entityId: picked?.id || null,
        entityName: picked?.id ? picked.name : '',
      });
    } catch {
      /* silent */
    }
  },

  onTabChange(e) {
    const key = e.currentTarget.dataset.key;
    if (!key || key === this.data.activeTab) return;
    this.setData({ activeTab: key }, () => {
      const panel = this.getPanel(key);
      if (!panel.entityOptions || panel.entityOptions.length <= 1) {
        this.initEntities(key).then(() => this.loadActive());
      } else {
        this.loadActive();
      }
    });
  },

  onEntityChange(e) {
    const tab = this.data.activeTab;
    const index = Number(e.detail.value);
    const panel = this.getPanel(tab);
    const picked = panel.entityOptions[index];
    this.setPanel(tab, {
      entityIndex: index,
      entityId: picked?.id || null,
      entityName: picked?.id ? picked.name : '',
      selectedDay: '',
      page: 1,
    });
    this.loadActive();
  },

  onYearChange(e) {
    const tab = this.data.activeTab;
    this.setPanel(tab, { yearIndex: Number(e.detail.value), selectedDay: '', page: 1 });
    this.loadActive();
  },

  onMonthChange(e) {
    const tab = this.data.activeTab;
    const monthIndex = Number(e.detail.value);
    this.setPanel(tab, {
      monthIndex,
      selectedDay: '',
      page: 1,
    });
    this.loadActive();
  },

  onViewModeChange(e) {
    const mode = e.currentTarget.dataset.mode;
    const tab = this.data.activeTab;
    if (!mode || this.getPanel(tab).viewMode === mode) return;
    this.setPanel(tab, { viewMode: mode, selectedDay: '', page: 1 });
    this.loadActive();
  },

  onSelectPartner(e) {
    const tab = this.data.activeTab;
    const entityId = Number(e.currentTarget.dataset.id);
    const panel = this.getPanel(tab);
    const index = panel.entityOptions.findIndex((o) => o.id === entityId);
    const picked = panel.entityOptions[index >= 0 ? index : 0];
    this.setPanel(tab, {
      entityIndex: index >= 0 ? index : 0,
      entityId,
      entityName: picked?.name || '',
      selectedDay: '',
      page: 1,
    });
    this.loadActive();
  },

  onSelectMonth(e) {
    const tab = this.data.activeTab;
    const month = Number(e.currentTarget.dataset.month);
    this.setPanel(tab, { selectedMonth: month, page: 1 });
    this.loadMonthItems(tab);
  },

  onSelectDay(e) {
    const tab = this.data.activeTab;
    const day = e.currentTarget.dataset.day || '';
    const panel = this.getPanel(tab);
    const detail = buildDetailView(panel.rawItems || [], day);
    this.setPanel(tab, {
      selectedDay: day,
      detailDays: detail.days,
      detailTotalText: detail.monthTotalText,
      detailTotalLabel: detail.totalLabel,
    });
  },

  onPageSizeChange(e) {
    const tab = this.data.activeTab;
    const pageSizeIndex = Number(e.detail.value);
    const pageSize = PAGE_SIZES[pageSizeIndex] || 5;
    const panel = paginatePanel({ ...this.getPanel(tab), pageSizeIndex, pageSize, page: 1 });
    this.setPanel(tab, panel);
  },

  onPrevPage() {
    const tab = this.data.activeTab;
    const panel = this.getPanel(tab);
    if (panel.page <= 1) return;
    this.setPanel(tab, paginatePanel({ ...panel, page: panel.page - 1 }));
  },

  onNextPage() {
    const tab = this.data.activeTab;
    const panel = this.getPanel(tab);
    if (panel.page >= panel.totalPages) return;
    this.setPanel(tab, paginatePanel({ ...panel, page: panel.page + 1 }));
  },

  async onExport() {
    const tab = this.data.activeTab;
    const panel = this.data.currentPanel;
    const isPurchase = tab === 'purchase';
    const filename = buildReconciliationExportFilename({
      isPurchase,
      entityId: panel.entityId,
      entityName: panel.entityName,
      year: panel.year,
      month: panel.month,
      viewMode: panel.viewMode,
    });
    wx.showLoading({ title: '导出中' });
    try {
      if (!panel.entityId) {
        const params = { year: panel.year };
        if (panel.viewMode === 'month') params.month = panel.month;
        if (isPurchase) await exportPurchasePartners(params, filename);
        else await exportSalesPartners(params, filename);
      } else if (panel.viewMode === 'year') {
        const params = { year: panel.year };
        if (isPurchase) {
          params.supplierId = panel.entityId;
          await exportPurchaseYear(params, filename);
        } else {
          params.customerId = panel.entityId;
          await exportSalesYear(params, filename);
        }
      } else {
        const params = { year: panel.year, month: panel.month };
        if (isPurchase) {
          params.supplierId = panel.entityId;
          await exportPurchaseMonth(params, filename);
        } else {
          params.customerId = panel.entityId;
          await exportSalesMonth(params, filename);
        }
      }
      wx.showToast({ title: '导出成功', icon: 'success' });
    } catch (e) {
      wx.showToast({ title: e.message || '导出失败', icon: 'none' });
    } finally {
      wx.hideLoading();
    }
  },

  loadActive() {
    return this.data.activeTab === 'purchase' ? this.loadPurchase() : this.loadSales();
  },

  async loadMonthItems(tab) {
    const panel = this.getPanel(tab);
    const year = this.data.yearOptions[panel.yearIndex];
    const month = panel.selectedMonth;
    const field = entityField(tab);
    if (!panel.entityId) return;
    this.setData({ loading: true });
    try {
      const params = { year, month, [field]: panel.entityId };
      const res =
        tab === 'purchase' ? await getPurchaseItems(params) : await getSalesItems(params);
      const items = res.data?.items || [];
      const detail = buildDetailView(items, panel.selectedDay);
      this.setPanel(tab, {
        rawItems: items,
        detailDays: detail.days,
        detailTotalText: detail.monthTotalText,
        detailTotalLabel: detail.totalLabel,
      });
    } catch (e) {
      wx.showToast({ title: e.message || '加载明细失败', icon: 'none' });
    } finally {
      this.setData({ loading: false });
    }
  },

  async loadPurchase() {
    this.setData({ loading: true });
    const panel = this.getPanel('purchase');
    const year = this.data.yearOptions[panel.yearIndex];
    const month = this.data.monthOptions[panel.monthIndex]?.value;
    try {
      if (!panel.entityId) {
        const params = { year };
        if (panel.viewMode === 'month') params.month = month;
        const res = await getPurchasePartners(params);
        const report = res.data || {};
        const partnerRows = mapPartnerRows(report.rows);
        const next = paginatePanel({
          ...panel,
          listMode: 'partner',
          partnerRows,
          partnerPendingText: fmtAmount(report.pendingAmount),
          summaryRows: [],
          dailyRows: [],
          rawItems: [],
          detailDays: [],
          detailTotalText: '0.00',
          selectedDay: '',
        });
        this.setData({ purchasePanel: next, loading: false }, () => this.syncView());
        return;
      }

      if (panel.viewMode === 'year') {
        const res = await getMonthlyPurchase({ year, supplierId: panel.entityId });
        const rows = mapSummaryRows(res.data?.rows);
        let selectedMonth = panel.selectedMonth;
        const months = rows.map((r) => r.month);
        if (months.length && !months.includes(selectedMonth)) {
          selectedMonth = months[0];
        }
        const next = paginatePanel({
          ...panel,
          listMode: 'summary',
          summaryRows: rows,
          partnerRows: [],
          dailyRows: [],
          selectedMonth,
          selectedDay: '',
          rawItems: [],
          detailDays: [],
        });
        this.setData({ purchasePanel: next, loading: false }, () => this.syncView());
        if (months.length) await this.loadMonthItems('purchase');
        return;
      }

      const dailyRes = await getDailyPurchaseDetail({
        supplierId: panel.entityId,
        year,
        month,
      });
      const dailyRows = mapDailyRows(dailyRes.data?.rows);
      const itemsRes = await getPurchaseItems({ year, month, supplierId: panel.entityId });
      const items = itemsRes.data?.items || [];
      const detail = buildDetailView(items, '');
      const next = paginatePanel({
        ...panel,
        listMode: 'daily',
        dailyRows,
        partnerRows: [],
        summaryRows: [],
        rawItems: items,
        detailDays: detail.days,
        detailTotalText: detail.monthTotalText,
        detailTotalLabel: detail.totalLabel,
        selectedDay: '',
      });
      this.setData({ purchasePanel: next, loading: false }, () => this.syncView());
    } catch (e) {
      this.setData({ loading: false });
      wx.showToast({ title: e.message || '查询失败', icon: 'none' });
    }
  },

  async loadSales() {
    this.setData({ loading: true });
    const panel = this.getPanel('sales');
    const year = this.data.yearOptions[panel.yearIndex];
    const month = this.data.monthOptions[panel.monthIndex]?.value;
    try {
      if (!panel.entityId) {
        const params = { year };
        if (panel.viewMode === 'month') params.month = month;
        const res = await getSalesPartners(params);
        const report = res.data || {};
        const partnerRows = mapPartnerRows(report.rows);
        const next = paginatePanel({
          ...panel,
          listMode: 'partner',
          partnerRows,
          partnerPendingText: fmtAmount(report.pendingAmount),
          summaryRows: [],
          dailyRows: [],
          rawItems: [],
          detailDays: [],
          detailTotalText: '0.00',
          selectedDay: '',
        });
        this.setData({ salesPanel: next, loading: false }, () => this.syncView());
        return;
      }

      if (panel.viewMode === 'year') {
        const res = await getMonthlySales({ year, customerId: panel.entityId });
        const rows = mapSummaryRows(res.data?.rows);
        let selectedMonth = panel.selectedMonth;
        const months = rows.map((r) => r.month);
        if (months.length && !months.includes(selectedMonth)) {
          selectedMonth = months[0];
        }
        const next = paginatePanel({
          ...panel,
          listMode: 'summary',
          summaryRows: rows,
          partnerRows: [],
          dailyRows: [],
          selectedMonth,
          selectedDay: '',
          rawItems: [],
          detailDays: [],
        });
        this.setData({ salesPanel: next, loading: false }, () => this.syncView());
        if (months.length) await this.loadMonthItems('sales');
        return;
      }

      const dailyRes = await getDailySalesDetail({
        customerId: panel.entityId,
        year,
        month,
      });
      const dailyRows = mapDailyRows(dailyRes.data?.rows);
      const itemsRes = await getSalesItems({ year, month, customerId: panel.entityId });
      const items = itemsRes.data?.items || [];
      const detail = buildDetailView(items, '');
      const next = paginatePanel({
        ...panel,
        listMode: 'daily',
        dailyRows,
        partnerRows: [],
        summaryRows: [],
        rawItems: items,
        detailDays: detail.days,
        detailTotalText: detail.monthTotalText,
        detailTotalLabel: detail.totalLabel,
        selectedDay: '',
      });
      this.setData({ salesPanel: next, loading: false }, () => this.syncView());
    } catch (e) {
      this.setData({ loading: false });
      wx.showToast({ title: e.message || '查询失败', icon: 'none' });
    }
  },
});
