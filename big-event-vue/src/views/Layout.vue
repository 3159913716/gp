<!-- 主页面
 实现主页面的布局功能 -->

<script setup>
// 引入Vue Router的useRouter钩子用于路由导航
import { useRouter } from 'vue-router'
// 引入Element Plus的消息提示和消息框组件
import { ElMessage, ElMessageBox } from 'element-plus'
// 引入Element Plus的图标组件用于菜单和下拉菜单
import { Setting,Management, Promotion, UserFilled, User, Crop, EditPen, SwitchButton, CaretBottom } from '@element-plus/icons-vue'
// 引入默认头像图片
import avatar from '@/assets/default.png'
// 引入获取用户信息的API服务
import { userInfoService } from '@/api/user.js'
// 引入管理用户信息的Pinia store
import useUserInfoStore from '@/stores/userInfo.js'
// 引入管理token的Pinia store
import { useTokenStore } from '@/stores/token.js'

// 状态管理
const router = useRouter() // 获取路由实例
const tokenStore = useTokenStore() // 使用token存储实例
const userInfoStore = useUserInfoStore() // 使用用户信息存储实例

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
 * 处理菜单命令
 * @param {string} command - 用户选择的命令标识
 * 功能：根据用户在下拉菜单或菜单项的选择执行相应操作
 */
const handleCommand = (command) => {
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
      .then(() => {
        // 用户点击确认后的处理
        tokenStore.removeToken() // 清除token
        userInfoStore.removeInfo() // 清除用户信息
        router.push('/') // 跳转到首页
        ElMessage.success('退出成功!') // 显示成功消息
      })
      .catch(() => {
        // 用户点击取消后的处理
        ElMessage({ type: 'info', message: '取消退出' }) // 显示取消消息
      })
  } else {
    // 其他命令（个人中心相关操作）
    router.push('/user/' + command) // 导航到对应页面
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
      <!-- 
      active-text-color 激活菜单项文字颜色
      background-color菜单背景色
      text-color 菜单文字颜色
      router 启用路由模式
       -->
      <el-menu active-text-color="#ffd04b" background-color="#232323" text-color="#fff" router>

        <!-- 用户中心菜单项 -->
        <el-menu-item index="/admin/ucenter/mine">
          <el-icon>
            <UserFilled /><!-- 我的图标 -->
          </el-icon>
          <span>我的</span>
        </el-menu-item>

        <!-- 文章分类菜单项 -->
        <el-menu-item index="/admin/article/category">
          <el-icon>
            <Management /> <!-- 管理图标 -->
          </el-icon>
          <span>文章分类</span>
        </el-menu-item>

        <!-- 文章管理菜单项 -->
        <el-menu-item index="/admin/article/manage">
          <el-icon>
            <Promotion /> <!-- 推广图标 -->
          </el-icon>
          <span>文章管理</span>
        </el-menu-item>

        <!-- 个人中心子菜单 -->
        <el-sub-menu index="/admin/user-center">
          <!-- 子菜单标题 -->
          <template #title>
            <el-icon>
              <Setting /> <!-- 设置图标 -->
            </el-icon>
            <span>设置</span>
          </template>

          <!-- 基本资料菜单项 -->
          <el-menu-item index="/admin/user/info">
            <el-icon>
              <User /> <!-- 用户图标 -->
            </el-icon>
            <span>基本资料</span>
          </el-menu-item>

          <!-- 更换头像菜单项 -->
          <el-menu-item index="/admin/user/avatar">
            <el-icon>
              <Crop /> <!-- 裁剪图标 -->
            </el-icon>
            <span>更换头像</span>
          </el-menu-item>

          <!-- 重置密码菜单项 -->
          <el-menu-item index="/admin/user/resetPassword">
            <el-icon>
              <EditPen /> <!-- 编辑图标 -->
            </el-icon>
            <span>重置密码</span>
          </el-menu-item>
        </el-sub-menu>
      </el-menu>
    </el-aside>

    <!-- 右侧主内容区域 -->
    <el-container>
      <!-- 顶部头部区域 -->
      <el-header>
        <!-- 显示当前登录用户昵称 -->
        <div>用户：<strong>{{userInfoStore?.info?.username || 
          '未登录用户'  }}</strong></div>

        <!-- 导航栏区域 -->
        <div class="nav-wrapper">
          <router-link to="/" class="nav-item">首页</router-link>
          <router-link to="/category/1" class="nav-item">技术资讯</router-link>
          <router-link to="/category/2" class="nav-item">行业动态</router-link>
          <router-link to="/category/3" class="nav-item">经验分享</router-link>
          <router-link to="/category/4" class="nav-item">教程学习</router-link>
        </div>

        <!-- 用户操作下拉菜单 -->
        <!-- // 下拉菜单位置（右下）
          // 菜单项选择事件处理 -->
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
              <!-- 基本资料菜单项 -->
              <el-dropdown-item command="info" :icon="User">基本资料</el-dropdown-item>
              <!-- 更换头像菜单项 -->
              <el-dropdown-item command="avatar" :icon="Crop">更换头像</el-dropdown-item>
              <!-- 重置密码菜单项 -->
              <el-dropdown-item command="resetpassword" :icon="EditPen">重置密码</el-dropdown-item>
              <!-- 退出登录菜单项 -->
              <el-dropdown-item command="logout" :icon="SwitchButton">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <!-- 中间主内容区域 -->
      <el-main>
        <!-- 路由视图容器 -->
        <router-view></router-view>
      </el-main>

      <!-- 底部区域 -->
      <el-footer>大事件 ©2023 Created by 黑马程序员 | Rewritten ©2025 by junioriry</el-footer>
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

    /* 下拉菜单容器样式 */
    .el-dropdown__box {
      display: flex;
      align-items: center; // 垂直居中
      margin-left: 20px;

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
  }

  /* 导航栏样式 */
  .nav-wrapper {
    display: flex;
    align-items: center;
    justify-content: center; // 水平居中对齐
    gap: 30px;
    flex: 1; // 让导航栏占据剩余空间
    flex-wrap: nowrap; // 防止换行
    overflow: visible; // 允许内容完整显示
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
  
  /* 活动状态样式 */
  .nav-item.router-link-active {
    color: #1890ff;
  }
  
  .nav-item.router-link-active::after {
    width: 100%;
    background-color: #1890ff;
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