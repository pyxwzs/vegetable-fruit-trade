import {
  getPurchaseOrder,
  completePurchaseOrder,
  payPurchaseOrder,
  getPurchasePayments,
  cancelPurchaseOrder,
} from '~/api/purchase';
import { requireLogin } from '~/utils/auth';
import { fmtMoney, fmtDate } from '~/utils/format';
import {
  PURCHASE_STATUS,
  paymentStatusLabel,
  purchaseStatusTheme,
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
    canPay: false,
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
        getPurchaseOrder(this.orderId),
        getPurchasePayments(this.orderId).catch(() => ({ data: [] })),
      ]);
      const order = orderRes.data || {};
      const unpaid = calcUnpaid(order.totalAmount, order.paidAmount);
      order.statusText = PURCHASE_STATUS[order.status] || order.status;
      order.paymentText = paymentStatusLabel(order.paymentStatus);
      order.statusTheme = purchaseStatusTheme(order.status);
      order.paymentTheme = paymentStatusTheme(order.paymentStatus);
      order.totalText = fmtMoney(order.totalAmount);
      order.paidText = fmtMoney(order.paidAmount);
      order.dateText = fmtDate(order.orderDate);
      order.supplierName = order.supplier?.name || '—';

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
        canPay: order.status !== 'CANCELLED' && order.paymentStatus !== 'PAID',
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
      title: '确认入库',
      content: '确认该采购单货物已入库？',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await completePurchaseOrder(this.orderId);
          wx.showToast({ title: '已入库', icon: 'success' });
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
      content: '确定取消该采购单？',
      confirmColor: '#e34d59',
      success: async (res) => {
        if (!res.confirm) return;
        try {
          await cancelPurchaseOrder(this.orderId);
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
      await payPurchaseOrder(this.orderId, {
        amount,
        paymentDate: new Date().toISOString().slice(0, 10),
        paymentMethod: '转账',
      });
      wx.showToast({ title: '付款成功', icon: 'success' });
      this.setData({ showPay: false });
      this.loadDetail();
    } catch (e) {
      wx.showToast({ title: e.message || '付款失败', icon: 'none' });
    }
  },

});
