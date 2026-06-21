import {
  getSuppliers,
  getActiveSuppliers,
  createSupplier,
  updateSupplier,
  deleteSupplier,
} from '~/api/partner';
import { requireLogin } from '~/utils/auth';
import { mapSupplier } from '~/utils/supplier';

const FILTERS = [
  { label: '全部', value: 'ALL' },
  { label: '启用', value: 'ACTIVE' },
  { label: '停用', value: 'INACTIVE' },
];

const PAGE_SIZES = [5, 10, 20];

const EMPTY_FORM = {
  id: null,
  name: '',
  contact: '',
  phone: '',
  address: '',
  status: 'ACTIVE',
};

function calcTotalPages(total, pageSize) {
  return Math.max(1, Math.ceil(Number(total || 0) / pageSize));
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
    activeCount: 0,
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
      { label: '启用', value: 'ACTIVE' },
      { label: '停用', value: 'INACTIVE' },
    ],
    statusIndex: 0,
  },

  onShow() {
    if (!requireLogin()) return;
    this.loadSummary();
    this.loadList();
  },

  onPullDownRefresh() {
    Promise.all([this.loadSummary(), this.loadList()]).finally(() => wx.stopPullDownRefresh());
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
      const [totalRes, activeRes] = await Promise.all([
        getSuppliers({ page: 0, size: 1 }),
        getActiveSuppliers(),
      ]);
      this.setData({
        totalCount: Number(totalRes.data?.totalElements || 0),
        activeCount: (activeRes.data || []).length,
      });
    } catch {
      this.setData({ totalCount: 0, activeCount: 0 });
    }
  },

  async loadList(scrollTop) {
    this.setData({ loading: true });
    try {
      const { page, pageSize, filterKey, keyword } = this.data;
      const params = { page: page - 1, size: pageSize };
      if (filterKey && filterKey !== 'ALL') params.status = filterKey;
      if (keyword) params.keyword = keyword;

      const res = await getSuppliers(params);
      const pageData = res.data || {};
      const total = Number(pageData.totalElements || 0);
      this.setData({
        list: (pageData.content || []).map(mapSupplier),
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
    const item = this.data.list.find((s) => s.id === id);
    if (!item) return;
    this.setData({
      showForm: true,
      formMode: 'edit',
      form: {
        id: item.id,
        name: item.name === '—' ? '' : item.name,
        contact: item.contact === '—' ? '' : item.contact,
        phone: item.phone === '—' ? '' : item.phone,
        address: item.address || '',
        status: item.status || 'ACTIVE',
      },
      statusIndex: item.status === 'INACTIVE' ? 1 : 0,
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

  onStatusChange(e) {
    const index = Number(e.detail.value);
    const status = this.data.statusOptions[index]?.value || 'ACTIVE';
    this.setData({ statusIndex: index, 'form.status': status });
  },

  onCallPhone(e) {
    const { phone } = e.currentTarget.dataset;
    if (!phone || phone === '—') return;
    wx.makePhoneCall({ phoneNumber: phone });
  },

  async submitForm() {
    if (this.data.submitting) return;
    const f = this.data.form;
    if (!f.name || !f.name.trim()) {
      wx.showToast({ title: '请输入供应商名称', icon: 'none' });
      return;
    }
    const payload = {
      name: f.name.trim(),
      contact: f.contact?.trim() || undefined,
      phone: f.phone?.trim() || undefined,
      address: f.address?.trim() || undefined,
      status: f.status || 'ACTIVE',
    };
    this.setData({ submitting: true });
    try {
      if (this.data.formMode === 'edit' && f.id) {
        await updateSupplier(f.id, payload);
        wx.showToast({ title: '已保存', icon: 'success' });
      } else {
        await createSupplier(payload);
        wx.showToast({ title: '已新增', icon: 'success' });
      }
      this.setData({ showForm: false, submitting: false });
      this.loadSummary();
      this.loadList();
    } catch (e) {
      this.setData({ submitting: false });
      wx.showToast({ title: e.message || '保存失败', icon: 'none' });
    }
  },

  onDelete(e) {
    const id = Number(e.currentTarget.dataset.id);
    const { name } = e.currentTarget.dataset;
    wx.showModal({
      title: '删除供应商',
      content: `确定删除「${name}」？`,
      confirmColor: '#e34d59',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await deleteSupplier(id);
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
