<!--
     我的管理组件：
      实现对用户点赞文章，我的评论，我的收藏的管理
-->

<template>
  <div class="ucenter-wrapper">
    <!-- 错误提示 -->
    <div v-if="error" class="error-message">
      {{ error }}
      <button @click="fetchUserInfo" class="retry-btn">重试</button>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-indicator">
      <div class="loading-spinner"></div>
      <span>正在加载数据...</span>
    </div>
    
    <!-- 我的管理组件 -->
      <div class="ucenter-mine">
        <!-- 排序选项卡 -->
        <div class="list-first">
          <router-link to="collect">文章收藏</router-link>                   
          <router-link to="follow">用户关注</router-link>
          <router-link to="fans">我的粉丝</router-link>
        </div>
        
        <div class="list-last">
          <div class="article-like">
            点赞数：{{likeCount}}
          </div>
           <div class="article-collect">
            收藏数：{{collectCount}}
          </div>
        </div>
       
        </div>
        <div class="sort-content">
          <router-view></router-view>
        </div>
  </div>
</template>

<script>
import request from '@/utils/request.js';
import { useTokenStore } from '@/stores/token.js';
export default {
  data() {
    return {
      // 直接使用扁平化的响应式数据，避免嵌套对象的响应式问题
      collectCount: 0,
      likeCount: 0,
      page: 1,
      pageSize: 10,
      // 用户体验优化相关状态
      loading: false,
      error: null
    };
  },
  mounted() {
    // 组件挂载后请求数据
    this.fetchUserInfo();
  },
  methods: {
  async fetchUserInfo() {
    // 创建tokenStore实例
    const tokenStore = useTokenStore();
    
    // 设置加载状态
    this.loading = true;
    this.error = null;
    
    // 获取点赞数 - 使用专门的计数接口
    try {
      // 使用request.js进行请求，它会自动处理Authorization头
      const likeResponse = await request.get(`/user/likes/count`);
      if (likeResponse && likeResponse.code === 200) {
        this.likeCount = likeResponse.data || 0;
        console.log('获取点赞数成功:', this.likeCount);
      } else if (likeResponse && likeResponse.success) {
        // 兼容不同的响应格式
        this.likeCount = likeResponse.data?.likeCount || 0;
      }
    } catch (err) {
      console.error('获取点赞数失败:', err);
      this.likeCount = 0;
      this.error = '获取点赞数据失败';
    }
    
    // 获取收藏数 - 使用专门的计数接口
    try {
      // 尝试使用专门的计数接口
      const collectResponse = await request.get(`/user/collections/count`);
      
      // 检查响应格式，支持多种可能的返回格式
      if (collectResponse && collectResponse.code === 200) {
        this.collectCount = collectResponse.data || 0;
        console.log('获取收藏数成功:', this.collectCount);
      } else if (collectResponse && collectResponse.success) {
        // 兼容不同的响应格式
        this.collectCount = collectResponse.data?.total || collectResponse.data?.collectCount || 0;
        console.log('获取收藏数成功:', this.collectCount);
      } else {
        // 如果专门的计数接口失败，尝试从分页接口获取
        const pageResponse = await request.get(`/user/collections?page=1&pageSize=1`);
        if (pageResponse && pageResponse.data && typeof pageResponse.data.total === 'number') {
          this.collectCount = pageResponse.data.total;
        }
      }
    } catch (err) {
      console.error('获取收藏数失败:', err);
      this.collectCount = 0;
      this.error = this.error || '获取收藏数据失败';
    } finally {
      // 无论成功失败，都重置加载状态
      this.loading = false;
    }
    
    // 强制更新视图
    this.$forceUpdate();
  },
    
    // 其他方法...
  }
}
</script>

<style scoped>
/* 外层弹性容器：垂直排列子元素，占满可用高度 */
.ucenter-wrapper {
  display: flex;
  flex-direction: column; /* 子元素垂直排列 */
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 15px 30px;
  box-sizing: border-box;
}
.ucenter-mine {
  /* 使用弹性布局，让选项卡水平排列 */
  display: flex; 
  /* 主轴两端对齐 */
  justify-content: space-between;
  /* 交叉轴居中对齐 */
  align-items: center;
  /* 给整个我的管理组件设置内边距，增加与外部的间距 */
  padding: 20px;
  /* 设置白色背景，与红色框内背景一致 */
  background-color: #fff;
  /* 设置圆角，让整体更美观 */
  border-radius: 8px;
  /* 添加阴影，增强立体感 */
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
  
}
.list-first, .list-last {
  /* 每个选项卡之间添加间距 */
  gap: 10px;
  display: flex;
  align-items: center;
}
.list-first a{
  /* 去除默认的下划线样式 */
  text-decoration: none;
  /* 设置文字颜色 */
  color: #333;
  /* 设置内边距，调整选项卡的点击区域和显示大小 */
  padding: 8px 16px;
  /* 设置圆角，让选项卡更美观 */
  border-radius: 4px;
  /* 鼠标悬浮时的过渡效果，让颜色变化更平滑 */
  transition: background-color 0.2s ease;
}
.list-last div{
  /* 设置文字颜色 */
  color: #333;
  /* 设置内边距，调整选项卡的点击区域和显示大小 */
  padding: 8px 16px;
  font-size: 14px;
}

.list-first a:hover {
  /* 鼠标悬浮时的背景颜色 */
  background-color: #f3f4f6;
}
.list-first a.router-link-active{
  /* 激活状态下的背景颜色 - 恢复为透明 */
  background-color: transparent;
  /* 激活状态下的文字颜色 - 保持蓝色 */
  color: #1890ff;
  /* 底部蓝色下划线 */
  border-bottom: 2px solid #1890ff;
  /* 保持圆角 */
  border-radius: 0;
  font-weight: 500;
}
/* 让 .sort-content 填充剩余垂直空间（可选，根据需求调整） */
.sort-content {
  flex: 1;
  width: 100%;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

/* 响应式设计 - 中等屏幕 */
@media (max-width: 768px) {
  .ucenter-mine {
    flex-direction: column;
    gap: 15px;
    padding: 15px;
    align-items: stretch;
  }
  
  .list-first {
    justify-content: center;
    flex-wrap: wrap;
  }
  
  .list-last {
    justify-content: space-around;
  }
  
  .list-last div {
    padding: 6px 12px;
    font-size: 13px;
  }
  
  .ucenter-wrapper {
    padding: 5px;
  }
}

/* 响应式设计 - 小屏幕 */
@media (max-width: 480px) {
  .list-first a {
    padding: 6px 12px;
    font-size: 14px;
  }
  
  .list-last {
    flex-direction: column;
    gap: 8px;
    align-items: center;
  }
  
  .sort-content {
    border-radius: 6px;
  }
  
  .error-message {
    padding: 10px;
    font-size: 14px;
  }
  
  .loading-indicator {
    padding: 20px;
    font-size: 14px;
  }
}

/* 错误提示样式 */
.error-message {
  background-color: #fff2f0;
  border: 1px solid #ffccc7;
  color: #f5222d;
  padding: 15px 20px;
  border-radius: 6px;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.retry-btn {
  background-color: #ff4d4f;
  color: white;
  border: none;
  padding: 6px 16px;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.retry-btn:hover {
  background-color: #ff7875;
}

/* 加载状态样式 */
.loading-indicator {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
  color: #666;
}

.loading-spinner {
  width: 30px;
  height: 30px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 10px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
</style>