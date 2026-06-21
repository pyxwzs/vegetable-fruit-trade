export const CUSTOMER_STATUS = {
  ACTIVE: '启用',
  INACTIVE: '停用',
};

export function customerStatusTheme(status) {
  return status === 'ACTIVE' ? 'success' : 'default';
}

export function mapCustomer(row) {
  return {
    id: row.id,
    name: row.name || '—',
    contact: row.contact || '—',
    phone: row.phone || '—',
    address: row.address || '',
    status: row.status || 'ACTIVE',
    statusText: CUSTOMER_STATUS[row.status] || row.status || '—',
    statusTheme: customerStatusTheme(row.status),
  };
}
