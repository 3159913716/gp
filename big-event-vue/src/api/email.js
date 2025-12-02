// api/email.js
import request from '@/utils/request.js';

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
}

export default {
  // 发送验证码 - 必须使用 POST 方法
  sendCode(email, type) {
    // 使用项目统一的request实例
    return request.post('/api/email/send-code', null, {
      params: {
        email: email,
        type: type
      }
    }).then(normalize);
  },

  // 验证验证码 - 必须使用 POST 方法
  verify(email, code, type) {
    // 使用项目统一的request实例
    return request.post('/api/email/verify', null, {
      params: {
        email: email,
        code: code,
        type: type
      }
    }).then(normalize);
  },
  
  // 根据邮箱查找用户 - 新增方法
  findUserByEmail(email) {
    return request.get('/api/email/findUser', {
      params: {
        email: email
      }
    }).then(normalize);
  },
  
  // 发送找回密码验证码 - 新增方法
  sendForgetCode(userId, email) {
    return request.post('/api/email/sendForgetCode', null, {
      params: {
        userId: userId,
        email: email
      }
    }).then(normalize);
  },
  
  // 重置密码 - 新增方法
  resetPassword(resetData) {
    return request.post('/api/email/resetPassword', resetData)
      .then(normalize);
  }
};