import request from './request'

/** 角色列表（用户管理分配角色） */
export function listRoles() {
  return request({
    url: '/roles',
    method: 'get'
  })
}
