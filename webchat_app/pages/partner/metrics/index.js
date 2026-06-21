import {
  getActiveSuppliers,
  getSupplierMetricCompletion,
  createSupplierProductMetric,
  updateSupplierProductMetric,
  deleteSupplierProductMetric,
} from '~/api/partner';
import { getAllEnabledProducts } from '~/api/product';
import { requireLogin } from '~/utils/auth';

const PAGE_SIZES = [5, 10, 20];

const PERIOD_TYPES = [
  { key: 'MONTH', label: '月指标' },
  { key: 'YEAR', label: '年指标' },
];

const STATUS_OPTIONS = [
  { value: 'ACTIVE', label: '启用' },
  { value: 'INACTIVE', label: '停用' },
];

function todayParts() {
  const now = new Date();
  const year = now.getFullYear();
  const month = now.getMonth() + 1;
  return {
    year,
    month,
    monthStr: `${year}-${String(month).padStart(2, '0')}`,
    yearStr: String(year),
  };
}

function calcTotalPages(total, pageSize) {
  return Math.max(1, Math.ceil(Number(total || 0) / pageSize));
}

function slicePage(rows, page, pageSize) {
  const start = (page - 1) * pageSize;
  return (rows || []).slice(start, start + pageSize);
}

function numFmt(v) {
  return Number(v || 0).toFixed(1);
}

function qtyFmt(v, unit) {
  const n = numFmt(v);
  return unit ? `${n} ${unit}` : n;
}

function gapFmt(v, unit) {
  const text = Math.abs(Number(v || 0)).toFixed(1);
  return unit ? `${text} ${unit}` : text;
}

function gapClass(v) {
  const n = Number(v || 0);
  if (n > 0) return 'excess';
  if (n < 0) return 'shortfall';
  return '';
}

function mapMetricRow(row) {
  const unit = row.productUnit || '';
  const percent = Math.min(100, Number(row.completionPercent || 0));
  const productTitle = unit ? `${row.productName || '-'}（${unit}）` : (row.productName || '-');
  return {
    id: row.id,
    supplierId: row.supplierId,
    supplierName: row.supplierName,
    productId: row.productId,
    productName: row.productName,
    productUnit: unit,
    productTitle,
    periodType: row.periodType,
    periodLabel: row.periodLabel,
    year: row.year,
    month: row.month,
    status: row.status,
    targetQty: row.targetQty,
    actualQty: row.actualQty,
    gapQty: row.gapQty,
    targetQtyText: qtyFmt(row.targetQty, unit),
    actualQtyText: qtyFmt(row.actualQty, unit),
    gapQtyText: gapFmt(row.gapQty, unit),
    gapClass: gapClass(row.gapQty),
    completionPercent: percent,
    progressWidth: percent,
    isCompleted: percent >= 100,
  };
}

function countCompleted(items) {
  return (items || []).filter((item) => item.isCompleted).length;
}

function mapCompletionGroup(g) {
  const items = (g.items || []).map(mapMetricRow);
  const productCount = items.length;
  const completedCount = countCompleted(items);
  return {
    supplierId: g.supplierId,
    supplierName: g.supplierName || '-',
    productCount,
    completedCount,
    pendingCount: productCount - completedCount,
    expanded: true,
    items,
  };
}

function mapCompletionReport(report, groups) {
  const allItems = (groups || []).flatMap((g) => g.items || []);
  const metricCount = allItems.length;
  const completedCount = countCompleted(allItems);
  const reachRate = metricCount > 0 ? Math.round((completedCount / metricCount) * 100) : 0;
  if (!report) {
    return {
      periodLabel: '-',
      supplierCount: 0,
      metricCount: 0,
      completedCount: 0,
      pendingCount: 0,
      reachRate: 0,
      progressWidth: 0,
    };
  }
  return {
    periodLabel: report.periodLabel || '-',
    supplierCount: (groups || []).length,
    metricCount,
    completedCount,
    pendingCount: metricCount - completedCount,
    reachRate,
    progressWidth: reachRate,
  };
}

function filterCompletionGroups(groups, statusFilter) {
  if (!statusFilter || statusFilter === 'ALL') {
    return (groups || []).map((g) => ({ ...g }));
  }
  const wantCompleted = statusFilter === 'COMPLETED';
  return (groups || [])
    .map((g) => {
      const items = (g.items || []).filter((item) => item.isCompleted === wantCompleted);
      if (!items.length) return null;
      const completedCount = countCompleted(items);
      return {
        ...g,
        items,
        productCount: items.length,
        completedCount,
        pendingCount: items.length - completedCount,
      };
    })
    .filter(Boolean);
}

function filterGroupsByQuery(groups, productId, keyword) {
  const kw = (keyword || '').trim().toLowerCase();
  return (groups || [])
    .map((g) => {
      let items = g.items || [];
      if (productId) {
        items = items.filter((item) => item.productId === productId);
      }
      if (kw) {
        items = items.filter(
          (item) =>
            String(item.supplierName || '').toLowerCase().includes(kw) ||
            String(item.productName || '').toLowerCase().includes(kw)
        );
      }
      if (!items.length) return null;
      const completedCount = countCompleted(items);
      return {
        ...g,
        items,
        productCount: items.length,
        completedCount,
        pendingCount: items.length - completedCount,
      };
    })
    .filter(Boolean);
}

function buildEmptyFilterText(statusFilter, hasQuery) {
  if (hasQuery) return '没有符合条件的订量指标';
  if (statusFilter === 'COMPLETED') return '暂无已达标指标';
  if (statusFilter === 'PENDING') return '暂无未达标指标';
  return '暂无订量指标';
}

function paginateCompletionGroups(groups, page, pageSize) {
  const total = (groups || []).length;
  const totalPages = calcTotalPages(total, pageSize);
  const safePage = Math.min(Math.max(page, 1), totalPages);
  return {
    displayGroups: slicePage(groups, safePage, pageSize),
    completionPage: safePage,
    completionTotalPages: totalPages,
    groupTotal: total,
  };
}

function pickLabel(list, index, fallback) {
  const item = list[index];
  return item && item.name ? item.name : fallback;
}

function showFormAlert(content) {
  wx.showModal({
    title: '提示',
    content: content || '请完善表单',
    showCancel: false,
  });
}

Page({
  data: {
    periodTypes: PERIOD_TYPES,
    statusOptions: STATUS_OPTIONS,
    periodType: 'MONTH',
    selectedMonth: todayParts().monthStr,
    selectedYear: todayParts().yearStr,
    supplierOptions: [{ id: '', name: '全部供应商' }],
    supplierIndex: 0,
    productOptions: [{ id: '', name: '全部商品' }],
    productIndex: 0,
    keyword: '',
    completionSummary: mapCompletionReport(null, []),
    allGroups: [],
    displayGroups: [],
    completionPage: 1,
    completionPageSize: 5,
    completionPageSizeIndex: 0,
    completionTotalPages: 1,
    groupTotal: 0,
    completionStatusFilter: 'ALL',
    hasCompletionData: false,
    emptyFilterText: '暂无订量指标',
    pageSizeOptions: PAGE_SIZES,
    loading: false,
    showForm: false,
    formMode: 'create',
    submitting: false,
    form: {
      id: null,
      periodType: 'MONTH',
      supplierIndex: 0,
      productIndex: 0,
      selectedMonth: todayParts().monthStr,
      selectedYear: todayParts().yearStr,
      targetQty: '',
      statusIndex: 0,
    },
    formSuppliers: [],
    formProducts: [],
    formSupplierLabel: '请选择',
    formProductLabel: '请选择',
    formStatusLabel: '启用',
  },

  onShow() {
    if (!requireLogin()) return;
    this.loadOptions().then(() => this.loadCompletion());
  },

  onPullDownRefresh() {
    this.loadOptions()
      .then(() => this.loadCompletion())
      .finally(() => wx.stopPullDownRefresh());
  },

  loadOptions() {
    return Promise.all([getActiveSuppliers(), getAllEnabledProducts()])
      .then(([sRes, pRes]) => {
        const suppliers = (sRes.data || []).map((s) => ({ id: s.id, name: s.name || '-' }));
        const products = (pRes.data || []).map((p) => ({ id: p.id, name: p.name || '-' }));
        this.setData({
          supplierOptions: [{ id: '', name: '全部供应商' }, ...suppliers],
          productOptions: [{ id: '', name: '全部商品' }, ...products],
          formSuppliers: suppliers,
          formProducts: products,
        });
      })
      .catch(() => {
        this.setData({
          supplierOptions: [{ id: '', name: '全部供应商' }],
          productOptions: [{ id: '', name: '全部商品' }],
          formSuppliers: [],
          formProducts: [],
        });
      });
  },

  onPeriodTypeChange(e) {
    const periodType = e.currentTarget.dataset.type;
    if (!periodType || periodType === this.data.periodType) return;
    this.setData({ periodType, completionPage: 1 }, () => this.loadCompletion());
  },

  onMonthChange(e) {
    this.setData({ selectedMonth: e.detail.value, completionPage: 1 }, () => this.loadCompletion());
  },

  onYearChange(e) {
    const val = e.detail.value || '';
    this.setData({ selectedYear: val.slice(0, 4), completionPage: 1 }, () => this.loadCompletion());
  },

  onSupplierChange(e) {
    this.setData({ supplierIndex: Number(e.detail.value), completionPage: 1 }, () => this.loadCompletion());
  },

  onProductChange(e) {
    this.setData({ productIndex: Number(e.detail.value), completionPage: 1 }, () => this.applyCompletionView());
  },

  onSearchInput(e) {
    this.setData({ keyword: (e.detail.value || '').trim() });
  },

  onSearchSubmit() {
    this.setData({ completionPage: 1 }, () => this.applyCompletionView());
  },

  onSearchClear() {
    this.setData({ keyword: '', completionPage: 1 }, () => this.applyCompletionView());
  },

  buildPeriodParams() {
    const { periodType, selectedMonth, selectedYear, supplierOptions, supplierIndex } = this.data;
    const params = { periodType };
    if (periodType === 'MONTH') {
      const parts = selectedMonth.split('-');
      params.year = Number(parts[0]);
      params.month = Number(parts[1]);
    } else {
      params.year = Number(selectedYear);
    }
    const supplier = supplierOptions[supplierIndex];
    if (supplier && supplier.id) params.supplierId = supplier.id;
    return params;
  },

  loadCompletion() {
    this.setData({ loading: true });
    return getSupplierMetricCompletion(this.buildPeriodParams())
      .then((res) => {
        const report = res.data || {};
        const allGroups = (report.groups || []).map(mapCompletionGroup);
        this.setData({
          completionSummary: mapCompletionReport(report, allGroups),
          allGroups,
          hasCompletionData: allGroups.length > 0,
          completionStatusFilter: 'ALL',
          completionPage: 1,
          loading: false,
        }, () => this.applyCompletionView());
      })
      .catch((e) => {
        this.setData({
          completionSummary: mapCompletionReport(null, []),
          allGroups: [],
          displayGroups: [],
          groupTotal: 0,
          hasCompletionData: false,
          completionStatusFilter: 'ALL',
          completionPage: 1,
          completionTotalPages: 1,
          loading: false,
        });
        wx.showToast({ title: (e && e.message) || '加载失败', icon: 'none' });
      });
  },

  applyCompletionView(page) {
    const pageNo = page || this.data.completionPage || 1;
    const product = this.data.productOptions[this.data.productIndex];
    const productId = product && product.id ? product.id : null;
    const hasQuery = !!productId || !!((this.data.keyword || '').trim());
    let groups = filterGroupsByQuery(this.data.allGroups, productId, this.data.keyword);
    groups = filterCompletionGroups(groups, this.data.completionStatusFilter);
    this.setData({
      emptyFilterText: buildEmptyFilterText(this.data.completionStatusFilter, hasQuery),
      ...paginateCompletionGroups(groups, pageNo, this.data.completionPageSize),
    });
  },

  findMetricRow(id) {
    for (let i = 0; i < this.data.allGroups.length; i += 1) {
      const group = this.data.allGroups[i];
      const row = (group.items || []).find((item) => String(item.id) === String(id));
      if (row) return row;
    }
    return null;
  },

  onCompletionStatusFilter(e) {
    const filter = e.currentTarget.dataset.filter;
    if (!filter) return;
    const next = this.data.completionStatusFilter === filter ? 'ALL' : filter;
    this.setData({ completionStatusFilter: next, completionPage: 1 }, () => this.applyCompletionView());
  },

  onCompletionPageSizeChange(e) {
    const completionPageSizeIndex = Number(e.detail.value);
    const completionPageSize = PAGE_SIZES[completionPageSizeIndex] || 5;
    this.setData({ completionPageSizeIndex, completionPageSize, completionPage: 1 }, () =>
      this.applyCompletionView(1)
    );
  },

  onCompletionPrevPage() {
    if (this.data.completionPage <= 1) return;
    this.applyCompletionView(this.data.completionPage - 1);
  },

  onCompletionNextPage() {
    if (this.data.completionPage >= this.data.completionTotalPages) return;
    this.applyCompletionView(this.data.completionPage + 1);
  },

  toggleGroup(e) {
    const supplierId = e.currentTarget.dataset.id;
    const allGroups = this.data.allGroups.map((g) => {
      if (g.supplierId === supplierId) {
        return { ...g, expanded: !g.expanded };
      }
      return g;
    });
    this.setData({ allGroups }, () => this.applyCompletionView());
  },

  syncFormLabels(form) {
    const { formSuppliers, formProducts, statusOptions } = this.data;
    return {
      formSupplierLabel: pickLabel(formSuppliers, form.supplierIndex, '请选择'),
      formProductLabel: pickLabel(formProducts, form.productIndex, '请选择'),
      formStatusLabel: (statusOptions[form.statusIndex] && statusOptions[form.statusIndex].label) || '启用',
    };
  },

  openCreate() {
    const { monthStr, yearStr } = todayParts();
    const form = {
      id: null,
      periodType: this.data.periodType,
      supplierIndex: 0,
      productIndex: 0,
      selectedMonth: monthStr,
      selectedYear: yearStr,
      targetQty: '',
      statusIndex: 0,
    };
    this.setData({
      showForm: true,
      formMode: 'create',
      form,
      ...this.syncFormLabels(form),
    });
  },

  openEdit(e) {
    const { id } = e.currentTarget.dataset;
    const row = this.findMetricRow(id);
    if (!row) return;
    const supplierIndex = this.data.formSuppliers.findIndex((s) => s.id === row.supplierId);
    const productIndex = this.data.formProducts.findIndex((p) => p.id === row.productId);
    const statusIndex = STATUS_OPTIONS.findIndex((s) => s.value === (row.status || 'ACTIVE'));
    const form = {
      id: row.id,
      periodType: row.periodType || 'MONTH',
      supplierIndex: supplierIndex >= 0 ? supplierIndex : 0,
      productIndex: productIndex >= 0 ? productIndex : 0,
      selectedMonth:
        row.periodType === 'YEAR'
          ? todayParts().monthStr
          : `${row.year}-${String(row.month || 1).padStart(2, '0')}`,
      selectedYear: String(row.year || todayParts().year),
      targetQty: row.targetQty != null ? String(row.targetQty) : '',
      statusIndex: statusIndex >= 0 ? statusIndex : 0,
    };
    this.setData({
      showForm: true,
      formMode: 'edit',
      form,
      ...this.syncFormLabels(form),
    });
  },

  closeForm(e) {
    if (e && e.detail && e.detail.visible === false) {
      this.setData({ showForm: false });
    }
  },

  onFormPeriodTypeChange(e) {
    const periodType = e.currentTarget.dataset.type;
    if (!periodType) return;
    this.setData({ 'form.periodType': periodType });
  },

  onFormSupplierChange(e) {
    const supplierIndex = Number(e.detail.value);
    this.setData({
      'form.supplierIndex': supplierIndex,
      formSupplierLabel: pickLabel(this.data.formSuppliers, supplierIndex, '请选择'),
    });
  },

  onFormProductChange(e) {
    const productIndex = Number(e.detail.value);
    this.setData({
      'form.productIndex': productIndex,
      formProductLabel: pickLabel(this.data.formProducts, productIndex, '请选择'),
    });
  },

  onFormMonthChange(e) {
    this.setData({ 'form.selectedMonth': e.detail.value });
  },

  onFormYearChange(e) {
    const val = e.detail.value || '';
    this.setData({ 'form.selectedYear': val.slice(0, 4) });
  },

  onFormStatusChange(e) {
    const statusIndex = Number(e.detail.value);
    this.setData({
      'form.statusIndex': statusIndex,
      formStatusLabel: (STATUS_OPTIONS[statusIndex] && STATUS_OPTIONS[statusIndex].label) || '启用',
    });
  },

  onFormTargetFocus() {
    const raw = String(this.data.form.targetQty || '').trim();
    if (!raw) return;
    this.setData({ 'form.targetQty': '' });
  },

  onFormTargetInput(e) {
    this.setData({ 'form.targetQty': e.detail.value });
  },

  buildFormPayload() {
    const { form, formSuppliers, formProducts, statusOptions } = this.data;
    const supplier = formSuppliers[form.supplierIndex];
    const product = formProducts[form.productIndex];
    if (!supplier || !supplier.id) throw new Error('请选择供应商');
    if (!product || !product.id) throw new Error('请选择商品');
    const raw = String(form.targetQty || '').trim();
    if (!raw) throw new Error('订量指标必填');
    const targetQty = Number(raw);
    if (!Number.isFinite(targetQty) || targetQty <= 0) throw new Error('订量指标必须大于0');
    const payload = {
      periodType: form.periodType,
      supplierId: supplier.id,
      productId: product.id,
      targetQty,
      status: (statusOptions[form.statusIndex] && statusOptions[form.statusIndex].value) || 'ACTIVE',
    };
    if (form.periodType === 'MONTH') {
      const parts = form.selectedMonth.split('-');
      payload.year = Number(parts[0]);
      payload.month = Number(parts[1]);
    } else {
      payload.year = Number(form.selectedYear);
      payload.month = 0;
    }
    return payload;
  },

  submitForm() {
    if (this.data.submitting) return;
    this.setData({ submitting: true });
    let payload;
    try {
      payload = this.buildFormPayload();
    } catch (e) {
      this.setData({ submitting: false });
      showFormAlert(e.message);
      return;
    }

    const { form } = this.data;
    const req = form.id
      ? updateSupplierProductMetric(form.id, payload)
      : createSupplierProductMetric(payload);

    req
      .then(() => {
        wx.showToast({ title: '保存成功', icon: 'success' });
        this.setData({
          showForm: false,
          periodType: payload.periodType,
          selectedMonth:
            payload.periodType === 'MONTH'
              ? `${payload.year}-${String(payload.month).padStart(2, '0')}`
              : this.data.selectedMonth,
          selectedYear: String(payload.year),
          completionPage: 1,
          submitting: false,
        });
        return this.loadCompletion();
      })
      .catch((e) => {
        this.setData({ submitting: false });
        wx.showToast({ title: (e && e.message) || '保存失败', icon: 'none' });
      });
  },

  onDelete(e) {
    const { id, title } = e.currentTarget.dataset;
    wx.showModal({
      title: '确认删除',
      content: `确定删除「${title}」的订量指标？`,
      confirmColor: '#e34d59',
      success: (res) => {
        if (!res.confirm) return;
        deleteSupplierProductMetric(id)
          .then(() => {
            wx.showToast({ title: '已删除', icon: 'success' });
            this.setData({ completionPage: 1 });
            return this.loadCompletion();
          })
          .catch((err) => {
            wx.showToast({ title: (err && err.message) || '删除失败', icon: 'none' });
          });
      },
    });
  },

  stopBubble() {},
});
