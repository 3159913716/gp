/**
 * 用户认证与信息管理API服务模块
 * 
 * 本模块封装了用户注册、登录、信息管理相关的API请求
 * 使用URLSearchParams处理表单数据，确保正确的内容类型
 */

// 导入自定义的Axios实例（已配置基础URL和拦截器）
import request from '@/utils/request.js';

// ==================== 用户认证相关API ====================

/**
 * 用户注册服务
 * @param {Object} registerData 注册数据对象
 *        - username: 用户名
 *        - password: 密码
 *        - nickname: 昵称（可选）
 *        - email: 邮箱（可选）
 * @returns {Promise} 包含注册结果的Promise
 * API路径：POST /user/register
 * 内容类型：application/x-www-form-urlencoded
 */
export const userRegisterService = (registerData) => {
  /**
   * 使用URLSearchParams处理表单数据：
   * 1. 确保正确的Content-Type (application/x-www-form-urlencoded)
   * 2. 兼容后端表单数据接收方式
   */
  const params = new URLSearchParams();
  
  // 遍历注册数据对象，添加到URLSearchParams
  for (let key in registerData) {
    params.append(key, registerData[key]);
  }
  
  return request.post('/user/register', params);
}

/**
 * 用户登录服务
 * @param {Object} loginData 登录数据对象
 *        - username: 用户名
 *        - password: 密码
 * @returns {Promise} 包含登录结果和token的Promise
 * API路径：POST /user/login
 * 内容类型：application/x-www-form-urlencoded
 */
export const userLoginService = (loginData) => {
  const params = new URLSearchParams();
  
  // 遍历登录数据对象，添加到URLSearchParams
  for (let key in loginData) {
    params.append(key, loginData[key]);
  }
  
  return request.post('/user/login', params);
}

/**
 * 重置密码服务
 * @param {Object} resetData 重置密码数据
 *        - email: 用户邮箱
 *        - newPassword: 新密码
 * @returns {Promise} 包含重置结果的Promise
 * API路径：POST /user/resetPassword
 * 内容类型：application/json
 */
export const resetPasswordService = (resetData) => {
  // 直接发送JSON对象
  return request.post('/user/resetPassword', resetData, {
    headers: {
      'Content-Type': 'application/json'
    }
  });
}

// ==================== 用户信息管理API ====================

/**
 * 获取当前用户详细信息
 * @returns {Promise} 包含用户信息的Promise
 * API路径：GET /user/userInfo
 * 注意：需要有效的认证token
 */
export const userInfoService = () => {
  // 不需要参数，直接调用GET请求
  return request.get('/user/userInfo');
}

/**
 * 更新用户基本信息
 * @param {Object} userInfoData 用户信息对象
 *        - nickname: 昵称
 *        - email: 邮箱
 * @returns {Promise} 包含更新结果的Promise
 * API路径：PUT /user/update
 * 内容类型：application/json
 */
export const userInfoUpdateService = (userInfoData) => {
  /**
   * 使用JSON格式发送数据：
   * 1. 适合复杂数据结构
   * 2. Axios默认使用application/json
   */
  return request.put('/user/update', userInfoData);
}

// ==================== 用户资料修改API ====================

/**
 * 更新用户头像
 * @param {string} avatarUrl 头像URL地址
 * @returns {Promise} 包含更新结果的Promise
 * API路径：PATCH /user/updateAvatar
 * 注意：avatarUrl必须作为URL查询参数传递
 */
export const userAvatarUpdateService = (avatarUrl) => {
  /**
   * 根据接口文档要求：
   * 1. avatarUrl必须作为URL查询参数传递
   * 2. 使用encodeURIComponent确保URL参数的安全性
   */
  return request.patch(`/user/updateAvatar?avatarUrl=${encodeURIComponent(avatarUrl)}`);
}

/**
 * 更新用户密码
 * @param {Object} pwdModel 密码数据对象
 *        - oldPwd: 原密码
 *        - newPwd: 新密码
 *        - rePwd: 确认密码
 * @returns {Promise} 包含更新结果的Promise
 * API路径：PATCH /user/updatePwd
 * 内容类型：application/json
 */
export const userUpdatePasswordService = (pwdModel) => {
  /**
   * 重构参数对象：
   * 1. 适配后端参数命名规范
   * 2. 保持前后端命名一致性
   */
  const params = {
    old_pwd: pwdModel.oldPwd,
    new_pwd: pwdModel.newPwd,
    re_pwd: pwdModel.rePwd
  };
  
  return request.patch('/user/updatePwd', params);
}

/**
 * 发送邮箱验证码服务
 * @param {Object} emailData 邮箱数据对象
 *        - email: 邮箱地址
 *        - type: 验证码类型(register/forgot)
 * @returns {Promise} 包含发送结果的Promise
 * API路径：POST /api/email/send-code
 * 内容类型：application/json
 */
export const sendEmailCodeService = (emailData) => {
  /**
   * 使用JSON格式发送数据：
   * 1. 符合后端API要求
   * 2. 保持与其他API调用一致
   */
  return request.post('/api/email/send-code', emailData);
}

/**
 * 验证邮箱验证码服务
 * @param {Object} verifyData 验证数据对象
 *        - email: 邮箱地址
 *        - code: 验证码(6位数字)
 *        - type: 验证码类型(register/forgot)
 * @returns {Promise} 包含验证结果的Promise
 * API路径：POST /api/email/verify
 * 内容类型：application/json
 */
export const verifyEmailCodeService = (verifyData) => {
  return request.post('/api/email/verify', verifyData);
}

/**
 * 获取作者申请状态
 * @returns {Promise} 包含申请状态信息的Promise
 * API路径：GET /user/author-apply/status
 * 注意：需要有效的认证token
 */
export const getAuthorApplyStatusService = () => {
  // 不需要参数，直接调用GET请求
  return request.get('/user/author-apply/status');
}

/**
 * 取消作者申请
 * @returns {Promise} 包含取消结果的Promise
 * API路径：DELETE /user/author-apply
 * 注意：需要有效的认证token
 */
export const cancelAuthorApplyService = () => {
  // 不需要参数，直接调用DELETE请求
  return request.delete('/user/author-apply');
}