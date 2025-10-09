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
import { ref } from 'vue'
import { ElMessage, type FormInstance } from 'element-plus'
import useUserInfoStore from '@/stores/userInfo.js'
import { useRouter } from 'vue-router'
import { redirectToLogin } from '@/router/index.js'
import { userUpdatePasswordService } from '@/api/user.js'

/* 
  状态管理初始化：
  router - Vue路由实例
  userInfoStore - 用户信息Pinia store
  formRef - Element Plus表单引用，带类型声明
*/
const router = useRouter()
const userInfoStore = useUserInfoStore()
const formRef = ref<FormInstance>()  // 带类型声明的表单引用

/* 
  密码表单模型：
  包含三个响应式字段：
  oldPwd - 原密码
  newPwd - 新密码
  rePwd - 确认密码
*/
const pwdModel = ref({
  oldPwd: '',
  newPwd: '',
  rePwd: '',
})

/* 
  自定义确认密码验证函数：
  确保"确认密码"与"新密码"一致
  @param {any} rule - 验证规则对象
  @param {string} value - 用户输入值
  @param {Function} callback - 验证回调函数
*/
const validateRePwd = (rule: any, value: string, callback: Function) => {
  if (value === '') {
    callback(new Error('请再次输入新密码'))  // 空值错误
  } else if (value !== pwdModel.value.newPwd) {
    callback(new Error("确认密码与新密码不匹配"))  // 密码不一致错误
  } else {
    callback()  // 验证通过
  }
}

/* 
  表单验证规则对象：
  包含三组字段规则（oldPwd, newPwd, rePwd）
  结合了必填、格式和自定义验证
*/
const rules = {
  // 原密码验证规则
  oldPwd: [
    {
      required: true,
      message: '请输入原密码',
      trigger: 'blur'
    },
    {
      pattern: /^\S{6,16}$/,  // 6-16位非空字符
      message: '密码长度必须是6-16位的非空字符串',
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
    // 自定义验证：新密码不能与原密码相同
    {
      validator: (rule: any, value: string, callback: Function) => {
        if (value === pwdModel.value.oldPwd) {
          callback(new Error('新密码不能与原密码相同'))
        } else {
          callback()
        }
      },
      trigger: 'blur'  // 触发条件：失去焦点
    }
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
  清空所有密码字段
*/
const resetForm = () => {
  pwdModel.value = {
    oldPwd: '',
    newPwd: '',
    rePwd: ''
  }
}

/* 
  表单提交处理：
  执行表单验证，验证通过后提交密码更新请求
*/
const submitForm = async () => {
  // 表单实例检查
  if (!formRef.value) return

  // 执行表单验证
  const valid = await formRef.value.validate()
  if (!valid) return  // 验证失败则中止

  try {
    // 调用密码更新API
    let result = await userUpdatePasswordService(pwdModel.value)
    if (result.code === 0) {
      // 清除用户信息
      ElMessage.success('密码修改成功！即将跳转登录页...')
      userInfoStore.removeInfo()
      resetForm()  // 重置表单
      // 2秒后跳转登录页
      setTimeout(() => {
        redirectToLogin()  // 执行登录页重定向
      }, 2000)
    } else {
      // 处理业务逻辑错误（后端返回的错误码1）
      ElMessage.error(result.message || '操作失败');
    }
  } catch {
    ElMessage.error('修改密码失败，请稍后重试')  // 通用错误提示
  }
}
</script>

<template>
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
    <el-form style="max-width: 600px" :model="pwdModel" status-icon :rules="rules" label-width="auto" ref="formRef"
      class="demo-ruleForm">
      <!-- 原密码表单项 -->
      <el-form-item label="原密码" prop="oldPwd">
        <!-- 
          密码输入框：
          v-model - 双向绑定原密码字段
          type="password" - 密码输入类型
          autocomplete="off" - 关闭自动填充
        -->
        <el-input v-model="pwdModel.oldPwd" type="password" autocomplete="off" placeholder="请输入原密码" />
      </el-form-item>

      <!-- 新密码表单项 -->
      <el-form-item label="新密码" prop="newPwd">
        <el-input v-model="pwdModel.newPwd" type="password" autocomplete="off" placeholder="6-16位非空字符" />
      </el-form-item>

      <!-- 确认密码表单项 -->
      <el-form-item label="确认新密码" prop="rePwd">
        <el-input v-model="pwdModel.rePwd" type="password" autocomplete="off" placeholder="请再次输入新密码" />
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
</template>