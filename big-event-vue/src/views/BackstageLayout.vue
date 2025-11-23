<!-- 后台主页面-->
<script setup>
// 引入Vue Router的useRouter钩子
import { useRouter } from 'vue-router'
// 引入Element Plus的消息提示和消息框组件
import { ElMessage, ElMessageBox } from 'element-plus'
// 引入Element Plus的图标组件用于菜单和下拉菜单
import { CaretBottom, User, Setting, PieChart, Avatar, DocumentCopy, Document, Edit } from '@element-plus/icons-vue'
// 引入默认头像图片
import avatar from '@/assets/default.png'
// 引入获取用户信息的API服务
import { userInfoService } from '@/api/user.js'
// 引入管理用户信息的Pinia store
import useUserInfoStore from '@/stores/userInfo.js'
// 引入Vue的计算属性
import { computed } from 'vue'
// 引入管理token的Pinia store
import { useTokenStore } from '@/stores/token.js'

// 状态管理
const router = useRouter() // 获取路由实例
const tokenStore = useTokenStore() // 使用token存储实例
const userInfoStore = useUserInfoStore() // 使用用户信息存储实例

// 计算属性：判断用户角色
const isAdmin = computed(() => userInfoStore?.info?.role === 0)
const isAuthor = computed(() => userInfoStore?.info?.role === 1)

/*
 * 获取用户信息
 * 功能：从API获取当前登录用户的信息并存储到Pinia store中
 */
const getUserInfo = async () => {
  // 调用用户信息API服务
  const result = await userInfoService()
  // 将获取到的用户信息存入Pinia store
  userInfoStore.setInfo(result.data)
}
// 组件挂载时立即获取用户信息
getUserInfo()

/*
 * 处理下拉菜单命令
 * @param {string} command - 用户选择的命令标识
 * 功能：根据用户在下拉菜单或菜单项的选择执行相应操作
 */
const handleCommand = (command) =>{
  // 如果命令是退出登录
  if (command === 'logout') {
    // 显示确认对话框
    ElMessageBox.confirm(
      '确认退出登录吗？', // 提示内容
      '温馨提示', // 标题
      {
        confirmButtonText: '确认', // 确认按钮文本
        cancelButtonText: '取消', // 取消按钮文本
        type: 'warning', // 对话框类型（警告）
      }
    )
      .then(() =>{
        // 用户点击确认后的处理
        tokenStore.removeToken() // 清除token
        userInfoStore.removeInfo() // 清除用户信息
        router.push('/') // 跳转到首页
        ElMessage.success('退出成功!') // 显示成功消息
      })
      .catch(() =>{
        // 用户点击取消后的处理
        ElMessage({ type: 'info', message: '取消退出' }) // 显示取消消息
      })
  } else if (command === 'home') {
    // 返回首页命令
    router.push('/') // 直接跳转到首页
  }
}
</script>

<template>
  <!-- Element Plus布局容器 -->
  <el-container class="layout-container">
    <!-- 左侧菜单区域 -->
    <el-aside width="200px">
      <!-- 顶部logo区域 -->
      <div class="el-aside__logo"></div>
      <!-- 菜单组件 -->
      <el-menu active-text-color="#ffd04b" background-color="#232323" text-color="#fff" router>
        <!-- 公共菜单项 - 所有角色都可见 -->
        <!-- 我的 -->
        <el-menu-item index="/admin/ucenter/mine">
          <el-icon>
            <User />
          </el-icon>
          <span>我的</span>
        </el-menu-item>
        
        <!-- 管理员菜单项 - 仅角色0可见 -->
        <template v-if="isAdmin">
          <el-menu-item index="/admin/dashboard">
            <el-icon>
                <PieChart />
              </el-icon>
              <span>仪表盘</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon>
                <Avatar />
              </el-icon>
              <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/applications">
              <el-icon>
                <DocumentCopy />
              </el-icon>
              <span>作者申请</span>
          </el-menu-item>
        </template>
        
        <!-- 作者菜单项 - 仅角色1可见 -->
        <template v-else-if="isAuthor">
          <el-menu-item index="/admin/article-category">
              <el-icon>
                <DocumentCopy />
              </el-icon>
              <span>文章分类</span>
          </el-menu-item>
          <el-menu-item index="/admin/article-manage">
              <el-icon>
                <Edit />
              </el-icon>
              <span>文章管理</span>
          </el-menu-item>
        </template>
        
        <!-- 普通用户菜单项 - 仅角色2可见 -->
        <template v-else>
          <el-menu-item index="/admin/author-center">
              <el-icon>
        <Document />
      </el-icon>
              <span>作者中心</span>
          </el-menu-item>
        </template>
        
        <!-- 设置菜单 - 所有角色都可见（公共菜单项） -->
        <el-sub-menu index="/admin/settings">
          <template #title>
            <el-icon>
                <Setting />
              </el-icon>
              <span>设置</span>
          </template>
          <el-menu-item index="/admin/user/info">
            <span>基本资料</span>
          </el-menu-item>
          <el-menu-item index="/admin/user/avatar">
            <span>更换头像</span>
          </el-menu-item>
          <el-menu-item index="/admin/user/password">
            <span>修改密码</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 右侧主内容区域 -->
    <el-container>
      <!-- 顶部头部区域 -->
      <el-header>
        <!-- 左侧：返回首页按钮 -->
        <div class="header-left">
          <router-link to="/" class="nav-item">返回首页</router-link>
        </div>
        
        <!-- 中间：显示当前登录用户昵称 -->
        <div class="header-center">
          <div>用户：<strong>{{userInfoStore?.info?.nickname || userInfoStore?.info?.username || 
            '未登录用户'  }}</strong>
            <el-tag v-if="isAdmin" type="primary" size="small" style="margin-left:8px">管理员</el-tag>
            <el-tag v-else-if="isAuthor" type="success" size="small" style="margin-left:8px">作者</el-tag>
            <el-tag v-else type="info" size="small" style="margin-left:8px">普通用户</el-tag>
          </div>
        </div>

        <!-- 右侧：用户操作下拉菜单 -->
        <div class="header-right">
          <el-dropdown placement="bottom-end" @command="handleCommand">
          <!-- 下拉菜单触发器 -->
          <span class="el-dropdown__box">
            <!-- 用户头像 -->
            <el-avatar :src="userInfoStore.info.userPic ? userInfoStore.info.userPic : avatar" />
            <!-- 下拉图标 -->
            <el-icon>
              <CaretBottom />
            </el-icon>
          </span>

          <!-- 下拉菜单内容 -->
          <template #dropdown>
            <el-dropdown-menu>
              <!-- 返回首页菜单项 -->
              <el-dropdown-item command="home">返回首页</el-dropdown-item>
              
              <!-- 退出登录菜单项 -->
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        </div>
      </el-header>

      <!-- 中间主内容区域 -->
      <el-main>
        <!-- 路由视图容器 -->
        <router-view></router-view>
      </el-main>

      <!-- 底部区域 -->
      <el-footer>大事件 ©2025 Created by AAA保险 版权所有</el-footer>
    </el-container>
  </el-container>
</template>

<style lang="scss" scoped>
/* 布局容器样式 */
.layout-container {
  height: 100vh; // 高度占满整个视口

  /* 左侧菜单区域样式 */
  .el-aside {
    background-color: #232323; // 深色背景

    /* logo区域样式 */
    &__logo {
      height: 120px;
      // 设置logo背景图
      background: url('@/assets/logo.png') no-repeat center / 120px auto;
    }

    /* 菜单样式 */
    .el-menu {
      border-right: none; // 去除右边框
    }
  }

  /* 头部区域样式 */
  .el-header {
    background-color: #fff; // 白色背景
    display: flex;
    align-items: center; // 垂直居中
    justify-content: space-between; // 两端对齐
    padding: 0 20px; // 添加内边距
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1); // 添加阴影效果
    height: 60px; // 设置高度
  }
  
  /* 头部三栏布局 */
  .header-left,
  .header-center,
  .header-right {
    display: flex;
    align-items: center;
  }
  
  .header-center {
    flex: 1;
    justify-content: center;
    min-width: 0;
  }
  
  .header-right {
    min-width: 0;
  }

  /* 下拉菜单容器样式 */
  .el-dropdown__box {
    display: flex;
    align-items: center; // 垂直居中

    /* 下拉图标样式 */
    .el-icon {
      color: #999; // 灰色
      margin-left: 10px; // 左侧间距
    }

    /* 激活和聚焦状态样式 */
    &:active,
    &:focus {
      outline: none; // 去除轮廓
    }
  }
  
  /* 导航栏样式 - 与首页导航菜单样式完全一致 */
  .nav-item {
    font-size: 16px;
    color: #333;
    text-decoration: none;
    cursor: pointer;
    transition: all 0.3s ease;
    padding: 8px 8px !important;
    white-space: nowrap;
    position: relative;
    border: none !important;
    background: none !important;
    box-shadow: none !important;
    font-weight: 500 !important;
    overflow: hidden;
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
  
  /* 激活状态样式 */
  .nav-item.router-link-active {
    color: #1890ff;
  }
  
  .nav-item.router-link-active::after {
    width: 100%;
    background-color: #1890ff;
  }
  
  /* 移除默认边框和阴影 */
  .nav-item:focus,
  .nav-item:active {
    border: none !important;
    box-shadow: none !important;
  }
  
  /* 移动端按钮样式调整 */
  @media (max-width: 768px) {
    .nav-item {
      padding: 6px 12px !important;
      font-size: 15px !important;
      font-weight: 500 !important;
    }
  }

  /* 底部区域样式 */
  .el-footer {
    display: flex;
    align-items: center; // 垂直居中
    justify-content: center; // 水平居中
    font-size: 14px; // 字体大小
    color: #666; // 字体颜色
  }
}
</style>