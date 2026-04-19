import request from './request'

export function getFiles(params) {
  return request({
    url: '/files',
    method: 'get',
    params
  })
}

/** @param {FormData} formData 含 file，可选 businessType、businessId */
export function uploadFile(formData) {
  return request({
    url: '/files/upload',
    method: 'post',
    data: formData
  })
}

export function deleteFile(fileId) {
  return request({
    url: `/files/${fileId}`,
    method: 'delete'
  })
}

export function downloadFile(fileId) {
  return request({
    url: `/files/download/${fileId}`,
    method: 'get',
    responseType: 'blob'
  })
}

export function getAccessUrl(fileId) {
  return request({
    url: `/files/${fileId}/access-url`,
    method: 'get'
  })
}
