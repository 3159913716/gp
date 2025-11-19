<!--
  用户密码重置组件：
  支持三种密码重置方式：旧密码、邮箱验证码、手机验证码
  包含完整的表单验证和用户交互流程
-->

<script lang="ts" setup>
/* 
  导入Vue和相关库
*/
import { ref, onMounted, onBeforeUnmount, watch, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance } from 'element-plus'
import useUserInfoStore from '@/stores/userInfo.js'
import { redirectToLogin } from '@/router/index.js'
import request from '@/utils/request.js'
import { userUpdatePasswordService } from '@/api/user.js'
import emailAndPasswordAPI from '@/api/email.js'

/* 
  状态管理初始化
*/
const router = useRouter()
const route = useRoute()
const userInfoStore = useUserInfoStore()
const formRef = ref<FormInstance>()  // 表单引用
const loading = ref(false)  // 加载状态

// 重置方式枚举
type ResetMethod = 'oldPwd' | 'email' | 'phone'

// 当前选择的重置方式
const currentMethod = ref<ResetMethod>('oldPwd')

// 密码表单模型 - 支持三种重置方式
const pwdModel = ref({
  // 旧密码重置所需字段
  oldPwd: '',
  // 通用字段
  newPwd: '',
  rePwd: '',
  // 邮箱重置所需字段
  email: '',
  emailCode: '',
  // 手机重置所需字段
  phone: '',
  smsCode: ''
})

// 验证码倒计时状态
const emailCountdown = ref(0)


const smsCountdown = ref(0)
let emailTimer: number | null = null
let smsTimer: number | null = null

// 密码可见性控制变量
const showOldPwd = ref(false)
const showNewPwd = ref(false)
const showRePwd = ref(false)

// 切换密码可见性
const togglePasswordVisibility = (field: 'oldPwd' | 'newPwd' | 'rePwd') => {
  if (field === 'oldPwd') showOldPwd.value = !showOldPwd.value
  if (field === 'newPwd') showNewPwd.value = !showNewPwd.value
  if (field === 'rePwd') showRePwd.value = !showRePwd.value
}

// 表单切换过渡状态 - 用于管理切换动画
const transitionState = ref('entering')

// 切换重置方式 - 带动画效果
const switchMethod = (method: ResetMethod) => {
  currentMethod.value = method
  // 切换时重置当前表单的验证状态
  nextTick(() => {
    if (formRef.value) {
      formRef.value.clearValidate()
    }
  })
}

// 修改切换方法，添加过渡状态管理
const changeMethod = (method: string) => {
  transitionState.value = 'exiting';
  setTimeout(() => {
    currentMethod.value = method as ResetMethod;
    transitionState.value = 'entering';
    // 重置表单和验证状态
    formRef.value?.clearValidate();
    // 确保清除所有可能存在的错误提示
    ElMessage.closeAll();
    
    // 当切换到手机验证方式时，确保相关状态正确初始化
    if (method === 'phone') {
      // 确保短信验证码相关状态正确
      smsCountdown.value = 0;
      if (smsTimer) {
        clearInterval(smsTimer);
        smsTimer = null;
      }
    }
  }, 200); // 等待退出动画完成
};

/*
  表单验证规则 - 移除前端验证，由后端处理验证逻辑
*/
const rules = ref({})


/**
 * 验证码倒计时功能实现
 * @param type 验证码类型（'email'或'sms'）
 * 功能：
 * 1. 设置倒计时为60秒
 * 2. 清除之前可能存在的定时器（避免重复执行）
 * 3. 创建新的定时器进行倒计时
 * 4. 倒计时结束后清理资源
 * 注意：避免使用eval函数，改用直接赋值方式，提高代码安全性
 */
const startCountdown = (type: 'email' | 'sms') => {
  // 设置倒计时为60秒
  if (type === 'email') {
    emailCountdown.value = 60
    // 清除之前的定时器，避免重复
    if (emailTimer) {
      clearInterval(emailTimer)
      emailTimer = null
    }
    // 设置新的定时器
    emailTimer = setInterval(() => {
      emailCountdown.value--
      if (emailCountdown.value <= 0) {
        if (emailTimer) {
          clearInterval(emailTimer)
          emailTimer = null
        }
      }
    }, 1000)
  } else {
    smsCountdown.value = 60
    // 清除之前的定时器，避免重复
    if (smsTimer) {
      clearInterval(smsTimer)
      smsTimer = null
    }
    // 设置新的定时器
    smsTimer = setInterval(() => {
      smsCountdown.value--
      if (smsCountdown.value <= 0) {
        if (smsTimer) {
          clearInterval(smsTimer)
          smsTimer = null
        }
      }
    }, 1000)
  }
}

/**
 * 发送邮箱验证码
 * 功能：
 * 1. 验证邮箱字段不为空
 * 2. 调用发送验证码API，确保传入正确的type="reset"参数
 * 3. 处理成功/失败情况
 * 4. 立即启动倒计时，确保按钮60秒内不能再次点击
 */
const sendEmailCode = async () => {
  // 前置检查：验证邮箱字段不为空
  if (!pwdModel.value.email) {
    ElMessage.warning('请先输入邮箱地址')
    return
  }
  
  // 立即开始倒计时，确保按钮在60秒内不能再次点击
  startCountdown('email')
  
  try {
    // 调试信息：请求参数
    console.log('【接口调试】发送邮箱验证码请求参数:', {
      email: pwdModel.value.email,
      type: 'forget'
    })
    
    // 使用专门的emailAndPasswordAPI发送验证码，确保参数格式正确
    // 将类型改为'forget'，因为后端只接受'register'或'forget'类型
    const result = await emailAndPasswordAPI.sendCode(pwdModel.value.email, 'forget')
    
    // 调试信息：响应结果
    console.log('【接口调试】发送邮箱验证码响应结果:', result)
    
    // 处理响应结果
    if (result && result.success) {
      ElMessage.success('验证码已发送，请查收邮件')
    } else {
      ElMessage.warning('验证码发送可能失败，请检查邮箱')
    }
  } catch (error) {
    // 检查是否为超时错误，如果是超时错误则不打印错误日志
    if (error && error.code === 'ECONNABORTED') {
      // 超时错误，不打印错误日志，仅显示用户友好的提示
      ElMessage.info('验证码正在发送中，请稍等片刻后检查邮箱')
    } else {
      // 非超时错误，仍然显示用户友好的错误提示
      ElMessage.error('发送验证码失败，请稍后重试')
    }
  }
}

/**
 * 发送短信验证码
 * 特别注意：根据系统要求，必须在请求参数中设置type字段的值为"reset"
 * 功能：
 * 1. 验证手机号不为空
 * 2. 验证手机号格式是否正确
 * 3. 调用发送验证码API，确保传入type="reset"参数
 * 4. 处理成功/失败情况
 * 5. 立即启动倒计时，确保按钮60秒内不能再次点击
 */
const sendSmsCode = async () => {
  // 前置检查：验证手机号不为空
  if (!pwdModel.value.phone) {
    ElMessage.warning('请先输入手机号')
    return
  }
  
  // 验证手机号格式是否符合中国大陆手机号标准
  if (!/^1[3-9]\d{9}$/.test(pwdModel.value.phone)) {
    ElMessage.warning('请输入有效的手机号码')
    return
  }
  
  // 立即开始倒计时，确保按钮在60秒内不能再次点击
  startCountdown('sms')
  
  try {
    // 调试信息：请求参数
    console.log('【接口调试】发送短信验证码请求参数:', {
      targets: pwdModel.value.phone,
      type: 'reset'
    })
    
    // 发送验证码请求，特别注意：使用targets参数名和reset类型
    // 增加超时设置到30秒，减少超时错误
    const result = await request.post('/api/sms/send-code', null, {
      params: {
        targets: pwdModel.value.phone,
        type: 'reset'  // 特别注意：重置密码功能必须使用reset类型
      },
      timeout: 30000 // 增加超时时间到30秒
    })
    
    // 调试信息：响应结果
    console.log('【接口调试】发送短信验证码响应结果:', result)
    
    // 处理响应结果 - 支持code=1表示成功的API响应格式
    if (result && result.code === 1) {
      ElMessage.success('验证码已发送，请查收短信')
    } else {
      ElMessage.warning('验证码发送可能失败，请检查短信')
    }
  } catch (error) {
    // 检查是否为超时错误，如果是超时错误则不打印错误日志
    if (error && error.code === 'ECONNABORTED') {
      // 超时错误，不打印错误日志，仅显示用户友好的提示
      ElMessage.info('验证码正在发送中，请稍等片刻后检查短信')
    } else {
      // 非超时错误，仍然显示用户友好的错误提示
      ElMessage.error('发送验证码失败，请稍后重试')
    }
  }
}

/* 
  表单重置方法
*/
const resetForm = () => {
  // 清理所有定时器
  if (emailTimer) {
    clearInterval(emailTimer)
    emailTimer = null
    emailCountdown.value = 0
  }
  if (smsTimer) {
    clearInterval(smsTimer)
    smsTimer = null
    smsCountdown.value = 0
  }
  
  // 清空表单数据
  pwdModel.value = {
    oldPwd: '',
    newPwd: '',
    rePwd: '',
    email: '',
    emailCode: '',
    phone: '',
    smsCode: ''
  }
  
  // 重置表单验证状态
  if (formRef.value) {
    formRef.value.resetFields()
  }
}

/**
 * 提交密码重置表单
 * 功能：
 * 1. 根据当前选择的重置方式，调用对应的API完成密码重置
 * 2. 支持三种重置方式：旧密码、邮箱验证、手机验证
 * 3. 统一处理成功/失败响应，提供友好的用户反馈
 * 4. 成功后清除用户信息并跳转到登录页
 */
const submitForm = async () => {
  // 表单实例检查 - 防御性编程
  if (!formRef.value) return

  // 移除前端验证，直接提交给后端处理
  
  // 设置加载状态，防止重复提交
  loading.value = true

  try {
    let success = false
    let message = ''

    // 根据不同的重置方式调用不同的接口
    if (currentMethod.value === 'oldPwd') {
      // 调试信息：使用旧密码重置
      console.log('【接口调试】使用旧密码重置密码 - 请求参数:', { 
        old_pwd: '***', // 隐藏敏感信息
        new_pwd: '***', // 隐藏敏感信息
        re_pwd: '***'  // 隐藏敏感信息
      })
      
      const result = await userUpdatePasswordService({
        old_pwd: pwdModel.value.oldPwd,
        new_pwd: pwdModel.value.newPwd,
        re_pwd: pwdModel.value.rePwd
      })
      
      // 调试信息：响应结果
      console.log('【接口调试】使用旧密码重置密码 - 响应结果:', result)
      
      // 处理响应结果 - 支持code=1表示成功
      if (result && result.code === 1) {
        success = true
        message = result.msg || '密码修改成功'
      } else {
        message = result?.msg || '修改密码失败，请检查旧密码是否正确'
      }
    } 
    else if (currentMethod.value === 'email') {
      // 调试信息：验证邮箱验证码
      console.log('【接口调试】验证邮箱验证码 - 请求参数:', { 
        email: pwdModel.value.email, 
        code: pwdModel.value.emailCode,
        type: 'forget'
      })
      
      // 验证码类型需要与发送时保持一致，使用'forget'
      const verifyResult = await emailAndPasswordAPI.verify(pwdModel.value.email, pwdModel.value.emailCode, 'forget')
      
      // 调试信息：邮箱验证码验证结果
      console.log('【接口调试】验证邮箱验证码 - 响应结果:', verifyResult)
      
      if (verifyResult && verifyResult.success) {
        // 验证码验证通过，更新密码
        console.log('【接口调试】邮箱验证码验证成功，开始更新密码 - 请求参数:', {
          email: pwdModel.value.email,
          new_pwd: '***', // 隐藏敏感信息
          re_pwd: '***'  // 隐藏敏感信息
        })
        
        const updateResult = await request.post('/user/resetPwdByEmail', {
          email: pwdModel.value.email,
          new_pwd: pwdModel.value.newPwd,
          re_pwd: pwdModel.value.rePwd
        })
        
        // 调试信息：密码更新结果
        console.log('【接口调试】邮箱重置密码 - 响应结果:', updateResult)
        
        // 处理响应结果 - 支持多种响应格式
        if (updateResult && (updateResult.code === 1 || updateResult.success)) {
          success = true
          message = updateResult.msg || updateResult.message || '密码重置成功'
        } else {
          message = updateResult?.msg || updateResult?.message || '重置密码失败'
        }
      } else {
        message = verifyResult?.message || '验证码错误或已过期'
      }
    }
    else if (currentMethod.value === 'phone') {
      // 调试信息：验证短信验证码
      console.log('【接口调试】验证短信验证码 - 请求参数:', { 
        target: pwdModel.value.phone, 
        code: pwdModel.value.smsCode,
        type: 'reset'
      })
      
      const verifyResult = await request.post('/api/sms/verify', null, {
        params: {
          target: pwdModel.value.phone,
          code: pwdModel.value.smsCode,
          type: 'reset'  // 特别注意：必须设置为reset
        }
      })
      
      // 调试信息：短信验证码验证结果
      console.log('【接口调试】验证短信验证码 - 响应结果:', verifyResult)
      
      if (verifyResult && (verifyResult.code === 1 || verifyResult.success)) {
        // 验证码验证通过，更新密码
        console.log('【接口调试】短信验证码验证成功，开始更新密码 - 请求参数:', {
          phone: pwdModel.value.phone,
          new_pwd: '***', // 隐藏敏感信息
          re_pwd: '***'  // 隐藏敏感信息
        })
        
        const updateResult = await request.post('/user/resetPwdByPhone', {
          phone: pwdModel.value.phone,
          new_pwd: pwdModel.value.newPwd,
          re_pwd: pwdModel.value.rePwd
        })
        
        // 调试信息：密码更新结果
        console.log('【接口调试】手机重置密码 - 响应结果:', updateResult)
        
        // 处理响应结果 - 支持多种响应格式
        if (updateResult && (updateResult.code === 1 || updateResult.success)) {
          success = true
          message = updateResult.msg || updateResult.message || '密码重置成功'
        } else {
          message = updateResult?.msg || updateResult?.message || '重置密码失败'
        }
      } else {
        message = verifyResult?.msg || verifyResult?.message || '验证码错误或已过期'
      }
    }

    // 处理结果
    if (success) {
      ElMessage.success(message)
      
      // 显示成功消息后，使用setTimeout确保消息完全显示
      setTimeout(async () => {
        try {
          // 提示用户密码已修改，需要重新登录
          await ElMessageBox.alert(
            '密码已成功修改，请重新登录',
            '操作成功',
            {
              confirmButtonText: '确定',
              type: 'success'
            }
          )
        } catch (error) {
          // 对话框关闭或取消时，不做特殊处理，但仍需执行后续操作
          console.log('【接口调试】对话框被关闭')
        } finally {
          // 无论对话框如何处理，都确保执行退出登录和页面跳转
          // 清除用户信息并跳转到登录页
          try {
            userInfoStore.removeInfo()
            resetForm()
            redirectToLogin()
          } catch (navError) {
            // 防止页面跳转失败导致的错误
            console.error('【接口调试】退出登录或页面跳转失败:', navError)
            // 降级处理：强制刷新到登录页
            window.location.href = '/login'
          }
        }
      }, 500)
    } else {
      ElMessage.error(message)
    }
  } catch (error: any) {
    // 错误处理 - 详细记录以便调试
    console.error('【接口调试】提交表单失败:', error)
    console.error('【接口调试】错误详情:', {
      message: error.message,
      response: error.response ? {
        status: error.response.status,
        data: error.response.data
      } : null
    })
    // 尝试从错误响应中获取更具体的错误信息
    let errorMsg = '操作失败，请稍后重试'
    if (error.response && error.response.data) {
      const { message, msg } = error.response.data
      errorMsg = message || msg || errorMsg
    }
    ElMessage.error(errorMsg)
  } finally {
    // 确保在任何情况下都清除加载状态
    loading.value = false
  }
}

// 监听路由变化，确保每次导航到该页面时都重置表单
watch(() => route.path, (newPath) => {
  if (newPath === '/user/resetPassword') {
    resetForm()
    // 确保清除所有可能存在的错误提示
    ElMessage.closeAll()
  }
})

// 组件挂载时自动重置表单
onMounted(() => {
  resetForm()
  // 确保清除所有可能存在的错误提示
  ElMessage.closeAll()
})

/**
 * 组件卸载前的资源清理
 * 功能：
 * 1. 清除所有定时器，防止内存泄漏
 * 2. 确保组件卸载后不再执行异步操作
 */
onBeforeUnmount(() => {
  // 清理邮箱验证码定时器
  if (emailTimer) {
    clearInterval(emailTimer)
    emailTimer = null
  }
  
  // 清理短信验证码定时器
  if (smsTimer) {
    clearInterval(smsTimer)
    smsTimer = null
  }
  
  // 重置状态，避免组件卸载后状态残留
  loading.value = false
})
</script>

<template>
  <div class="container">
    <el-card class="reset-password-card" shadow="hover">
      <template #header class="card-header">
        <h2>密码重置</h2>
        <p class="subtitle">请选择您需要的密码重置方式</p>
      </template>

      <!-- 重置方式切换选项卡 -->
      <div class="method-tabs">
        <el-radio-group v-model="currentMethod" size="large" @change="changeMethod">
          <el-radio-button label="oldPwd" class="method-tab">使用旧密码</el-radio-button>
          <el-radio-button label="email" class="method-tab">邮箱验证</el-radio-button>
          <el-radio-button label="phone" class="method-tab">手机验证</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 密码重置表单 - 整合所有字段在同一个表单内 -->
      <el-form 
          style="max-width: 600px; text-align: left;"
          :model="pwdModel" 
          status-icon 
          :rules="rules" 
          label-width="auto" 
          ref="formRef"
          class="reset-form unified-form"
          autocomplete="off"
        >
        <!-- 验证方式部分 - 根据选择显示不同的验证字段 -->
        <transition name="form-transition" mode="out-in">
          <div :key="currentMethod + transitionState">
            <!-- 使用旧密码重置 -->
            <el-form-item v-if="currentMethod === 'oldPwd'" label="旧密码" prop="oldPwd" class="form-item">
              <el-input 
                v-model="pwdModel.oldPwd" 
                :type="showOldPwd ? 'text' : 'password'" 
                placeholder="请输入当前密码" 
                show-password
                @click:password-toggle="togglePasswordVisibility('oldPwd')"
                class="form-input"
              />
            </el-form-item>

            <!-- 使用邮箱重置 -->
            <template v-if="currentMethod === 'email'">
              <!-- 邮箱表单项 -->
              <el-form-item label="邮箱地址" prop="email" class="form-item">
                <el-input 
                  v-model="pwdModel.email" 
                  type="text" 
                  placeholder="请输入您的邮箱地址" 
                  class="form-input"
                />
              </el-form-item>
              
              <!-- 邮箱验证码表单项 -->
              <el-form-item label="邮箱验证码" prop="emailCode" class="form-item">
                <div class="code-input-wrapper">
                  <el-input 
                    v-model="pwdModel.emailCode" 
                    type="text" 
                    placeholder="请输入邮箱验证码" 
                    class="form-input code-input"
                  />
                  <el-button 
                    type="primary" 
                    @click="sendEmailCode"
                    :disabled="emailCountdown > 0"
                    class="code-button"
                  >
                    {{ emailCountdown > 0 ? `${emailCountdown}秒后重试` : '获取验证码' }}
                  </el-button>
                </div>
              </el-form-item>
            </template>

            <!-- 使用手机重置 -->
            <template v-if="currentMethod === 'phone'">
              <!-- 手机号表单项 -->
              <el-form-item label="手机号码" prop="phone" class="form-item">
                <el-input 
                  v-model="pwdModel.phone" 
                  type="text" 
                  placeholder="请输入您的手机号码" 
                  class="form-input"
                />
              </el-form-item>
              
              <!-- 短信验证码表单项 -->
              <el-form-item label="短信验证码" prop="smsCode" class="form-item">
                <div class="code-input-wrapper">
                  <el-input 
                    v-model="pwdModel.smsCode" 
                    type="text" 
                    placeholder="请输入短信验证码" 
                    class="form-input code-input"
                  />
                  <el-button 
                    type="primary" 
                    @click="sendSmsCode"
                    :disabled="smsCountdown > 0"
                    class="code-button"
                  >
                    {{ smsCountdown > 0 ? `${smsCountdown}秒后重试` : '获取验证码' }}
                  </el-button>
                </div>
              </el-form-item>
            </template>
          </div>
        </transition>

        <!-- 分隔线，增强视觉区隔 -->
        <div class="form-divider">
          <span class="divider-text">设置新密码</span>
        </div>

        <!-- 通用密码设置部分 - 始终显示 -->
        <!-- 新密码表单项 -->
        <el-form-item label="新密码" prop="newPwd" class="form-item">
          <el-input 
            v-model="pwdModel.newPwd" 
            :type="showNewPwd ? 'text' : 'password'" 
            placeholder="请输入新密码（6-16位，包含字母和数字）" 
            show-password
            @click:password-toggle="togglePasswordVisibility('newPwd')"
            class="form-input"
          />
          <div class="password-tip">请确认新密码不能为空，限制长度6-16位</div>
        </el-form-item>

        <!-- 确认密码表单项 -->
        <el-form-item label="确认新密码" prop="rePwd" class="form-item">
          <el-input 
            v-model="pwdModel.rePwd" 
            :type="showRePwd ? 'text' : 'password'" 
            placeholder="请再次输入新密码" 
            show-password
            @click:password-toggle="togglePasswordVisibility('rePwd')"
            class="form-input"
          />
        </el-form-item>

        <!-- 表单操作按钮 -->
        <el-form-item class="form-actions">
          <el-button 
            type="primary" 
            @click="submitForm" 
            :loading="loading"
            class="submit-button"
          >
            确认重置
          </el-button>
          <el-button @click="resetForm" class="reset-button">重置表单</el-button>
        </el-form-item>
      </el-form>
      </el-card>
  </div>
</template>

<style lang="scss" scoped>
/* 容器样式 */
.container {
  display: flex;
  align-items: flex-start;
  width: 100%;
  padding: 20px 35px;
  margin: 0 auto;
  box-sizing: border-box;
  background-color: #f5f7fa;

  @media (max-width: 768px) {
    padding: 20px 15px;
  }
}

/* 卡片样式 */
.reset-password-card {
  width: 100%;
  border-radius: 6px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  background-color: #fff;
  overflow: hidden;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 6px 24px rgba(0, 0, 0, 0.12);
  }
}

/* 卡片头部样式 */
.card-header {
  text-align: left; /* 修改为靠左对齐 */
  padding-bottom: 10px;
  padding-left: 20px; /* 添加左侧内边距 */

  h2 {
    margin: 0 0 8px 0;
    font-size: 24px;
    font-weight: 600;
    color: #303133;
  }

  .subtitle {
    margin: 0;
    font-size: 14px;
    color: #909399;
  }
}

/* 切换方式选项卡样式 */
.method-tabs {
  margin: 0 20px 30px;
  border-bottom: 1px solid #ebeef5;
  padding-bottom: 10px;

  .method-tab {
    font-size: 16px;
    padding: 8px 24px;
    transition: all 0.3s ease;

    &:first-child {
      border-left: 1px solid #ebeef5;
    }

    &.is-active {
      color: #409eff;
      font-weight: 500;
    }
  }
}

/* 表单样式 */
.reset-form {
  animation: fadeIn 0.3s ease;
  text-align: left; /* 确保表单内容靠左 */
  margin: 0 20px 30px;
}

.form-item {
  margin-bottom: 24px;
  transition: all 0.3s ease;
  text-align: left; /* 确保表单项靠左 */

  &.is-error {
    .el-form-item__error {
      font-size: 12px;
      color: #f56c6c;
    }
  }
}

.form-input {
  width: 100%;
  transition: all 0.3s ease;

  &:focus {
    border-color: #409eff;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
  }
}

/* 验证码输入框容器 */
.code-input-wrapper {
  display: flex;
  gap: 12px;
  

  .code-input {
    flex: 1;
  }

  .code-button {
    min-width: 120px;
    transition: all 0.3s ease;

    &:disabled {
      opacity: 0.6;
      cursor: not-allowed;
    }
  }
}

/* 密码提示样式 */
.password-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
}

/* 表单分隔线 */
.form-divider {
  display: flex;
  align-items: flex-start;
  margin: 20px 0;
  height: 1px;
  background-color: #ebeef5;
  position: relative;
  
  .divider-text {
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
    padding: 0 15px;
    background-color: #ffffff;
    color: #606266;
    font-size: 14px;
    font-weight: 500;
  }
}

/* 表单操作按钮区域 */
.form-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin: 20px;
  margin-top: 32px;
  padding-bottom: 20px;

  .submit-button {
    min-width: 120px;
    font-size: 16px;
    transition: all 0.3s ease;
  }

  .reset-button {
    min-width: 100px;
    transition: all 0.3s ease;
  }
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .method-tabs {
    margin: 0 15px 20px;

    .method-tab {
      font-size: 14px;
      padding: 6px 16px;
    }
  }

  .reset-form {
    margin: 0 15px;
  }

  .form-item {
    margin-bottom: 20px;
  }

  .code-input-wrapper {
    flex-direction: column;
    align-items: stretch;

    .code-button {
      width: 100%;
      margin-top: 8px;
    }
  }

  .form-actions {
    flex-direction: column;
    align-items: stretch;
    margin-top: 24px;

    .submit-button,
    .reset-button {
      width: 100%;
    }
  }
}

/* 加载状态动画 */
.el-button--loading {
  opacity: 0.8;
}

/* 输入框聚焦状态增强 */
.el-input__wrapper:focus-within {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}



/* 增强表单切换动画 */
.form-transition-enter-active,
.form-transition-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.form-transition-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.form-transition-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* 增强选项卡切换动画 */
.method-tab {
  position: relative;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  z-index: 1;

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 50%;
    width: 0;
    height: 2px;
    background-color: #409eff;
    transition: all 0.3s ease;
    transform: translateX(-50%);
    z-index: -1;
  }

  &.is-active {
    color: #409eff;
    font-weight: 500;

    &::after {
      width: 100%;
    }
  }

  &:hover:not(.is-active) {
    color: #66b1ff;
    background-color: #ecf5ff;
  }
}

/* 增强验证码按钮动画 */
.code-button {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    background-color: rgba(255, 255, 255, 0.2);
    border-radius: 50%;
    transition: width 0.6s, height 0.6s;
    transform: translate(-50%, -50%);
  }

  &:hover::before {
    width: 300px;
    height: 300px;
  }

  &:disabled {
    opacity: 0.6;
    cursor: not-allowed;
    transform: scale(0.98);
  }
}

/* 增强输入框动画 */
.form-input {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;

  .el-input__wrapper {
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    border-radius: 4px;
  }

  &:focus-within {
    .el-input__wrapper {
      box-shadow: 0 0 0 3px rgba(64, 158, 255, 0.25);
      transform: translateY(-1px);
    }
  }
}

/* 表单验证反馈增强 */
.form-item {
  transition: all 0.3s ease;

  &.is-error {
    animation: shake 0.5s cubic-bezier(0.36, 0.07, 0.19, 0.97) both;
    .el-form-item__error {
      font-size: 12px;
      color: #f56c6c;
      animation: fadeInError 0.3s ease;
    }
  }
}

@keyframes shake {
  10%, 90% { transform: translateX(-1px); }
  20%, 80% { transform: translateX(2px); }
  30%, 50%, 70% { transform: translateX(-4px); }
  40%, 60% { transform: translateX(4px); }
}

@keyframes fadeInError {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 增强按钮动画 */
.submit-button,
.reset-button {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
  z-index: 1;

  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 0;
    background-color: rgba(0, 0, 0, 0.1);
    transition: all 0.3s ease;
    z-index: -1;
  }

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);

    &::after {
      height: 100%;
    }
  }

  &:active {
    transform: translateY(0);
  }
}

.submit-button {
  &::after {
    background-color: rgba(64, 158, 255, 0.2);
  }
}

/* 增强响应式设计 */
@media (max-width: 768px) {
  .method-tabs {
    margin: 0 15px 20px;

    .method-tab {
      font-size: 14px;
      padding: 6px 16px;
    }
  }

  .reset-form {
    margin: 0 15px;
  }

  .form-item {
    margin-bottom: 20px;
  }

  .code-input-wrapper {
    flex-direction: column;
    align-items: stretch;

    .code-button {
      width: 100%;
      margin-top: 8px;
    }
  }

  .form-actions {
    flex-direction: column;
    align-items: stretch;
    margin-top: 24px;

    .submit-button,
    .reset-button {
      width: 100%;
    }
  }
}

@media (max-width: 480px) {
  .container {
    padding: 15px 10px;
  }

  .card-header h2 {
    font-size: 20px;
  }

  .form-input {
    height: 44px;
  }

  .submit-button,
  .reset-button {
    height: 44px;
    font-size: 16px;
  }
}

/* 加载状态优化 */
.el-button--loading {
  position: relative;
  overflow: hidden;

  .el-loading-spinner {
    animation: spin 1s linear infinite;
  }

  &::before {
    content: '';
    position: absolute;
    top: -1px;
    left: -1px;
    right: -1px;
    bottom: -1px;
    background: linear-gradient(45deg, transparent 33.333%, rgba(255,255,255,0.2) 66.667%, transparent 100%);
    background-size: 150% 150%;
    animation: shimmer 1.5s infinite;
    z-index: 0;
  }
}

@keyframes shimmer {
  0% { background-position: 100% 100%; }
  100% { background-position: 0 0; }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
</style>