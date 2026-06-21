import {
  getProducts,
  getAllEnabledProducts,
  getProductCategories,
  createProduct,
  updateProduct,
  deleteProduct,
} from '~/api/product';
import { requireLogin } from '~/utils/auth';
import { PRODUCT_STATUS, productStatusTheme } from '~/utils/product';

const FILTERS = [
  { label: '全部', value: 'ALL' },
  { label: '启用', value: 'ENABLED' },
  { label: '停用', value: 'DISABLED' },
];

const PAGE_SIZES = [5, 10, 20];

const EMPTY_FORM = {
  id: null,
  name: '',
  category: '',
  unit: '',
  specification: '',
  status: 'ENABLED',
};

function calcTotalPages(total, pageSize) {
  return Math.max(1, Math.ceil(Number(total || 0) / pageSize));
}

function mapProduct(row) {
  const specUnit = [row.specification, row.unit].filter(Boolean).join(' / ');
  return {
    id: row.id,
    name: row.name || '—',
    category: row.category || '—',
    unit: row.unit || '—',
    specification: row.specification || '',
    specUnit: specUnit || '—',
    status: row.status,
    statusText: PRODUCT_STATUS[row.status] || row.status,
    statusTheme: productStatusTheme(row.status),
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
    totalCount: 0,
    enabledCount: 0,
    page: 1,
    pageSize: 5,
    pageSizeOptions: PAGE_SIZES,
    pageSizeIndex: 0,
    total: 0,
    totalPages: 1,
    showForm: false,
    formMode: 'create',
    form: { ...EMPTY_FORM },
    submitting: false,
    statusOptions: [
      { label: '启用', value: 'ENABLED' },
      { label: '停用', value: 'DISABLED' },
    ],
    statusIndex: 0,
    categorySuggestions: [],
  },

  onShow() {
    if (!requireLogin()) return;
    this.loadSummary();
    this.loadList();
    this.loadCategorySuggestions();
  },

  onPullDownRefresh() {
    Promise.all([this.loadSummary(), this.loadList(), this.loadCategorySuggestions()])
      .finally(() => wx.stopPullDownRefresh());
  },

  onFilterChange(e) {
    const index = Number(e.detail.value);
    const item = FILTERS[index] || FILTERS[0];
    this.setData({ filterIndex: index, filterKey: item.value, page: 1 }, () => this.loadList());
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

  async loadSummary() {
    try {
      const [totalRes, enabledRes] = await Promise.all([
        getProducts({ page: 0, size: 1 }),
        getAllEnabledProducts(),
      ]);
      this.setData({
        totalCount: Number(totalRes.data?.totalElements || 0),
        enabledCount: (enabledRes.data || []).length,
      });
    } catch {
      this.setData({ totalCount: 0, enabledCount: 0 });
    }
  },

  async loadCategorySuggestions() {
    try {
      const res = await getProductCategories();
      this.setData({ categorySuggestions: res.data || [] });
    } catch {
      this.setData({ categorySuggestions: [] });
    }
  },

  async loadList(scrollTop) {
    this.setData({ loading: true });
    try {
      const { page, pageSize, filterKey, keyword } = this.data;
      const params = { page: page - 1, size: pageSize };
      if (filterKey && filterKey !== 'ALL') params.status = filterKey;
      if (keyword) params.keyword = keyword;

      const res = await getProducts(params);
      const pageData = res.data || {};
      const total = Number(pageData.totalElements || 0);
      this.setData({
        list: (pageData.content || []).map(mapProduct),
        total,
        totalPages: calcTotalPages(total, pageSize),
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

  openCreate() {
    this.setData({
      showForm: true,
      formMode: 'create',
      form: { ...EMPTY_FORM },
      statusIndex: 0,
    });
  },

  openEdit(e) {
    const id = Number(e.currentTarget.dataset.id);
    const item = this.data.list.find((p) => p.id === id);
    if (!item) return;
    this.setData({
      showForm: true,
      formMode: 'edit',
      form: {
        id: item.id,
        name: item.name === '—' ? '' : item.name,
        category: item.category === '—' ? '' : item.category,
        unit: item.unit === '—' ? '' : item.unit,
        specification: item.specification || '',
        status: item.status || 'ENABLED',
      },
      statusIndex: item.status === 'DISABLED' ? 1 : 0,
    });
  },

  closeForm(e) {
    if (e && e.detail && e.detail.visible === false) {
      this.setData({ showForm: false, submitting: false });
    }
  },

  onFormField(e) {
    const { field } = e.currentTarget.dataset;
    this.setData({ [`form.${field}`]: e.detail.value || '' });
  },

  onPickCategory(e) {
    const value = e.currentTarget.dataset.value || '';
    this.setData({ 'form.category': value });
  },

  onStatusChange(e) {
    const index = Number(e.detail.value);
    const status = this.data.statusOptions[index]?.value || 'ENABLED';
    this.setData({ statusIndex: index, 'form.status': status });
  },

  async submitForm() {
    if (this.data.submitting) return;
    const f = this.data.form;
    if (!f.name || !f.name.trim()) {
      wx.showToast({ title: '请输入商品名称', icon: 'none' });
      return;
    }
    if (!f.unit || !f.unit.trim()) {
      wx.showToast({ title: '请输入单位', icon: 'none' });
      return;
    }
    const payload = {
      name: f.name.trim(),
      category: f.category?.trim() || '',
      unit: f.unit.trim(),
      specification: f.specification?.trim() || '',
      status: f.status || 'ENABLED',
    };
    this.setData({ submitting: true });
    try {
      if (this.data.formMode === 'edit' && f.id) {
        await updateProduct(f.id, payload);
        wx.showToast({ title: '已保存', icon: 'success' });
      } else {
        await createProduct(payload);
        wx.showToast({ title: '已新增', icon: 'success' });
      }
      this.setData({ showForm: false, submitting: false });
      this.loadSummary();
      this.loadList();
      this.loadCategorySuggestions();
    } catch (e) {
      this.setData({ submitting: false });
      wx.showToast({ title: e.message || '保存失败', icon: 'none' });
    }
  },

  onDelete(e) {
    const id = Number(e.currentTarget.dataset.id);
    const { name } = e.currentTarget.dataset;
    wx.showModal({
      title: '删除商品',
      content: `确定删除「${name}」？`,
      confirmColor: '#e34d59',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await deleteProduct(id);
          wx.showToast({ title: '已删除', icon: 'success' });
          this.loadSummary();
          this.loadList();
        } catch (err) {
          wx.showToast({ title: err.message || '删除失败', icon: 'none' });
        }
      },
    });
  },

  stopBubble() {},
});
