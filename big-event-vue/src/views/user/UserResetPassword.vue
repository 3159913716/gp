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
import { Loading } from '@element-plus/icons-vue';
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

// 调试标志 - 生产环境应设置为false
const DEBUG_MODE = import.meta.env.MODE !== 'production';

// 调试日志函数
const debugLog = (...args: any[]) => {
  if (DEBUG_MODE) {
    console.log('【密码重置页面调试】', ...args);
  }
};

// 错误日志函数
const errorLog = (...args: any[]) => {
  console.error('【密码重置页面错误】', ...args);
};

// 重置方式枚举
type ResetMethod = 'oldPwd' | 'email' | 'phone'

// 当前选择的重置方式
const currentMethod = ref<ResetMethod>('oldPwd')

// 是否处于忘记密码模式
const isForgotPasswordMode = ref(false)

// 身份验证方式 - 在忘记密码模式下使用
const authMethod = ref<'email' | 'phone'>('email')

// 旧密码模式下的验证方式
const oldPwdVerifyMethod = ref<'email' | 'phone'>('email')

// 密码表单模型 - 支持三种重置方式
const pwdModel = ref({
  // 旧密码重置所需字段
  oldPwd: '',
  // 旧密码模式下的验证码字段
  verifyCode: '',
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
// 旧密码模式下的验证码倒计时
const verifyCodeCountdown = ref(0)
let emailTimer: number | null = null
let smsTimer: number | null = null
let verifyCodeTimer: number | null = null

// 验证码发送加载状态
const emailLoading = ref(false)
const smsLoading = ref(false)
const verifyCodeLoading = ref(false)

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
const transitionState = ref<'initial' | 'entering' | 'entered' | 'exiting' | 'exited'>('initial');

// 监听过渡状态变化，确保状态正确流转
watch(transitionState, (newState) => {
  console.debug('【状态调试】过渡状态变化:', newState);
  
  // 当过渡进入完成时，标记为entered
  if (newState === 'entering') {
    nextTick(() => {
      // 确保DOM更新后设置状态
      setTimeout(() => {
        if (transitionState.value === 'entering') {
          transitionState.value = 'entered';
        }
      }, 300); // 与动画时长匹配
    });
  }
});

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
  // 防止重复切换
  if (currentMethod.value === method) return;
  
  transitionState.value = 'exiting';
  
  // 使用nextTick确保动画平滑执行
  nextTick(() => {
    setTimeout(() => {
      currentMethod.value = method as ResetMethod;
      transitionState.value = 'entering';
      // 重置表单和验证状态
      formRef.value?.clearValidate();
      // 确保清除所有可能存在的错误提示
      ElMessage.closeAll();
    }, 200); // 等待退出动画完成
  });
};

// 切换到忘记密码模式
const switchToForgotPasswordMode = () => {
  // 设置过渡状态，触发退出动画
  transitionState.value = 'exiting';
  
  // 清理定时器
  clearCountdownTimers();
  
  setTimeout(() => {
    // 切换模式
    isForgotPasswordMode.value = true;
    // 设置进入动画状态
    transitionState.value = 'entering';
    // 默认选择邮箱验证方式
    authMethod.value = 'email';
    // 重置表单和验证状态
    formRef.value?.clearValidate();
    // 确保清除所有可能存在的错误提示
    ElMessage.closeAll();
    // 显示切换成功提示
    ElMessage.info('请选择验证方式重置密码');
  }, 200);
};

// 切换回旧密码模式
const switchToOldPasswordMode = () => {
  // 设置过渡状态，触发退出动画
  transitionState.value = 'exiting';
  
  // 清理定时器
  clearCountdownTimers();
  
  setTimeout(() => {
    // 切换模式
    isForgotPasswordMode.value = false;
    // 设置进入动画状态
    transitionState.value = 'entering';
    // 重置表单和验证状态
    formRef.value?.clearValidate();
    // 确保清除所有可能存在的错误提示
    ElMessage.closeAll();
    // 显示切换成功提示
    ElMessage.info('请输入旧密码修改');
  }, 200);
};

/**
 * 切换身份验证方式
 * 功能：
 * 1. 检查是否需要切换（避免重复操作）
 * 2. 触发动画过渡效果
 * 3. 清理所有定时器和倒计时状态
 * 4. 重置表单验证状态和错误提示
 * 5. 完全清理不使用的验证方式相关字段
 * 6. 重置所有加载状态
 * 7. 提供用户反馈，确认切换完成
 */
const changeAuthMethod = (method: 'email' | 'phone') => {
  // 如果选择的方式与当前相同，则不进行切换
  if (authMethod.value === method) return;
  
  // 设置过渡状态，触发退出动画
  transitionState.value = 'exiting';
  
  // 清理定时器 - 确保清理干净所有倒计时状态
  if (emailTimer) {
    clearInterval(emailTimer);
    emailTimer = null;
    emailCountdown.value = 0;
  }
  if (smsTimer) {
    clearInterval(smsTimer);
    smsTimer = null;
    smsCountdown.value = 0;
  }
  
  setTimeout(() => {
    // 切换验证方式
    authMethod.value = method;
    // 设置进入动画状态
    transitionState.value = 'entering';
    // 重置表单和验证状态
    formRef.value?.clearValidate();
    // 确保清除所有可能存在的错误提示
    ElMessage.closeAll();
    
    // 重置对应验证方式的状态和表单字段
    if (method === 'email') {
      pwdModel.value.email = '';
      pwdModel.value.emailCode = '';
      // 确保短信相关状态完全清理
      pwdModel.value.phone = '';
      pwdModel.value.smsCode = '';
      smsLoading.value = false;
    } else if (method === 'phone') {
      pwdModel.value.phone = '';
      pwdModel.value.smsCode = '';
      // 确保邮箱相关状态完全清理
      pwdModel.value.email = '';
      pwdModel.value.emailCode = '';
      emailLoading.value = false;
    }
    
    // 显示切换成功提示
    ElMessage.info(`已切换到${method === 'email' ? '邮箱' : '手机'}验证方式`);
  }, 200);
};

/*
  表单验证规则
  功能：
  1. 提供全面的前端验证，减轻后端压力
  2. 提供即时反馈，提升用户体验
  3. 验证内容包括：必填项、格式验证、密码复杂度、密码一致性等
*/
const rules = ref({
  // 旧密码验证
  oldPwd: [
    { required: true, message: '请输入旧密码', trigger: 'blur' },
    { min: 5, max: 17, message: '密码长度应为5-17个字符', trigger: 'blur' }
  ],
  // 旧密码模式下的验证码验证
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { min: 4, max: 6, message: '验证码长度为4-6位', trigger: 'blur' },
    { pattern: /^\d+$/, message: '验证码只能包含数字', trigger: 'blur' }
  ],
  // 新密码验证 - 根据后端要求修改
  newPwd: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 5, max: 17, message: '密码长度应为5-17个字符', trigger: 'blur' },
    { 
      pattern: /^\S{5,17}$/, 
      message: '密码必须是5-17位非空字符（不能包含空格）', 
      trigger: 'blur' 
    },
    { 
      validator: (rule, value, callback) => {
        // 如果是旧密码修改模式，检查新密码是否与旧密码相同
        if (!isForgotPasswordMode.value && value === pwdModel.value.oldPwd) {
          callback(new Error('新密码不能与旧密码相同'));
        } else {
          callback();
        }
      },
      trigger: 'blur' 
    }
  ],
  // 确认密码验证
  rePwd: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { 
      validator: (rule, value, callback) => {
        if (value !== pwdModel.value.newPwd) {
          callback(new Error('两次输入的密码不一致'));
        } else {
          callback();
        }
      },
      trigger: 'blur' 
    }
  ],
  // 邮箱验证
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { 
      type: 'email', 
      message: '请输入有效的邮箱地址', 
      trigger: 'blur' 
    }
  ],
  // 邮箱验证码验证
  emailCode: [
    { required: true, message: '请输入邮箱验证码', trigger: 'blur' },
    { min: 4, max: 6, message: '验证码长度为4-6位', trigger: 'blur' },
    { pattern: /^\d+$/, message: '验证码只能包含数字', trigger: 'blur' }
  ],
  // 手机号验证
  phone: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }
  ],
  // 短信验证码验证
  smsCode: [
    { required: true, message: '请输入短信验证码', trigger: 'blur' },
    { min: 4, max: 6, message: '验证码长度为4-6位', trigger: 'blur' },
    { pattern: /^\d+$/, message: '验证码只能包含数字', trigger: 'blur' }
  ]
})


/**
 * 清理所有倒计时定时器
 * 功能：
 * 1. 清除邮箱和短信验证码的定时器
 * 2. 重置倒计时状态
 */
const clearCountdownTimers = () => {
  // 清除邮箱验证码定时器
  if (emailTimer) {
    clearInterval(emailTimer);
    emailTimer = null;
    emailCountdown.value = 0;
  }
  // 清除短信验证码定时器
  if (smsTimer) {
    clearInterval(smsTimer);
    smsTimer = null;
    smsCountdown.value = 0;
  }
  // 清除旧密码模式下的验证码定时器
  if (verifyCodeTimer) {
    clearInterval(verifyCodeTimer);
    verifyCodeTimer = null;
    verifyCodeCountdown.value = 0;
  }
};

/**
 * 发送旧密码模式下的验证码
 * 功能：
 * 1. 根据选择的验证方式（邮箱或手机）发送验证码
 * 2. 处理成功/失败情况
 * 3. 启动倒计时，确保按钮60秒内不能再次点击
 */
const sendVerifyCode = async () => {
  // 防止重复点击
  if (verifyCodeLoading.value || verifyCodeCountdown.value > 0) {
    return
  }

  // 设置加载状态
  verifyCodeLoading.value = true

  try {
    let result
    const type = oldPwdVerifyMethod.value
    
    // 获取用户信息，确保有最新的email和phone
    const userInfo = userInfoStore.info || {};
    const userEmail = userInfo.email || '';
    const userPhone = userInfo.phone || '';
    
    // 调试日志：打印用户信息
    console.log('【调试】用户信息:', userInfo);
    console.log('【调试】验证类型:', type);
    console.log('【调试】用户邮箱:', userEmail);
    console.log('【调试】用户手机号:', userPhone);
    
    // 验证必要信息是否存在
    if (type === 'email' && !userEmail) {
      ElMessage.warning('未获取到您的邮箱信息');
      verifyCodeLoading.value = false;
      return;
    } else if (type === 'phone' && !userPhone) {
      ElMessage.warning('未获取到您的手机号信息');
      verifyCodeLoading.value = false;
      return;
    }
    
    if (type === 'email') {
      // 发送邮箱验证码 - 使用URL参数格式
      console.log('【调试】准备发送邮箱验证码:', userEmail, '类型:', 'forget');
      result = await request.post('/api/email/send-code', null, {
        params: {
          email: userEmail,
          type: 'forget'
        }
      })
      console.log('【调试】邮箱验证码发送响应:', result);
    } else {
      // 发送短信验证码
      console.log('【调试】准备发送短信验证码:', userPhone, '类型:', 'reset');
      result = await request.post('/api/sms/send-code', null, {
        params: {
          targets: userPhone,
          type: 'reset'
        }
      })
      console.log('【调试】短信验证码发送响应:', result);
    }

    // 处理响应结果
    if (result && (result.success || result.code === 1)) {
      ElMessage.success('验证码已发送，请查收')
      // 发送成功后开始倒计时
      
      // 倒计时时间常量
      const countdownSeconds = 60;
      verifyCodeCountdown.value = countdownSeconds;
      
      // 安全清除之前的定时器
      if (verifyCodeTimer) {
        clearInterval(verifyCodeTimer);
        verifyCodeTimer = null;
      }
      
      // 设置精确倒计时
      verifyCodeTimer = setInterval(() => {
        verifyCodeCountdown.value--;
        
        // 倒计时结束时的清理
        if (verifyCodeCountdown.value <= 0) {
          clearInterval(verifyCodeTimer);
          verifyCodeTimer = null;
          verifyCodeCountdown.value = 0; // 确保值为0，避免显示负数
        }
      }, 1000);
    } else {
      ElMessage.warning('验证码发送失败，请稍后重试')
    }
  } catch (error) {
    // 详细的错误日志记录
    console.error('【调试】发送验证码时发生错误:', error);
    console.error('【调试】错误详情:', error.response || error);
    
    // 检查是否为超时错误
    if (error && error.code === 'ECONNABORTED') {
      ElMessage.info('验证码正在发送中，请稍等片刻后检查')
    } else {
      // 显示更具体的错误信息给用户
      const errorMsg = error.response?.data?.message || error.message || '发送验证码失败，请稍后重试';
      ElMessage.error(errorMsg);
    }
  } finally {
    // 无论成功失败，都清除加载状态
    verifyCodeLoading.value = false
  }
};

/**
 * 切换旧密码模式下的验证方式
 */
const changeOldPwdVerifyMethod = (method: 'email' | 'phone') => {
  // 如果选择的方式与当前相同，则不进行切换
  if (oldPwdVerifyMethod.value === method) return;
  
  // 切换验证方式
  oldPwdVerifyMethod.value = method;
  
  // 重置验证码字段
  pwdModel.value.verifyCode = '';
  
  // 清除倒计时
  if (verifyCodeTimer) {
    clearInterval(verifyCodeTimer);
    verifyCodeTimer = null;
    verifyCodeCountdown.value = 0;
  }
  
  // 清除验证状态
  formRef.value?.clearValidate(['verifyCode']);
};

/**
 * 验证码倒计时功能实现
 * @param type 验证码类型（'email'或'sms'）
 * 功能：
 * 1. 设置倒计时为60秒
 * 2. 清除之前可能存在的定时器（避免重复执行）
 * 3. 创建新的定时器进行倒计时
 * 4. 倒计时结束后清理资源
 * 5. 提供精确的倒计时控制
 */
const startCountdown = (type: 'email' | 'sms') => {
  // 倒计时时间常量
  const countdownSeconds = 60;
  
  // 针对不同类型设置倒计时
  if (type === 'email') {
    // 重置倒计时状态
    emailCountdown.value = countdownSeconds;
    
    // 安全清除之前的定时器
    if (emailTimer) {
      clearInterval(emailTimer);
      emailTimer = null;
    }
    
    // 设置精确倒计时
    emailTimer = setInterval(() => {
      emailCountdown.value--;
      
      // 倒计时结束时的清理
      if (emailCountdown.value <= 0) {
        clearInterval(emailTimer);
        emailTimer = null;
        emailCountdown.value = 0; // 确保值为0，避免显示负数
      }
    }, 1000);
  } else {
    // 重置倒计时状态
    smsCountdown.value = countdownSeconds;
    
    // 安全清除之前的定时器
    if (smsTimer) {
      clearInterval(smsTimer);
      smsTimer = null;
    }
    
    // 设置精确倒计时
    smsTimer = setInterval(() => {
      smsCountdown.value--;
      
      // 倒计时结束时的清理
      if (smsCountdown.value <= 0) {
        clearInterval(smsTimer);
        smsTimer = null;
        smsCountdown.value = 0; // 确保值为0，避免显示负数
      }
    }, 1000);
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
  
  // 验证邮箱格式
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(pwdModel.value.email)) {
    ElMessage.warning('请输入有效的邮箱地址')
    return
  }
  
  // 防止重复点击
  if (emailLoading.value || emailCountdown.value > 0) {
    return
  }
  
  // 设置加载状态
  emailLoading.value = true
  
  try {
    // 使用专门的emailAndPasswordAPI发送验证码，确保参数格式正确
    // 将类型改为'forget'，因为后端只接受'register'或'forget'类型
    const result = await emailAndPasswordAPI.sendCode(pwdModel.value.email, 'forget')
    
    // 调试信息：响应结果
    console.log('【接口调试】发送邮箱验证码响应结果:', result)
    
    // 处理响应结果
    if (result && result.success) {
      ElMessage.success('验证码已发送，请查收邮件')
      // 发送成功后再开始倒计时
      startCountdown('email')
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
  } finally {
    // 无论成功失败，都清除加载状态
    emailLoading.value = false
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
  
  // 防止重复点击
  if (smsLoading.value || smsCountdown.value > 0) {
    return
  }
  
  // 设置加载状态
  smsLoading.value = true
  
  try {
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
      // 发送成功后再开始倒计时
      startCountdown('sms')
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
  } finally {
    // 无论成功失败，都清除加载状态
    smsLoading.value = false
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
  if (verifyCodeTimer) {
    clearInterval(verifyCodeTimer)
    verifyCodeTimer = null
    verifyCodeCountdown.value = 0
  }
  
  // 清空表单数据
  pwdModel.value = {
    oldPwd: '',
    newPwd: '',
    rePwd: '',
    verifyCode: '',
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
 * 1. 执行表单验证
 * 2. 根据当前模式和选择的验证方式，调用对应的API完成密码重置
 * 3. 支持两种模式：旧密码修改和身份验证修改
 * 4. 身份验证模式支持邮箱验证和手机验证
 * 5. 统一处理成功/失败响应，提供友好的用户反馈
 * 6. 成功后清除用户信息并跳转到登录页
 */
const submitForm = async () => {
  // 表单实例检查 - 防御性编程
  if (!formRef.value) return

  // 执行表单验证 - 添加错误处理
  if (!formRef.value) {
    errorLog('表单引用不存在');
    ElMessage.error('表单初始化失败，请刷新页面重试');
    return;
  }
  
  formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请检查表单中的错误项');
      return false;
    }

    // 设置加载状态，防止重复提交
    loading.value = true;

    try {
      let success = false
      let message = ''

    // 根据不同的模式和验证方式调用不同的接口
    if (!isForgotPasswordMode.value) {
      // 使用旧密码修改模式
      // 验证验证码不为空
      if (!pwdModel.value.verifyCode) {
        ElMessage.warning('请输入验证码');
        loading.value = false;
        return;
      }
      
      // 获取用户信息，确保有最新的email和phone
      const userInfo = userInfoStore.info || {};
      const userEmail = userInfo.email || '';
      const userPhone = userInfo.phone || '';
      
      // 添加验证码信息到请求参数
      const result = await userUpdatePasswordService({
        old_pwd: pwdModel.value.oldPwd,
        new_pwd: pwdModel.value.newPwd,
        re_pwd: pwdModel.value.rePwd,
        type: oldPwdVerifyMethod.value === 'email' ? 'email' : 'phone',
        target: oldPwdVerifyMethod.value === 'email' ? userEmail : userPhone,
        code: pwdModel.value.verifyCode
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
    else if (isForgotPasswordMode.value) {
      // 身份验证修改模式
      if (authMethod.value === 'email') {
        // 验证码类型需要与发送时保持一致，使用'forget'
        const verifyResult = await emailAndPasswordAPI.verify(pwdModel.value.email, pwdModel.value.emailCode, 'forget')
        
        // 调试信息：邮箱验证码验证结果
        console.log('【接口调试】验证邮箱验证码 - 响应结果:', verifyResult)
        
        if (verifyResult && verifyResult.success) {
          
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
      else if (authMethod.value === 'phone') {
        
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
            message = verifyResult?.msg || verifyResult?.message || '验证码错误或已过期'
          }
        } else {
          message = verifyResult?.msg || verifyResult?.message || '验证码错误或已过期'
        }
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
      errorLog('提交表单失败:', error);
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
  })
}

// 监听路由变化，确保每次导航到该页面时都重置表单
watch(() => route.path, (newPath) => {
  if (newPath === '/user/resetPassword') {
    debugLog('路由变化，重置表单');
    resetForm();
    // 确保清除所有可能存在的错误提示
    ElMessage.closeAll();
  }
}, { immediate: true }); // 立即执行一次，确保初始状态正确

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
 * 3. 清理所有全局状态
 */
onBeforeUnmount(() => {
  debugLog('组件卸载，清理资源');
  
  // 清理所有定时器
  clearCountdownTimers();
  
  // 确保定时器完全清理
  if (emailTimer) {
    clearInterval(emailTimer);
    emailTimer = null;
  }
  
  if (smsTimer) {
    clearInterval(smsTimer);
    smsTimer = null;
  }
  
  // 重置所有状态
  loading.value = false;
  emailLoading.value = false;
  smsLoading.value = false;
  
  // 清除所有消息提示
  ElMessage.closeAll();
});

/**
 * 全局错误处理 - 捕获未处理的Promise错误
 */
onMounted(() => {
  // 组件挂载时重置表单
  resetForm();
  
  // 清除所有可能存在的错误提示
  ElMessage.closeAll();
  
  // 调试信息
  if (DEBUG_MODE) {
    debugLog('密码重置页面已加载');
  }
});

// 防止页面刷新时数据丢失提示（仅在开发环境）
if (DEBUG_MODE) {
  window.addEventListener('beforeunload', (event) => {
    if (loading.value || emailLoading.value || smsLoading.value) {
      event.preventDefault();
      event.returnValue = '正在执行操作，确定要离开吗？';
    }
  });
}
</script>

<template>
  <div class="container">
    <el-card class="reset-password-card" shadow="hover">
      <template #header class="card-header">
        <h2>修改密码</h2>
       
      </template>

          <!-- 忘记密码模式切换 -->
      <div class="mode-toggle">
        <!-- 旧密码修改模式标题和忘记密码按钮 -->
        <div v-if="!isForgotPasswordMode" class="mode-header">
        
        </div>
        <template v-else>
          <div class="auth-header">
            <el-button 
              type="primary" 
              plain 
              size="small"
              @click="switchToOldPasswordMode"
              class="back-btn"
            >
              ← 返回
            </el-button>
          
          </div>
          
          <!-- 身份验证方式选择 -->
          <div class="auth-method-tabs">
            <el-radio-group v-model="authMethod" size="medium" @change="changeAuthMethod">
              <el-radio-button label="email" class="auth-method-tab">邮箱验证</el-radio-button>
              <el-radio-button label="phone" class="auth-method-tab">手机短信验证</el-radio-button>
            </el-radio-group>
          </div>
        </template>
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
          @keyup.enter.prevent="submitForm"
        >
        <!-- 表单内容 - 根据模式显示不同的验证字段 -->
        <!-- 增强版过渡动画 - 添加更流畅的动画效果 -->
<transition name="enhanced-form-transition" mode="out-in" appear>
  <div 
    :key="`${isForgotPasswordMode}-${authMethod || currentMethod || oldPwdVerifyMethod}`"
    :class="['form-content-wrapper', { 'is-loading': loading.value }]"
  >
            <!-- 旧密码修改模式 -->
            <template v-if="!isForgotPasswordMode">
              <!-- 旧密码输入框，右侧带忘记密码按钮 -->
              <el-form-item 
                label="旧密码" 
                prop="oldPwd" 
                class="form-item" 
                label-width="80px"
              >
                <div style="display: flex; align-items: center; gap: 10px;">
                  <el-input 
                    v-model="pwdModel.oldPwd" 
                    :type="showOldPwd ? 'text' : 'password'" 
                    placeholder="请输入当前密码" 
                    show-password
                    @click:password-toggle="togglePasswordVisibility('oldPwd')"
                    class="form-input"
                    style="flex: 1;"
                  />
                  <a href="javascript:void(0)" @click="switchToForgotPasswordMode" class="forgot-password-btn">忘记密码？</a>
                </div>
              </el-form-item>
              
              <!-- 验证码验证方式选择 -->
              <el-form-item class="form-item">
                <span class="form-label">验证方式</span>
                <div class="verify-method-tabs">
                  <el-radio-group v-model="oldPwdVerifyMethod" size="medium" @change="changeOldPwdVerifyMethod">
                    <el-radio-button label="email" class="verify-method-tab">邮箱验证</el-radio-button>
                    <el-radio-button label="phone" class="verify-method-tab">手机短信验证</el-radio-button>
                  </el-radio-group>
                </div>
              </el-form-item>
              
              <!-- 验证码输入框 -->
              <el-form-item label="验证码" prop="verifyCode" class="form-item">
                <div class="code-input-wrapper">
                  <el-input 
                    v-model="pwdModel.verifyCode" 
                    type="text" 
                    placeholder="请输入验证码" 
                    class="form-input code-input"
                  />
                  <el-button 
                    type="primary" 
                    @click="sendVerifyCode"
                    :disabled="verifyCodeCountdown > 0 || verifyCodeLoading"
                    :loading="verifyCodeLoading"
                    class="code-button"
                  >
                    {{ verifyCodeCountdown > 0 ? `${verifyCodeCountdown}秒后重试` : '获取验证码' }}
                  </el-button>
                </div>
              </el-form-item>
            </template>

            <!-- 身份验证修改模式 -->
            <template v-else>
              <!-- 邮箱验证方式 -->
              <div v-if="authMethod === 'email'">
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
              :disabled="emailCountdown > 0 || !pwdModel.email || emailLoading"
              :loading="emailLoading"
              class="code-button"
              :title="emailCountdown > 0 ? `请${emailCountdown}秒后重试` : '获取邮箱验证码'"
            >
                      {{ emailCountdown > 0 ? `${emailCountdown}秒后重试` : '获取验证码' }}
                    </el-button>
                  </div>
                </el-form-item>
              </div>

              <!-- 手机验证方式 -->
              <div v-else-if="authMethod === 'phone'">
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
              :disabled="smsCountdown > 0 || !pwdModel.phone || smsLoading"
              :loading="smsLoading"
              class="code-button"
              :title="smsCountdown > 0 ? `请${smsCountdown}秒后重试` : '获取短信验证码'"
            >
                      {{ smsCountdown > 0 ? `${smsCountdown}秒后重试` : '获取验证码' }}
                    </el-button>
                  </div>
                </el-form-item>
              </div>
            </template>
          </div>
        </transition>
        
        <!-- 全局加载遮罩 - 增强用户体验 -->
        <transition name="fade">
          <div v-if="loading.value" class="global-loading-mask">
            <div class="loading-spinner">
              <el-icon size="48"><Loading /></el-icon>
              <p class="loading-text">正在处理中...</p>
            </div>
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
            :title="loading ? '正在处理中...' : '提交修改'"
            native-type="submit"
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

/* 表单验证错误样式优化 */
.el-form-item.is-error .el-input__wrapper {
  border-color: #f56c6c;
  box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.1);
}

.el-form-item.is-error .el-input__inner {
  color: #f56c6c;
}

/* 输入框焦点优化 */
.el-input__wrapper:focus-within {
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
  border-color: #409eff;
}

/* 忘记密码按钮交互优化 */
.forgot-password-btn {
  color: #409eff;
  text-decoration: none;
  transition: all 0.3s ease;
  font-size: 14px;
  padding: 4px 8px;
  border-radius: 4px;
}

.forgot-password-btn:hover {
  color: #66b1ff;
  background-color: rgba(64, 158, 255, 0.1);
  text-decoration: underline;
}

/* 提示信息样式优化 */
.el-message {
  
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 按钮加载状态优化 */
.el-button--loading {
  opacity: 0.8;
  cursor: not-allowed;
}

/* 验证状态图标动画 */
.el-input__validateIcon {
  transition: all 0.3s ease;
}

.el-form-item.is-error .el-input__validateIcon {
  animation: shake 0.3s ease-in-out;
}

@keyframes shake {
  0%, 100% {
    transform: translateX(0);
  }
  25% {
    transform: translateX(-4px);
  }
  75% {
    transform: translateX(4px);
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


}

/* 模式切换样式 */
.mode-toggle {
  margin: 0 20px 30px;
  padding-bottom: 10px;
}


.forgot-password-btn {
  color: #409eff;
  font-size: 14px;
  margin-left: auto;
  transition: color 0.3s ease;
  
  &:hover {
    color: #66b1ff;
    text-decoration: underline;
  }
}

/* 身份验证头部样式 */
.auth-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.back-btn {
  font-size: 14px;
}

/* 增强表单验证和过渡效果 */
.auth-method-tabs {
  margin-bottom: 20px;
  background-color: #f5f7fa;
  border-radius: 8px;
  padding: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  border: 1px solid #ebeef5;
  transition: all 0.3s ease;

  .auth-method-tab {
    font-size: 14px;
    padding: 8px 20px;
    transition: all 0.3s ease;

    &.is-active {
      color: #409eff;
      font-weight: 500;
    }
  }
}

/* 增强过渡动画效果 */
.form-transition-enter-active,
.form-transition-leave-active {
  transition: all 0.3s ease;
}

.form-transition-enter-from,
.form-transition-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

/* 增强版过渡动画 - 更流畅的效果 */
.enhanced-form-transition-enter-active,
.enhanced-form-transition-leave-active {
  transition: all 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: hidden;
}

.enhanced-form-transition-enter-from {
  opacity: 0;
  transform: translateX(20px);
}

.enhanced-form-transition-leave-to {
  opacity: 0;
  transform: translateX(-20px);
}

/* 表单内容容器增强样式 */
.form-content-wrapper {
  transition: all 0.3s ease;
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.form-content-wrapper.is-loading {
  opacity: 0.7;
  pointer-events: none;
}

/* 全局加载遮罩 */
.global-loading-mask {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
}

.loading-spinner {
  background-color: white;
  padding: 30px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 15px;
}

.loading-text {
  color: #606266;
  font-size: 14px;
  margin: 0;
}

/* 淡入淡出过渡 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 身份验证选项卡悬停效果 */
.auth-method-tab:hover {
  background-color: #ecf5ff;
  transition: background-color 0.3s ease;
}

/* 按钮过渡效果 */
.el-button {
  transition: all 0.3s ease;
}

/* 响应式设计增强 */
@media (max-width: 768px) {
  .reset-form {
    padding: 15px;
  }
  
  .form-content-wrapper {
    padding: 15px;
  }
  
  .auth-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}

/* 旧密码输入框包装器 */
.old-pwd-input-wrapper {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
  gap: 10px;
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

/* 表单标签样式 */
.form-label {
  display: inline-block;
  margin: 0 20px;
  font-size: 14px;
  color: #606266;
  font-weight: 500;
  vertical-align: middle;
}

.form-input {
  width: 100%;
  transition: all 0.3s ease;

  &:focus {
    border-color: #409eff;
    box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
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
    
    font-size: 16px;
    transition: all 0.3s ease;
    background-color: #409eff;
    border-color: #409eff;
    box-shadow: 0 2px 4px rgba(64, 158, 255, 0.3);
  }

  .submit-button:hover:not(:disabled) {
    background-color: #66b1ff;
    border-color: #66b1ff;
    transform: translateY(-1px);
    box-shadow: 0 4px 8px rgba(64, 158, 255, 0.4);
  }

  .reset-button {
   
    transition: all 0.3s ease;
  }

  /* 输入框交互优化 */
  .form-input {
    transition: all 0.3s ease;
  }

  /* 完整的响应式设计 */
  @media (max-width: 768px) {
    .reset-password-card {
      margin: 10px;
      padding: 15px;
    }
    
    .code-input-wrapper {
      flex-direction: column;
      gap: 10px;
    }
    
    .code-button {
      width: 100%;
      margin-left: 0;
    }
    
    .old-pwd-input-wrapper {
      flex-direction: column;
      align-items: stretch;
    }
    
    .forgot-password-btn {
      margin-top: 10px;
      text-align: right;
    }
    
    .auth-method-tabs {
      overflow-x: auto;
      padding: 5px;
    }
  }

  /* 高对比度模式支持 */
  @media (prefers-contrast: high) {
    .reset-password-card {
      border: 2px solid #000;
    }
    
    .submit-button {
      border-width: 2px;
    }
    
    .auth-method-tabs {
      border: 2px solid #000;
    }
  }

  /* 减少动画模式支持 */
  @media (prefers-reduced-motion: reduce) {
    *,
    *::before,
    *::after {
      animation-duration: 0.01ms !important;
      animation-iteration-count: 1 !important;
      transition-duration: 0.01ms !important;
    }
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