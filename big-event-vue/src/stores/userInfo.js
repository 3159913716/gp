/**
 * Pinia 状态管理模块 - 用户信息存储
 * 
 * 本模块实现了一个全局的用户信息状态管理Store，用于集中管理当前登录用户的信息
 * 主要功能包括：
 * 1. 存储和更新用户信息
 * 2. 清除用户信息（登出时）
 * 3. 自动持久化用户信息到本地存储
 */

// 导入Vue的ref响应式API
import { ref } from 'vue'

// 导入Pinia的状态管理库，用于创建store
import { defineStore } from 'pinia'

/**
 * 创建Pinia store实例 - 用户信息存储
 * 
 * defineStore参数说明：
 * 第一个参数 'userInfo' - store的唯一标识符，在整个应用中必须是唯一的
 * 第二个参数 - 一个工厂函数，使用组合式API定义状态和行为
 * 第三个参数 - 可选的store配置对象
 */
const useUserInfoStore = defineStore(
  'userInfo', // Store的唯一名称

  /**
   * 工厂函数：定义store的状态和行为
   * 使用组合式API (composition API) 风格
   */
  () => {
    /**
     * 定义响应式用户信息状态
     * ref({}) 创建一个响应式引用，初始值为空对象
     * 存储结构：{ id, username, nickname, email, userPic, etc. }
     */
    const info = ref({})

    /**
     * 定义设置用户信息的方法
     * @param {Object} newInfo - 包含用户信息的对象
     */
    const setInfo = (newInfo) => {
      // 更新用户信息
      info.value = newInfo
    }

    /**
     * 定义清除用户信息的方法
     * 用于用户登出时清除个人信息
     */
    const removeInfo = () => {
      // 将用户信息重置为空对象
      info.value = {}
    }

    /**
     * 导出状态和方法
     * 这些内容将作为store的公共接口暴露给外部使用
     */
    return {
      info,       // 响应式用户信息对象
      setInfo,    // 设置用户信息的方法
      removeInfo  // 清除用户信息的方法
    }
  },

  /**
   * Store配置对象
   * 设置额外的store配置
   */
  {
    // 配置Pinia持久化插件
    persist: {
      // 使用localStorage作为存储方式
      storage: localStorage,
      // 持久化所有状态
      paths: ['info'],
      // 自定义键名
      key: 'userInfo'
    }
  }
)

// 导出创建好的store
export default useUserInfoStore