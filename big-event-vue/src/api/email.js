// api/email.js - 融合邮箱验证和找回密码功能
import axios from 'axios';

// 创建axios实例
const api = axios.create({
  baseURL: '/api',
  timeout: 10000,
  method: 'POST' // 明确设置默认方法为POST
});

// 创建专门用于邮箱验证的axios实例
const emailApi = axios.create({
  baseURL: '/api/email',
  timeout: 10000,
  method: 'POST', // 明确设置默认方法为POST
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded' // 使用表单格式发送数据，确保与后端@RequestParam兼容
  }
});

// 确保所有请求都使用POST方法
emailApi.defaults.method = 'POST';
api.defaults.method = 'POST';

// 统一后端返回结构，映射为 { success, message, data }
const normalize = (resp) => {
  const raw = resp && resp.data !== undefined ? resp.data : resp;
  if (raw && typeof raw === 'object') {
    if (raw.success !== undefined) {
      return {
        success: !!raw.success,
        message: raw.message || raw.msg || '',
        data: raw.data
      }
    }
    if (raw.code !== undefined) {
      return {
        success: raw.code === 0,
        message: raw.msg || raw.message || '',
        data: raw.data
      }
    }
  }
  return {
    success: true,
    message: '',
    data: raw
  }
};

/**
 * 邮箱和密码管理API
 * 融合了验证码发送/验证和找回密码功能
 */
const emailAndPasswordAPI = {
  // ================ 验证码相关功能 ================
  
  /**
   * 发送验证码 - 必须使用 POST 方法
   * @param {string} email - 用户邮箱
   * @param {string} type - 验证码类型（register/forget等）
   * @returns {Promise} 响应结果
   */
  sendCode(email, type) {
    // 使用URLSearchParams确保参数正确传递，匹配后端@RequestParam注解
    const params = new URLSearchParams();
    params.append('email', email);
    params.append('type', type);
    
    return emailApi.post('/send-code', params, {
      method: 'POST', // 显式指定POST方法
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      },
      timeout: 30000 // 增加超时时间到30秒，减少超时错误
    }).then(normalize);
  },

  /**
   * 验证验证码 - 必须使用 POST 方法
   * @param {string} email - 用户邮箱
   * @param {string} code - 验证码
   * @param {string} type - 验证码类型
   * @returns {Promise} 验证结果
   */
  verify(email, code, type) {
    // 使用URLSearchParams确保参数正确传递，匹配后端@RequestParam注解
    const params = new URLSearchParams();
    params.append('email', email);
    params.append('code', code);
    params.append('type', type);
    
    return emailApi.post('/verify', params, {
      method: 'POST', // 显式指定POST方法
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    }).then(normalize);
  },
  
  // ================ 找回密码相关功能 ================
  
  /**
   * 根据邮箱查找用户
   * @param {string} email - 用户邮箱
   * @returns {Promise} 用户信息
   */
  findUserByEmail(email) {
    // 使用URLSearchParams确保参数正确传递，匹配后端@RequestParam注解
    const params = new URLSearchParams();
    params.append('email', email);
    
    return api.post('/user/find-by-email', params, {
      method: 'POST', // 显式指定POST方法
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    }).then(normalize);
  },

  /**
   * 发送找回密码验证码
   * @param {number} userId - 用户ID
   * @param {string} email - 用户邮箱
   * @returns {Promise}
   */
  sendForgetCode(userId, email) {
    // 使用URLSearchParams确保参数正确传递，匹配后端@RequestParam注解
    const params = new URLSearchParams();
    params.append('userId', userId);
    params.append('email', email);
    
    return api.post('/user/send-forget-code', params, {
      method: 'POST', // 显式指定POST方法
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded'
      }
    }).then(normalize);
  },

  /**
   * 重置密码
   * @param {Object} resetData - 重置密码所需数据
   * @param {number} resetData.userId - 用户ID
   * @param {string} resetData.email - 用户邮箱
   * @param {string} resetData.code - 验证码
   * @param {string} resetData.newPwd - 新密码
   * @param {string} resetData.rePwd - 确认密码
   * @returns {Promise}
   */
  resetPassword(resetData) {
    // 直接发送JSON对象，使用application/json格式
    return api.post('/user/reset-pwd', resetData, {
      method: 'POST', // 显式指定POST方法
      headers: {
        'Content-Type': 'application/json'
      }
    }).then(normalize);
  }
};

export default emailAndPasswordAPI;
