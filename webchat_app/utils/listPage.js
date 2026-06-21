import { getSuppliers, getCustomers, getExpenses } from '~/api/partner';
import { requireLogin } from '~/utils/auth';
import { fmtMoney, fmtDate } from '~/utils/format';

function createListPage(type) {
  const loaders = {
    suppliers: () => getSuppliers({ page: 0, size: 100 }),
    customers: () => getCustomers({ page: 0, size: 100 }),
    expenses: () => getExpenses({ page: 0, size: 100 }),
  };

  const mappers = {
    suppliers: (row) => ({
      id: row.id,
      title: row.name,
      note: row.contactPerson || row.phone || '-',
      desc: row.address || row.remark || '',
    }),
    customers: (row) => ({
      id: row.id,
      title: row.name,
      note: row.contactPerson || row.phone || '-',
      desc: row.address || row.remark || '',
    }),
    expenses: (row) => ({
      id: row.id,
      title: row.category || row.description || '支出',
      note: `¥${fmtMoney(row.amount)}`,
      desc: fmtDate(row.expenseDate || row.createTime),
    }),
  };

  return Page({
    data: { list: [], loading: false },

    onShow() {
      if (!requireLogin()) return;
      this.loadList();
    },

    onPullDownRefresh() {
      this.loadList().finally(() => wx.stopPullDownRefresh());
    },

    async loadList() {
      this.setData({ loading: true });
      try {
        const res = await loaders[type]();
        const page = res.data || {};
        const rows = page.content || page || [];
        const list = rows.map(mappers[type]);
        this.setData({ list, loading: false });
      } catch (e) {
        this.setData({ loading: false });
        wx.showToast({ title: e.message || '加载失败', icon: 'none' });
      }
    },
  });
}

export default createListPage;
