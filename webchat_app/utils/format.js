export function fmtMoney(value) {
  const n = Number(value);
  if (Number.isNaN(n)) return '0.00';
  return n.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

export function fmtDate(value) {
  if (!value) return '-';
  const s = String(value);
  return s.length >= 10 ? s.slice(0, 10) : s;
}

export function currentMonthLabel() {
  const d = new Date();
  return `${d.getFullYear()}年${d.getMonth() + 1}月`;
}
