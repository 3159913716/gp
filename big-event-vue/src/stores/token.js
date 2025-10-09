/**
 * Pinia 状态管理模块 - Token存储
 * 
 * 本模块实现了一个全局的Token状态管理Store，用于管理用户的认证令牌
 * 主要功能包括：
 * 1. 集中管理用户的认证令牌
 * 2. 提供设置和清除Token的方法
 * 3. 自动实现数据的本地持久化存储
 * 
 * 设计目标：在用户登录后，Token将在整个应用的所有组件中共享，有效期为12小时
 */

// 导入Vue的ref响应式API
import { ref } from 'vue';

// 导入Pinia的状态管理库，用于创建store
import { defineStore } from 'pinia';

/**
 * 创建Pinia store实例
 * 
 * defineStore参数说明：
 * 第一个参数 - store的唯一标识符（字符串），在整个应用中必须是唯一的
 * 第二个参数 - 一个工厂函数，定义状态、getters和actions
 * 第三个参数 - 可选的store配置对象
 */
export const useTokenStore = defineStore('token', // store名称 - 'token'
  /**
   * 工厂函数：定义store的状态和行为
   * 使用组合式API (composition API) 风格
   */
  () => {
    /**
     * 定义响应式Token状态
     * ref('') 创建一个响应式引用，初始值为空字符串
     */
    const token = ref('');

    /**
     * 定义设置Token的方法
     * @param {string} newToken - 要设置的新Token
     */
    const setToken = (newToken) => {
      // 更新响应式Token的值
      token.value = newToken;
    }

    /**
     * 定义移除Token的方法
     * 用于用户登出或Token过期时清除认证状态
     */
    const removeToken = () => {
      // 清空Token值
      token.value = '';
    }

    /**
     * 导出状态和方法
     * 这些内容将作为store的公共接口暴露给外部使用
     */
    return { 
      token,       // 响应式Token状态
      setToken,    // 设置Token的方法
      removeToken  // 移除Token的方法
    }
  },
  /**
   * Store配置对象
   * 可选参数，用于设置store的额外配置
   */
  {
    // 开启数据持久化
    // 当设为true时，Pinia会自动将state存入本地存储（localStorage）
    // 刷新页面或重新打开应用时自动恢复状态
    persist: true
  }
)