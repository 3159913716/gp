<script setup>
// 引入Vue的响应式API ref
import { ref } from 'vue'
// 引入Element Plus的图标组件用于表单输入框
import { User, Lock, Loading } from '@element-plus/icons-vue'
// 引入Element Plus的消息提示组件
import { ElMessage } from 'element-plus'
// 引入Vue Router的路由实例
import router from '@/router'
// 引入用户注册和登录的API服务
import { userRegisterService, userLoginService } from '@/api/user.js'
// 引入用于管理token的Pinia store
import { useTokenStore } from '@/stores/token.js'

// 状态管理
const form = ref(null) // 用于引用表单DOM元素
const isLoading = ref(false) // 加载状态，控制按钮的加载动画
const isRegister = ref(false) // 标记当前是注册模式还是登录模式
const tokenStore = useTokenStore() // 使用token存储实例

// 注册数据模型（响应式对象，用于绑定表单数据）
const registerData = ref({
  username: '', // 用户名输入
  password: '', // 密码输入
  rePassword: '', // 重复密码输入
})

/*
 * 自定义验证函数：验证用户名
 * @param rule - 验证规则对象
 * @param value - 待验证的值（用户名）
 * @param callback - 验证完成后的回调函数
 * 规则要求：
 *   - 不能为空
 *   - 长度必须在5-16个字符之间
 */
const validateUsername = (rule, value, callback) => {
  if (!value) {
    // 用户名为空时调用回调返回错误
    callback(new Error('用户名不能为空'))
  } else if (value.length < 5 || value.length > 16) {
    // 用户名长度不符合要求
    callback(new Error('用户名长度必须为5-16个字符'))
  } else {
    // 验证通过
    callback()
  }
}

/*
 * 自定义验证函数：验证密码
 * 规则要求：
 *   - 不能为空
 *   - 长度必须在5-16个字符之间
 */
const validatePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('密码不能为空'))
  } else if (value.length < 5 || value.length > 16) {
    callback(new Error('密码长度必须为5-16个字符'))
  } else {
    callback()
  }
}

/*
 * 自定义验证函数：验证重复密码
 * 规则要求：
 *   - 不能为空
 *   - 必须与第一次输入的密码一致
 */
const validateRePassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerData.value.password) {
    // 检查两次输入的密码是否一致
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

// 表单验证规则对象
const rules = {
  // 绑定username字段到validateUsername验证函数
  username: [{ validator: validateUsername, trigger: 'blur' }],
  // 绑定password字段到validatePassword验证函数
  password: [{ validator: validatePassword, trigger: 'blur' }],
  // 绑定rePassword字段到validateRePassword验证函数
  rePassword: [{ validator: validateRePassword, trigger: 'blur' }]
}

/*
 * 清空表单数据
 * 功能：重置表单中所有输入字段的值
 */
const clearRegisterData = () => {
  registerData.value = {
    username: '',
    password: '',
    rePassword: '',
  }
}

/*
 * 处理注册逻辑
 * 1. 设置加载状态为true，显示加载动画
 * 2. 执行表单验证
 * 3. 验证通过后调用注册API
 * 4. 根据API返回结果提示用户
 */
const handleRegister = async () => {
  isLoading.value = true // 开始加载，显示加载动画

  // 调用表单验证方法
  form.value.validate(async (valid) => {
    // 如果验证不通过
    if (!valid) {
      isLoading.value = false // 关闭加载状态
      return // 停止执行后续代码
    }

    try {
      // 调用注册API服务，传入表单数据
      const result = await userRegisterService(registerData.value)
      
      // 检查返回结果状态码
      if (result.code === 0) {
        // 注册成功，显示成功消息
        ElMessage.success(result.msg || '注册成功')
        isRegister.value = false // 切换到登录界面
      } else {
        // 注册失败，显示错误消息
        ElMessage.error(result.msg || '注册失败，请重试')
      }
    } catch (error) {
      // 捕获网络请求错误
      ElMessage.error('网络错误，请稍后再试')
      console.error('注册接口错误:', error)
    } finally {
      // 无论成功或失败，最终关闭加载状态
      isLoading.value = false
    }
  })
}

/*
 * 处理登录逻辑
 * 1. 设置加载状态为true，显示加载动画
 * 2. 调用登录API服务
 * 3. 处理API响应结果
 * 4. 登录成功后设置token并跳转页面
 */
const login = async () => {
  isLoading.value = true // 开始加载，显示加载动画
  try {
    // 调用登录API服务，传入用户名和密码
    const result = await userLoginService({
      username: registerData.value.username,
      password: registerData.value.password
    })
    
    // 检查登录结果状态码
    if (result.code === 0) {
      // 登录成功
      ElMessage.success(result.msg || '登录成功')
      // 将token存储到Pinia中
      tokenStore.setToken(result.data)
      // 跳转到首页
      router.push('/')
    } else {
      // 登录失败
      ElMessage.error(result.msg || '用户名或密码错误，请重试')
    }
  } catch (error) {
    // 捕获网络请求错误
    ElMessage.error('网络错误，请稍后再试')
    console.error('登录接口错误:', error)
  } finally {
    // 无论成功或失败，最终关闭加载状态
    isLoading.value = false
  }
}
</script>

<template>
  <!-- 登录页面布局 - 使用Element Plus的栅格系统 -->
  <el-row class="login-page">
    <!-- 左侧背景区域 -->
    <el-col :span="12" class="bg"></el-col>
    
    <!-- 右侧表单区域，偏移3列，占据6列 -->
    <el-col :span="6" :offset="3" class="form">
      <!-- 注册表单部分 -->
      <!-- 使用v-if根据isRegister状态显示/隐藏 -->
      <!-- ref绑定form引用 -->
      <!-- size设置表单元素大小为large -->
      <!-- autocomplete关闭浏览器自动填充 -->
      <!-- model绑定数据对象 -->
      <!-- rules绑定验证规则 -->
      <el-form 
        ref="form" 
        v-if="isRegister" 
        size="large" 
        autocomplete="off" 
        :model="registerData" 
        :rules="rules"
      >
        <el-form-item>
          <h1>注册</h1> <!-- 注册标题 -->
        </el-form-item>
        
        <!-- 用户名输入框 -->
        <el-form-item prop="username">
          <!-- 带用户图标的输入框 -->
          <el-input 
            :prefix-icon="User" 
            placeholder="请输入用户名" 
            v-model="registerData.username" 
          />
        </el-form-item>
        
        <!-- 密码输入框 -->
        <el-form-item prop="password">
          <!-- 带锁图标，输入类型为password -->
          <el-input 
            :prefix-icon="Lock" 
            type="password" 
            placeholder="请输入密码" 
            v-model="registerData.password" 
          />
        </el-form-item>
        
        <!-- 重复密码输入框 -->
        <el-form-item prop="rePassword">
          <el-input 
            :prefix-icon="Lock" 
            type="password" 
            placeholder="请输入再次密码" 
            v-model="registerData.rePassword" 
          />
        </el-form-item>
        
        <!-- 注册按钮 -->
        <el-form-item>
          <!-- 主要类型按钮，自动添加空格 -->
          <!-- 点击触发handleRegister方法 -->
          <!-- :loading根据isLoading状态显示加载动画 -->
          <!-- :icon动态绑定加载图标 -->
          <el-button 
            class="button" 
            type="primary" 
            auto-insert-space 
            @click="handleRegister" 
            :loading="isLoading"
            :icon="isLoading ? Loading : null"
          >
            注册
          </el-button>
        </el-form-item>
        
        <!-- 返回登录链接 -->
        <el-form-item class="flex">
          <!-- 信息类型链接，点击返回登录界面 -->
          <el-link type="info" @click="isRegister = false; clearRegisterData()">
            ← 返回
          </el-link>
        </el-form-item>
      </el-form>

      <!-- 登录表单部分（与注册表单类似） -->
      <el-form 
        v-else 
        size="large" 
        autocomplete="off" 
        :model="registerData" 
        :rules="rules"
      >
        <el-form-item>
          <h1>登录</h1> <!-- 登录标题 -->
        </el-form-item>
        
        <!-- 用户名输入框 -->
        <el-form-item prop="username">
          <el-input 
            :prefix-icon="User" 
            placeholder="请输入用户名" 
            v-model="registerData.username" 
          />
        </el-form-item>
        
        <!-- 密码输入框 -->
        <el-form-item prop="password">
          <el-input 
            :prefix-icon="Lock" 
            type="password" 
            placeholder="请输入密码" 
            v-model="registerData.password" 
          />
        </el-form-item>
        
        <!-- 记住我和忘记密码选项 -->
        <el-form-item class="flex">
          <div class="flex">
            <el-checkbox>记住我</el-checkbox> <!-- 记住我复选框 -->
            <el-link type="primary">忘记密码？</el-link> <!-- 忘记密码链接 -->
          </div>
        </el-form-item>
        
        <!-- 登录按钮 -->
        <el-form-item>
          <!-- 点击触发login方法 -->
          <el-button 
            class="button" 
            type="primary" 
            auto-insert-space 
            @click="login"
          >
            登录
          </el-button>
        </el-form-item>
        
        <!-- 注册链接 -->
        <el-form-item class="flex">
          <!-- 点击切换到注册表单 -->
          <el-link 
            type="info" 
            @click="isRegister = true; clearRegisterData()"
          >
            注册 →
          </el-link>
        </el-form-item>
      </el-form>
    </el-col>
  </el-row>
</template>

<style lang="scss" scoped>
/* 登录页面容器，高度100%视口 */
.login-page {
  height: 100vh;
  background-color: #fff;

  /* 左侧背景区域样式 */
  .bg {
    background: 
      url('@/assets/logo2.png') no-repeat 60% center / 240px auto, /* logo背景 */
      url('@/assets/login_bg.jpg') no-repeat center / cover; /* 主背景 */
    border-radius: 0 20px 20px 0; /* 右侧圆角 */
  }

  /* 右侧表单区域样式 */
  .form {
    display: flex;
    flex-direction: column; /* 垂直布局 */
    justify-content: center; /* 垂直居中 */
    user-select: none; /* 禁止文本选中 */

    /* 表单内组件样式 */
    .title {
      margin: 0 auto; /* 居中标题 */
    }
    
    .button {
      width: 100%; /* 按钮宽度100% */
    }
    
    .flex {
      width: 100%;
      display: flex;
      justify-content: space-between; /* 两端对齐 */
    }
  }
}
</style>