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
// 新增：引入手机验证码API
import { sendSmsCode, registerByPhone, loginByPhone } from '@/api/phoneAuthApi.js'
// 引入用于管理token的Pinia store
import { useTokenStore } from '@/stores/token.js'

// 状态管理
const form = ref(null) // 用于引用表单DOM元素
const isLoading = ref(false) // 加载状态，控制按钮的加载动画
const isRegister = ref(false) // 标记当前是注册模式还是登录模式
const isForgotPassword = ref(false) // 标记当前是找回密码模式
const isEmailRegister = ref(true) // 标记当前是邮箱注册还是手机号注册
const tokenStore = useTokenStore() // 使用token存储实例

// 注册数据模型（响应式对象，用于绑定表单数据）
const registerData = ref({
  username: '', // 用户名输入
  email: '', // 邮箱输入
  phone: '', // 手机号输入
  password: '', // 密码输入
  rePassword: '', // 重复密码输入
  code: '' // 验证码
})

// 找回密码数据模型
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
  // 如果当前不是邮箱注册模式，则不验证邮箱
  if (!isEmailRegister.value) {
    callback()
    return
  }
  
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
 * 自定义验证函数：验证手机号
 */
const validatePhone = (rule, value, callback) => {
  // 如果当前不是手机号注册模式，则不验证手机号
  if (isEmailRegister.value) {
    callback()
    return
  }
  
  if (!value) {
    callback(new Error('手机号不能为空'))
  } else {
    const pattern = /^1[3-9]\d{9}$/
    if (!pattern.test(value)) {
      callback(new Error('手机号格式不正确'))
    } else {
      callback()
    }
  }
}

/*
 * 自定义验证函数：验证用户名
 */
const validateUsername = (rule, value, callback) => {
  if (!value) {
    callback(new Error('用户名不能为空'))
  } else if (value.length < 3 || value.length > 20) {
    callback(new Error('用户名长度必须为3-20个字符'))
  } else if (!/^[a-zA-Z0-9_\u4e00-\u9fa5]+$/.test(value)) {
    callback(new Error('用户名只能包含字母、数字、下划线和中文字符'))
  } else {
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
  // 如果是手机号注册模式，不验证重复密码
  if (!isEmailRegister.value) {
    callback()
    return
  }
  
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
 * 自定义验证函数：验证找回密码时的确认密码
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
  username: [{ validator: validateUsername, trigger: 'blur' }],
  email: [{ validator: validateEmail, trigger: 'blur' }],
  phone: [{ validator: validatePhone, trigger: 'blur' }],
  password: [{ validator: validatePassword, trigger: 'blur' }],
  rePassword: [{ validator: validateRePassword, trigger: 'blur' }],
  code: [{ validator: validateCode, trigger: 'blur' }]
}

// 找回密码表单验证规则
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
    username: '',
    email: '',
    phone: '',
    password: '',
    rePassword: '',
    code: ''
  }
}

/*
 * 清空找回密码表单数据
 */
// 用于存储找回密码过程中的用户ID
const userId = ref(null)

const clearForgotPasswordData = () => {
  forgotPasswordData.value = {
    email: '',
    code: '',
    newPassword: '',
    confirmPassword: ''
  }
  userId.value = null
}

/*
 * 发送验证码
 * @param {string} type - 验证码类型：'register' 或 'forgotPassword'
 */
const sendEmailCode = async (type = 'register') => {
  if (countdown.value > 0 || isSending.value) return
  
  // 根据注册方式获取相应的联系方式
  let contactInfo = ''
  let contactType = 'email'
  let validationPattern = null
  
  if (type === 'register') {
    if (isEmailRegister.value) {
      contactInfo = registerData.value.email
      contactType = 'email'
      validationPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
    } else {
      contactInfo = registerData.value.phone
      contactType = 'phone'
      validationPattern = /^1[3-9]\d{9}$/
    }
  } else {
    // 找回密码只支持邮箱
    contactInfo = forgotPasswordData.value.email
    contactType = 'email'
    validationPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  }
  
  if (!validationPattern.test(contactInfo)) {
    ElMessage.error(`请输入有效的${contactType === 'email' ? '邮箱地址' : '手机号'}`)
    return
  }
  
  isSending.value = true
  try {
    if (type === 'forgotPassword') {
      // 找回密码流程：先查找用户，再发送验证码
      
      // 1. 根据邮箱查找用户 - 使用email.js中的findUserByEmail接口
      const findUserRes = await emailApi.findUserByEmail(contactInfo)
      
      // 优化成功状态判断
      const isFindSuccess = findUserRes.success === true || findUserRes.code === 0
      if (!isFindSuccess) {
        console.log('未找到该邮箱对应的用户:', findUserRes.message)
        isSending.value = false
        // 即使未找到用户也启动倒计时，避免频繁尝试
        countdown.value = 60
        startCountdown()
        return
      }
      
      // 2. 保存用户ID
      userId.value = findUserRes.data?.id
      
      // 3. 发送找回密码验证码 - 使用email.js中的sendForgetCode接口
      const sendCodeRes = await emailApi.sendForgetCode(userId.value, contactInfo)
      
      // 立即启动60秒冷却期，无论成功失败
      countdown.value = 60
      startCountdown()
      
      // 控制台记录结果，不显示弹窗提示
      console.log('找回密码验证码发送结果:', sendCodeRes.success ? '成功' : '失败', sendCodeRes)
    } else {
      // 注册流程：根据注册方式发送不同类型的验证码
      let res
      if (isEmailRegister.value) {
        // 邮箱注册
        res = await emailApi.sendCode(contactInfo, 'register')
      } else {
        // 手机号注册 - 使用手机验证码API
        res = await sendSmsCode(contactInfo, 'register')
      }
      
      // 增加多种成功状态判断
      const isSuccess = res && (res.success === true || res.code === 0)
      
      // 无论成功失败都启动倒计时，让用户有机会检查验证码
      countdown.value = 60
      startCountdown()
      
      // 不显示成功或失败提示，让用户自行查收验证码
      // 只在控制台记录结果以便调试
      console.log('注册验证码发送结果:', isSuccess ? '成功' : '失败', res)
    }
  } catch (error) {
      // 不显示错误提示，但仍记录日志以便调试
      console.error('发送验证码错误:', error)
      // 即使发生错误也启动倒计时，避免用户频繁点击
      if (countdown.value <= 0) {
        countdown.value = 60
        startCountdown()
      }
  } finally {
    isSending.value = false
  }
}

/*
 * 处理找回密码逻辑
 */
// 倒计时函数封装，避免重复代码
const startCountdown = () => {
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
}

const handleForgotPassword = async () => {
  isLoading.value = true
  
  // 调用表单验证方法
  form.value.validate(async (valid) => {
    if (!valid) {
      isLoading.value = false
      return
    }
    
    try {
      // 确保用户ID已存在（通过发送验证码流程获取）
      if (!userId.value) {
        console.log('请先获取验证码')
        isLoading.value = false
        return
      }
      
      // 使用email.js中的resetPassword接口重置密码
      const resetData = {
        userId: userId.value,
        email: forgotPasswordData.value.email,
        code: forgotPasswordData.value.code,
        newPwd: forgotPasswordData.value.newPassword,
        rePwd: forgotPasswordData.value.confirmPassword
      }
      
      const resetResult = await emailApi.resetPassword(resetData)
      
      // 优化密码重置成功状态判断
      const isResetSuccess = resetResult.success === true || resetResult.code === 0
      if (isResetSuccess) {
        console.log('密码重置成功，请使用新密码登录')
        isForgotPassword.value = false // 返回登录页
        clearForgotPasswordData()
      } else {
        console.log('密码重置失败:', resetResult.message)
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
 * 处理注册逻辑（含验证码校验）
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
      // 根据注册方式使用不同的验证码验证和注册接口
      if (isEmailRegister.value) {
        // 邮箱注册流程
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

        // 准备注册数据
        const registerParams = {
          username: registerData.value.username,
          password: registerData.value.password,
          email: registerData.value.email
        }

        // 调用注册API服务
        const result = await userRegisterService(registerParams)
        
        // 检查返回结果状态码
        if (result.code === 0) {
          ElMessage.success(result.msg || '注册成功')
          isRegister.value = false // 切换到登录界面
          clearRegisterData()
        } else {
          ElMessage.error(result.msg || '注册失败，请重试')
        }
      } else {
        // 手机号注册流程
        // 直接使用手机号注册接口
        const result = await registerByPhone(
          registerData.value.username,
          registerData.value.password,
          registerData.value.phone,
          registerData.value.code
        )
        
        // 检查返回结果状态码，适配不同的成功状态格式
        const isRegisterSuccess = result.success === true || result.code === 0
        if (isRegisterSuccess) {
          ElMessage.success(result.msg || result.message || '注册成功')
          isRegister.value = false // 切换到登录界面
          clearRegisterData()
        } else {
          ElMessage.error(result.msg || result.message || '注册失败，请重试')
        }
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
    // 根据注册方式使用不同的登录接口
    let result;
    
    if (isEmailRegister.value) {
        // 用户名登录 - 使用密码登录
        result = await userLoginService({
          username: registerData.value.username,
          password: registerData.value.password
        })
        
        if (result.code === 0) {
          ElMessage.success(result.msg || '登录成功')
          console.log(result.data)
          tokenStore.setToken(result.data)
          router.push('/')
        } else {
          // 不显示失败提示
          console.log('登录失败:', result.msg || '邮箱或密码错误')
        }
    } else {
        // 手机号登录 - 使用验证码登录
        result = await loginByPhone(
          registerData.value.phone,
          registerData.value.code
        )
        
        // 检查返回结果状态码，适配不同的成功状态格式
        const isLoginSuccess = result.success === true || result.code === 0
        if (isLoginSuccess) {
          // 不显示成功提示，直接设置token并跳转
          // 设置token，适配不同的返回格式
          tokenStore.setToken(result.data || result.data?.token)
          router.push('/')
        } else {
          ElMessage.error(result.msg || result.message || '手机号或验证码错误，请重试')
        }
    }
  } catch (error) {
    ElMessage.error('网络错误，请稍后再试')
    console.error('登录接口错误:', error)
  } finally {
    isLoading.value = false
  }
}

// 处理手机号登录时发送验证码
const handleSendSmsCode = async () => {
  if (countdown.value > 0 || isSending.value) return
  
  // 验证手机号格式
  const phone = registerData.value.phone
  const validationPattern = /^1[3-9]\d{9}$/
  
  if (!validationPattern.test(phone)) {
    ElMessage.error('请输入有效的手机号')
    return
  }
  
  isSending.value = true
  try {
    // 调用发送验证码API
    const res = await sendSmsCode(phone, 'login')
    
    // 无论成功失败都启动倒计时
    countdown.value = 60
    startCountdown()
    
    // 控制台记录结果
    console.log('登录验证码发送结果:', res)
  } catch (error) {
    console.error('发送验证码错误:', error)
    // 即使发生错误也启动倒计时，避免频繁点击
    if (countdown.value <= 0) {
      countdown.value = 60
      startCountdown()
    }
  } finally {
    isSending.value = false
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
        <!-- 美化后的注册方式切换按钮 -->
        <el-form-item>
          <div class="register-tabs">
            <el-button 
              :type="isEmailRegister ? 'primary' : ''" 
              :class="{ active: isEmailRegister }"
              @click="isEmailRegister = true"
              class="tab-button"
              style="border-radius: 8px 0 0 8px;"
            >
              邮箱注册
            </el-button>
            <el-button 
              :type="!isEmailRegister ? 'primary' : ''" 
              :class="{ active: !isEmailRegister }"
              @click="isEmailRegister = false"
              class="tab-button"
              style="border-radius: 0 8px 8px 0;"
            >
              手机号注册
            </el-button>
          </div>
        </el-form-item>
        
        <!-- 用户名输入框 -->
        <el-form-item prop="username">
          <el-input 
            :prefix-icon="User" 
            placeholder="请输入用户名" 
            v-model="registerData.username" 
          />
        </el-form-item>
        
        <!-- 邮箱输入框 -->
        <el-form-item v-if="isEmailRegister" prop="email">
          <el-input 
            :prefix-icon="User" 
            placeholder="请输入邮箱" 
            v-model="registerData.email" 
          />
        </el-form-item>
        
        <!-- 手机号输入框 -->
        <el-form-item v-else prop="phone">
          <el-input 
            :prefix-icon="User" 
            placeholder="请输入手机号" 
            v-model="registerData.phone" 
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
        
        <!-- 手机号注册不显示重复密码输入框 -->
        <el-form-item v-if="isEmailRegister" prop="rePassword">
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

      <!-- 找回密码表单部分 -->
      <el-form 
        v-else-if="isForgotPassword"
        ref="form" 
        size="large" 
        autocomplete="off" 
        :model="forgotPasswordData" 
        :rules="forgotPasswordRules"
      >
        <el-form-item>
          <h1>找回密码</h1>
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
        
        <!-- 登录方式切换按钮 -->
        <el-form-item>
          <div class="register-tabs">
            <el-button 
              :type="isEmailRegister ? 'primary' : ''" 
              :class="{ active: isEmailRegister }"
              @click="isEmailRegister = true"
              class="tab-button"
              style="border-radius: 8px 0 0 8px;"
            >
              用户名登录
            </el-button>
            <el-button 
              :type="!isEmailRegister ? 'primary' : ''" 
              :class="{ active: !isEmailRegister }"
              @click="isEmailRegister = false"
              class="tab-button"
              style="border-radius: 0 8px 8px 0;"
            >
              手机号登录
            </el-button>
          </div>
        </el-form-item>
        
        <!-- 用户名登录相关输入框 -->
        <template v-if="isEmailRegister">
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
              <el-checkbox>记住我</el-checkbox>
              <div class="right-links">
                <el-link type="primary" @click="isForgotPassword = true">找回密码？</el-link>
              </div>
            </div>
          </el-form-item>
        </template>
        
        <!-- 手机号登录相关输入框 -->
        <template v-else>
          <!-- 手机号输入框 -->
          <el-form-item prop="phone">
            <el-input 
              :prefix-icon="User" 
              placeholder="请输入手机号" 
              v-model="registerData.phone" 
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
                @click="handleSendSmsCode" 
                :disabled="countdown>0 || isSending"
                :icon="isSending ? Loading : null"
              >
                {{ countdown>0 ? `重新获取(${countdown}s)` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
        </template>
        
        <!-- 登录按钮 -->
        <el-form-item>
          <el-button 
            class="button" 
            type="primary" 
            auto-insert-space 
            @click="login"
            :loading="isLoading"
            :icon="isLoading ? Loading : null"
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

<style scoped>
.register-tabs {
  display: flex;
  margin-bottom: 16px;
}

.tab-button {
  flex: 1;
}

.button {
  width: 100%;
}
</style>

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
    
    /* 注册方式切换按钮样式 */
    .register-tabs {
      display: flex;
      width: 100%;
      margin-bottom: 24px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      border-radius: 8px;
      overflow: hidden;
    }
    
    .tab-button {
      flex: 1;
      padding: 12px 0;
      font-size: 16px;
      transition: all 0.3s ease;
    }
    
    .tab-button.active {
      font-weight: bold;
      transform: translateY(-2px);
    }
    
    .tab-button:hover {
      transform: translateY(-1px);
    }
  }
}
</style>