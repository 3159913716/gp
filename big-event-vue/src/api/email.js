// api/email.js
import axios from 'axios';

const emailApi = axios.create({
  baseURL: '/api/email',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded'
  }
});

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
    const params = new URLSearchParams();
    params.append('email', email);
    params.append('type', type);
    
    // 确保使用 post 方法而不是 get
    return emailApi.post('/send-code', params)
      .then(normalize);
  },

  // 验证验证码 - 必须使用 POST 方法
  verify(email, code, type) {
    const params = new URLSearchParams();
    params.append('email', email);
    params.append('code', code);
    params.append('type', type);
    
    // 确保使用 post 方法而不是 get
    return emailApi.post('/verify', params)
      .then(normalize);
  }
}
