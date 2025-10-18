<!--
     我的管理组件：
      实现对用户点赞文章，我的评论，我的收藏的管理
-->

<template>
  <div class="ucenter-wrapper">
    <!-- 我的管理组件 -->
      <div class="ucenter-mine">
        <!-- 排序选项卡 -->
        <div class="list-first">
          <router-link to="/admin/ucenter/collect">文章收藏</router-link>                   
          <router-link to="/admin/ucenter/follow">用户关注</router-link>
          <router-link to="/admin/ucenter/fans">我的米线</router-link>
        </div>
        
        <div class="list-last">
          <div class="article-like">
            点赞数：{{data.like}}
          </div>
           <div class="article-collect">
            收藏数：{{data.collect}}
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
      data:{
        like:0,
        collect:0
      }       // 存储接口返回的数据
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
    
    // 获取当前文章ID（这里假设通过路由参数获取，实际可能需要根据你的应用逻辑调整）
    const articleId = this.$route.params.id || 1; // 默认值1作为示例
    
    // 获取点赞数
    try {
      // 使用request.js进行请求，它会自动处理Authorization头
      const likeResponse = await request.post(``);
      if (likeResponse.success) {
        this.data.like = likeResponse.data.likeCount; // request.js已经处理了响应，直接访问数据
        console.log('获取点赞数成功:', this.data.like);
      }
    } catch (err) {
      console.error('获取点赞数失败:', err);
    }
    
    // 获取收藏数
    try {
      const collectResponse = await request.post(``);
      if (collectResponse.success) {
        this.data.collect = collectResponse.data.collectCount;
        console.log('获取收藏数成功:', this.data.collect);
      }
    } catch (err) {
      console.error('获取收藏数失败:', err);
    }
  }
  }
}
</script>

<style scoped>
/* 外层弹性容器：垂直排列子元素，占满可用高度 */
.ucenter-wrapper {
  display: flex;
  flex-direction: column; /* 子元素垂直排列 */
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 10px;
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
}
</style>