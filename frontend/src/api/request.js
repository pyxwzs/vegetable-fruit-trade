import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const baseURL =
    process.env.VUE_APP_BASE_API ||
    (process.env.NODE_ENV === 'development' ? '/api' : 'http://localhost:8080/api')

const service = axios.create({
    baseURL,
    timeout: 30000
})

const PUBLIC_AUTH_URLS = [
    '/auth/config',
    '/auth/login',
    '/auth/login/mfa-challenge',
    '/auth/login/mfa-verify',
    '/auth/register/challenge',
    '/auth/register/verify',
    '/auth/refresh',
    '/auth/forgot-password',
    '/auth/reset-password'
]

function isPublicAuthUrl(url) {
    return PUBLIC_AUTH_URLS.some(p => url.includes(p))
}

// 请求拦截器
service.interceptors.request.use(
    config => {
        const token = localStorage.getItem('token')
        if (token && !isPublicAuthUrl(config.url)) {
            config.headers['Authorization'] = 'Bearer ' + token
        }
        if (isPublicAuthUrl(config.url)) {
            delete config.headers['Authorization']
        }
        return config
    },
    error => Promise.reject(error)
)

// 使用独立 axios 实例刷新令牌，避免与本实例拦截器互相递归
const rawAxios = axios.create({ baseURL, timeout: 30000 })

// 响应拦截器：401 时尝试用 refreshToken 续期一次
service.interceptors.response.use(
    response => response.data,
    async error => {
        const originalRequest = error.config
        const status = error.response?.status

        if (status === 401 && originalRequest && !originalRequest._retry && !isPublicAuthUrl(originalRequest.url)) {
            const refresh = localStorage.getItem('refreshToken')
            if (refresh) {
                originalRequest._retry = true
                try {
                    const res = await rawAxios.post('/auth/refresh', { refreshToken: refresh })
                    const body = res.data
                    const payload = body.data !== undefined ? body.data : body
                    const newToken = payload.token
                    const newRefresh = payload.refreshToken
                    if (newToken) {
                        localStorage.setItem('token', newToken)
                    }
                    if (newRefresh) {
                        localStorage.setItem('refreshToken', newRefresh)
                    }
                    originalRequest.headers['Authorization'] = 'Bearer ' + (newToken || localStorage.getItem('token'))
                    return service(originalRequest)
                } catch (e) {
                    localStorage.removeItem('token')
                    localStorage.removeItem('refreshToken')
                    if (!window.location.pathname.includes('/login')) {
                        ElMessage.error('登录已过期，请重新登录')
                        router.push('/login')
                    }
                    return Promise.reject(e)
                }
            }
        }

        if (error.response) {
            if (status === 401) {
                localStorage.removeItem('token')
                localStorage.removeItem('refreshToken')
                if (!window.location.pathname.includes('/login')) {
                    ElMessage.error('登录已过期，请重新登录')
                    router.push('/login')
                }
            } else {
                ElMessage.error(error.response.data?.message || '请求失败')
            }
        } else if (error.code === 'ECONNABORTED') {
            ElMessage.error('请求超时，请稍后重试')
        } else {
            ElMessage.error('网络连接失败，请检查后端服务')
        }

        return Promise.reject(error)
    }
)

export default service
