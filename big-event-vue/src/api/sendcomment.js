// api/comment.js
// 使用统一的请求实例，确保走 /api 基础路径与拦截器
import request from '@/utils/request.js';
import { useTokenStore } from '@/stores/token.js';

/**
 * 评论相关 API 接口
 */
export default {
  /**
   * 发布评论接口（带认证头）
   * @param {number} articleId - 文章ID（路径参数）
   * @param {Object} commentData - 评论数据
   * @param {string} commentData.content - 评论内容
   * @param {number|null} commentData.parentId - 父评论ID，null表示一级评论
   * @returns {Promise<Object>} 返回评论结果
   */
  async addComment(articleId, commentData) {
    // 统一构造后端期望的字段
    const parent = commentData.parentId ?? commentData.parent_id ?? 0;
    const payload = {
      articleId,
      article_id: articleId,
      aid: articleId,
      content: commentData.content,
      parentId: parent,
      parent_id: parent
    };
    // 读取认证令牌（优先Pinia，其次localStorage），并统一成 Bearer 格式
    const tokenStore = useTokenStore();
    const raw = tokenStore?.token || localStorage.getItem('token') || '';
    const auth = raw.startsWith('Bearer ') ? raw : (raw ? `Bearer ${raw}` : '');
    const headers = auth ? { Authorization: auth } : {};

    // 1) 优先 REST: POST /article/{id}/comment
    try {
      return await request.post(`/article/${articleId}/comment`, payload, { headers });
    } catch (e1) {
      // 2) 传统: POST /article/comment
      try {
        return await request.post('/article/comment', payload, { headers });
      } catch (e2) {
        // 3) 常见变体：POST /comment/add
        try {
          return await request.post('/comment/add', payload, { headers });
        } catch (e3) {
          // 4) 常见变体：POST /comment/create
          try {
            return await request.post('/comment/create', payload, { headers });
          } catch (e4) {
            // 5) 最后尝试：POST /comment
            return await request.post('/comment', payload, { headers });
          }
        }
      }
    }
  },

  /**
   * 点赞/取消点赞评论接口
   * @param {number} commentId - 评论ID（路径参数）
   * @returns {Promise<Object}} 返回点赞状态
   */
  toggleCommentLike(commentId) {
    const tokenStore = useTokenStore();
    const raw = tokenStore?.token || localStorage.getItem('token') || '';
    const auth = raw.startsWith('Bearer ') ? raw : (raw ? `Bearer ${raw}` : '');
    const headers = auth ? { Authorization: auth } : {};
    return request.post(`/comment/${commentId}/like`, null, { headers }).then(response => response);
  },

  /**
   * 点赞/取消点赞文章接口
   * @param {number} articleId - 文章ID（路径参数）
   * @returns {Promise<Object}} 返回点赞状态
   */
  toggleArticleLike(articleId) {
    const tokenStore = useTokenStore();
    const raw = tokenStore?.token || localStorage.getItem('token') || '';
    const auth = raw.startsWith('Bearer ') ? raw : (raw ? `Bearer ${raw}` : '');
    const headers = auth ? { Authorization: auth } : {};
    return request.post(`/article/like/${articleId}`, null, { headers }).then(response => response);
  },

  /**
   * 收藏/取消收藏文章接口
   * @param {number} articleId - 文章ID（路径参数）
   * @returns {Promise<Object}} 返回收藏状态
   */
  toggleArticleCollect(articleId) {
    const tokenStore = useTokenStore();
    const raw = tokenStore?.token || localStorage.getItem('token') || '';
    const auth = raw.startsWith('Bearer ') ? raw : (raw ? `Bearer ${raw}` : '');
    const headers = auth ? { Authorization: auth } : {};
    return request.post(`/article/collect/${articleId}`, null, { headers }).then(response => response);
  }
}
