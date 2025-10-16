 <!--
     我的关注管理组件：
      实现对我的关注的用户的管理
-->

<template>
  <div class="container"> 
    <!-- 关注用户接口数据展示 -->
    <div class="follow-table" :style="{ display: userInfo.following ? 'block' : 'none' }">
        <div class="table-header">
            <div class="title-cell">用户昵称</div>
            <div class="img-cell">用户头像</div>
            <div class="author-cell">关注时间</div>
            <div class="time-cell">操作</div>
        </div>
        <div class="table-row" v-for="item in orders.list" :key="item.userId">
            <div class="title-cell">{{ item.nickname }}</div>
            <div class="img-cell"><img :src="item.userPic" alt="用户头像" class="user-img"></div>
            <div class="author-cell">{{ item.followTime }}</div>
            <div class="time-cell">
                <el-button type="danger" size="mini" @click="removeFollow(item.userId)" class="delete-btn">取消关注</el-button>
            </div>
        </div>
        
    </div>
    <!-- 空数据状态 -->
        <div class="empty-state" :style="{ display: userInfo.following ? 'none' : 'block' }">
            <div>暂无关注的用户</div>
        </div>
  </div>
</template>

<script>
import axios from 'axios';
import { WarningFilled } from '@element-plus/icons-vue'; // 引入Element Plus图标

export default {
 components: { WarningFilled }, // 注册图标组件
  // 数据定义
  data() {
    return {
      userInfo: {
        following: true
      },  // 存储第一个接口数据
      orders: {
        code: 200,
        message: "success",
        list: [
            {
            userId: 0,
            username: "",
            nickname: "",
            userPic: "",
            followTime: ""
            }
        ],
      },    // 存储第二个接口数据
    };
  },
  
  // 组件挂载后调用接口
  mounted() {
    this.fetchAllData();
  },
  
  // 方法定义
  methods: {
    // 调用第一个接口：获取用户信息
    async fetchUserInfo() {
      // try {
      //   const response = await axios.post('/users/{id}/follow');
      //   console.error('获取用户信息成功！');
      // } catch (err) {
      //   console.error('获取用户信息失败:', err);
      // }
    },
    
    // 调用第二个接口：获取关注作者列表
    async fetchOrders() {    
        const response = await axios.get('/users/following');
        this.orders.list = response.data.data.list||response.data.list||[];
    },
    
    // 统一处理所有接口调用
    async fetchAllData() {
        // 并行调用接口
        await Promise.all([
          this.fetchUserInfo(),
          this.fetchOrders()
        ]);
    },

    // 取消收藏方法（原有逻辑补充）
    removeFollow(id) {
      this.$confirm('确定要取消关注这个作者吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await axios.delete(`/users/${id}/follow`);
          this.orders.list = this.orders.list.filter(item => item.id !== id);
          this.$message.success('取消关注成功');
        } catch (err) {
          this.$message.error('取消关注失败，请稍后重试');
        }
      }).catch(() => {
        this.$message.info('已取消操作');
      });
    }
  }
};
</script>

<style>
.container {
  width: 100%;
  max-width: 1200px;
  padding: 20px 0;
  box-sizing: border-box;
}

/* 表格外层容器 */
.follow-table {
  width: 100%;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

/* 表头样式 */
.table-header {
  display: flex;
  align-items: center;
  height: 50px;
  background: #f5f7fa;
  border-bottom: 1px solid #ebeef5;
  font-weight: 500;
  color: #606266;
}

/* 表格行样式 */
.table-row {
  display: flex;
  align-items: center;
  height: 80px;
  border-bottom: 1px solid #ebeef5;
  transition: background-color 0.2s;
}

/* 行hover效果 */
.table-row:hover {
  background: #fafafa;
}

/* 单元格通用样式 */
.title-cell, .img-cell, .author-cell, .time-cell {
  padding: 0 16px;
  box-sizing: border-box;
}

/* 单元格宽度分配 */
.title-cell {
  width: 25%;
  color: #303133;
}
.img-cell {
  width: 20%;
  display: flex;
  align-items: center;
}
.author-cell {
  width: 35%;
  color: #909399;
}
.time-cell {
  width: 20%;
  display: flex;
  justify-content: flex-start;
}

/* 头像样式 */
.user-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #ebeef5;
}

/* 取消关注按钮样式调整 */
.delete-btn {
  padding: 4px 12px;
  border-radius: 4px;
}
.delete-btn:hover {
  background: #fef0f0;
}

/* 空数据状态 */
.empty-state {
  width: 100%;
  /* 响应式高度：随父容器或视口变化 */
  min-height: 50vh; /* 至少占视口高度的50%，大窗口会自动适应 */
  display: flex;
  align-items: center; /* 垂直居中 */
  justify-content: center; /* 水平居中 */
  color: #909399;
  font-size: clamp(16px, 2vw, 20px); /* 响应式字体：随窗口变化，有范围限制 */
  padding: 20px;
}

.loading-state {
  height: 200px;
  display: flex;
  justify-content: center;
  align-items: center;
  flex-direction: column;
  color: #909399;
  font-size: 14px;
}

.loading-text {
  margin-top: 12px;
}
</style>