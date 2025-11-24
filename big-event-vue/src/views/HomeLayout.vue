<script setup>
import { ref, onMounted, computed, watch, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTokenStore } from '@/stores/token.js'
import useUserInfoStore from '@/stores/userInfo.js'
import { ElDropdown, ElDropdownMenu, ElDropdownItem, ElAvatar, ElButton } from 'element-plus'
import { articleCategoryListService } from '@/api/article.js'
import { ElMessageBox } from 'element-plus'
// 使用在线默认头像链接，不再导入本地资源
import { userInfoService } from '@/api/user.js'

// 路由和状态管理
const router = useRouter()
const route = useRoute()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()
const isFetchingUserInfo = ref(false)
const hasLoadedUserInfo = ref(false)

// 响应式数据
const searchKeyword = ref('')
const searchTimer = ref(null)
const progressWidth = ref(0)
const isScrolled = ref(false)
const isLoggedIn = computed(() => !!tokenStore.token)

// 分类列表（接口数据）
const categories = ref([])

// 按文章数量降序排序分类的辅助函数
const getSortedCategories = () => {
  return [...categories.value].sort((a, b) => {
    const countA = Number(a.articleCount ?? a.article_count ?? 0)
    const countB = Number(b.articleCount ?? b.article_count ?? 0)
    return countB - countA
  })
}

// 热门分类展示（按文章数量降序排序，取前4个）
const allCategories = computed(() => {
  const sorted = getSortedCategories()
  // 返回前4个热门分类
  return sorted.slice(0, 4)
})

// 剩余的分类（用于下拉菜单，保留排序后的顺序）
const remainingCategories = computed(() => {
  const sorted = getSortedCategories()
  // 返回排序后的剩余分类
  return sorted.slice(4)
})

// 处理分类点击事件
const handleCategoryClick = (categoryId) => {
  if (categoryId === null) {
    // 点击首页，跳转到首页
    router.push('/')
  } else {
    // 点击分类，跳转到对应分类页面
    router.push({
      path: '/',
      params: { categoryId: categoryId }
    })
  }
}

// 生命周期钩子
onMounted(() => {
  // 设置响应式监听
  window.addEventListener('scroll', handleScroll)
  
  // 设置页面标题
  updatePageTitle()
  
  // 初始化进度条效果
  simulatePageLoad()
  
  // 加载导航分类（限制4个）
  loadNavCategories()
  // 登录后首屏主动拉取用户信息，避免头像回退默认
  loadUserInfoIfNeeded()
})

onUnmounted(() => {
  // 清理事件监听
  window.removeEventListener('scroll', handleScroll)
  
  // 清理定时器
  if (searchTimer.value) {
    clearTimeout(searchTimer.value)
  }
})

// 更新页面标题
const updatePageTitle = () => {
  if (route.meta.title) {
    document.title = `${route.meta.title} - 大事件资讯`
  } else {
    document.title = '大事件资讯'
  }
}

// 监听路由变化，更新页面标题
watch(() => route.meta, () => {
  updatePageTitle()
}, { immediate: true, deep: true })

// 防抖搜索
watch(searchKeyword, (newVal) => {
  if (searchTimer.value) {
    clearTimeout(searchTimer.value)
  }
  searchTimer.value = setTimeout(() => {
    // 可以在这里添加搜索建议功能
    console.log('搜索建议:', newVal)
  }, 300)
})

// 窗口大小变化处理已移除，因为相关功能不再需要

// 处理滚动事件
const handleScroll = () => {
  isScrolled.value = window.scrollY > 20
}

// 模拟页面加载进度条
const simulatePageLoad = () => {
  // 重置进度
  progressWidth.value = 0
  
  // 模拟加载过程
  const interval = setInterval(() => {
    progressWidth.value += Math.random() * 20
    
    // 确保进度条最终达到100%
    if (progressWidth.value >= 95) {
      clearInterval(interval)
      setTimeout(() => {
        progressWidth.value = 100
        // 加载完成后隐藏进度条
        setTimeout(() => {
          progressWidth.value = 0
        }, 500)
      }, 300)
    }
  }, 200)
}

// 搜索文章（带防抖优化）
const handleSearch = () => {
  const keyword = searchKeyword.value.trim()
  if (keyword) {
    router.push({ path: '/search', query: { keyword } })
  }
}

// 处理分类点击函数已在上方定义

// 处理登录
const handleLogin = () => {
  router.push({ name: 'Login', query: { redirect: '/admin/ucenter/mine' } })
}

// 处理登出（添加错误处理）
const clearUserCaches = () => {
  try {
    const keys = Object.keys(localStorage)
    keys.forEach(k => {
      if (k.startsWith('article:interactions:')) {
        localStorage.removeItem(k)
      }
    })
  } catch (e) {
    console.warn('清理本地缓存失败:', e?.message || e)
  }
}
const handleLogout = async () => {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      type: 'warning',
      confirmButtonText: '是',
      cancelButtonText: '否',
      showClose: true,
      closeOnClickModal: false
    })
    tokenStore.removeToken()
    // 修正为实际存在的方法名
    if (typeof userInfoStore.removeInfo === 'function') {
      userInfoStore.removeInfo()
    }
    // 动态路由已移除，无需清除
    // 清理与用户相关的前端缓存
    clearUserCaches()
    // 路由跳转到首页后，强制刷新页面以完全重渲染
    await router.replace('/')
    setTimeout(() => {
      window.location.reload()
    }, 0)
  } catch (error) {
    // 用户取消或关闭弹窗，留在当前页面
    if (error !== 'cancel' && error !== 'close') {
      console.error('登出确认/处理失败:', error)
    }
  }
}

// 跳转到个人中心
const goToProfile = () => {
  // 直接跳转到个人中心页面，确保触发到收藏页面的重定向
  router.push('/admin/ucenter/mine')
}

// 加载导航分类（显示所有分类，前4个主分类，其余在下拉菜单中）
const loadNavCategories = async () => {
  try {
    // 不传递userId参数，获取所有分类
    const res = await articleCategoryListService()
    const payload = res?.data ?? res
    let list = Array.isArray(payload?.items)
      ? payload.items
      : (Array.isArray(payload?.list) ? payload.list : (Array.isArray(payload) ? payload : []))

    categories.value = list.map(c => ({
      id: c.id,
      categoryName: c.categoryName ?? c.category_name ?? '',
      categoryAlias: c.categoryAlias ?? c.category_alias ?? ''
    }))
  } catch (e) {
    console.error('加载导航分类失败:', e?.message || e)
    // 使用默认分类兜底展示，避免导航为空
    categories.value = defaultCategories.slice(0,4)
  }
}

// 首屏及登录后拉取用户信息，确保头像及时可用
const loadUserInfoIfNeeded = async () => {
  if (!tokenStore.token) return
  if (hasLoadedUserInfo.value || isFetchingUserInfo.value) return
  const info = userInfoStore.info || {}
  const needFetch = !info || Object.keys(info).length === 0 || !info.userPic || !info.username
  if (!needFetch) { hasLoadedUserInfo.value = true; return }
  try {
    isFetchingUserInfo.value = true
    const res = await userInfoService()
    if (res && res.data) {
      userInfoStore.setInfo(res.data)
      hasLoadedUserInfo.value = true
    }
  } catch (e) {
    console.warn('首次加载用户信息失败:', e?.message || e)
  } finally {
    isFetchingUserInfo.value = false
  }
}

// 监听token变化，登录成功后立即拉取用户信息
watch(() => tokenStore.token, (newToken, oldToken) => {
  if (newToken && newToken !== oldToken) {
    hasLoadedUserInfo.value = false
    loadUserInfoIfNeeded()
  }
})
</script>

<template>
  <div class="home-layout">
    <!-- 顶部加载进度条 -->
    <div class="progress-bar" :style="{ width: progressWidth + '%' }"></div>
    
    <!-- 导航栏 -->
      <header class="navbar" :class="{ 'scrolled': isScrolled }">
      <div class="container">
        <div class="logo">
          <h1 class="logo-text">大事件资讯</h1>
        </div>      
        <!-- 导航菜单 -->
        <div class="nav-bar" style="display:flex;align-items:center;gap:4px;padding:16px 0 8px 0;">
          <ElButton type="primary" @click="handleCategoryClick(null)">首页</ElButton>
          <template v-for="cat in allCategories" :key="cat.id">
            <ElButton type="default" @click="handleCategoryClick(cat.id)">{{ cat.categoryName }}</ElButton>
          </template>
          <!-- 下拉菜单展示剩余分类 -->
          <ElDropdown v-if="remainingCategories.length > 0">
            <ElButton type="default" class="dropdown-button">
              更多分类
              <span class="el-icon-arrow-down el-icon--right"></span>
              
            </ElButton>
            <template #dropdown>
              <ElDropdownMenu>
                <ElDropdownItem 
                  v-for="cat in remainingCategories" 
                  :key="cat.id"
                  @click="handleCategoryClick(cat.id)"
                >
                  {{ cat.categoryName }}
                </ElDropdownItem>
              </ElDropdownMenu>
            </template>
          </ElDropdown>
        </div>
        
        <!-- 搜索区域 -->
        <div class="search-area">
          <input 
            type="text" 
            class="search-input" 
            v-model="searchKeyword"
            placeholder="搜索文章..."
            @keyup.enter="handleSearch"
            autocomplete="off"
          >
          <button class="search-btn" @click="handleSearch">搜索</button>
        </div>
        
        <!-- 用户区域 -->
        <div class="user-area">
          <template v-if="isLoggedIn">
           <ElDropdown>
  <span class="user-dropdown">
    <ElAvatar :src="userInfoStore.info?.userPic ? userInfoStore.info.userPic : 'https://n.sinaimg.cn/sinacn20122/80/w440h440/20181223/62bf-hqqzpku8165766.jpg'" />
    <span class="username">{{ userInfoStore.info?.nickname || userInfoStore.info?.username || '用户' }}</span>
  </span>
  <template #dropdown>
    <ElDropdownMenu>
      <ElDropdownItem @click="goToProfile">个人中心</ElDropdownItem>
      <ElDropdownItem @click="handleLogout">退出登录</ElDropdownItem>
    </ElDropdownMenu>
  </template>
</ElDropdown>
          </template>
          <template v-else>
            <button class="login-btn" @click="handleLogin">登录/注册</button>
          </template>
        </div>
      </div>
    </header>
    
    <!-- 主要内容区域 -->
    <main class="main-content">
      <div class="container">
        <router-view></router-view>
      </div>
    </main>
    
    <!-- 底部版权栏 -->
    <footer class="footer">
      <div class="container">
        <p class="copyright">© 2025 大事件资讯 AAA保险 版权所有</p>
      </div>
    </footer>
  </div>
</template>

<style scoped>
.home-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* 顶部加载进度条 */
.progress-bar {
  position: fixed;
  top: 0;
  left: 0;
  height: 3px;
  background-color: #1890ff;
  z-index: 2000;
  transition: width 0.3s ease;
}

/* 导航栏样式 */
.navbar {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 15px 0;
  position: sticky;
  top: 0;
  z-index: 1000;
  transition: all 0.3s ease;
}

/* 导航栏滚动效果 */
.navbar.scrolled {
  padding: 10px 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.container {
  width: 1200px;
  margin: 0 100px 0 50px;
  padding: 0 20px;
}

/* Logo 样式 */
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  transition: all 0.3s ease;
}

.logo-img {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  transition: transform 0.3s ease;
}

.logo:hover .logo-img {
  transform: scale(1.1);
}

.logo-text {
  font-size: 24px;
  font-weight: bold;
  color: #1890ff;
  margin: 0;
  white-space: nowrap;
  transition: color 0.3s ease;
}

.logo:hover .logo-text {
  color: #40a9ff;
}

/* 导航内容布局 */
.navbar .container {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 导航菜单样式已简化 */

/* 导航按钮样式定制 */
.nav-bar {
  flex: 1;
  margin-left: 30px;
}

.nav-bar .el-button {
   border: none !important;
   box-shadow: none !important;
   background: none !important;
   padding: 8px 6px;
   position: relative;
   color: #333;
   font-size: 16px;
}

.nav-bar .el-button:hover {
  color: #1890ff;
  background: none !important;
}

.nav-bar .el-button::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  height: 2px;
  background-color: #1890ff;
  transition: width 0.3s ease;
}

.nav-bar .el-button:hover::after {
  width: 80%;
}

/* 下拉按钮样式调整 - 与导航按钮保持一致 */
.nav-bar .dropdown-button {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 6px !important;
}

/* 下拉菜单样式 */
.el-dropdown-menu {
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.el-dropdown-item {
  padding: 8px 16px;
  transition: all 0.3s ease;
}

.el-dropdown-item:hover {
  color: #1890ff;
  background-color: rgba(24, 144, 255, 0.05);
}

/* 移除选中按钮的特殊样式 */
.nav-bar .el-button--primary {
  color: #333;
}

.nav-bar .el-button--primary:hover {
  color: #1890ff;
}

/* 搜索区域 */
.search-area {
  display: flex;
  align-items: center;
  margin: 0 35px;
  transition: all 0.3s ease;
  position: relative;
}

.search-input {
  width: 300px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-right: none;
  border-radius: 4px 0 0 4px;
  font-size: 14px;
  outline: none;
  transition: all 0.3s ease;
  background-color: #fafafa;
  box-sizing: border-box;
}

.search-input:focus {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
  background-color: #fff;
}

.search-input::placeholder {
  color: #bfbfbf;
  transition: color 0.3s ease;
}

.search-input:focus::placeholder {
  color: #d9d9d9;
}

.search-btn {
  height: 36px;
  padding: 0 20px;
  background-color: #1890ff;
  color: white;
  border: 1px solid #1890ff;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  white-space: nowrap;
  box-sizing: border-box;
}

.search-btn:hover {
  background-color: #40a9ff;
  border-color: #40a9ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
}

.search-btn:active {
  transform: translateY(0);
}

/* 用户区域 */
.user-area {
  display: flex;
  align-items: center;
  transition: all 0.3s ease;
}

.login-btn {
  padding: 8px 20px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.3s ease;
  white-space: nowrap;
}

.login-btn:hover {
  background-color: #40a9ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3);
}

.login-btn:active {
  transform: translateY(0);
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 4px;
  transition: all 0.3s ease;
  position: relative;
}

.user-dropdown:hover {
  background-color: #f5f5f5;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
  border: 2px solid #fff;
  transition: all 0.3s ease;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.user-dropdown:hover .avatar {
  transform: scale(1.1);
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.2);
}

.username {
  font-size: 14px;
  color: #333;
  white-space: nowrap;
  font-weight: 500;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  padding: 40px 0;
  background-color: #f5f5f5;
  transition: padding 0.3s ease;
}

/* 底部样式 */
.footer {
  background-color: #fff;
  padding: 30px 0;
  text-align: center;
  margin-top: 40px;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.copyright {
  margin: 0;
  color: #666;
  font-size: 14px;
  white-space: nowrap;
  transition: color 0.3s ease;
}

.copyright:hover {
  color: #1890ff;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .container {
    width: 100%;
  }
  
  .search-input {
    width: 200px;
  }
  
  /* 导航菜单响应式样式已简化 */
}

@media (max-width: 768px) {
  .navbar {
    padding: 10px 0;
  }
  
  .navbar .container {
    flex-wrap: wrap;
  }
  
  /* 移动端菜单按钮和导航菜单样式已移除 */
  
  /* 移动端导航按钮样式 */
  .nav-bar {
    margin-left: 0;
    order: 3;
    width: 100%;
    margin-top: 10px;
    display: flex;
    flex-direction: row;
    overflow-x: auto;
    padding-bottom: 5px;
    justify-content: center;
    gap: 10px;
  }
  
  .nav-bar .el-button {
    white-space: nowrap;
    font-size: 14px;
    padding: 6px 12px;
  }
  
  .nav-bar .el-button:active {
    background-color: rgba(24, 144, 255, 0.05);
  }
  
  /* 移动端下拉按钮样式 - 与导航按钮保持一致 */
  .nav-bar .dropdown-button {
    display: flex;
    align-items: center;
    padding: 8px 6px !important;
  }
  
  /* 移动端下拉菜单样式 */
  .el-dropdown-menu {
    max-width: 200px;
    width: 100%;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  }
  
  .el-dropdown-item {
    padding: 10px 16px;
    font-size: 14px;
  }
  
  .search-area,
  .user-area {
    margin: 0 10px;
  }
  
  .search-input {
    width: 120px;
  }
  
  .main-content {
    padding: 20px 0;
  }
  
  .footer {
    padding: 20px 0;
    margin-top: 20px;
  }
}

@media (max-width: 480px) {
  .logo-img {
    width: 32px;
    height: 32px;
  }
  
  .logo-text {
    font-size: 18px;
  }
  
  .category-item {
    font-size: 14px;
  }
  
  .search-input {
    width: 100px;
  }
  
  .search-btn {
    padding: 0 15px;
    font-size: 12px;
  }
}
</style>