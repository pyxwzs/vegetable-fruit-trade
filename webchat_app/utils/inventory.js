export function fmtQty(value) {
  const n = Number(value);
  if (Number.isNaN(n)) return '0.0';
  return n.toFixed(1);
}

export function mapInventoryItem(row) {
  const category = row.category || '未分类';
  return {
    id: row.productId || row.id,
    name: row.productName || row.name || '—',
    category,
    unit: row.unit || '',
    quantity: Number(row.quantity || 0),
    quantityText: fmtQty(row.quantity),
    unitCost: Number(row.unitCost || 0),
    value: Number(row.value || 0),
  };
}

export function buildCategoryFilters(items) {
  const categories = [...new Set((items || []).map((i) => i.category || '未分类'))].sort();
  return [{ label: '全部分类', value: 'ALL' }].concat(
    categories.map((c) => ({ label: c, value: c })),
  );
}

export function filterInventoryItems(items, keyword, categoryKey) {
  let rows = items || [];
  if (categoryKey && categoryKey !== 'ALL') {
    rows = rows.filter((r) => (r.category || '未分类') === categoryKey);
  }
  if (keyword) {
    const kw = keyword.toLowerCase();
    rows = rows.filter(
      (r) =>
        (r.name || '').toLowerCase().includes(kw) ||
        (r.category || '').toLowerCase().includes(kw),
    );
  }
  return rows;
}

export function paginateItems(items, page, pageSize) {
  const total = items.length;
  const start = (page - 1) * pageSize;
  return {
    list: items.slice(start, start + pageSize),
    total,
    totalPages: Math.max(1, Math.ceil(total / pageSize)),
  };
}
