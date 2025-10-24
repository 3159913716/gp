// api/article.js
import request from '@/utils/request.js';
import { useTokenStore } from '@/stores/token.js';

/**
 * 文章相关 API 接口
 */
export default {
  /**
   * 获取首页文章列表接口（使用通用 /article 列表接口）
   * @param {Object} params - 请求参数
   * @param {number} params.pageNum - 当前页码，默认1
   * @param {number} params.pageSize - 每页条数，默认10
   * @returns {Promise<Object>} 返回分页数据
   */
  getHomeArticles(params = {}) {
    const defaultParams = {
      pageNum: 1,
      pageSize: 10
    };
    const tokenStore = useTokenStore();
    const normalized = {
      pageNum: params.pageNum ?? params.page ?? defaultParams.pageNum,
      pageSize: params.pageSize ?? defaultParams.pageSize,
      categoryId: params.categoryId ?? undefined,
      state: params.state ?? (tokenStore?.token ? undefined : '已发布')
    }
    // 优先使用通用 /article 列表
    return request.get('/article', { params: normalized }).catch(async (e1) => {
      // 逐级回退以适配不同后端命名
      try {
        return await request.get('/article/list', { params: normalized })
      } catch (e2) {
        try {
          return await request.post('/article/list', normalized)
        } catch (e3) {
          try {
            return await request.get('/article/home', { params: normalized })
          } catch (e4) {
            try {
              return await request.get('/home/article', { params: normalized })
            } catch (e5) {
              try {
                return await request.get('/article/published', { params: normalized })
              } catch (e6) {
                return await request.get('/article/public', { params: normalized })
              }
            }
          }
        }
      }
    })
  },

  /**
   * 获取文章详情接口（支持未登录访问的多端点回退，全部使用 GET）
   * @param {number} id - 文章ID
   * @returns {Promise<Object>} 返回文章详细信息
   */
  async getArticleDetail(id) {
    const tokenStore = useTokenStore();
    const isGuest = !tokenStore?.token;
    const baseParams = isGuest ? { state: '已发布' } : {};

    // 采用多级回退策略，尽可能命中公开端点（仅 GET）
    // 1) 常规：GET /article/detail?id=...
    try {
      return await request.get('/article/detail', { params: { id, ...baseParams } })
    } catch (e1) {
      // 2) 通用查询：GET /article?id=...
      try {
        return await request.get('/article', { params: { id, ...baseParams } })
      } catch (e2) {
        // 3) REST：GET /article/{id}
        try {
          return await request.get(`/article/${id}`, { params: baseParams })
        } catch (e3) {
          // 4) 公开详情：/article/public/detail 或 /article/published/detail
          try {
            return await request.get('/article/public/detail', { params: { id } })
          } catch (e4) {
            try {
              return await request.get('/article/published/detail', { params: { id } })
            } catch (e5) {
              // 5) 其他别名：/home/article/detail、/article/detail/{id}
              try {
                return await request.get('/home/article/detail', { params: { id } })
              } catch (e6) {
                // 注意：仍保持 GET，不再尝试 POST，避免后端 405
                return await request.get(`/article/detail/${id}`, { params: baseParams })
              }
            }
          }
        }
      }
    }
  },

  /**
   * 获取文章评论列表接口
   * 优先尝试 REST 风格：GET /article/{articleId}/comments
   * 回退到常规列表：GET /article/comment/list?articleId=&pageNum=&pageSize=
   * 兼容部分后端使用 POST 的场景
   * @param {number} articleId - 文章ID（路径参数）
   * @param {Object} params - 请求参数
   * @param {number} params.page - 当前页码，默认1
   * @param {number} params.pageSize - 每页条数，默认10
   * @returns {Promise<Object>} 返回评论列表数据
   */
  async getArticleComments(articleId, params = {}) {
    const defaultParams = {
      page: 1,
      pageSize: 10
    };
    const requestParams = { ...defaultParams, ...params };

    // 统一 altParams 结构
    const altParams = {
      articleId,
      pageNum: requestParams.page,
      pageSize: requestParams.pageSize
    }

    // 1) 优先尝试 REST 风格 GET /article/{id}/comments
    try {
      return await request.get(`/article/${articleId}/comments`, {
        params: requestParams
      })
    } catch (e1) {
      // 2) 尝试 GET /article/comment/list
      try {
        return await request.get('/article/comment/list', { params: altParams })
      } catch (e2) {
        // 3) 兼容部分后端采用 POST 列表查询
        try {
          return await request.post('/article/comment/list', altParams)
        } catch (e3) {
          // 4) 进一步回退到 /article/comment（GET）
          try {
            return await request.get('/article/comment', { params: altParams })
          } catch (e4) {
            // 5) 最后尝试 POST /article/comment
            try {
              return await request.post('/article/comment', altParams)
            } catch (e5) {
              throw e5
            }
          }
        }
      }
    }
  },

  /**
   * 搜索文章接口
   * @param {Object} params - 请求参数
   * @param {string} params.keyword - 搜索关键词（必填）
   * @param {number} params.page - 当前页码，默认1
   * @param {number} params.pageSize - 每页条数，默认10
   * @returns {Promise<Object>} 返回搜索结果
   */
  searchArticles(params) {
    if (!params.keyword || !params.keyword.trim()) {
      throw new Error('搜索关键词不能为空');
    }
    const defaultParams = { page: 1, pageSize: 10 };
    const tokenStore = useTokenStore();
    const requestParams = { ...defaultParams, ...params };
    if (!tokenStore?.token && requestParams.state == null) {
      requestParams.state = '已发布'
    }
    return request.get('/search', { params: requestParams })
  },

  /**
   * 获取分类详情接口（统一使用 request 实例）
   * @param {number} id - 分类ID
   * @returns {Promise<Object>} 返回分类详细信息
   */
  getCategoryDetail(id) {
    return request.get('/category/detail', {
      params: { id }
    })
  }
}
