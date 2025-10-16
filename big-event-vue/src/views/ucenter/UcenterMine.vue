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
          <router-link to="/admin/ucenter/collect">我的收藏</router-link>                  
          <router-link to="/admin/ucenter/follow">我的关注</router-link>
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
import axios from 'axios';
export default {
  data() {
    return {
      data:{
        like:0,
        collect:0
      },       // 存储接口返回的数据
    };
  },
  mounted() {
    // 组件挂载后请求数据
    this.fetchData();
  },
  methods: {
    async fetchData() {
        // // 发起 POST请求
        // const response = await axios.post('/articles/{id}/like');
        // // 存储接口返回的数据
        // this.data.like = response.data.data.likeCount;
        // // 发起 POST请求
        // const response2 = await axios.post('/articles/{id}/collect');
        // // 存储接口返回的数据
        // this.data.collect = response2.data.data.collectCount;
    }
  }
}

</script>

<style scoped>
/* 外层弹性容器：垂直排列子元素，占满可用高度 */
.ucenter-wrapper {
  display: flex;
  flex-direction: column; /* 子元素垂直排列 */
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
  
}
.list-first, .list-last {
  /* 每个选项卡之间添加间距 */
  gap: 10px;
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
  /* 设置浮动，使文字靠左对齐 */
  float: left;
  /* 设置文字颜色 */
  color: #333;
  /* 设置内边距，调整选项卡的点击区域和显示大小 */
  padding:16px;
}

.list-first a:hover {
  /* 鼠标悬浮时的背景颜色 */
  background-color: #f3f4f6;
}
.list-first a.router-link-active{
  /* 激活状态下的背景颜色 */
  background-color: #1890ff;
  /* 激活状态下的文字颜色 */
  color: #fff;
}
/* 让 .sort-content 填充剩余垂直空间（可选，根据需求调整） */
.sort-content {
  flex: 1; 
}
</style>