import request from '@/utils/request.js'

export const frontArticleList = (params) => request.get('/article', { params })
export const frontArticleDetail = (id) => request.get('/article/' + id)
export const frontCategoryList = () => request.get('/category/list')
export const loginService = (data) => request.post('/auth/login', data)
export const registerService = (data) => request.post('/auth/register', data)
export const likeArticle = (id) => request.post(`/article/${id}/like`)
export const favoriteArticle = (id) => request.post(`/article/${id}/favorite`)

export default {
  frontArticleList,
  frontArticleDetail,
  frontCategoryList,
  loginService,
  registerService,
  likeArticle,
  favoriteArticle
}
