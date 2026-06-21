import * as XLSX from 'xlsx-js-style'

const CELL_CENTER = {
  alignment: { horizontal: 'center', vertical: 'center' }
}

function applyCenterAlign(ws) {
  if (!ws['!ref']) return
  const range = XLSX.utils.decode_range(ws['!ref'])
  for (let r = range.s.r; r <= range.e.r; r++) {
    for (let c = range.s.c; c <= range.e.c; c++) {
      const addr = XLSX.utils.encode_cell({ r, c })
      if (!ws[addr]) ws[addr] = { t: 's', v: '' }
      ws[addr].s = CELL_CENTER
    }
  }
}

function normalizeDate(raw) {
  if (!raw) return ''
  if (Array.isArray(raw)) {
    const [y, m, d] = raw
    return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`
  }
  return String(raw).slice(0, 10)
}

function excelSerial(year, month, day) {
  return Math.floor((Date.UTC(year, month - 1, day) - Date.UTC(1899, 11, 30)) / 86400000)
}

function buildMerges(productCount) {
  const merges = []
  for (let i = 0; i < productCount; i++) {
    const startCol = 1 + i * 3
    merges.push({ s: { r: 0, c: startCol }, e: { r: 0, c: startCol + 2 } })
  }
  const totalCol = 1 + productCount * 3
  merges.push({ s: { r: 0, c: totalCol }, e: { r: 1, c: totalCol } })
  return merges
}

function styleCell(ws, r, c, patch) {
  const addr = XLSX.utils.encode_cell({ r, c })
  if (!ws[addr]) return
  Object.assign(ws[addr], patch)
  ws[addr].s = CELL_CENTER
}

/** 数字列必须指定格式，否则 WPS/Excel 会把 250 当成日期序列号显示成 1900-08-12 */
function applyMonthNumberFormats(ws, { dataDates, productCount }) {
  const totalCol = 1 + productCount * 3
  dataDates.forEach((dateStr, idx) => {
    const r = 2 + idx
    const [y, m, d] = dateStr.split('-').map(Number)
    styleCell(ws, r, 0, { t: 'n', v: excelSerial(y, m, d), z: 'yyyy-mm-dd' })
    for (let p = 0; p < productCount; p++) {
      const base = 1 + p * 3
      styleCell(ws, r, base, { z: '0.0' })
      styleCell(ws, r, base + 1, { z: '0.00' })
      styleCell(ws, r, base + 2, { z: '0.00' })
    }
    styleCell(ws, r, totalCol, { z: '0.00' })
  })
  const totalRow = 2 + dataDates.length
  for (let p = 0; p < productCount; p++) {
    const base = 1 + p * 3
    styleCell(ws, totalRow, base, { z: '0.0' })
    styleCell(ws, totalRow, base + 1, { z: '0.00' })
    styleCell(ws, totalRow, base + 2, { z: '0.00' })
  }
  styleCell(ws, totalRow, totalCol, { z: '0.00' })
}

function applySheetMeta(ws, { dataDates, productCount }) {
  ws['!merges'] = buildMerges(productCount)
  const colWidths = [{ wch: 12 }]
  for (let i = 0; i < productCount; i++) {
    colWidths.push({ wch: 12 }, { wch: 14 }, { wch: 12 })
  }
  colWidths.push({ wch: 12 })
  ws['!cols'] = colWidths

  applyCenterAlign(ws)
  applyMonthNumberFormats(ws, { dataDates, productCount })
}

function buildProductUnits(items) {
  const map = {}
  for (const item of items || []) {
    if (item.productName && item.unit) map[item.productName] = item.unit
  }
  return map
}

function qtyLabel(unit) {
  return unit ? `重量(${unit})` : '重量'
}

function priceLabel(unit) {
  return unit ? `单价(元/${unit})` : '单价(元)'
}

const AMOUNT_LABEL = '金额(元)'

/** 按月对账表：品种名 + 重量(单位)/单价(元/单位)/金额(元) */
export function buildMonthSheetAoa(items) {
  const products = [...new Set((items || []).map(i => i.productName).filter(Boolean))].sort()
  const productUnits = buildProductUnits(items)

  const byDateProduct = {}
  for (const item of items || []) {
    const date = normalizeDate(item.date)
    const name = item.productName
    if (!date || !name) continue
    if (!byDateProduct[date]) byDateProduct[date] = {}
    if (!byDateProduct[date][name]) byDateProduct[date][name] = { qty: 0, amount: 0 }
    byDateProduct[date][name].qty += Number(item.quantity || 0)
    byDateProduct[date][name].amount += Number(item.amount || 0)
  }

  const dataDates = Object.keys(byDateProduct)
    .filter(d => Object.values(byDateProduct[d]).some(c => c.qty > 0 || c.amount > 0))
    .sort()

  const row1 = ['']
  for (const p of products) row1.push(p, '', '')
  row1.push('总计')

  const row2 = ['日期']
  products.forEach(p => {
    const u = productUnits[p] || ''
    row2.push(qtyLabel(u), priceLabel(u), AMOUNT_LABEL)
  })
  row2.push(AMOUNT_LABEL)

  const rows = [row1, row2]
  const productTotals = Object.fromEntries(products.map(p => [p, { qty: 0, amount: 0 }]))
  let grandTotal = 0

  for (const dateStr of dataDates) {
    const row = ['']
    let dayTotal = 0

    for (const p of products) {
      const cell = byDateProduct[dateStr]?.[p]
      const qty = cell?.qty || 0
      const amount = cell?.amount || 0
      const price = qty > 0 ? amount / qty : 0
      row.push(
        qty ? Number(qty.toFixed(1)) : '',
        price ? Number(price.toFixed(2)) : '',
        amount ? Number(amount.toFixed(2)) : ''
      )
      productTotals[p].qty += qty
      productTotals[p].amount += amount
      dayTotal += amount
    }
    row.push(dayTotal ? Number(dayTotal.toFixed(2)) : '')
    grandTotal += dayTotal
    rows.push(row)
  }

  const totalRow = ['总计']
  for (const p of products) {
    const t = productTotals[p]
    const price = t.qty > 0 ? t.amount / t.qty : 0
    totalRow.push(
      t.qty ? Number(t.qty.toFixed(1)) : '',
      price ? Number(price.toFixed(2)) : '',
      t.amount ? Number(t.amount.toFixed(2)) : ''
    )
  }
  totalRow.push(Number(grandTotal.toFixed(2)))
  rows.push(totalRow)

  return {
    rows,
    productCount: products.length,
    dataDates,
    monthTotal: grandTotal,
    hasData: dataDates.length > 0
  }
}

function buildYearSummarySheetAoa(monthEntries, { entityName, year, type }) {
  const label = type === 'purchase' ? '采购' : '销售'
  const rows = [
    [`${entityName} · ${year}年${label}月度汇总`],
    ['月份', '金额(元)']
  ]
  let yearTotal = 0
  for (const { month, amount } of monthEntries) {
    yearTotal += amount
    rows.push([`${month}月`, Number(amount.toFixed(2))])
  }
  rows.push(['总计', Number(yearTotal.toFixed(2))])
  return rows
}

function applySummarySheetMeta(ws) {
  ws['!merges'] = [{ s: { r: 0, c: 0 }, e: { r: 0, c: 1 } }]
  ws['!cols'] = [{ wch: 10 }, { wch: 14 }]
  applyCenterAlign(ws)
  if (!ws['!ref']) return
  const range = XLSX.utils.decode_range(ws['!ref'])
  for (let r = 2; r <= range.e.r; r++) {
    styleCell(ws, r, 1, { z: '0.00' })
  }
}

export function downloadMonthExcel(items, opts) {
  const { entityName, year, month, type } = opts
  const label = type === 'purchase' ? '采购' : '销售'
  const { rows, productCount, dataDates, hasData } = buildMonthSheetAoa(items)
  if (!hasData) return false
  const ws = XLSX.utils.aoa_to_sheet(rows)
  applySheetMeta(ws, { dataDates, productCount })
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, `${month}月`)
  XLSX.writeFile(wb, `${entityName}_${year}年${month}月${label}对账.xlsx`)
  return true
}

export async function downloadYearExcel(fetchMonthItems, opts) {
  const { entityName, year, type } = opts
  const label = type === 'purchase' ? '采购' : '销售'
  const wb = XLSX.utils.book_new()

  const monthData = await Promise.all(
    Array.from({ length: 12 }, (_, i) => fetchMonthItems(year, i + 1))
  )

  const monthSheets = monthData
    .map((items, idx) => ({
      month: idx + 1,
      ...buildMonthSheetAoa(items)
    }))
    .filter(s => s.hasData)

  if (!monthSheets.length) return false

  const summaryWs = XLSX.utils.aoa_to_sheet(
    buildYearSummarySheetAoa(
      monthSheets.map(s => ({ month: s.month, amount: s.monthTotal })),
      { entityName, year, type }
    )
  )
  applySummarySheetMeta(summaryWs)
  XLSX.utils.book_append_sheet(wb, summaryWs, '年度汇总')

  monthSheets.forEach(({ rows, productCount, dataDates, month }) => {
    const ws = XLSX.utils.aoa_to_sheet(rows)
    applySheetMeta(ws, { dataDates, productCount })
    XLSX.utils.book_append_sheet(wb, ws, `${month}月`)
  })

  XLSX.writeFile(wb, `${entityName}_${year}年${label}对账.xlsx`)
  return true
}

/** 导出供应商/客户汇总表 */
export function downloadPartnerListExcel(report, { type, periodLabel }) {
  const label = type === 'purchase' ? '采购' : '销售'
  const settledLabel = type === 'purchase' ? '已付' : '已收'
  const pendingLabel = type === 'purchase' ? '未付' : '未收'
  const title = `${periodLabel}${label}对账汇总`
  const headers = ['名称', '单数', `${label}总额(元)`, `${settledLabel}(元)`, `${pendingLabel}(元)`]
  const rows = [[title], headers]
  for (const r of report.rows || []) {
    rows.push([
      r.name,
      r.orderCount,
      Number(r.totalAmount || 0).toFixed(2),
      Number(r.settledAmount || 0).toFixed(2),
      Number(r.pendingAmount || 0).toFixed(2)
    ])
  }
  rows.push([
    '合计',
    (report.rows || []).reduce((s, r) => s + Number(r.orderCount || 0), 0),
    Number(report.totalAmount || 0).toFixed(2),
    Number(report.settledAmount || 0).toFixed(2),
    Number(report.pendingAmount || 0).toFixed(2)
  ])
  const ws = XLSX.utils.aoa_to_sheet(rows)
  ws['!cols'] = [{ wch: 16 }, { wch: 8 }, { wch: 14 }, { wch: 14 }, { wch: 14 }]
  ws['!merges'] = [{ s: { r: 0, c: 0 }, e: { r: 0, c: 4 } }]
  applyCenterAlign(ws)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '汇总')
  XLSX.writeFile(wb, `${periodLabel}${label}对账汇总.xlsx`)
}
