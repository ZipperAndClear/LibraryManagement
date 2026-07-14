import { post, del } from './http'

/**
 * 上传文件到服务器
 * @param {File} file - 要上传的文件对象
 * @param {string} [oldUrl] - 旧文件的相对URL，上传新文件替换时自动删除旧文件
 * @param {string} [subdir] - 存储子目录（avatars | covers）
 * @returns {Promise} 返回 { url: "/uploads/{subdir}/xxx.png" }
 */
export const uploadFile = (file, oldUrl, subdir) => {
  const formData = new FormData()
  formData.append('file', file)
  if (oldUrl) {
    formData.append('oldUrl', oldUrl)
  }
  if (subdir) {
    formData.append('subdir', subdir)
  }
  return post('/api/file/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
}

/**
 * 删除服务器上的文件
 * @param {string} url - 文件的相对URL（如 /uploads/avatars/xxx.png）
 * @returns {Promise}
 */
export const deleteFile = (url) => del(`/api/file/delete`, null, { params: { url } })
