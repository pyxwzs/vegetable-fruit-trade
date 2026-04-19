import dayjs from 'dayjs'

export function formatDate(date, format = 'YYYY-MM-DD') {
    return date ? dayjs(date).format(format) : '-'
}

export function formatDateTime(date) {
    return date ? dayjs(date).format('YYYY-MM-DD HH:mm:ss') : '-'
}

export function formatMoney(amount, decimals = 2) {
    if (amount === null || amount === undefined) return '-'
    return '¥' + Number(amount).toFixed(decimals).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

export function formatNumber(num, decimals = 0) {
    if (num === null || num === undefined) return '-'
    return Number(num).toFixed(decimals)
}