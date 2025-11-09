// phoneAuthApi.js
import request from '@/utils/request.js'

/**
 * 发送短信验证码
 * @param {string} phone - 手机号码
 * @param {string} type - 验证码类型 (register/login/reset)
 * @returns {Promise} 发送结果
 */
export const sendSmsCode = (phone, type) => {
  return request.post('/api/sms/send-code', null, {
    params: {
      targets: phone,
      type: type
    }
  })
}


/**
 * 手机号注册
 * @param {string} username - 用户名
 * @param {string} password - 密码
 * @param {string} phone - 手机号
 * @param {string} code - 验证码
 * @returns {Promise} 注册结果
 */
export const registerByPhone = (username, password, phone, code) => {
  return request.post('/user/register-by-phone', null, {
    params: {
      username: username,
      password: password,
      phone: phone,
      code: code
    }
  })
}

/**
 * 手机号登录
 * @param {string} phone - 手机号
 * @param {string} code - 验证码
 * @returns {Promise} 登录结果，包含JWT token
 */
export const loginByPhone = (phone, code) => {
  return request.post('/user/login-by-phone', null, {
    params: {
      phone: phone,
      code: code
    }
  })
}

/**
 * 检查手机号是否已注册
 * @param {string} phone - 手机号
 * @returns {Promise} 检查结果
 */
export const checkPhoneExists = (phone) => {
  // 可以通过登录接口间接判断，或后端提供专门的检查接口
  return request.get('/user/check-phone', {
    params: {
      phone: phone
    }
  })
}
