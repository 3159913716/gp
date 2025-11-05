// followApi.js
import request from '@/utils/request.js'

// 直接使用项目中已配置的request实例，确保baseURL和拦截器配置一致

export default {
  /**
   * 关注/取消关注用户
   * @param {number} userId - 被关注用户的ID
   * @returns {Promise}
   */
  toggleFollow(userId) {
    return request.post(`/user/${userId}/follow`)
  },

  /**
   * 获取用户关注列表
   * @returns {Promise}
   */
  getFollowingList() {
    return request.get('/user/following')
  },

  /**
   * 获取用户粉丝列表
   * @returns {Promise}
   */
  getFollowersList() {
    return request.get('/user/followers')
  }
}
