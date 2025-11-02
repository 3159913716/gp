<script setup>
// 引入Vue的响应式API ref
import { ref, onUnmounted } from 'vue'
// 引入Element Plus的图标组件用于表单输入框
import { User, Lock, Loading } from '@element-plus/icons-vue'
// 引入Element Plus的消息提示组件
import { ElMessage } from 'element-plus'
// 引入Vue Router的路由实例
import router from '@/router'
// 引入用户注册、登录和重置密码的API服务
import { userRegisterService, userLoginService, resetPasswordService } from '@/api/user.js'
// 新增：引入邮箱验证码API
import emailApi from '@/api/email.js'
// 引入用于管理token的Pinia store
import { useTokenStore } from '@/stores/token.js'

// 状态管理
const form = ref(null) // 用于引用表单DOM元素
const isLoading = ref(false) // 加载状态，控制按钮的加载动画
const isRegister = ref(false) // 标记当前是注册模式还是登录模式
const isForgotPassword = ref(false) // 标记当前是忘记密码模式
const tokenStore = useTokenStore() // 使用token存储实例

// 注册数据模型（响应式对象，用于绑定表单数据）
const registerData = ref({
  email: '', // 邮箱输入（替换原用户名）
  password: '', // 密码输入
  rePassword: '', // 重复密码输入
  code: '' // 邮箱验证码
})

// 忘记密码数据模型
const forgotPasswordData = ref({
  email: '', // 邮箱输入
  code: '', // 验证码
  newPassword: '', // 新密码
  confirmPassword: '' // 确认新密码
})

// 发送验证码相关状态
const isSending = ref(false)
const countdown = ref(0)
let timer = null

/*
 * 自定义验证函数：验证邮箱
 */
const validateEmail = (rule, value, callback) => {
  if (!value) {
    callback(new Error('邮箱不能为空'))
  } else {
    const pattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    if (!pattern.test(value)) {
      callback(new Error('邮箱格式不正确'))
    } else {
      callback()
    }
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

/*
 * 自定义验证函数：验证忘记密码时的确认密码
 */
const validateConfirmPassword = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
  } else if (value !== forgotPasswordData.value.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

/*
 * 自定义验证函数：验证验证码
 */
const validateCode = (rule, value, callback) => {
  if (!value) {
    callback(new Error('请输入验证码'))
  } else if (!/^\d{6}$/.test(value)) {
    callback(new Error('验证码为6位数字'))
  } else {
    callback()
  }
}

// 表单验证规则对象
const rules = {
  email: [{ validator: validateEmail, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  rePassword: [{ validator: validateRePassword, trigger: 'blur' }],
  code: [{ validator: validateCode, trigger: 'blur' }]
}

// 忘记密码表单验证规则
const forgotPasswordRules = {
  email: [{ validator: validateEmail, trigger: 'blur' }],
  code: [{ validator: validateCode, trigger: 'blur' }],
  newPassword: [{ validator: validatePassword, trigger: 'blur' }],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: 'blur' }]
}

/*
 * 清空表单数据
 * 功能：重置表单中所有输入字段的值
 */
const clearRegisterData = () => {
  registerData.value = {
    email: '',
    password: '',
    rePassword: '',
    code: ''
  }
}

/*
 * 清空忘记密码表单数据
 */
const clearForgotPasswordData = () => {
  forgotPasswordData.value = {
    email: '',
    code: '',
    newPassword: '',
    confirmPassword: ''
  }
}

/*
 * 发送邮箱验证码
 * @param {string} type - 验证码类型：'register' 或 'forgotPassword'
 */
const sendEmailCode = async (type = 'register') => {
  if (countdown.value > 0 || isSending.value) return
  const email = type === 'register' ? registerData.value.email : forgotPasswordData.value.email
  const pattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!pattern.test(email)) {
    ElMessage.error('请输入有效的邮箱地址')
    return
  }
  isSending.value = true
  try {
    // 转换类型参数，确保与后端接口匹配
    const apiType = type === 'forgotPassword' ? 'forget' : type
    const res = await emailApi.sendCode(email, apiType)
    const ok = (res && res.success === true) || (res && res.code === 0)
    if (ok) {
      // 移除成功提示，直接启动倒计时
      // 立即启动60秒冷却期
      countdown.value = 60
      if (timer) {
        clearInterval(timer)
        timer = null
      }
      timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
          timer = null
        }
      }, 1000)
    } else {
      ElMessage.error((res && (res.message || res.msg)) || '验证码发送失败，请稍后再试')
    }
  } catch (error) {
    ElMessage.error('验证码发送失败，请稍后再试')
    console.error('发送验证码错误:', error)
  } finally {
    isSending.value = false
  }
}

/*
 * 处理忘记密码逻辑
 */
const handleForgotPassword = async () => {
  isLoading.value = true
  
  // 调用表单验证方法
  form.value.validate(async (valid) => {
    if (!valid) {
      isLoading.value = false
      return
    }
    
    try {
      // 先验证验证码，使用正确的类型参数
      const verifyRes = await emailApi.verify(
        forgotPasswordData.value.email,
        forgotPasswordData.value.code,
        'forget'
      )
      if (!verifyRes.success) {
        ElMessage.error(verifyRes.message || '验证码错误或已过期')
        isLoading.value = false
        return
      }
      
      // 调用重置密码API
      const result = await resetPasswordService({
        email: forgotPasswordData.value.email,
        newPassword: forgotPasswordData.value.newPassword
      })
      
      if (result.code === 0) {
        ElMessage.success(result.msg || '密码重置成功，请使用新密码登录')
        isForgotPassword.value = false // 返回登录页
        clearForgotPasswordData()
      } else {
        ElMessage.error(result.msg || '密码重置失败，请重试')
      }
    } catch (error) {
      ElMessage.error('网络错误，请稍后再试')
      console.error('重置密码接口错误:', error)
    } finally {
      isLoading.value = false
    }
  })
}

/*
 * 处理注册逻辑（含邮箱验证码校验）
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
      // 先验证验证码
      const verifyRes = await emailApi.verify(
        registerData.value.email,
        registerData.value.code,
        'register'
      )
      if (!verifyRes.success) {
        ElMessage.error(verifyRes.message || '验证码错误或已过期')
        isLoading.value = false
        return
      }

      // 调用注册API服务，传入邮箱作为用户名
      const result = await userRegisterService({
        username: registerData.value.email, // 后端若仍使用username，这里用邮箱值
        email: registerData.value.email,
        password: registerData.value.password
      })
      
      // 检查返回结果状态码
      if (result.code === 0) {
        ElMessage.success(result.msg || '注册成功')
        isRegister.value = false // 切换到登录界面
        clearRegisterData()
      } else {
        ElMessage.error(result.msg || '注册失败，请重试')
      }
    } catch (error) {
      ElMessage.error('网络错误，请稍后再试')
      console.error('注册接口错误:', error)
    } finally {
      isLoading.value = false
    }
  })
}

/*
 * 处理登录逻辑
 */
const login = async () => {
  isLoading.value = true // 开始加载，显示加载动画
  try {
    // 使用邮箱作为用户名登录
    const result = await userLoginService({
      username: registerData.value.email,
      password: registerData.value.password
    })
    
    if (result.code === 0) {
      ElMessage.success(result.msg || '登录成功')
      tokenStore.setToken(result.data)
      router.push('/')
    } else {
      ElMessage.error(result.msg || '邮箱或密码错误，请重试')
    }
  } catch (error) {
    ElMessage.error('网络错误，请稍后再试')
    console.error('登录接口错误:', error)
  } finally {
    isLoading.value = false
  }
}

// 页面卸载时清理倒计时定时器
onUnmounted(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
</script>

<template>
  <!-- 登录页面布局 - 使用Element Plus的栅格系统 -->
  <el-row class="login-page">
    <!-- 左侧背景区域 -->
    <el-col :span="12" class="bg"></el-col>
    
    <!-- 右侧表单区域，偏移3列，占据6列 -->
    <el-col :span="6" :offset="3" class="form">
      <!-- 注册表单部分 -->
      <el-form 
        ref="form" 
        v-if="isRegister" 
        size="large" 
        autocomplete="off" 
        :model="registerData" 
        :rules="rules"
      >
        <el-form-item>
          <h1>注册</h1>
        </el-form-item>
        
        <!-- 邮箱输入框（替换原用户名） -->
        <el-form-item prop="email">
          <el-input 
            :prefix-icon="User" 
            placeholder="请输入邮箱" 
            v-model="registerData.email" 
          />
        </el-form-item>
        
        <!-- 验证码输入框 + 获取按钮 -->
        <el-form-item prop="code">
          <div style="display:flex;gap:8px;width:100%">
            <el-input 
              placeholder="请输入6位验证码" 
              v-model="registerData.code" 
              maxlength="6"
            />
            <el-button 
              type="primary" 
              @click="sendEmailCode('register')" 
              :disabled="countdown>0 || isSending"
              :icon="isSending ? Loading : null"
            >
              {{ countdown>0 ? `重新获取(${countdown}s)` : '获取验证码' }}
            </el-button>
          </div>
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
          <el-link type="info" @click="isRegister = false; clearRegisterData()">
            ← 返回
          </el-link>
        </el-form-item>
      </el-form>

      <!-- 忘记密码表单部分 -->
      <el-form 
        v-else-if="isForgotPassword"
        ref="form" 
        size="large" 
        autocomplete="off" 
        :model="forgotPasswordData" 
        :rules="forgotPasswordRules"
      >
        <el-form-item>
          <h1>忘记密码</h1>
        </el-form-item>
        
        <!-- 邮箱输入框 -->
        <el-form-item prop="email">
          <el-input 
            :prefix-icon="User" 
            placeholder="请输入邮箱" 
            v-model="forgotPasswordData.email" 
          />
        </el-form-item>
        
        <!-- 验证码输入框 + 获取按钮 -->
        <el-form-item prop="code">
          <div style="display:flex;gap:8px;width:100%">
            <el-input 
              placeholder="请输入6位验证码" 
              v-model="forgotPasswordData.code" 
              maxlength="6"
            />
            <el-button 
              type="primary" 
              @click="sendEmailCode('forgotPassword')" 
              :disabled="countdown>0 || isSending"
              :icon="isSending ? Loading : null"
            >
              {{ countdown>0 ? `重新获取(${countdown}s)` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        
        <!-- 新密码输入框 -->
        <el-form-item prop="newPassword">
          <el-input 
            :prefix-icon="Lock" 
            type="password" 
            placeholder="请输入新密码" 
            v-model="forgotPasswordData.newPassword" 
          />
        </el-form-item>
        
        <!-- 确认新密码输入框 -->
        <el-form-item prop="confirmPassword">
          <el-input 
            :prefix-icon="Lock" 
            type="password" 
            placeholder="请再次输入新密码" 
            v-model="forgotPasswordData.confirmPassword" 
          />
        </el-form-item>
        
        <!-- 重置密码按钮 -->
        <el-form-item>
          <el-button 
            class="button" 
            type="primary" 
            auto-insert-space 
            @click="handleForgotPassword" 
            :loading="isLoading"
            :icon="isLoading ? Loading : null"
          >
            重置密码
          </el-button>
        </el-form-item>
        
        <!-- 返回登录链接 -->
        <el-form-item class="flex">
          <el-link type="info" @click="isForgotPassword = false; clearForgotPasswordData()">
            ← 返回登录
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
          <h1>登录</h1>
        </el-form-item>
        
        <!-- 邮箱输入框（替换原用户名） -->
        <el-form-item prop="email">
          <el-input 
            :prefix-icon="User" 
            placeholder="请输入邮箱" 
            v-model="registerData.email" 
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
            <el-checkbox>记住我</el-checkbox>
            <div class="right-links">
              <el-link type="primary" @click="isForgotPassword = true">忘记密码？</el-link>
            </div>
          </div>
        </el-form-item>
        
        <!-- 登录按钮 -->
        <el-form-item>
          <el-button 
            class="button" 
            type="primary" 
            auto-insert-space 
            @click="login"
          >
            登录
          </el-button>
        </el-form-item>
        
        <!-- 注册 / 返回首页 -->
        <el-form-item class="form-footer">
          <el-link 
            type="info" 
            @click="isRegister = true; clearRegisterData()"
          >
            注册 →
          </el-link>
          <el-link 
            type="info" 
            @click="router.push('/')"
          >
            返回首页
          </el-link>
        </el-form-item>
      </el-form>
    </el-col>
  </el-row>
</template>

<style lang="scss" scoped>
/* 保持原样式 */
.login-page {
  height: 100vh;
  background-color: #fff;

  .bg {
    background: 
      url('@/assets/logo2.png') no-repeat 60% center / 240px auto,
      url('@/assets/login_bg.jpg') no-repeat center / cover;
    border-radius: 0 20px 20px 0;
  }

  .form {
    display: flex;
    flex-direction: column;
    justify-content: center;
    user-select: none;

    .title {
      margin: 0 auto;
    }
    
    .button {
      width: 100%;
    }
    
    .flex {
      width: 100%;
      display: flex;
      justify-content: space-between;
    }

    .right-links {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    /* 登录页底部链接：左侧注册，右侧返回首页 */
    .form-footer :deep(.el-form-item__content) {
      width: 100%;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }

    .form-footer :deep(.el-link) {
      font-size: 14px;
      color: #909399;
    }

    .form-footer :deep(.el-link:hover) {
      color: #409eff;
    }
  }
}
</style>