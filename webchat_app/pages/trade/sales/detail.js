import {
  getSalesOrder,
  completeSalesOrder,
  collectSalesOrder,
  getSalesPayments,
  cancelSalesOrder,
} from '~/api/sales';
import { requireLogin } from '~/utils/auth';
import { fmtMoney, fmtDate } from '~/utils/format';
import {
  SALES_STATUS,
  paymentStatusLabel,
  salesStatusTheme,
  paymentStatusTheme,
  calcUnpaid,
} from '~/utils/order';

Page({
  data: {
    loading: true,
    order: null,
    items: [],
    payments: [],
    payAmount: '',
    showPay: false,
    unpaid: 0,
    unpaidText: '0.00',
    canComplete: false,
    canCollect: false,
    canCancel: false,
  },

  onLoad(options) {
    this.orderId = options.id;
  },

  onShow() {
    if (!requireLogin() || !this.orderId) return;
    this.loadDetail();
  },

  async loadDetail() {
    this.setData({ loading: true });
    try {
      const [orderRes, payRes] = await Promise.all([
        getSalesOrder(this.orderId),
        getSalesPayments(this.orderId).catch(() => ({ data: [] })),
      ]);
      const order = orderRes.data || {};
      const unpaid = calcUnpaid(order.totalAmount, order.receivedAmount);
      order.statusText = SALES_STATUS[order.status] || order.status;
      order.paymentText = paymentStatusLabel(order.paymentStatus);
      order.statusTheme = salesStatusTheme(order.status);
      order.paymentTheme = paymentStatusTheme(order.paymentStatus);
      order.totalText = fmtMoney(order.totalAmount);
      order.receivedText = fmtMoney(order.receivedAmount);
      order.dateText = fmtDate(order.orderDate);
      order.customerName = order.customer?.name || '—';

      const items = (order.items || []).map((it) => ({
        id: it.id,
        name: it.product?.name || '商品',
        qty: it.quantity,
        price: it.price,
        amount: it.amount,
        unit: it.product?.unit || '',
        priceText: fmtMoney(it.price),
        amountText: fmtMoney(it.amount),
      }));

      this.setData({
        order,
        items,
        payments: (payRes.data || []).map((p) => ({
          ...p,
          dateText: fmtDate(p.paymentDate),
          amountText: fmtMoney(p.amount),
        })),
        unpaid,
        unpaidText: fmtMoney(unpaid),
        canComplete: order.status === 'PENDING',
        canCollect: order.status !== 'CANCELLED' && order.paymentStatus !== 'PAID',
        canCancel: order.status === 'PENDING',
        loading: false,
      });
    } catch (e) {
      this.setData({ loading: false });
      wx.showToast({ title: e.message || '加载失败', icon: 'none' });
    }
  },

  onComplete() {
    wx.showModal({
      title: '确认出库',
      content: '确认该销售单货物已出库？',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await completeSalesOrder(this.orderId);
          wx.showToast({ title: '已出库', icon: 'success' });
          this.loadDetail();
        } catch (e) {
          wx.showToast({ title: e.message || '操作失败', icon: 'none' });
        }
      },
    });
  },

  onCancel() {
    wx.showModal({
      title: '取消订单',
      content: '确定取消该销售单？',
      confirmColor: '#e34d59',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await cancelSalesOrder(this.orderId);
          wx.showToast({ title: '已取消', icon: 'success' });
          this.loadDetail();
        } catch (e) {
          wx.showToast({ title: e.message || '操作失败', icon: 'none' });
        }
      },
    });
  },

  openPay() {
    const unpaid = this.data.unpaid;
    this.setData({ showPay: true, payAmount: unpaid > 0 ? String(unpaid.toFixed(2)) : '' });
  },

  closePay(e) {
    if (e && e.detail && e.detail.visible === false) {
      this.setData({ showPay: false });
    }
  },

  onPayAmountChange(e) {
    this.setData({ payAmount: e.detail.value || '' });
  },

  async submitPay() {
    const amount = Number(this.data.payAmount);
    if (!amount || amount <= 0) {
      wx.showToast({ title: '请输入有效金额', icon: 'none' });
      return;
    }
    try {
      await collectSalesOrder(this.orderId, {
        amount,
        paymentDate: new Date().toISOString().slice(0, 10),
        paymentMethod: '转账',
      });
      wx.showToast({ title: '收款成功', icon: 'success' });
      this.setData({ showPay: false });
      this.loadDetail();
    } catch (e) {
      wx.showToast({ title: e.message || '收款失败', icon: 'none' });
    }
  },
});
