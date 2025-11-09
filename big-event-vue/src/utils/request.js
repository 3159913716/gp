/**
 * Axios 实例配置与拦截器设置
 * 
 * 本模块创建一个自定义的Axios实例，并配置请求拦截器和响应拦截器
 * 主要功能包括：
 * 1. 设置统一的API基础路径
 * 2. 自动在请求头中添加认证token
 * 3. 处理401认证失败，自动跳转登录页
 * 4. 统一处理业务响应数据
 */

// 导入Axios库用于HTTP请求
import axios from 'axios'

// 导入Element Plus的消息提示组件
import { ElMessage } from 'element-plus'

// 导入Pinia token store管理认证令牌
import { useTokenStore } from '@/stores/token.js'

// 导入Vue Router实例用于页面导航
import router from '@/router'

/**
 * 设置API基础路径
 * 所有请求URL将以'/api'开头，通常配合代理服务器使用
 * 开发环境下可避免跨域问题
 */
const baseURL = '/api'

/**
 * 创建自定义Axios实例
 * 使用自定义配置替代全局默认配置
 */
const instance = axios.create({ 
  baseURL // 设置基础路径
})

/**
 * 请求拦截器配置
 * 在发送请求前统一处理配置
 */
instance.interceptors.request.use(
  (config) => {
    const tokenStore = useTokenStore();
    const hasToken = !!tokenStore.token

    const url = config.url || ''
    const method = (config.method || 'GET').toUpperCase()
    // 公开端点：列表/搜索/分类等（详情端点不视为公开，以便登录态携带认证）
    const isPublicEndpoint = 
      // 注意：这里不包含普通的/article路径，以便作者管理文章时需要认证
      // /^\/article(?:\?.*)?$/.test(url) ||
      /^\/search/.test(url) || 
      /^\/article\/\d+\/comments/.test(url) ||
      (method === 'GET' && /^\/article\/comment(?:\/list)?/.test(url)) ||
      // 分类端点不再视为公开，以便在登录态下附带认证头
      // (method === 'GET' && /^\/category(?:\?.*)?$/.test(url)) ||
      /^\/article\/list/.test(url) ||
      /^\/article\/home/.test(url) ||
      /^\/home\/article/.test(url) ||
      /^\/article\/published/.test(url) ||
      /^\/article\/public/.test(url) ||
      /^\/public-detail\//.test(url) ||
      /^\/article\/detail-page/.test(url) ||
      // 手机验证码相关接口
      /^\/api\/sms\//.test(url) ||
      // 用户注册登录相关接口
      /^\/user\/register-by-phone/.test(url) ||
      /^\/user\/login-by-phone/.test(url) ||
      /^\/user\/check-phone/.test(url)

    if (hasToken && !isPublicEndpoint) {
      const raw = tokenStore.token
      config.headers.Authorization = raw.startsWith('Bearer ') ? raw : `Bearer ${raw}`;
    } else {
      if (config.headers && 'Authorization' in config.headers) {
        delete config.headers.Authorization
      }
    }
    return config;
  },
  (err) => Promise.reject(err)
)

/**
 * 响应拦截器配置
 * 处理服务器响应，统一处理业务逻辑
 */
instance.interceptors.response.use(
  /**
   * 响应成功拦截处理函数
   * @param {Object} result 原始响应对象
   * @returns {Object} 直接返回业务数据
   */
  result => {
    /**
     * 直接返回响应数据中的data字段
     * 简化后续.then()方法中的数据获取
     * 注：假设后端统一在data中包含业务数据
     */
    return result.data;
  },
  
  /**
   * 响应错误拦截处理函数
   * @param {Error} err 错误对象
   * @returns {Promise} 拒绝的Promise
   */
  err => {
    if (err.response && err.response.status === 401) {
      const url = (err.config && err.config.url) || ''
      const method = (err.config && err.config.method || 'GET').toUpperCase()
      // 与请求阶段保持一致：不把详情端点视为公开
      const isPublicEndpoint = 
        /^\/article(?:\?.*)?$/.test(url) ||
        /^\/search/.test(url) || 
        /^\/article\/\d+\/comments/.test(url) ||
        (method === 'GET' && /^\/article\/comment(?:\/list)?/.test(url)) ||
        // 分类端点不再视为公开
        // (method === 'GET' && /^\/category(?:\?.*)?$/.test(url)) ||
        /^\/article\/list/.test(url) ||
        /^\/article\/home/.test(url) ||
        /^\/home\/article/.test(url) ||
        /^\/article\/published/.test(url) ||
        /^\/article\/public/.test(url) ||
        /^\/public-detail\//.test(url) ||
        /^\/article\/detail-page/.test(url) ||
        // 手机验证码相关接口
        /^\/api\/sms\//.test(url) ||
        // 用户注册登录相关接口
        /^\/user\/register-by-phone/.test(url) ||
        /^\/user\/login-by-phone/.test(url) ||
        /^\/user\/check-phone/.test(url)
      const tokenStore = useTokenStore();
      const hasToken = !!tokenStore.token
      if (!isPublicEndpoint && hasToken) {
        tokenStore.removeToken()
        router.push({ name: 'Login' });
        ElMessage.error('认证失败，请重新登录');
      }
    }
    return Promise.reject(err);
  }
)

// 导出配置好的Axios实例
export default instance;