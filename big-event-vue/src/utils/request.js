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
  /**
   * 请求成功拦截处理函数
   * @param {Object} config 请求配置对象
   * @returns {Object} 修改后的请求配置
   */
  (config) => {
    // 从Pinia存储获取token store实例
    const tokenStore = useTokenStore();
    
    // 检查是否存在有效token
    if (tokenStore.token) {
      /**
       * 在请求头中添加Authorization字段
       * 使用Bearer Token认证方案
       * 格式：Authorization: Bearer <token>
       */
      const raw = tokenStore.token
      config.headers.Authorization = raw.startsWith('Bearer ') ? raw : `Bearer ${raw}`;
    } 
    
    // 返回处理后的配置对象
    return config;
  },
  
  /**
   * 请求错误拦截处理函数
   * @param {Error} err 请求错误对象
   * @returns {Promise} 拒绝的Promise
   */
  (err) => {
    // 直接将错误传递给后续处理
    return Promise.reject(err)
  }
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
      // 扩展公开接口白名单：
      // 首页文章列表 /article、详情 /article/detail、搜索 /search、评论（GET） /article/:id/comments 与 /article/comment/list
      // 新增：分类列表 /category（首页导航需要）以及多种可能的公开文章端点
      const isPublicEndpoint = 
        /^\/article(?:\?.*)?$/.test(url) ||
        /^\/article\/detail/.test(url) || 
        /^\/search/.test(url) || 
        /^\/article\/\d+\/comments/.test(url) ||
        (method === 'GET' && /^\/article\/comment(?:\/list)?/.test(url)) ||
        (method === 'GET' && /^\/category(?:\?.*)?$/.test(url)) ||
        /^\/article\/list/.test(url) ||
        /^\/article\/home/.test(url) ||
        /^\/home\/article/.test(url) ||
        /^\/article\/published/.test(url) ||
        /^\/article\/public/.test(url)
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