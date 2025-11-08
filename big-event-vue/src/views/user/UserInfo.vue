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

// 跟踪原始nickname是否为空
const isOriginalNicknameEmpty = ref(
  !userInfoStore.info.nickname || userInfoStore.info.nickname.trim() === ''
)

// 保留store中的nickname，如果存在的话
// 只有在nickname为空时才设置默认值
if (!userInfo.value.nickname || userInfo.value.nickname.trim() === '') {
  userInfo.value.nickname = userInfo.value.username || '用户' + Date.now().toString().slice(-4)
}

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
    // 自定义验证器，会在验证前自动设置默认值
    {
      validator: (rule, value, callback) => {
        // 如果昵称为空，自动设置默认值
        if (!value || value.trim() === '') {
          const defaultNickname = userInfoStore.info?.username || '用户' + Date.now().toString().slice(-4);
          userInfo.value.nickname = defaultNickname;
          // 验证通过
          callback();
        } else if (!/^\S{2,10}$/.test(value.trim())) {
          // 格式不正确
          callback(new Error('昵称必须是2-10位的非空字符串'));
        } else {
          // 验证通过
          callback();
        }
      },
      trigger: 'blur' // 触发时机：失去焦点
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
  
  // 重置用户信息，保留用户名和邮箱，并设置默认昵称
  const username = userInfoStore.info?.username || ''
  const email = userInfoStore.info?.email || ''
  // 设置默认昵称逻辑
  const defaultNickname = username || '用户' + Date.now().toString().slice(-4);
  
  // 重新计算原始nickname是否为空
  isOriginalNicknameEmpty.value = 
    !userInfoStore.info.nickname || userInfoStore.info.nickname.trim() === ''
  
  // 使用store中的实际nickname（如果存在），否则使用默认值
  const actualNickname = isOriginalNicknameEmpty.value ? defaultNickname : userInfoStore.info.nickname
  
  Object.assign(userInfo.value, { username: username, nickname: actualNickname, email: email })
  Object.assign(initialUserInfo, { username: username, nickname: actualNickname, email: email })
  
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
  // 如果原始昵称不为空，才进行验证和更新
  if (!isOriginalNicknameEmpty.value) {
    // 在验证前确保昵称不为空
    if (!userInfo.value.nickname || userInfo.value.nickname.trim() === '') {
      userInfo.value.nickname = userInfoStore.info?.username || '用户' + Date.now().toString().slice(-4);
    }
    
    // 调用Element表单验证方法
    formRef.value?.validate(async (valid) => {
    if (valid) {  // 验证通过
      try {
        // 确保提交数据包含所有必要字段：nickname、email、id、username
        // 当nickname为空时，使用username作为替代值
        const nicknameValue = userInfo.value.nickname.trim()
        const finalNickname = nicknameValue !== '' ? nicknameValue.slice(0, 10) : userInfo.value.username
        const updateData = {
          nickname: finalNickname,
          email: userInfo.value.email || '',
          id: userInfo.value.id || 0,
          username: userInfo.value.username || ''
        }
        
        // 调用API更新用户信息
        await userInfoUpdateService(updateData)
        ElMessage.success('个人信息更新成功！')  // 显示成功提示
        
        /* 
          更新Pinia store中的用户信息：
          使用深拷贝更新，避免引用问题
          保持store中的数据纯净
        */
        // 合并更新后的数据，保留原有信息
        const updatedUserInfo = {
          ...userInfoStore.info,
          ...updateData
        }
        
        userInfoStore.setInfo(JSON.parse(JSON.stringify(updatedUserInfo)))
        
        // 更新isOriginalNicknameEmpty状态，确保后续操作正确
        isOriginalNicknameEmpty.value = false
        
        // 更新本地userInfo对象，确保UI显示一致
        Object.assign(userInfo.value, updateData)
        
        // 更新initialUserInfo以匹配更新后的状态
        // 这样后续修改时isFormModified才能正确反映变化
        Object.assign(initialUserInfo, JSON.parse(JSON.stringify(updatedUserInfo)))
        
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
  } else {
    // 原始昵称为空时，不允许修改
    ElMessage.info('您的昵称使用的是登录名，无法修改')
  }
}

// 组件挂载时重新初始化数据并监听路由变化
onMounted(() => {
  // 强制重新初始化所有数据，确保使用最新的store信息
  // 重新创建userInfo对象，确保使用store中的最新数据
  userInfo.value = JSON.parse(JSON.stringify(userInfoStore.info))
  
  // 使用Object.assign更新initialUserInfo对象而不是重新赋值
  Object.assign(initialUserInfo, JSON.parse(JSON.stringify(userInfoStore.info)))
  
  // 重新计算原始nickname状态
  isOriginalNicknameEmpty.value = 
    !userInfoStore.info.nickname || userInfoStore.info.nickname.trim() === ''
  
  // 只在nickname确实为空时设置默认值
  if (isOriginalNicknameEmpty.value && (!userInfo.value.nickname || userInfo.value.nickname.trim() === '')) {
    userInfo.value.nickname = userInfo.value.username || '用户' + Date.now().toString().slice(-4)
    // 更新initialUserInfo的nickname属性
    initialUserInfo.nickname = userInfo.value.nickname
  }
  
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
          <el-form-item label="用户昵称" :prop="isOriginalNicknameEmpty ? undefined : 'nickname'">
            <!-- 
              根据原始nickname是否为空显示不同内容：
              1. 原始nickname为空时，只读显示username
              2. 原始nickname不为空时，正常显示可编辑的nickname
            -->
            <template v-if="isOriginalNicknameEmpty">
              <el-input v-model="userInfo.username" disabled placeholder="使用登录名作为昵称"></el-input>
            </template>
            <template v-else>
              <el-input 
                v-model="userInfo.nickname" 
                autocomplete="new-nickname"
                maxlength="10" 
                show-word-limit
                placeholder="请输入2-10个字符的昵称"
              ></el-input>
            </template>
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