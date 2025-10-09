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
      config.headers.Authorization = tokenStore.token;
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
    /**
     * 处理401 Unauthorized状态码
     * 表示认证失败（token过期或无效）
     */
    if (err.response && err.response.status === 401) {
      // 获取token store实例
      const tokenStore = useTokenStore();
      
      // 清除本地存储的token
      tokenStore.removeToken()
      
      /**
       * 重定向到登录页
       * 使用命名路由导航确保路由一致性
       */
      router.push({ name: 'Login' });
      
      // 显示错误提示信息
      ElMessage.error('认证失败，请重新登录');
    }
    
    // 将错误传递给调用方处理
    return Promise.reject(err);
  }
)

// 导出配置好的Axios实例
export default instance;