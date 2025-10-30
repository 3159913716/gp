<template>
  <div class="admin-layout">
    <el-aside :width="asideWidth" style="background:#fff; border-right:1px solid #ebeef5">
      <div class="brand" style="height:64px; display:flex; align-items:center; justify-content:center; font-weight:600">
        <span v-if="!collapsed">后台管理</span>
        <span v-else>管理</span>
      </div>
      <el-menu :default-active="$route.path" router @select="handleSelect" class="el-menu-vertical-demo" :collapse="collapsed">
        <el-menu-item index="/admin/dashboard">仪表盘</el-menu-item>
        <el-menu-item index="/admin/users">用户管理</el-menu-item>
        <el-menu-item index="/admin/applications">作者申请</el-menu-item>
      </el-menu>
    </el-aside>

    <div class="admin-main">
      <el-header class="header">
        <div>用户：<strong>{{ userInfoStore?.info?.username || '管理员' }}</strong>
          <el-tag type="primary" size="small" style="margin-left:8px">管理员</el-tag>
        </div>
        
        <div class="nav-wrapper">
          <router-link to="/" class="nav-item">首页</router-link>
          <router-link to="/category/1" class="nav-item">技术资讯</router-link>
          <router-link to="/category/2" class="nav-item">行业动态</router-link>
          <router-link to="/category/3" class="nav-item">经验分享</router-link>
          <router-link to="/category/4" class="nav-item">教程学习</router-link>
        </div>
        
        <el-dropdown placement="bottom-end" @command="handleCommand">
          <span class="el-dropdown__box">
            <el-avatar :size="34" :src="userAvatar || avatar" />
            <el-icon>
              <CaretBottom />
            </el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="info" :icon="User">基本资料</el-dropdown-item>
              <el-dropdown-item command="avatar" :icon="Crop">更换头像</el-dropdown-item>
              <el-dropdown-item command="resetpassword" :icon="EditPen">重置密码</el-dropdown-item>
              <el-dropdown-item command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <main class="admin-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Setting, Management, Promotion, UserFilled, User, Crop, EditPen, SwitchButton, CaretBottom } from '@element-plus/icons-vue'
import avatar from '@/assets/default.png'
import useUserInfoStore from '@/stores/userInfo.js'
import { useTokenStore } from '@/stores/token.js'

const router = useRouter()
const route = useRoute()
const userInfoStore = useUserInfoStore()
const tokenStore = useTokenStore()

const collapsed = ref(false)
const asideWidth = computed(() => (collapsed.value ? '64px' : '200px'))

const userAvatar = computed(() => {
  return userInfoStore.info?.userPic || ''
})

function handleSelect(index) {
  router.push(index)
}

function toggleCollapse() {
  collapsed.value = !collapsed.value
}

function navigateTo(routeMatch) {
  router.push(routeMatch.path)
}

function handleCommand(command) {
  if (command === 'logout') {
    ElMessageBox.confirm(
      '确认退出登录吗？',
      '温馨提示',
      {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
      .then(() => {
        tokenStore.removeToken()
        userInfoStore.removeInfo()
        router.push('/')
        ElMessage.success('退出成功!')
      })
      .catch(() => {
        ElMessage({ type: 'info', message: '取消退出' })
      })
  } else {
    router.push('/admin/user/' + command)
  }
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background-color: #f5f7fa;
}

.brand {
  color: #1890ff;
  font-size: 18px;
}

.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.header {
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  height: 60px;
}

.el-dropdown__box {
  display: flex;
  align-items: center;
  margin-left: 20px;
}

.el-dropdown__box .el-icon {
  color: #999;
  margin-left: 10px;
}

.nav-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 30px;
  flex: 1;
  flex-wrap: nowrap;
  overflow: visible;
}

.nav-item {
  font-size: 16px;
  color: #333;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 8px 0;
  white-space: nowrap;
  position: relative;
}

.nav-item {
  font-size: 16px;
  color: #333;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.3s ease;
  padding: 8px 0;
  white-space: nowrap;
  position: relative;
}

.nav-item:hover {
  color: #1890ff;
  transform: translateY(-2px);
}

.nav-item:hover::after {
  width: 100%;
}

.nav-item::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 0;
  height: 2px;
  background-color: #1890ff;
  transition: width 0.3s ease;
}

.nav-item.router-link-active {
  color: #1890ff;
}

.nav-item.router-link-active::after {
  width: 100%;
  background-color: #1890ff;
}

.admin-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

@media (max-width: 768px) {
  .nav-wrapper {
    display: none;
  }
  .admin-layout {
    flex-direction: column;
  }
  .admin-main {
    width: 100%;
  }
  .el-aside {
    display: none;
  }
}
</style>
