export const SUPPLIER_STATUS = {
  ACTIVE: '启用',
  INACTIVE: '停用',
};

export function supplierStatusTheme(status) {
  return status === 'ACTIVE' ? 'success' : 'default';
}

export function mapSupplier(row) {
  return {
    id: row.id,
    name: row.name || '—',
    contact: row.contact || '—',
    phone: row.phone || '—',
    address: row.address || '',
    status: row.status || 'ACTIVE',
    statusText: SUPPLIER_STATUS[row.status] || row.status || '—',
    statusTheme: supplierStatusTheme(row.status),
  };
}
