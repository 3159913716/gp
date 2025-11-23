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

// 导入Pinia store用于状态管理
import { useTokenStore } from '@/stores/token.js'
import useUserInfoStore from '@/stores/userInfo.js'

// 导入API服务
import { userInfoService } from '@/api/user.js'

// 导入视图组件 - 按模块分组

// 登录视图组件
import LoginVue from '@/views/Login.vue';

// 前台布局和首页组件
import HomeLayoutVue from '@/views/HomeLayout.vue';
import HomePageVue from '@/views/HomePage.vue';
import ArticleDetailVue from '@/views/article/ArticleDetail.vue'

// 用户管理相关视图
import UserAvatarVue from '@/views/user/UserAvatar.vue';
import UserInfoVue from '@/views/user/UserInfo.vue';
import UserResetPasswordVue from '@/views/user/UserResetPassword.vue';

//用户中心相关视图
import UcenterMineVue from '@/views/ucenter/UcenterMine.vue';
import UcenterArticle_collectVue from '@/views/ucenter/UcenterArticle_collect.vue';
import UcenterUser_followVue from '@/views/ucenter/UcenterUser_follow.vue';
import UcenterFansVue from '@/views/ucenter/UcenterFans.vue';

//成为作者相关视图
import UcenterAuthorVue from '@/views/author/UcenterAuthor.vue';


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
      name: 'HomeRoot',
      component: HomeLayoutVue,
      meta: { requiresAuth: false },
      children: [
        { path: '', name: 'HomePage', component: HomePageVue, meta: { requiresAuth: false } },
        { path: 'category/:id', name: 'CategoryPage', component: HomePageVue, meta: { requiresAuth: false } },
        { path: 'search', name: 'SearchPage', component: HomePageVue, meta: { requiresAuth: false } },
        { path: 'article/:id', name: 'ArticleDetail', component: ArticleDetailVue, meta: { requiresAuth: false } }
      ]
    },
    
    // === 后台个人中心路由（需要认证）===
    {
      path: '/admin',
      name: 'Admin',
      component: () => import('@/views/BackstageLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        // 公共路由
        { 
          path: '', 
          name: 'UcenterMine', 
          component: UcenterMineVue, 
          meta: { requiresAuth: true } 
        },
        { 
          path: 'ucenter/mine', 
          name: 'UcenterMineDetail', 
          component: UcenterMineVue, 
          meta: { requiresAuth: true },
          children: [
            { path: '', name: 'UcenterMineDefault', redirect: 'collect' }, // 默认重定向到收藏页面
            { path: 'collect', name: 'UcenterCollect', component: UcenterArticle_collectVue, meta: { requiresAuth: true } },
            { path: 'follow', name: 'UcenterFollow', component: UcenterUser_followVue, meta: { requiresAuth: true } },
            { path: 'fans', name: 'UcenterFansList', component: UcenterFansVue, meta: { requiresAuth: true } }
          ]
        },
        { 
          path: 'user/info', 
          name: 'UserInfo', 
          component: UserInfoVue, 
          meta: { requiresAuth: true } 
        },
        { 
          path: 'user/avatar', 
          name: 'UserAvatar', 
          component: UserAvatarVue, 
          meta: { requiresAuth: true } 
        },
        { 
          path: 'user/password', 
          name: 'UserResetPassword', 
          component: UserResetPasswordVue, 
          meta: { requiresAuth: true } 
        },
        
        // 管理员路由
        { 
          path: 'dashboard', 
          name: 'AdminDashboard', 
          component: () => import('@/views/admin/Dashboard.vue'), 
          meta: { requiresAuth: true } 
        },
        { 
          path: 'users', 
          name: 'AdminUsers', 
          component: () => import('@/views/admin/Users.vue'), 
          meta: { requiresAuth: true } 
        },
        { 
          path: 'applications', 
          name: 'AdminApplications', 
          component: () => import('@/views/admin/Applications.vue'), 
          meta: { requiresAuth: true } 
        },
        
        // 作者路由
        { 
          path: 'article-category', 
          name: 'ArticleCategory', 
          component: () => import('@/views/article/ArticleCategory.vue'), 
          meta: { requiresAuth: true } 
        },
        { 
          path: 'article-manage', 
          name: 'ArticleManage', 
          component: () => import('@/views/article/ArticleManage.vue'), 
          meta: { requiresAuth: true } 
        },
        
        // 普通用户路由
        { 
          path: 'author-center', 
          name: 'AuthorCenter', 
          component: UcenterAuthorVue, 
          meta: { requiresAuth: true } 
        }
      ]
    }
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
router.beforeEach(async (to, from, next) => {
  // 从Pinia store获取认证token和用户信息
  const tokenStore = useTokenStore();
  const userInfoStore = useUserInfoStore();
  const isAuthenticated = !!tokenStore.token;  // !!确保转为布尔值
  
  /**
   * 检查目标路由是否需要认证：
   * 遍历匹配的路由记录，查看是否有路由设置了requiresAuth为true
   */
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth);
  
  // 情况1: 需要认证但用户未登录
  if (requiresAuth && !isAuthenticated) {
    // 显示错误提示
    ElMessage.error('请先登录哟！');
    // 重定向到登录页
    next({ name: 'Login', replace: true });
    return;
  }
  
  // 情况2: 用户已登录但尝试访问登录页
  else if (to.path === '/login' && isAuthenticated) {
    // 确保获取最新的用户信息
    await ensureUserInfoLoaded(userInfoStore, tokenStore);
    // 重定向到个人中心
    next('/admin/ucenter/mine');
  }
  
  // 情况3: 需要认证且用户已登录
  else if (requiresAuth && isAuthenticated) {
    // 确保用户信息已加载
    await ensureUserInfoLoaded(userInfoStore, tokenStore);
    next();
  }
  
  // 情况4: 正常访问无需认证的页面
  else {
    next();  // 放行路由导航
  }
});

// 导出必要的函数

/**
 * 确保用户信息已加载
 * 如果本地存储没有用户信息，则从服务器获取
 * 
 * @param {Object} userInfoStore - 用户信息store
 * @param {Object} tokenStore - token store
 * @returns {Promise<void>}
 */
async function ensureUserInfoLoaded(userInfoStore, tokenStore) {
  // 如果用户信息为空且有token，则获取用户信息
  // 使用!userInfoStore.info更严谨地判断用户信息是否已加载
  if (tokenStore.token && !userInfoStore.info) {
    try {
      const result = await userInfoService();
      userInfoStore.setInfo(result.data);
    } catch (error) {
      console.error('获取用户信息失败:', error);
      // 获取失败时清除token并重定向到登录页
      redirectToLogin();
    }
  }
}

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
  const tokenStore = useTokenStore();
  
  // 清除token
  tokenStore.removeToken();

  /**
   * 跳转到登录页
   * replace: true - 替换当前历史记录
   * 使用命名路由确保正确导航
   */
  router.push({ name: 'Login', replace: true }).then(() => {
    // 显示友好提示
    ElMessage.warning('您的登录状态已过期，请重新登录');
  });
};

// 导出配置好的路由实例
export default router;