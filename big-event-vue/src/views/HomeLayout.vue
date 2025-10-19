<script setup>
import { ref, onMounted, computed, watch, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useTokenStore } from '@/stores/token.js'
import useUserInfoStore from '@/stores/userInfo.js'
import { ElDropdown, ElDropdownMenu, ElDropdownItem } from 'element-plus'

// 路由和状态管理
const router = useRouter()
const route = useRoute()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()

// 响应式数据
const searchKeyword = ref('')
const searchTimer = ref(null)
const mobileMenuOpen = ref(false)
const progressWidth = ref(0)
const isScrolled = ref(false)
const isMobile = computed(() => window.innerWidth < 768)
const isLoggedIn = computed(() => !!tokenStore.token)

// 分类列表（模拟数据）
const categories = ref([
  { id: 1, categoryName: '技术资讯' },
  { id: 2, categoryName: '行业动态' },
  { id: 3, categoryName: '经验分享' },
  { id: 4, categoryName: '教程学习' }
])

// 生命周期钩子
onMounted(() => {
  // 设置响应式监听
  window.addEventListener('resize', handleResize)
  window.addEventListener('scroll', handleScroll)
  
  // 设置页面标题
  updatePageTitle()
  
  // 初始化进度条效果
  simulatePageLoad()
})

onUnmounted(() => {
  // 清理事件监听
  window.removeEventListener('resize', handleResize)
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

// 处理窗口大小变化
const handleResize = () => {
  if (window.innerWidth >= 768 && mobileMenuOpen.value) {
    mobileMenuOpen.value = false
  }
}

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

// 切换移动端菜单
const toggleMobileMenu = () => {
  mobileMenuOpen.value = !mobileMenuOpen.value
}

// 搜索文章（带防抖优化）
const handleSearch = () => {
  const keyword = searchKeyword.value.trim()
  if (keyword) {
    router.push({ path: '/search', query: { keyword } })
  }
}

// 处理分类点击
const handleCategoryClick = (categoryId) => {
  router.push(`/category/${categoryId}`)
  // 在移动端点击后关闭菜单
  if (isMobile.value) {
    mobileMenuOpen.value = false
  }
}

// 处理登录
const handleLogin = () => {
  router.push('/admin/ucenter/mine')
}

// 处理登出（添加错误处理）
const handleLogout = () => {
  try {
    tokenStore.removeToken()
    userInfoStore.removeUserInfo()
    router.push('/')
  } catch (error) {
    console.error('登出失败:', error)
    // 即使出错也清除本地状态并跳转
    tokenStore.removeToken()
    userInfoStore.removeUserInfo()
    router.push('/')
  }
}

// 跳转到个人中心
const goToProfile = () => {
  router.push('/admin/')
}
</script>

<template>
  <div class="home-layout">
    <!-- 顶部加载进度条 -->
    <div class="progress-bar" :style="{ width: progressWidth + '%' }"></div>
    
    <!-- 导航栏 -->
      <header class="navbar" :class="{ 'scrolled': isScrolled }">
      <div class="container">
        <div class="logo">
          <img src="@/assets/logo.png" alt="大事件" class="logo-img" loading="lazy">
          <h1 class="logo-text">大事件资讯</h1>
        </div>
        
        <!-- 移动端菜单按钮 -->
        <button class="mobile-menu-btn" v-if="isMobile" @click="toggleMobileMenu">
          <span class="menu-icon" :class="{ 'open': mobileMenuOpen }"></span>
        </button>
        
        <!-- 导航菜单 -->
        <nav class="nav-menu" :class="{ 'mobile-open': mobileMenuOpen && isMobile }">
          <ul class="category-list">
            <li class="category-item" @click="router.push('/')">首页</li>
            <li 
              v-for="category in categories" 
              :key="category.id" 
              class="category-item"
              @click="handleCategoryClick(category.id)"
            >
              {{ category.categoryName }}
            </li>
          </ul>
        </nav>
        
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
        <div class="user-area" :class="{ 'mobile-hidden': mobileMenuOpen && isMobile }">
          <template v-if="isLoggedIn">
            <ElDropdown>
              <span class="user-dropdown">
                <img 
                  :src="userInfoStore.info?.avatar || '@/assets/avatar.jpg'" 
                  alt="用户头像" 
                  class="avatar"
                  loading="lazy"
                >
                <span class="username">{{ userInfoStore.info?.username || '用户' }}</span>
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
        <p class="copyright">© 2025 大事件资讯 版权所有</p>
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
  margin: 0 auto;
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

/* 移动端菜单按钮 */
.mobile-menu-btn {
  display: none;
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  z-index: 1001;
}

.menu-icon {
  display: block;
  width: 24px;
  height: 2px;
  background-color: #333;
  position: relative;
  transition: background-color 0.3s;
}

.menu-icon::before,
.menu-icon::after {
  content: '';
  position: absolute;
  width: 24px;
  height: 2px;
  background-color: #333;
  transition: all 0.3s;
}

.menu-icon::before {
  top: -6px;
}

.menu-icon::after {
  top: 6px;
}

.menu-icon.open {
  background-color: transparent;
}

.menu-icon.open::before {
  transform: rotate(45deg);
  top: 0;
}

.menu-icon.open::after {
  transform: rotate(-45deg);
  top: 0;
}

/* 导航菜单 */
.nav-menu {
  flex: 1;
  margin-left: 60px;
  transition: all 0.3s ease;
}

.category-list {
  display: flex;
  list-style: none;
  gap: 30px;
  margin: 0;
  padding: 0;
}

.category-item {
  font-size: 16px;
  color: #333;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 8px 0;
  white-space: nowrap;
  position: relative;
}

.category-item:hover {
  color: #1890ff;
  transform: translateY(-2px);
}

.category-item::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 2px;
  background-color: #1890ff;
  transition: width 0.3s ease;
}

.category-item:hover::after {
  width: 100%;
}

/* 搜索区域 */
.search-area {
  display: flex;
  align-items: center;
  margin: 0 40px;
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
  
  .nav-menu {
    margin-left: 40px;
  }
  
  .category-list {
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .navbar {
    padding: 10px 0;
  }
  
  .navbar .container {
    flex-wrap: wrap;
  }
  
  .mobile-menu-btn {
    display: block;
  }
  
  .nav-menu {
    margin-left: 0;
    order: 3;
    width: 100%;
    margin-top: 15px;
    max-height: 0;
    overflow: hidden;
    opacity: 0;
    visibility: hidden;
  }
  
  .nav-menu.mobile-open {
    max-height: 500px;
    opacity: 1;
    visibility: visible;
    margin-top: 10px;
    padding-bottom: 10px;
  }
  
  .category-list {
    overflow-x: auto;
    white-space: nowrap;
    gap: 15px;
    padding-bottom: 5px;
    justify-content: center;
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