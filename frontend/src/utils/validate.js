export function isValidUsername(str) {
    return /^[a-zA-Z0-9_-]{3,20}$/.test(str)
}

export function isValidEmail(email) {
    return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
}

export function isValidPhone(phone) {
    return /^1[3-9]\d{9}$/.test(phone)
}

export function isValidPassword(password) {
    return password.length >= 6 && password.length <= 20
}