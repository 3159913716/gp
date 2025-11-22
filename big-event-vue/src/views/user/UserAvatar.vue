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
const handleChange = (file, fileList) => {
 
  
  // 检查文件是否有效
  if (!file || !file.raw) {
    console.error('文件无效或未包含raw属性');
    ElMessage.error('文件格式错误，请重新选择!');
    return;
  }
  
 
  
  // 验证文件类型
  const isValidType = file.raw.type.startsWith('image/');
 
  
  if (!isValidType) {
    ElMessage.error('只能上传图片文件!');
    return;
  }
  
  // 验证文件大小 (2MB)
  const isLt2M = file.raw.size / 1024 / 1024 < 2;
 
  
  if (!isLt2M) {
    ElMessage.error('上传图片大小不能超过 2MB!');
    return;
  }
  
  // 创建文件URL用于预览
  const fileUrl = URL.createObjectURL(file.raw);
  imgUrl.value = fileUrl;
  fileSelected.value = true;
  selectedFile.value = file;
  

}

/*
  文件上传错误回调函数
  @param {Object} err - 错误对象
  @param {Object} file - 上传的文件对象
  @param {Array} fileList - 文件列表
*/
const handleUploadError = (err) => {
  console.error('错误对象:', err);
  
  // 显示错误信息
  const errorMsg = err?.response?.data?.message || err?.message || '上传失败，请重试';
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
      // 创建FormData对象，用于上传文件
      const formData = new FormData();
      formData.append('file', selectedFile.value.raw);
      
      // 先上传文件到文件服务器获取URL
      
      const uploadResult = await fetch('/api/upload', {
        method: 'POST',
        headers: {
          'Authorization': tokenStore.token ? (tokenStore.token.startsWith('Bearer ') ? tokenStore.token : `Bearer ${tokenStore.token}`) : ''
        },
        body: formData
      });
      
      const uploadData = await uploadResult.json();
     console.log('文件上传响应:', uploadData);
      
      if (uploadData.code !== 0) {
        throw new Error(uploadData.message || '文件上传失败');
      }
      
      const avatarUrl = uploadData.data; // 获取上传后的图片URL
     
      
      // 调用头像更新API

      const updateResult = await userAvatarUpdateService(avatarUrl);
      
      
      
      // 只根据code判断是否成功，不关心data字段
      if (updateResult && updateResult.code === 0) {
        // 更新成功，同步更新用户信息store
        const currentInfo = userInfoStore.info || {};
        const updatedInfo = {
          ...currentInfo,
          userPic: avatarUrl + '?' + new Date().getTime() // 添加时间戳防止缓存问题
        };
        
        userInfoStore.setInfo(updatedInfo);
        
        // 显示成功消息
        ElMessage.success(updateResult.message || '头像更新成功，刷新后将保持');
        
        // 重置状态
        fileSelected.value = false;
        selectedFile.value = null;
      } else {
        throw new Error(updateResult?.message || '头像更新失败');
      }
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