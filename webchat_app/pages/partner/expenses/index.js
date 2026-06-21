import {
  getExpenses,
  getExpenseCategories,
  createExpense,
  updateExpense,
  deleteExpense,
} from '~/api/partner';
import { requireLogin } from '~/utils/auth';
import { fmtMoney } from '~/utils/format';
import {
  todayStr,
  mapExpense,
  aggregateExpenses,
} from '~/utils/expense';
import {
  defaultDateState,
  applyDateQuery,
  periodPrefix,
  createDateQueryHandlers,
} from '~/utils/dateQuery';

const PAGE_SIZES = [5, 10, 20];

const EMPTY_FORM = {
  id: null,
  expenseDate: todayStr(),
  category: '',
  amount: '',
};

function calcTotalPages(total, pageSize) {
  return Math.max(1, Math.ceil(Number(total || 0) / pageSize));
}

Page({
  data: {
    list: [],
    loading: false,
    periodSummary: { count: 0, totalAmount: 0, items: [] },
    periodAmountText: '0.00',
    periodCount: 0,
    periodPrefix: '当日',
    keyword: '',
    ...defaultDateState(),
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
    categorySuggestions: [],
    summaryLoading: false,
  },

  onShow() {
    if (!requireLogin()) return;
    this.loadCategorySuggestions();
    this.loadSummary();
    this.loadList();
  },

  onPullDownRefresh() {
    Promise.all([this.loadCategorySuggestions(), this.loadSummary(), this.loadList()])
      .finally(() => wx.stopPullDownRefresh());
  },

  ...createDateQueryHandlers(function reloadDateQuery() {
    this.loadSummary();
    this.loadList();
  }),

  async loadCategorySuggestions() {
    try {
      const res = await getExpenseCategories();
      this.setData({ categorySuggestions: res.data || [] });
    } catch {
      this.setData({ categorySuggestions: [] });
    }
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
    this.setData({ summaryLoading: true });
    try {
      const { dateMode, selectedDate, selectedMonth, selectedYear } = this.data;
      const params = { page: 0, size: 500 };
      applyDateQuery(params, dateMode, selectedDate, selectedMonth, selectedYear);

      const res = await getExpenses(params);
      const rows = (res.data?.content || []).map(mapExpense);
      const summary = aggregateExpenses(rows);
      const totalAmount = summary.totalAmount || 0;
      summary.items = summary.items.map((item) => {
        const percent = totalAmount > 0 ? (item.amount / totalAmount) * 100 : 0;
        return {
          ...item,
          barWidth: Math.round(percent),
          percentText: `${percent.toFixed(1)}%`,
        };
      });

      this.setData({
        periodSummary: summary,
        periodAmountText: fmtMoney(summary.totalAmount),
        periodCount: summary.count,
        periodPrefix: periodPrefix(dateMode),
        summaryLoading: false,
      });
    } catch {
      this.setData({
        periodSummary: { count: 0, totalAmount: 0, items: [] },
        periodAmountText: '0.00',
        periodCount: 0,
        periodPrefix: periodPrefix(this.data.dateMode),
        summaryLoading: false,
      });
    }
  },

  async loadList(scrollTop) {
    this.setData({ loading: true });
    try {
      const { page, pageSize, keyword, dateMode, selectedDate, selectedMonth, selectedYear } = this.data;
      const params = { page: page - 1, size: pageSize };
      applyDateQuery(params, dateMode, selectedDate, selectedMonth, selectedYear);
      if (keyword) params.keyword = keyword;

      const res = await getExpenses(params);
      const pageData = res.data || {};
      const total = Number(pageData.totalElements || 0);
      this.setData({
        list: (pageData.content || []).map(mapExpense),
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
      form: { ...EMPTY_FORM, expenseDate: todayStr() },
    });
  },

  openEdit(e) {
    const id = Number(e.currentTarget.dataset.id);
    const item = this.data.list.find((r) => r.id === id);
    if (!item) return;
    this.setData({
      showForm: true,
      formMode: 'edit',
      form: {
        id: item.id,
        expenseDate: item.expenseDate,
        category: item.category === '—' ? '' : item.category,
        amount: String(item.amount),
      },
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

  onFormDateChange(e) {
    this.setData({ 'form.expenseDate': e.detail.value });
  },

  onPickCategory(e) {
    const value = e.currentTarget.dataset.value || '';
    this.setData({ 'form.category': value });
  },

  async submitForm() {
    if (this.data.submitting) return;
    const f = this.data.form;
    if (!f.expenseDate) {
      wx.showToast({ title: '请选择日期', icon: 'none' });
      return;
    }
    if (!f.category || !f.category.trim()) {
      wx.showToast({ title: '请填写分类', icon: 'none' });
      return;
    }
    const amount = Number(f.amount);
    if (!amount || amount <= 0) {
      wx.showToast({ title: '请输入有效金额', icon: 'none' });
      return;
    }

    const payload = {
      expenseDate: f.expenseDate,
      category: f.category.trim(),
      amount,
    };
    this.setData({ submitting: true });
    try {
      if (this.data.formMode === 'edit' && f.id) {
        await updateExpense(f.id, payload);
        wx.showToast({ title: '已保存', icon: 'success' });
      } else {
        await createExpense(payload);
        wx.showToast({ title: '已新增', icon: 'success' });
      }
      this.setData({ showForm: false, submitting: false });
      this.loadCategorySuggestions();
      this.loadSummary();
      this.loadList();
    } catch (e) {
      this.setData({ submitting: false });
      wx.showToast({ title: e.message || '保存失败', icon: 'none' });
    }
  },

  onDelete(e) {
    const id = Number(e.currentTarget.dataset.id);
    const { category, amount } = e.currentTarget.dataset;
    wx.showModal({
      title: '删除支出',
      content: `确定删除「${category} ¥${amount}」？`,
      confirmColor: '#e34d59',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await deleteExpense(id);
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
