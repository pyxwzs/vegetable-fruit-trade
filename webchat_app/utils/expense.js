export {
  todayStr,
  thisMonthStr,
  thisYearStr,
  monthRange,
  yearRange,
} from './dateQuery';

export function mapExpense(row) {
  const date = row.expenseDate || '';
  return {
    id: row.id,
    expenseDate: typeof date === 'string' ? date.slice(0, 10) : date,
    category: row.category || '—',
    amount: Number(row.amount || 0),
  };
}

export function aggregateExpenses(rows) {
  const map = {};
  (rows || []).forEach((r) => {
    const key = r.category || '其他';
    if (!map[key]) map[key] = { category: key, amount: 0, count: 0 };
    map[key].amount += Number(r.amount || 0);
    map[key].count += 1;
  });
  const items = Object.values(map).sort((a, b) => b.amount - a.amount);
  const totalAmount = items.reduce((s, r) => s + r.amount, 0);
  return {
    count: (rows || []).length,
    totalAmount,
    items,
  };
}
