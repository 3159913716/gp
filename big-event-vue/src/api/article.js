/**
 * 文章管理API服务模块
 * 
 * 本模块封装了所有与文章分类和文章内容相关的API请求
 * 基于之前封装的Axios实例（已添加拦截器）
 * 提供完整的CRUD操作接口
 */

// 导入自定义的Axios实例（已配置基础URL和拦截器）
import request from '@/utils/request.js';

// ==================== 文章分类相关API ====================

/**
 * 获取文章分类列表（公开端点）
 * @returns {Promise} 包含文章分类列表的Promise
 * API路径：GET /category/list
 */
export const articleCategoryListService = () => {
  // 注意：在Pinia中定义的数据都不需要.value
  return request.get('/category/list');
}

/**
 * 添加文章分类
 * @param {Object} categoryData 分类数据对象
 * @returns {Promise} 包含操作结果的Promise
 * API路径：POST /category
 */
export const articleCategoryAddService = (categoryData) => {
  return request.post('/category', categoryData);
}

/**
 * 更新文章分类
 * @param {Object} categoryData 包含ID和更新字段的分类数据对象
 * @returns {Promise} 包含操作结果的Promise
 * API路径：PUT /category
 */
export const articleCategoryUpdateService = (categoryData) => {
  return request.put('/category', categoryData);
}

/**
 * 删除文章分类
 * @param {number|string} id 分类ID
 * @returns {Promise} 包含操作结果的Promise
 * API路径：DELETE /category?id={id}
 */
export const articleCategoryDeleteService = (id) => {
  // 使用URL查询参数传递ID
  return request.delete('/category?id=' + id);
}

// ==================== 文章内容相关API ====================

/**
 * 获取文章列表（支持分页和筛选）
 * @param {Object} params 查询参数对象
 *        - pageNum: 页码
 *        - pageSize: 每页条数
 *        - categoryId: 分类ID（可选）
 *        - state: 文章状态（可选）
 * @returns {Promise} 包含分页文章列表的Promise
 * API路径：GET /article
 */
export const articleListService = (params) => {
  // 使用params选项传递查询参数（Axios自动序列化）
  return request.get('/article', { params: params });
}

/**
 * 添加新文章
 * @param {Object} articleData 文章数据对象
 *        - title: 标题
 *        - content: 内容
 *        - categoryId: 分类ID
 *        - state: 状态（草稿/已发布）
 *        - coverImg: 封面图URL
 * @returns {Promise} 包含操作结果的Promise
 * API路径：POST /article
 */
export const articleAddService = (articleData) => {
  return request.post('/article', articleData);
}

/**
 * 更新文章内容
 * @param {Object} articleData 文章数据对象（必须包含ID）
 * @returns {Promise} 包含操作结果的Promise
 * API路径：PUT /article
 */
export const articleUpdateService = (articleData) => {
  return request.put('/article', articleData);
}

/**
 * 删除文章
 * @param {number|string} id 文章ID
 * @returns {Promise} 包含操作结果的Promise
 * API路径：DELETE /article?id={id}
 */
export const articleDeleteService = (id) => {
  // 使用URL查询参数传递ID
  return request.delete('/article?id=' + id);
}