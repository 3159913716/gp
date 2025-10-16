<!--
     我的收藏管理组件：
      实现对我的收藏的文章的管理
-->

<template>
   <div class="container"> 
    <!-- 文章接口数据展示 -->
    <div class="article-table" :style="{ display:userInfo.collected? 'block' : 'none' }">
      <div class="table-header">
            <div class="title-cell">文章标题</div>
            <div class="img-cell">封面图</div>
            <div class="author-cell">作者</div>
            <div class="time-cell">收藏时间</div>
            <div class="action-cell">操作</div>
        </div>
        <div class="table-row" v-for="item in orders.list" :key="item.id">
            <div class="title-cell">{{ item.title }}</div>
            <div class="img-cell"><img :src="item.coverImg" alt="封面图" class="cover-img"></div>
            <div class="author-cell">{{ item.author }}</div>
            <div class="time-cell">{{ item.collectTime }}</div>
            <div class="action-cell">
                <el-button type="danger" size="mini" @click="removeCollect(item.id)" class="delete-btn">取消收藏</el-button>
            </div>
        </div>
        <!-- 新增分页组件 -->
      <div class="pagination">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="orders.page"
          :page-sizes="[5, 10, 20, 50]"
          :page-size="orders.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="orders.total"
        ></el-pagination>
      </div>
    </div>

      <!-- 空数据状态 -->
        <div class="empty-state" :style="{ display: userInfo.collected? 'none' : 'block' }">
            <div>暂无收藏的文章</div>
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
        collected: true,
        collectCount: 0
      },  // 存储第一个接口数据
      orders: {
        code: 200,
        message: "success",
        list: [
            {
            id: 0,
            title: "",
            coverImg: "",
            author: "",
            collectTime: ""
            }
        ],
        total: 50,
        page: 1,
        pageSize: 10
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
      //   const response = await axios.post('/articles/{id}/like');
      //   console.error('获取用户信息成功！');
      // } catch (err) {
      //   console.error('获取用户信息失败:', err);
      // }
    },


    // 调用第二个接口：获取收藏文章列表
    async fetchOrders() {    
        const response = await axios.get('/users/collections');
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

    // 新增：每页条数改变时触发
    handleSizeChange(val) {
      this.orders.pageSize = val;
      this.orders.page = 1; // 条数改变时重置为第一页
      this.fetchOrders(); // 重新请求数据
    },
    
    // 新增：当前页码改变时触发
    handleCurrentChange(val) {
      this.orders.page = val;
      this.fetchOrders(); // 重新请求数据
    },

    // 取消收藏方法（原有逻辑补充）
    removeCollect(id) {
      this.$confirm('确定要取消收藏这篇文章吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await axios.delete(`/articles/${id}/collect`);
          this.orders.list = this.orders.list.filter(item => item.id !== id);
          this.$message.success('取消收藏成功');
        } catch (err) {
          this.$message.error('取消收藏失败，请稍后重试');
        }
      }).catch(() => {
        this.$message.info('已取消操作');
      });
    }
  }
};
</script>

<style scoped>
/* 容器样式 */
.container {
  padding: 20px 0;
  background-color: #f5f7fa;
  min-height: calc(100vh - 40px);
}

/* 表格整体样式 */
.article-table {
  width: 100%;
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

/* 表格头部 */
.table-header {
  display: flex;
  align-items: center;
  height: 50px;
  padding: 0 20px;
  background-color: #fafbfc;
  border-bottom: 1px solid #ebeef5;
  font-weight: 500;
  color: #606266;
}

/* 表格行 */
.table-row {
  display: flex;
  align-items: center;
  height: 80px;
  padding: 0 20px;
  border-bottom: 1px solid #f2f3f5;
  transition: background-color 0.2s;
}

/* 行 hover 效果 */
.table-row:hover {
  background-color: #fafbfc;
}

/* 单元格通用样式 */
.title-cell, .img-cell, .author-cell, .time-cell, .action-cell {
  display: flex;
  align-items: center;
  padding: 0 8px;
  overflow: hidden;
}

/* 单元格宽度分配 */
.title-cell {
  flex: 3;
  white-space: nowrap;
  text-overflow: ellipsis;
  color: #303133;
}

.img-cell {
  flex: 1;
  justify-content: center;
}

.author-cell {
  flex: 1;
  justify-content: center;
  color: #606266;
}

.time-cell {
  flex: 1.5;
  justify-content: center;
  color: #909399;
}

.action-cell {
  flex: 1;
  justify-content: center;
}

/* 封面图样式 */
.cover-img {
  width: 60px;
  height: 40px;
  object-fit: cover;
  border-radius: 4px;
  border: 1px solid #f2f3f5;
}

/* 取消收藏按钮样式 */
.delete-btn {
  transition: all 0.2s;
}

.delete-btn:hover {
  opacity: 0.8;
  transform: scale(1.05);
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

/* 加载状态 */
.loading-state {
  height: 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.loading-text {
  margin-top: 12px;
  font-size: 14px;
}

/* 错误状态 */
.error-state {
  height: 200px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #f56c6c;
  font-size: 14px;
}

.error-icon {
  font-size: 18px;
}

.reload-btn {
  margin-left: 8px;
  color: #409eff;
  background-color: rgba(64, 158, 255, 0.1);
  border-color: transparent;
}

.reload-btn:hover {
  background-color: rgba(64, 158, 255, 0.2);
  border-color: transparent;
}

/* 响应式适配 - 屏幕宽度小于992px时 */
@media (max-width: 992px) {
  .time-cell {
    display: none;
  }
  
  .title-cell {
    flex: 2;
  }
  
  .img-cell, .author-cell, .action-cell {
    flex: 1;
  }
}

/* 响应式适配 - 屏幕宽度小于768px时 */
@media (max-width: 768px) {
  .author-cell {
    display: none;
  }
  
  .container {
    padding: 10px;
  }
  
  .table-header, .table-row {
    padding: 0 10px;
  }
  
  .cover-img {
    width: 40px;
    height: 30px;
  }
}

/* 新增分页样式 */
.pagination {
  padding: 20px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  border-top: 1px solid #f2f3f5;
}

/* 响应式适配：小屏幕下分页居中 */
@media (max-width: 768px) {
  .pagination {
    justify-content: center;
    padding: 15px 10px;
  }
}
</style>