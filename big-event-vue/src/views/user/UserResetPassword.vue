<!--
  用户密码修改组件：
  实现用户密码的修改和重置功能
  使用TypeScript编写，包含强类型检查和表单验证
-->

<script lang="ts" setup>
/* 
  导入Vue和相关库：
  ref - 创建响应式引用
  ElMessage - Element Plus消息提示组件
  FormInstance - Element Plus表单实例类型
  useRouter - Vue路由实例
*/
import { ref, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import useUserInfoStore from '@/stores/userInfo.js'
import { redirectToLogin } from '@/router/index.js'
import request from '@/utils/request.js'
import { userUpdatePasswordService } from '@/api/user.js'

/* 
  状态管理初始化：
  router - Vue路由实例
  userInfoStore - 用户信息Pinia store
  formRef - Element Plus表单引用，带类型声明
*/
const router = useRouter()
const route = useRoute()
const userInfoStore = useUserInfoStore()
const formRef = ref<FormInstance>()  // 带类型声明的表单引用
const formKey = ref(0)  // 用于强制重新渲染表单

/* 
  密码表单模型：
  包含三个响应式字段：
  oldPwd - 原密码
  newPwd - 新密码
  rePwd - 确认密码
*/
// 添加验证码倒计时状态
const countdown = ref(0)
let timer: number | null = null

// 密码可见性控制变量
const showNewPwd = ref(false)
const showRePwd = ref(false)

// 切换新密码可见性
const toggleNewPwdVisibility = () => {
  showNewPwd.value = !showNewPwd.value
}

// 切换确认密码可见性
const toggleRePwdVisibility = () => {
  showRePwd.value = !showRePwd.value
}

const pwdModel = ref({
  newPwd: '',
  rePwd: '',
  email: '',
  code: ''
})

/*
  自定义确认密码验证函数：
  确保"确认密码"与"新密码"一致
  @param {any} rule - 验证规则对象
  @param {string} value - 用户输入值
  @param {Function} callback - 验证回调函数
*/
const validateRePwd = (rule: any, value: string, callback: any) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))  // 空值错误
  } else if (value !== pwdModel.value.newPwd) {
    callback(new Error("确认密码与新密码不匹配"))  // 密码不一致错误
  } else {
    callback()  // 验证通过
  }
}

/*
  发送验证码方法
  1. 验证邮箱格式
  2. 调用发送验证码API
  3. 实现倒计时功能
*/
const sendCode = async () => {
  // 验证邮箱是否有效
  if (!pwdModel.value.email) {
    ElMessage.warning('请先输入邮箱地址')
    return
  }
  
  // 邮箱格式正则验证
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(pwdModel.value.email)) {
    ElMessage.warning('请输入有效的邮箱地址')
    return
  }
  
  // 记录参数值用于调试
  console.log('Sending verification code with:', { email: pwdModel.value.email, type: 'forget' })
  
  try {
        // 使用完整的API路径格式
        const result = await request.post('/api/email/send-code', null, {
          params: {
            email: pwdModel.value.email,
            type: 'forget'
          }
        })
    
    // 由于request拦截器已经提取了data，直接判断success
    if (result && result.success) {
      ElMessage.success('验证码已发送，请查收邮件')
      // 开始倒计时
      startCountdown()
    } else {
      // 确保传递给ElMessage.error的是字符串类型
      const errorMessage = typeof result === 'string' 
        ? result 
        : (result && typeof result.message === 'string' 
          ? result.message 
          : '发送验证码失败')
      ElMessage.error(errorMessage)
    }
  } catch (error) {
    console.error('发送验证码失败:', error)
    ElMessage.error('发送验证码失败，请稍后重试')
  }
}

/*
  倒计时功能实现
  防止用户频繁点击发送验证码
*/
const startCountdown = () => {
  countdown.value = 60  // 设置倒计时为60秒
  
  // 清除之前的定时器
  if (timer) {
    clearInterval(timer)
  }
  
  timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      clearInterval(timer)
      timer = null
    }
  }, 1000)
}

/* 
  表单验证规则对象：
  包含三组字段规则（oldPwd, newPwd, rePwd）
  结合了必填、格式和自定义验证
*/
const rules = {
  // 邮箱验证规则
  email: [
    {
      required: true,
      message: '请输入邮箱地址',
      trigger: 'blur'
    },
    {
      pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      message: '请输入有效的邮箱地址',
      trigger: 'blur'
    }
  ],
  // 验证码验证规则
  code: [
    {
      required: true,
      message: '请输入验证码',
      trigger: 'blur'
    },
    {
      pattern: /^\d{6}$/,
      message: '验证码应为6位数字',
      trigger: 'blur'
    }
  ],
  // 新密码验证规则
  newPwd: [
    {
      required: true,
      message: '请输入新密码',
      trigger: 'blur'
    },
    {
      pattern: /^\S{6,16}$/,  // 6-16位非空字符
      message: '密码长度必须是6-16位的非空字符串',
      trigger: 'blur'
    },

  ],
  // 确认密码验证规则
  rePwd: [
    {
      validator: validateRePwd,  // 使用自定义验证函数
      trigger: 'blur'
    },
    {
      pattern: /^\S{6,16}$/,  // 格式验证
      message: '密码长度必须是6-16位的非空字符串',
      trigger: 'blur'
    }
  ],
}

/* 
  表单重置方法：
  清空所有密码字段并重置表单验证状态
*/
const resetForm = () => {
  // 清理倒计时定时器
  if (timer) {
    clearInterval(timer)
    timer = null
    countdown.value = 0
  }
  
  // 先清空表单数据
  pwdModel.value = {
    newPwd: '',
    rePwd: '',
    email: '',
    code: ''
  }
  
  // 重置表单验证状态 - 使用Element Plus的标准方法
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

// 监听路由变化，确保每次导航到该页面时都重置表单
watch(() => route.path, (newPath) => {
  if (newPath === '/user/resetPassword') {
    resetForm()
  }
})

// 组件挂载时自动重置表单
onMounted(() => {
  resetForm()
})

// 组件卸载前清理定时器，防止内存泄漏
onBeforeUnmount(() => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
})
  
  // 定义API响应类型
  interface ApiResponse {
    success?: boolean;
    message?: string;
    code?: number;
  }
  
  // 表单提交处理：
  // 1. 执行表单验证
  // 2. 验证邮箱验证码
  // 3. 验证通过后提交密码更新请求
  const submitForm = async () => {
  // 表单实例检查
  if (!formRef.value) return

  // 执行表单验证
  const valid = await formRef.value.validate()
  if (!valid) return  // 验证失败则中止

  try {
         // 记录参数值用于调试
         console.log('Verifying code with:', { email: pwdModel.value.email, code: pwdModel.value.code, type: 'forget' })
         
         // 使用完整的API路径格式
         const verifyResult: ApiResponse = await request.post('/api/email/verify', null, {
           params: {
             email: pwdModel.value.email,
             code: pwdModel.value.code,
             type: 'forget'
           }
         })
    
    // 验证验证码是否成功
    if (verifyResult && verifyResult.success) {
      // 验证码验证通过，调用密码更新API
      let updateResult: ApiResponse = await userUpdatePasswordService({
        newPwd: pwdModel.value.newPwd
      })
      
      if (updateResult && updateResult.code === 0) {
        // 清除用户信息
        ElMessage.success('密码修改成功！即将跳转登录页...')
        userInfoStore.removeInfo()
        resetForm()  // 重置表单
        // 2秒后跳转登录页
        setTimeout(() => {
          redirectToLogin()  // 执行登录页重定向
        }, 2000)
      } else {
        // 处理密码更新业务逻辑错误
        ElMessage.error(updateResult && updateResult.message || '修改密码失败');
      }
    } else {
      // 验证码验证失败
      ElMessage.error(verifyResult && verifyResult.message || '验证码错误或已过期')
    }
  } catch (error) {
    console.error('提交表单失败:', error)
    ElMessage.error('操作失败，请稍后重试')  // 通用错误提示
  }
}
</script>

<template>
  <div class="container">
     <!-- Element Plus卡片容器 -->
  <el-card>
    <!-- 
      表单组件：
      style - 限制最大宽度
      :model - 绑定密码数据模型
      :rules - 绑定验证规则
      label-width="auto" - 自动标签宽度
      ref - 表单实例引用
      class="demo-ruleForm" - 样式类名
    -->
    <!-- 添加随机ID和额外的防自动填充措施 -->
    <el-form style="max-width: 600px" :model="pwdModel" status-icon :rules="rules" label-width="auto" ref="formRef"
      class="demo-ruleForm" autocomplete="off" :key="formKey">
      <!-- 邮箱表单项 -->
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="pwdModel.email" type="text" autocomplete="new-email" placeholder="请输入邮箱" />
      </el-form-item>
      
      <!-- 验证码表单项 -->
      <el-form-item label="验证码" prop="code">
        <div style="display: flex; gap: 10px;">
          <el-input v-model="pwdModel.code" type="text" autocomplete="one-time-code" placeholder="请输入验证码" style="flex: 1;" />
          <!-- 
            发送验证码按钮：
            type="primary" - 主按钮样式
            @click - 触发发送验证码
            :disabled - 倒计时期间禁用
          -->
          <el-button 
            type="primary" 
            @click="sendCode()"
            :disabled="countdown > 0"
          >
            {{ countdown > 0 ? `${countdown}秒后重试` : '发送验证码' }}
          </el-button>
        </div>
      </el-form-item>

      <!-- 新密码表单项 -->
      <el-form-item label="新密码" prop="newPwd">
        <el-input 
          v-model="pwdModel.newPwd" 
          :type="showNewPwd ? 'text' : 'password'" 
          autocomplete="new-password" 
          placeholder="6-16位非空字符" 
          show-password
          @click:password-toggle="toggleNewPwdVisibility"
        />
      </el-form-item>

      <!-- 确认密码表单项 -->
      <el-form-item label="确认新密码" prop="rePwd">
        <el-input 
          v-model="pwdModel.rePwd" 
          :type="showRePwd ? 'text' : 'password'" 
          autocomplete="new-password" 
          placeholder="请再次输入新密码" 
          show-password
          @click:password-toggle="toggleRePwdVisibility"
        />
      </el-form-item>

      <!-- 表单操作按钮 -->
      <el-form-item>
        <!-- 
          提交按钮：
          type="primary" - 主按钮样式
          @click - 触发表单提交
          style - 自定义左侧边距
        -->
        <el-button type="primary" @click="submitForm()" style="margin-left: 30%;">
          确认修改
        </el-button>

        <!-- 重置按钮：清除所有表单输入 -->
        <el-button @click="resetForm()">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
  </div>
 
</template>

<style lang="scss" scoped>
/* 容器样式 */
.container {
  display: flex;
  flex-direction: column; /* 子元素垂直排列 */
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  box-sizing: border-box;
  font-size: 16px;
}
</style>