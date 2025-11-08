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

// 导入用户信息相关的API服务
import { userInfoUpdateService, userInfoService } from '@/api/user.js'

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

// 生成用户专属的localStorage键名
const getLocalStorageKey = () => {
  // 使用用户名作为唯一标识，确保不同用户的数据隔离
  const username = userInfoStore.info?.username || 'default';
  return `userNickname_${username}`;
};

// 确保nickname有值
const ensureNicknameExists = () => {
  if (!userInfo.value.nickname || userInfo.value.nickname.trim() === '') {
    // 获取用户专属的localStorage键名
    const localStorageKey = getLocalStorageKey();
    // 优先级：localStorage中的nickname > store中已有的nickname > username > 随机昵称
    const storedNickname = localStorage.getItem(localStorageKey) || '';
    userInfo.value.nickname = storedNickname || userInfoStore.info?.nickname || userInfoStore.info?.username || '用户' + Date.now().toString().slice(-4)
    // 立即保存到localStorage
    localStorage.setItem(localStorageKey, userInfo.value.nickname);
  }
}

// 初始调用确保nickname有值
ensureNicknameExists()

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
  表单验证规则：
  使用Element UI的表单验证功能
*/
const rules = {
  nickname: [
    { 
      required: true, 
      message: '请输入用户昵称', 
      trigger: 'blur' 
    },
    { 
      min: 2, 
      max: 10, 
      message: '昵称长度应为2-10个字符', 
      trigger: 'blur' 
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
  console.log('重置表单...')
  
  // 强制表单重新渲染
  formKey.value++
  
  // 等待DOM更新
  await nextTick()
  
  // 获取当前store中的信息
  const currentStoreInfo = userInfoStore.info || {}
  // 获取用户专属的localStorage键名
  const localStorageKey = getLocalStorageKey();
  // 从localStorage获取保存的nickname
  const storedNickname = localStorage.getItem(localStorageKey) || '';
  
  // 重置用户信息，确保所有字段都存在，优先使用localStorage中的nickname
  const resetData = {
    username: currentStoreInfo.username || '',
    email: currentStoreInfo.email || '',
    // 优先级：localStorage中的nickname > store中的nickname > username > 随机昵称
    nickname: storedNickname || currentStoreInfo.nickname || currentStoreInfo.username || '用户' + Date.now().toString().slice(-4)
  }
  
  // 应用重置数据
  Object.assign(userInfo.value, resetData)
  Object.assign(initialUserInfo, resetData)
  
  // 确保nickname不为空（作为最后的保障）
  ensureNicknameExists()
  // 同步更新initialUserInfo的nickname
  initialUserInfo.nickname = userInfo.value.nickname
  
  console.log('重置后的用户信息:', userInfo.value)
  console.log('重置后的initialUserInfo:', initialUserInfo)
  
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
  成功处理后更新Pinia store中的数据和localStorage
*/
const updateUserInfo = async () => {
    // 在验证前确保昵称不为空
    if (!userInfo.value.nickname || userInfo.value.nickname.trim() === '') {
      userInfo.value.nickname = userInfoStore.info?.username || '用户' + Date.now().toString().slice(-4);
    }
    
    // 调用Element表单验证方法
    formRef.value?.validate(async (valid) => {
    if (valid) {  // 验证通过
      try {
        // 提交所有必要字段进行修改：nickname、email、id、username
        const nicknameValue = userInfo.value.nickname.trim()
        // 确保finalNickname不为空，优先级：用户输入 > 用户名 > 随机昵称
        const finalNickname = nicknameValue !== '' ? nicknameValue.slice(0, 10) : 
                             (userInfoStore.info?.username || '用户' + Date.now().toString().slice(-4))
        const updateData = {
          nickname: finalNickname,
          email: userInfo.value.email || '',
          id: userInfoStore.info?.id || '',
          username: userInfoStore.info?.username || ''
        }
        
        console.log('准备更新的昵称数据:', updateData)
        console.log('API请求路径:', '/user/update')
        
        // 调用API更新用户信息
        const response = await userInfoUpdateService(updateData)
        console.log('API响应完整对象:', response)
        
        // 根据后端实际返回格式检查响应是否成功
        // 后端返回格式为{code: 0, message: '操作成功', data: {}}表示成功
        if (response && response.code === 0) {
          console.log('API调用成功，准备更新本地数据')
          
          // 将nickname保存到用户专属的localStorage中
          localStorage.setItem(getLocalStorageKey(), finalNickname);
          
          /* 
            更新Pinia store中的用户信息
          */
          // 创建更新后的用户信息对象
          const currentUserInfo = { ...userInfoStore.info }
          // 确保username存在
          const currentUsername = currentUserInfo.username || userInfo.value.username || ''
          // 设置nickname
          const updatedUserInfo = {
            ...currentUserInfo,
            username: currentUsername,
            nickname: finalNickname
          }
          
          console.log('更新后的用户信息:', updatedUserInfo)
          
          // 更新store
          userInfoStore.setInfo(JSON.parse(JSON.stringify(updatedUserInfo)))
          
          // 更新本地userInfo对象，确保UI显示一致
          userInfo.value.nickname = finalNickname
          
          // 更新initialUserInfo以匹配更新后的状态
          Object.assign(initialUserInfo, JSON.parse(JSON.stringify(updatedUserInfo)))
          
          // 重置表单修改状态
          isFormModified.value = false
          
          ElMessage.success('昵称更新成功！')  // 显示成功提示
        } else {
          // API调用返回但不成功
          console.error('API调用返回失败:', response)
          ElMessage.error(response?.message || '更新失败，请稍后重试')
        }
        
      } catch (error) {  // 错误处理
        console.error('更新用户昵称时发生异常:', error)
        console.error('错误详情:', error.response || error.message)
        
        // 不要在API失败时更新localStorage，避免与数据库数据不一致
        ElMessage.error('更新失败，请检查网络连接或稍后重试')
      }
    } else {  // 表单验证失败
        ElMessage.warning('请检查表单输入是否正确')
        return false  // 阻止后续流程
      }
  })
}

// 组件挂载时重新初始化数据并监听路由变化
onMounted(async () => {
  try {
    // 获取用户专属的localStorage键名
    const localStorageKey = getLocalStorageKey();
    // 从localStorage获取保存的nickname
    const storedNickname = localStorage.getItem(localStorageKey) || '';
    console.log('从localStorage读取的nickname:', storedNickname, '键名:', localStorageKey);
    
    // 主动获取最新的用户信息
    console.log('开始获取用户信息...')
    const userData = await userInfoService()
    console.log('获取到的用户数据:', userData)
    
    // 更新store中的用户信息
    if (userData && userData.data) {
      // 确保data对象包含所有必要字段
      const userInfoWithDefaults = {
        ...userData.data,
        // 确保username存在
        username: userData.data.username || '',
        // 确保nickname不为空，优先级：localStorage中的nickname > API返回的nickname > username > 随机昵称
        nickname: storedNickname || userData.data.nickname || userData.data.username || '用户' + Date.now().toString().slice(-4)
      }
      userInfoStore.setInfo(userInfoWithDefaults)
      console.log('更新后的store信息:', userInfoStore.info)
    }
    
    // 重新创建userInfo对象，确保使用store中的最新数据
    userInfo.value = JSON.parse(JSON.stringify(userInfoStore.info))
    
    // 使用Object.assign更新initialUserInfo对象而不是重新赋值
    Object.assign(initialUserInfo, JSON.parse(JSON.stringify(userInfoStore.info)))
    
    // 确保nickname有值
    ensureNicknameExists()
    // 更新initialUserInfo的nickname属性
    initialUserInfo.nickname = userInfo.value.nickname
    
    resetForm()
    
  } catch (error) {
    console.error('获取用户信息失败:', error)
    // 即使获取失败也要确保表单有默认值
    ensureNicknameExists()
  }
  
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
            <el-input 
              v-model="userInfo.nickname" 
              autocomplete="new-nickname"
              maxlength="10" 
              show-word-limit
              placeholder="请输入2-10个字符的昵称"
            ></el-input>
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