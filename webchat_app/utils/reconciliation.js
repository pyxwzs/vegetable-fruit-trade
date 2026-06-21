const PAGE_SIZES = [5, 10, 20];

export function buildYearOptions(count = 5) {
  const y = new Date().getFullYear();
  return Array.from({ length: count }, (_, i) => y - i);
}

export function buildMonthOptions() {
  return Array.from({ length: 12 }, (_, i) => ({ label: `${i + 1}月`, value: i + 1 }));
}

export function fmtAmount(v) {
  return Number(v || 0).toFixed(2);
}

export function calcTotalPages(total, pageSize) {
  return Math.max(1, Math.ceil(Number(total || 0) / pageSize));
}

export function slicePage(rows, page, pageSize) {
  const start = (page - 1) * pageSize;
  return (rows || []).slice(start, start + pageSize);
}

export function mapPartnerRows(rows) {
  return (rows || []).map((r) => ({
    entityId: r.entityId,
    name: r.name || '—',
    orderCount: r.orderCount || 0,
    totalAmountText: fmtAmount(r.totalAmount),
    settledAmountText: fmtAmount(r.settledAmount),
    pendingAmountText: fmtAmount(r.pendingAmount),
    hasPending: Number(r.pendingAmount) > 0,
  }));
}

export function mapSummaryRows(rows) {
  return (rows || []).map((r) => ({
    month: r.month,
    orderCount: r.orderCount || 0,
    totalAmountText: fmtAmount(r.totalAmount),
    pendingAmountText: fmtAmount(r.pendingAmount),
    hasPending: Number(r.pendingAmount) > 0,
  }));
}

export function mapDailyRows(rows) {
  return (rows || []).map((r) => ({
    date: r.date || '',
    dayLabel: r.date ? `${r.date.slice(8)}日` : '—',
    orderCount: r.orderCount || 0,
    totalAmountText: fmtAmount(r.totalAmount),
    pendingAmountText: fmtAmount(r.pendingAmount),
    hasPending: Number(r.pendingAmount) > 0,
  }));
}

function priceText(row) {
  const p = fmtAmount(row.price);
  return row.unit ? `${p} 元/${row.unit}` : `${p} 元`;
}

function qtyText(row) {
  const q = Number(row.quantity || 0).toFixed(1);
  return row.unit ? `${q} ${row.unit}` : q;
}

export function buildDetailView(items, filterDate) {
  const list = filterDate ? (items || []).filter((i) => i.date === filterDate) : (items || []);
  const map = new Map();
  list.forEach((item) => {
    const d = item.date || '未知';
    if (!map.has(d)) map.set(d, { date: d, items: [], total: 0, totalText: '0.00' });
    const g = map.get(d);
    g.items.push({
      productName: item.productName || '—',
      priceText: priceText(item),
      qtyText: qtyText(item),
      amountText: fmtAmount(item.amount),
    });
    g.total += Number(item.amount || 0);
    g.totalText = fmtAmount(g.total);
  });
  const days = [...map.values()].sort((a, b) => String(a.date).localeCompare(String(b.date)));
  const monthTotal = days.reduce((s, d) => s + d.total, 0);
  return {
    days,
    monthTotalText: fmtAmount(monthTotal),
    totalLabel: filterDate ? '当日合计' : '本月合计',
  };
}

export function createPanelState(entityLabel) {
  const month = new Date().getMonth() + 1;
  return {
    entityLabel,
    entityId: null,
    entityIndex: 0,
    entityName: '',
    entityOptions: [{ id: '', name: `全部${entityLabel}` }],
    yearIndex: 0,
    monthIndex: month - 1,
    viewMode: 'year',
    selectedMonth: month,
    selectedDay: '',
    partnerRows: [],
    partnerPendingText: '0.00',
    summaryRows: [],
    dailyRows: [],
    detailDays: [],
    detailTotalText: '0.00',
    detailTotalLabel: '本月合计',
    listMode: 'partner',
    page: 1,
    pageSize: 5,
    pageSizeIndex: 0,
    totalPages: 1,
    displayRows: [],
  };
}

export function paginatePanel(panel) {
  let source = [];
  if (panel.listMode === 'partner') source = panel.partnerRows;
  else if (panel.listMode === 'summary') source = panel.summaryRows;
  else if (panel.listMode === 'daily') source = panel.dailyRows;
  const totalPages = calcTotalPages(source.length, panel.pageSize);
  const page = Math.min(panel.page, totalPages);
  return {
    ...panel,
    page,
    totalPages,
    displayRows: slicePage(source, page, panel.pageSize),
  };
}

export function buildReconciliationExportFilename({
  isPurchase,
  entityId,
  entityName,
  year,
  month,
  viewMode,
}) {
  const label = isPurchase ? '采购' : '销售';
  const period =
    viewMode === 'month' && month ? `${year}年${month}月` : `${year}年`;
  if (!entityId) {
    return `${period}${label}对账汇总.xlsx`;
  }
  const name = (entityName || label).replace(/[/\\:*?"<>|]/g, '_');
  if (viewMode === 'year') {
    return `${name}_${year}年${label}对账.xlsx`;
  }
  return `${name}_${year}年${month}月${label}对账.xlsx`;
}

export { PAGE_SIZES };
