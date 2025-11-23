<!--
  用户头像更换组件：
  实现用户头像的上传、预览和更新功能
  使用了Element Plus的上传组件和Vue的状态管理
-->

<script setup>
/* 从Vue引入ref函数：用于创建响应式数据 */
import { ref } from 'vue'

/* 
  导入Element Plus图标组件：
  Plus - 添加图标
  Upload - 上传图标
*/
import { Plus, Upload } from '@element-plus/icons-vue'

/* 
  导入Element Plus消息组件：
  ElMessage - 全局提示消息
*/
import { ElMessage } from 'element-plus'

/* 导入默认头像图片 */
import avatar from '@/assets/default.png'

/* 导入Pinia store模块 */
import { useTokenStore } from '@/stores/token.js'         // token存储
import useUserInfoStore from '@/stores/userInfo.js'      // 用户信息存储

/* 导入更新用户头像的API函数 */
import { userAvatarUpdateService } from '@/api/user.js'

/* 
  状态管理：
  tokenStore - 用于获取认证token
  userInfoStore - 存储用户信息
  isUploaded - 用于跟踪是否已上传成功
*/
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()


/* 
  响应式引用：
  uploadRef - 上传组件的DOM引用
  fileSelected - 标记用户是否已选择文件（用于控制上传按钮状态）
  selectedFile - 存储用户选择的文件对象
*/
const uploadRef = ref()
const fileSelected = ref(false)
const selectedFile = ref(null)

/* 
  头像URL响应式数据：
  初始值为用户信息中的头像URL
*/
const imgUrl = ref(userInfoStore.info.userPic)

/* 
  文件选择变化处理函数：
  只负责预览，不进行实际上传
*/
const handleUploadSuccess = (response, file, fileList) => {
  // 由于我们的request拦截器会处理响应，这里需要适配处理
  console.log('上传接口响应:', response);
  
  // 尝试从不同可能的数据结构中获取图片URL
  let avatarUrl = null;
  
  // 根据response的实际结构获取URL
  if (response && typeof response === 'object') {
    // 检查是否有code字段（标准API响应格式）
    if (response.code === 0) {
      // 如果data字段为空但操作成功，可以使用file的临时URL作为预览
      if (!response.data) {
        // 对于成功上传但没有返回URL的情况，使用浏览器的临时URL
        avatarUrl = URL.createObjectURL(file.raw);
        console.log('上传成功，使用临时URL作为预览:', avatarUrl);
      } else {
        avatarUrl = response.data || response.url || response.fileUrl;
      }
    } else if (response.data) {
      // 尝试直接从data中获取
      avatarUrl = response.data;
    } else {
      // 尝试其他可能的字段名
      avatarUrl = response.url || response.fileUrl || response.path;
    }
  } else if (typeof response === 'string') {
    // 如果返回的是字符串，直接作为URL
    avatarUrl = response;
  }
  
  if (avatarUrl) {
    imgUrl.value = avatarUrl      // 更新预览头像URL
    fileSelected.value = true     // 标记已选择文件
    console.log('头像上传成功，URL:', avatarUrl);
  } else {
    // 即使获取不到URL，如果操作成功也可以标记为已选择
    if (response && response.code === 0) {
      // 使用浏览器的临时URL作为预览
      avatarUrl = URL.createObjectURL(file.raw);
      imgUrl.value = avatarUrl;
      fileSelected.value = true;
      console.log('操作成功，使用临时URL作为预览:', avatarUrl);
    } else {
      console.error('上传失败：无法获取头像URL', response);
      ElMessage.error('上传失败：无法获取图片地址')
    }
  }
}

/*
  文件上传错误回调函数
  @param {Object} err - 错误对象
  @param {Object} file - 上传的文件对象
  @param {Array} fileList - 文件列表
*/
const handleUploadError = (err, file, fileList) => {
  console.error('上传错误:', err);
  const errorMsg = err?.response?.data?.msg || err?.message || '文件上传失败，请重试';
  ElMessage.error(errorMsg);
}

/*
  文件上传前检查函数 - 这里不再需要，因为我们在handleChange中已经进行了验证
*/

/*
  文件上传进度回调函数 - 暂时不使用
*/

/* 
    选择图片处理函数：
    触发隐藏的文件输入框点击事件
  */
  const handleSelectImage = () => {
    // 触发隐藏的文件输入框点击事件
    if (uploadRef.value) {
      uploadRef.value.$el.querySelector('input').click()
    }
  }
  
  /* 
    更新头像到服务器：
    调用API更新用户头像，根据后端返回的code和message判断是否成功
  */
  const updateAvatar = async () => {
    // 如果没有选择文件，不执行更新
    if (!fileSelected.value || !selectedFile.value) {
      ElMessage.warning('请先选择要上传的头像')
      return
    }
    
    try {
      // 调用头像更新API
      const result = await userAvatarUpdateService(imgUrl.value)
      
      console.log('更新头像API响应:', result);
      
      // 更新成功，从API响应中获取正确的头像URL
      // 注意：必须使用服务器返回的URL，而不是临时URL，这样刷新后才会保持
      let serverAvatarUrl = imgUrl.value; // 默认使用当前URL
      
      // 尝试从API响应中获取更准确的URL
      if (result && result.data) {
        // 检查是否有data字段包含URL信息
        if (typeof result.data === 'string') {
          serverAvatarUrl = result.data;
        } else if (result.data.url) {
          serverAvatarUrl = result.data.url;
        } else if (result.data.userPic) {
          serverAvatarUrl = result.data.userPic;
        }
      }
      
      console.log('最终使用的头像URL:', serverAvatarUrl);
      
      // 更新成功，同步更新用户信息store
      // 确保使用服务器返回的URL更新到store中，使所有使用该store的组件（UserLayout和article/Layout）能够实时渲染更新后的头像
      console.log('更新前userInfoStore:', userInfoStore.info);
      
      // 方案优化：采用更彻底的响应式更新策略
      // 1. 先获取当前用户信息，确保有完整的数据结构
      const currentInfo = userInfoStore.info || {};
      // 2. 创建一个全新的对象，而不仅仅是修改属性，确保响应式系统能够检测到变化
      const updatedInfo = {
        ...currentInfo,
        // 关键：强制更新userPic属性，这将触发所有使用此属性的组件重新渲染
        userPic: serverAvatarUrl + '?' + new Date().getTime() // 添加时间戳防止缓存问题
      };
      
      // 3. 调用setInfo方法更新store
      userInfoStore.setInfo(updatedInfo);
      
      // 4. 验证更新是否成功
      console.log('更新后userInfoStore:', userInfoStore.info);
      console.log('头像URL已更新为:', serverAvatarUrl);
      
      // 5. 为了确保立即生效，可以使用一个小技巧：强制重新获取一次用户信息
      // 这是一个额外的保险措施，确保store中的数据是最新的
      setTimeout(() => {
        console.log('强制刷新store中的头像数据');
        userInfoStore.setInfo({...userInfoStore.info, userPic: serverAvatarUrl});
      }, 100);
      
      ElMessage.success('头像更新成功，刷新后将保持')
      fileSelected.value = false // 标记未选择文件
    } catch (error) {
      console.error('更新头像时发生异常:', error);
      ElMessage.error(error.message || '更新失败，请检查网络连接或稍后重试');
    }
  }
</script>

<template>
<div class="container">
   <!-- 
    Element Plus卡片组件：
    class="page-container" - 自定义样式类
  -->
  <el-card class="page-container">
    <!-- 卡片头部插槽 -->
    <template #header>
      <div class="header">
        <span>更换头像</span>
      </div>
    </template>

    <!-- 使用栅格布局 -->
    <el-row>
      <el-col :span="12">
        <!-- 
          头像上传组件：
          ref="uploadRef" - 创建模板引用
          class="avatar-uploader" - 自定义样式类
          :show-file-list="false" - 不显示文件列表
          :auto-upload="true" - 开启自动上传
          action="/api/upload" - 上传API地址
          name="file" - 上传字段名
          :headers - 上传请求头（带认证token）
          :on-success - 上传成功回调
        -->
        <el-upload 
          ref="uploadRef" 
          class="avatar-uploader" 
          :show-file-list="false" 
          :auto-upload="false"
          :on-change="handleChange">
          <!-- 头像预览：如果有头像URL则显示，否则显示默认头像 -->
          <img v-if="imgUrl" :src="imgUrl" class="avatar" />
          <img v-else :src="avatar" width="278" />
        </el-upload>

        <!-- 
          隐藏的文件选择输入框：
          type="file" - 文件选择类型
          accept="image/*" - 只接受图片文件
          @change - 文件选择变化事件
          ref="fileInput" - 创建模板引用
          style="display: none" - 隐藏不显示
          不需要因为使用,因为使用 <el-upload >标签,最终都会生成一个隐藏的 input[type="file"]
        -->
        <!-- <input type="file" accept="image/*" style="display: none" ref="fileInput" /> -->

        <br />

        <!-- 
          选择图片按钮：
          type="primary" - 主按钮样式
          :icon="Plus" - 使用Plus图标
          size="large" - 大尺寸按钮
          @click - 点击事件：触发隐藏的文件输入框
          $el 获取到该组件渲染后的根DOM元素。
          querySelector('input')选择第一个input标签
          click()模拟点击
        -->
        <el-button type="primary" :icon="Plus" size="large" @click="handleSelectImage">
          选择图片
        </el-button>

        <!-- 
          上传头像按钮：
          type="success" - 成功状态按钮样式
          :icon="Upload" - 使用Upload图标
          size="large" - 大尺寸按钮
          @click - 点击事件：触发头像更新
          :disabled="!fileSelected" - 未选择文件时禁用
        -->
        <el-button type="success" :icon="Upload" size="large" @click="updateAvatar" :disabled="!fileSelected">
          上传头像
        </el-button>
      </el-col>
    </el-row>
  </el-card>
</div>
</template>

<!-- 
  Scoped CSS:
  使用深度选择器(:deep())修改Element组件内部样式
-->
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

/* 头像上传组件样式 */
.avatar-uploader {

  /* 深度选择器（穿透scoped） */
  :deep() {

    /* 头像预览样式 */
    .avatar {
      width: 278px;
      /* 固定宽度 */
      height: 278px;
      /* 固定高度 */
      display: block;
      /* 块级显示 */
      object-fit: cover;
      /* 保持比例填充 */
      border-radius: 6px;
      /* 圆角效果 */
    }

    /* 上传区域样式 */
    .el-upload {
      border: 1px dashed var(--el-border-color);
      /* 虚线边框 */
      border-radius: 6px;
      /* 圆角 */
      cursor: pointer;
      /* 手型指针 */
      position: relative;
      /* 相对定位 */
      overflow: hidden;
      /* 溢出隐藏 */
      transition: var(--el-transition-duration-fast);
      /* 过渡动画 */
    }

    /* 鼠标悬停效果 */
    .el-upload:hover {
      border-color: var(--el-color-primary);
      /* 主色调边框 */
    }

    /* 上传图标占位样式 */
    .el-icon.avatar-uploader-icon {
      font-size: 28px;
      color: #8c939d;
      /* 图标颜色 */
      width: 278px;
      height: 278px;
      text-align: center;
      line-height: 278px;
      /* 垂直居中 */
    }
  }
}
</style>