import dayjs from 'dayjs'

/**
 * 格式化日期
 * @param {string|Date} date
 * @param {string} format
 * @returns {string}
 */
export const formatDate = (date, format = 'YYYY-MM-DD') => {
    if (!date) return ''
    return dayjs(date).format(format)
}

/**
 * 格式化日期时间
 * @param {string|Date} date
 * @param {string} format
 * @returns {string}
 */
export const formatDateTime = (date, format = 'YYYY-MM-DD HH:mm:ss') => {
    if (!date) return ''
    return dayjs(date).format(format)
}

/**
 * 获取相对时间
 * @param {string|Date} date
 * @returns {string}
 */
export const getRelativeTime = (date) => {
    if (!date) return ''
    const now = dayjs()
    const target = dayjs(date)
    const diffSeconds = now.diff(target, 'second')

    if (diffSeconds < 60) return '刚刚'
    if (diffSeconds < 3600) return Math.floor(diffSeconds / 60) + '分钟前'
    if (diffSeconds < 86400) return Math.floor(diffSeconds / 3600) + '小时前'
    if (diffSeconds < 2592000) return Math.floor(diffSeconds / 86400) + '天前'

    return formatDate(date)
}