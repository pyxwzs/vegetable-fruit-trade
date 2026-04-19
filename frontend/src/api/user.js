import request from './request'

/** 系统是否开放「登录二次验证」能力（auth.mfa-login-enabled）；账号是否启用另见用户字段 */
export function getAuthConfig() {
    return request({
        url: '/auth/config',
        method: 'get'
    })
}

/**
 * 登录第一步：返回 token，或返回 mfaRequired + sessionId（需再走 loginMfaVerify）
 */
export function postLogin(data) {
    return request({
        url: '/auth/login',
        method: 'post',
        data
    })
}

/** 修改当前用户安全设置（二次验证、登录邮件提醒） */
export function updateMySecurity(data) {
    return request({
        url: '/auth/me/security',
        method: 'patch',
        data
    })
}

/** 登录第一步：校验密码，发送邮箱验证码 */
export function loginMfaChallenge(data) {
    return request({
        url: '/auth/login/mfa-challenge',
        method: 'post',
        data
    })
}

/** 登录第二步：校验验证码，换取 token */
export function loginMfaVerify(data) {
    return request({
        url: '/auth/login/mfa-verify',
        method: 'post',
        data
    })
}

// 刷新访问令牌
export function refreshToken(refreshToken) {
    return request({
        url: '/auth/refresh',
        method: 'post',
        data: { refreshToken }
    })
}

// 忘记密码：发送邮箱验证码
export function forgotPassword(email) {
    return request({
        url: '/auth/forgot-password',
        method: 'post',
        data: { email }
    })
}

// 重置密码
export function resetPasswordByEmail(data) {
    return request({
        url: '/auth/reset-password',
        method: 'post',
        data
    })
}

/** 注册第一步：提交信息并发送邮箱验证码 */
export function registerChallenge(data) {
    return request({
        url: '/auth/register/challenge',
        method: 'post',
        data
    })
}

/** 注册第二步：提交验证码完成注册 */
export function registerVerify(data) {
    return request({
        url: '/auth/register/verify',
        method: 'post',
        data: data
    })
}

// 获取当前用户
export function getCurrentUser() {
    return request({
        url: '/auth/me',
        method: 'get'
    })
}

/** 管理员数据视角（仅存服务端，由登录用户 id 查库，不写入 token） */
export function getDataScope() {
    return request({
        url: '/auth/data-scope',
        method: 'get'
    })
}

/** 设置数据视角：targetUserId 传 null 表示全量数据 */
export function setDataScope(targetUserId) {
    return request({
        url: '/auth/data-scope',
        method: 'put',
        data: { targetUserId }
    })
}

// 用户详情
export function getUser(id) {
  return request({
    url: `/users/${id}`,
    method: 'get'
  })
}

// 获取用户列表
export function getUsers(params) {
    return request({
        url: '/users',
        method: 'get',
        params
    })
}

// 创建用户
export function createUser(data) {
    return request({
        url: '/users',
        method: 'post',
        data
    })
}

// 更新用户
export function updateUser(id, data) {
    return request({
        url: `/users/${id}`,
        method: 'put',
        data
    })
}

/** 账号状态：ENABLED 启用 / DISABLED 禁用 / LOCKED 锁定 */
export function updateUserStatus(id, status) {
    return request({
        url: `/users/${id}/status`,
        method: 'patch',
        data: { status }
    })
}

// 删除用户
export function deleteUser(id) {
    return request({
        url: `/users/${id}`,
        method: 'delete'
    })
}

/** 管理员重置指定用户密码 */
export function resetUserPassword(id, newPassword) {
    return request({
        url: `/users/${id}/reset-password`,
        method: 'post',
        data: { newPassword }
    })
}