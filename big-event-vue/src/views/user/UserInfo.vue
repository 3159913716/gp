<!--
  用户个人资料管理组件：
  实现用户基本信息的展示、编辑和更新功能
  使用了Element Plus的表单组件和Vue的响应式系统
-->

<script setup>
// 导入Vue响应式函数、生命周期钩子和路由
import { ref, reactive, watch, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'

// 导入Pinia状态管理
import useUserInfoStore from '@/stores/userInfo.js'

// 导入更新用户信息的API服务
import { userInfoUpdateService } from '@/api/user.js'

/* 
  状态管理初始化：
  userInfoStore - 存储用户信息的Pinia store
*/
const userInfoStore = useUserInfoStore()

/* 
  表单引用与响应式数据：
  formRef - 表单实例的引用，用于调用表单方法
  formKey - 用于强制表单重新渲染，防止保留历史数据
*/
const formRef = ref(null)
const formKey = ref(0)

// 路由实例
const route = useRoute()

// 路由监听器引用
let routeUnwatch = null

/* 
  初始用户信息（深拷贝）：
  使用JSON序列化和反序列化创建原始值的完全独立副本
  防止直接引用store中的数据导致意外修改
*/
const initialUserInfo = reactive(JSON.parse(JSON.stringify(userInfoStore.info)))

/* 
  当前用户信息（深拷贝）：
  作为表单的绑定模型
*/
const userInfo = ref(JSON.parse(JSON.stringify(userInfoStore.info)))

/* 
  表单修改状态：
  用于跟踪用户是否修改了表单内容
  初始状态为false（未修改）
*/
const isFormModified = ref(false)

/* 
  深度监听用户信息变化：
  比较当前用户信息与初始用户信息的差异
  只检查昵称是否有更改
*/
watch(
  userInfo,
  () => {
    // 检查昵称是否有更改
    isFormModified.value =
      userInfo.value.nickname !== initialUserInfo.nickname
  },
  { deep: true } // 深度监听，检测对象内部属性变化
)

/* 
  表单验证规则对象：
  使用Element Plus的表单验证机制
  包含字段验证规则和触发条件
*/
const rules = {
  // 昵称验证规则
  nickname: [
    // 必填验证
    { 
      required: true, 
      message: '请输入用户昵称', // 错误提示信息
      trigger: 'blur'           // 触发时机：失去焦点
    },
    // 格式验证
    {
      pattern: /^\S{2,10}$/,    // 2-10位非空字符正则
      message: '昵称必须是2-10位的非空字符串',
      trigger: 'blur'           // 触发时机：失去焦点
    }
  ],
  // 邮箱不再需要验证规则，因为是只读字段
}

/* 
  重置表单函数：
  确保表单在每次加载时都重置为初始状态
  使用nextTick确保DOM更新后执行重置
*/
const resetForm = async () => {
  // 强制表单重新渲染
  formKey.value++
  
  // 等待DOM更新
  await nextTick()
  
  // 重置用户信息，保留用户名和邮箱，只清空昵称
  const username = userInfoStore.info?.username || ''
  const email = userInfoStore.info?.email || ''
  Object.assign(userInfo.value, { username: username, nickname: '', email: email })
  Object.assign(initialUserInfo, { username: username, nickname: '', email: email })
  
  // 重置修改状态
  isFormModified.value = false
  
  // 等待DOM更新后重置验证状态
  setTimeout(() => {
    if (formRef.value) {
      formRef.value.clearValidate()
    }
  }, 0)
}

/* 
  更新用户信息函数：
  调用API提交修改后的用户信息
  成功处理后更新Pinia store中的数据
*/
const updateUserInfo = async () => {
  // 调用Element表单验证方法
  formRef.value?.validate(async (valid) => {
    if (valid) {  // 验证通过
      try {
        // 调用API更新用户信息
        await userInfoUpdateService(userInfo.value)
        ElMessage.success('个人信息更新成功！')  // 显示成功提示
        
        /* 
          更新Pinia store中的用户信息：
          使用深拷贝更新，避免引用问题
          保持store中的数据纯净
        */
        userInfoStore.setInfo(JSON.parse(JSON.stringify(userInfo.value)))
        
        // 更新initialUserInfo以匹配更新后的状态
        // 这样后续修改时isFormModified才能正确反映变化
        Object.assign(initialUserInfo, JSON.parse(JSON.stringify(userInfo.value)))
        
        // 重置表单修改状态
        isFormModified.value = false
        
      } catch (error) {  // 错误处理
        console.error('更新用户信息失败:', error)
        ElMessage.error('更新失败，请稍后重试')  // 显示错误提示
      }
    } else {  // 表单验证失败
      ElMessage.warning('请检查表单输入是否正确')
      return false  // 阻止后续流程
    }
  })
}

// 组件挂载时重置表单并监听路由变化
onMounted(() => {
  resetForm()
  
  // 监听路由变化
  routeUnwatch = watch(
    () => route.path,
    (newPath) => {
      // 当路由路径指向当前页面时，重置表单
      if (newPath.includes('/user/info')) {
        resetForm()
      }
    }
  )
})

// 组件卸载前清理
onBeforeUnmount(() => {
  if (routeUnwatch) {
    routeUnwatch()
  }
})
</script>

<template>
  <div class="container">
    <!-- Element Plus卡片组件容器 -->
  <el-card class="page-container">
    <!-- 卡片头部插槽 -->
    <template #header>
      <div class="header">
        <span>个人资料设置</span>
      </div>
    </template>
    
    <!-- 使用栅格布局 -->
    <el-row>
      <el-col :span="12">
        <!-- 
          表单组件：
          :model - 绑定表单数据对象
          :rules - 绑定验证规则
          label-width - 标签宽度100px
          size="large" - 大尺寸表单
          ref - 表单实例引用
        -->
        <el-form 
          :model="userInfo" 
          :rules="rules" 
          label-width="100px" 
          size="large" 
          ref="formRef"
          :key="formKey"
          autocomplete="off"
        >
          <!-- 登录名称字段（只读） -->
          <el-form-item label="登录邮箱">
            <!-- 
              禁用状态输入框：
              v-model - 双向绑定用户名字段
              disabled - 禁用状态（不可编辑）
            -->
            <el-input v-model="userInfo.username" disabled></el-input>
          </el-form-item>
          
          <!-- 联系邮箱字段（只读） -->
          <el-form-item label="联系邮箱">
            <!-- 双向绑定邮箱字段，设为只读 -->
            <el-input v-model="userInfo.email" disabled></el-input>
          </el-form-item>
          
          <!-- 用户昵称字段（带验证） -->
          <el-form-item label="用户昵称" prop="nickname">
            <!-- 
              双向绑定昵称字段：
              可编辑状态
              禁用自动填充
            -->
            <el-input v-model="userInfo.nickname" autocomplete="new-nickname"></el-input>
          </el-form-item>

          <!-- 表单提交按钮 -->
          <el-form-item>
            <!-- 
              提交按钮：
              type="primary" - 主按钮样式
              @click - 点击事件触发信息更新
              :disabled - 根据表单修改状态控制是否禁用
            -->
            <el-button 
              type="primary" 
              @click="updateUserInfo" 
              :disabled="!isFormModified"
            >
              保存修改
            </el-button>
          </el-form-item>
        </el-form>
      </el-col>
    </el-row>
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