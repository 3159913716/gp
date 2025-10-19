/**
 * Vue Router 配置模块
 * 
 * 本模块实现了前端路由的核心配置，包含：
 * 1. 路由视图定义
 * 2. 权限认证流程
 * 3. 登录状态管理
 * 4. 全局路由守卫
 */

// 导入Vue Router库和创建路由所需的方法
import { createRouter, createWebHistory } from 'vue-router'

// 导入Element Plus消息组件用于路由守卫中的提示
import { ElMessage } from 'element-plus'

// 导入Pinia token store用于认证状态管理
import { useTokenStore } from '@/stores/token.js'

// 导入视图组件 - 按模块分组

// 登录视图组件
import LoginVue from '@/views/Login.vue';

// 应用主布局视图组件
import LayoutVue from '@/views/Layout.vue';

// 前台布局和首页组件
import HomeLayoutVue from '@/views/HomeLayout.vue';
import HomePageVue from '@/views/HomePage.vue';

// 文章管理相关视图
import ArticleCategoryVue from '@/views/article/ArticleCategory.vue';
import ArticleManageVue from '@/views/article/ArticleManage.vue';

// 用户管理相关视图
import UserAvatarVue from '@/views/user/UserAvatar.vue';
import UserInfoVue from '@/views/user/UserInfo.vue';
import UserResetPasswordVue from '@/views/user/UserResetPassword.vue';

//用户中心相关视图
import UcenterMineVue from '@/views/ucenter/UcenterMine.vue';
import UcenterAuthorVue from '@/views/ucenter/UcenterAuthor.vue';
import UcenterArticle_collectVue from '@/views/ucenter/UcenterArticle_collect.vue';
import UcenterUser_followVue from '@/views/ucenter/UcenterUser_follow.vue';
import UcenterFansVue from '@/views/ucenter/UcenterFans.vue';

import UserLayoutVue from '@/views/UserLayout.vue';
/**
 * 创建路由实例
 * 
 * @property {Object} history - 使用HTML5 History模式的路由历史记录管理器
 * @property {Array} routes - 定义所有路由路径与组件的映射关系
 */
const router = createRouter({
  /**
   * 使用Web History API
   * import.meta.env.BASE_URL - 从Vite环境配置中获取基础URL
   */
  history: createWebHistory(import.meta.env.BASE_URL),
  
  /**
   * 路由配置数组
   * 包含路径、组件映射、元数据和嵌套路由配置
   */
  routes: [
    // === 公共路由（无需认证）===
    { path: '/login', name: 'Login', component: LoginVue, meta: { requiresAuth: false } },
    
    // === 前台路由（无需认证）===
    {
      path: '/',
      component: HomeLayoutVue,
      meta: { requiresAuth: false },
      children: [
        { path: '', component: HomePageVue, meta: { requiresAuth: false } },
        { path: 'category/:id', component: HomePageVue, meta: { requiresAuth: false } },
        { path: 'search', component: HomePageVue, meta: { requiresAuth: false } },
        { path: 'article/:id', component: HomePageVue, meta: { requiresAuth: false } }
      ]
    },
    
    // === 主应用路由（需认证）===
    {
      path: '/admin',
      component: UserLayoutVue,
      redirect: '/admin/ucenter/mine',
      meta: { requiresAuth: true },
      children: [
        // 文章管理子路由
        { path: '/admin/article/category', component: ArticleCategoryVue, meta: { requiresAuth: true } },
        { path: '/admin/article/manage', component: ArticleManageVue, meta: { requiresAuth: true } },
        
        // 用户管理子路由
        { path: '/admin/user/avatar', component: UserAvatarVue, meta: { requiresAuth: true } },
        { path: '/admin/user/info', component: UserInfoVue, meta: { requiresAuth: true } },
        { path: '/admin/user/resetPassword', component: UserResetPasswordVue, meta: { requiresAuth: true } },

        //个人中心
        { path: '/admin/ucenter/mine', 
          component: UcenterMineVue,
          redirect: '/admin/ucenter/collect',
          meta: { requiresAuth: true }, 
          children:[
            { path: '/admin/ucenter/collect', component: UcenterArticle_collectVue, meta: { requiresAuth: true } },
            { path: '/admin/ucenter/follow', component: UcenterUser_followVue, meta: { requiresAuth: true } },
            { path: '/admin/ucenter/fans', component: UcenterFansVue, meta: { requiresAuth: true } },
          ]
          },
        //作者中心
        { path: '/admin/ucenter/author', component: UcenterAuthorVue, meta: { requiresAuth: true } },
      ]
    },
  ],
})

/**
 * 全局前置守卫
 * 在每次路由切换前执行
 * 
 * @param {Route} to - 即将进入的路由对象
 * @param {Route} from - 当前导航正要离开的路由
 * @param {Function} next - 回调函数，控制路由的放行或重定向
 */
router.beforeEach((to, from, next) => {
  // 从Pinia store获取认证token
  const tokenStore = useTokenStore()
  const isAuthenticated = !!tokenStore.token  // !!确保转为布尔值
  
  /**
   * 检查目标路由是否需要认证：
   * 遍历匹配的路由记录，查看是否有路由设置了requiresAuth为true
   */
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  
  // 情况1: 需要认证但用户未登录
  if (requiresAuth && !isAuthenticated) {
    // 显示错误提示
    ElMessage.error('请先登录哟！')
    /**
     * 重定向到登录页
     * replace: true - 替换当前导航（避免历史记录回退到受限页面）
     * name: 'Login' - 使用命名路由确保正确导航
     */
    next({ name: 'Login', replace: true })
    return
  }
  
  // 情况2: 用户已登录但尝试访问登录页
  else if (to.path === '/login' && isAuthenticated) {
    /**
     * 重定向到管理员首页
     * 避免已登录用户重复访问登录页，并直接跳转到管理界面
     */
    next('/admin/article/manage')
  }
  
  // 情况3: 正常访问有权限的页面
  else {
    next()  // 放行路由导航
  }
});

/**
 * 强制跳转到登录页的辅助函数
 * 
 * 用途：
 * 1. 处理token过期场景
 * 2. 在全局拦截401错误后使用
 * 
 * 功能：
 * 1. 清除本地存储的token
 * 2. 重定向到登录页
 * 3. 显示登录状态过期提示
 */
export const redirectToLogin = () => {
  // 获取token store实例
  const tokenStore = useTokenStore()
  
  // 清除token
  tokenStore.removeToken()

  /**
   * 跳转到登录页
   * replace: true - 替换当前历史记录
   * 使用命名路由确保正确导航
   */
  router.push({ name: 'Login', replace: true }).then(() => {
    // 显示友好提示
    ElMessage.warning('您的登录状态已过期，请重新登录')
  })
}

// 导出配置好的路由实例
export default router