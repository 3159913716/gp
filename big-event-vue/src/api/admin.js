import useUserInfoStore from '@/stores/userInfo.js'
import request from '@/utils/request.js'

// ========== 统计 ===========
export const getStatistics = () => {
  return request.get('/admin/statistics')
}

// ========== 用户管理 ===========
/**
 * 获取用户列表
 * params: { page, pageSize, role, username, status }
 */
export const getUsers = (params) => {
  return request.get('/admin/users', { params })
}

/**
 * 修改用户角色
 * body: { role }
 */
export const updateUserRole = async (id, body) => {
  const res = await request.put(`/admin/users/${id}/role`, body)
  return res
}

/**
 * 修改用户状态（启用/禁用）
 * body: { status }
 */
export const updateUserStatus = async (id, body) => {
  const res = await request.put(`/admin/users/${id}/status`, body)
  return res
}

// ========== 作者申请审核 ===========
/**
 * 获取作者申请列表
 * params: { page, pageSize, status }
 */
export const getAuthorApplies = (params) => {
  return request.get('/admin/author-applies', { params })
}

/**
 * 审核作者申请（新接口）
 * body: { status, rejectReason }
 */
export const auditAuthorApply = async (id, body) => {
  const res = await request.put(`/admin/author-applies/${id}/audit`, body)
  return res
}

// 兼容旧方法：保留 approve/reject 封装，调用新接口
export const approveAuthorApply = async (id) => {
  return auditAuthorApply(id, { status: 1 })
}

export const rejectAuthorApply = async (id, reason) => {
  return auditAuthorApply(id, { status: 2, rejectReason: reason || '' })
}

// ========== 作者申请审核 ===========

export default {
  getStatistics,
  getUsers,
  updateUserRole,
  updateUserStatus,
  getAuthorApplies,
  approveAuthorApply,
  rejectAuthorApply,
  // createLog intentionally removed from default export to avoid accidental usage
}