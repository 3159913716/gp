<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useTokenStore } from '@/stores/token.js'
import useUserInfoStore from '@/stores/userInfo.js'
import { ElDropdown, ElDropdownMenu, ElDropdownItem } from 'element-plus'

console.log('HomeLayout.vue 组件加载')

const router = useRouter()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()

console.log('Token store:', tokenStore)
console.log('User info store:', userInfoStore)

// 搜索关键词
const searchKeyword = ref('')

// 检查用户是否登录
const isLoggedIn = ref(false)

// 分类列表（模拟数据，实际应从API获取）
const categories = ref([
  { id: 1, categoryName: '技术资讯' },
  { id: 2, categoryName: '行业动态' },
  { id: 3, categoryName: '经验分享' },
  { id: 4, categoryName: '教程学习' }
])

onMounted(() => {
  console.log('HomeLayout onMounted 执行')
  isLoggedIn.value = !!tokenStore.token
  console.log('用户登录状态:', isLoggedIn.value)
  if (isLoggedIn.value && userInfoStore.info) {
    console.log('用户信息存在:', userInfoStore.info)
  }
})

// 搜索文章
const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/search', query: { keyword: searchKeyword.value.trim() } })
  }
}

// 处理分类点击
const handleCategoryClick = (categoryId) => {
  router.push(`/category/${categoryId}`)
}

// 处理登录
const handleLogin = () => {
  router.push('/admin/ucenter/mine')
}

// 处理登出
const handleLogout = () => {
  tokenStore.removeToken()
  userInfoStore.removeUserInfo()
  isLoggedIn.value = false
  router.push('/')
}

// 跳转到个人中心
const goToProfile = () => {
 router.push('/admin/ucenter/mine')
}
</script>

<template>
  <div class="home-layout">
    <!-- 导航栏 -->
    <header class="navbar">
      <div class="container">
        <div class="logo">
          <img src="@/assets/logo.png" alt="大事件" class="logo-img">
          <h1 class="logo-text">大事件资讯</h1>
        </div>
        
        <nav class="nav-menu">
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
        
        <div class="search-area">
          <input 
            type="text" 
            class="search-input" 
            v-model="searchKeyword"
            placeholder="搜索文章..."
            @keyup.enter="handleSearch"
          >
          <button class="search-btn" @click="handleSearch">搜索</button>
        </div>
        
        <div class="user-area">
          <template v-if="isLoggedIn">
            <ElDropdown>
              <span class="user-dropdown">
                <img 
                  :src="userInfoStore.info?.avatar || '@/assets/avatar.jpg'" 
                  alt="用户头像" 
                  class="avatar"
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
}

/* 导航栏样式 */
.navbar {
  background-color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  padding: 15px 0;
  position: sticky;
  top: 0;
  z-index: 1000;
}

.container {
  width: 1200px;
  margin: 0 auto;
  padding: 0 20px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-img {
  width: 40px;
  height: 40px;
}

.logo-text {
  font-size: 24px;
  font-weight: bold;
  color: #1890ff;
  margin: 0;
}

/* 导航内容布局 */
.navbar .container {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.nav-menu {
  flex: 1;
  margin-left: 60px;
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
  transition: color 0.3s;
  padding: 8px 0;
}

.category-item:hover {
  color: #1890ff;
}

/* 搜索区域 */
.search-area {
  display: flex;
  align-items: center;
  margin: 0 40px;
}

.search-input {
  width: 300px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px 0 0 4px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.3s;
}

.search-input:focus {
  border-color: #1890ff;
}

.search-btn {
  height: 36px;
  padding: 0 20px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.search-btn:hover {
  background-color: #40a9ff;
}

/* 用户区域 */
.user-area {
  display: flex;
  align-items: center;
}

.login-btn {
  padding: 8px 20px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.login-btn:hover {
  background-color: #40a9ff;
}

.user-dropdown {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.user-dropdown:hover {
  background-color: #f5f5f5;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  object-fit: cover;
}

.username {
  font-size: 14px;
  color: #333;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  padding: 40px 0;
  background-color: #f5f5f5;
}

/* 底部样式 */
.footer {
  background-color: #fff;
  padding: 30px 0;
  text-align: center;
  margin-top: 40px;
}

.copyright {
  margin: 0;
  color: #666;
  font-size: 14px;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .container {
    width: 100%;
  }
  
  .search-input {
    width: 200px;
  }
}

@media (max-width: 768px) {
  .navbar .container {
    flex-wrap: wrap;
  }
  
  .nav-menu {
    margin-left: 0;
    order: 3;
    width: 100%;
    margin-top: 15px;
  }
  
  .category-list {
    overflow-x: auto;
    white-space: nowrap;
    gap: 20px;
  }
  
  .search-area {
    margin: 0 20px;
  }
  
  .search-input {
    width: 150px;
  }
}
</style>