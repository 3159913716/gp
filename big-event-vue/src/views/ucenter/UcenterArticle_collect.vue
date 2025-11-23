<!--
     我的收藏管理组件：
      实现对我的收藏的文章的管理
-->

<template>
  <div class="container"> 
    <!-- 文章接口数据展示 -->
    <div class="article-table" v-show="success && list.length > 0">
      <div class="table-header">
        <div class="title-cell">文章标题</div>
        <div class="img-cell">封面图</div>
        <div class="author-cell">作者</div>
        <div class="time-cell">收藏时间</div>
        <div class="action-cell">操作</div>
      </div>
      <div class="table-content">
        <div class="table-row" v-for="item in list" :key="item.id">
          <div class="title-cell" @click="navigateToArticle(item.id)">{{ item.title }}</div>
          <div class="img-cell"><img :src="item.coverImg || '/src/assets/default.png'" alt="封面图" class="cover-img" @click="navigateToArticle(item.id)"></div>
          <div class="author-cell">{{ item.author }}</div>
          <div class="time-cell">{{ item.collectTime }}</div>
          <div class="action-cell">
            <el-button type="danger" size="small" @click="removeCollect(item.id)" class="delete-btn">取消收藏</el-button>
          </div>
        </div>
      </div>
      <!-- 新增分页组件 -->
      <div class="pagination">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="page"
          :page-sizes="[5, 10, 20, 50]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
        ></el-pagination>
      </div>
    </div>

    <!-- 空数据状态 -->
    <div class="empty-state" v-show="!success || list.length === 0">
      <div>暂无收藏的文章</div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request.js';
import { Warning } from '@element-plus/icons-vue'; // 引入Element Plus图标

export default {
  components: { Warning }, // 注册图标组件
  // 数据定义
  data() {
    return {
        success: false,
        message: "获取成功",
        list: [],
        total: 0,
        page: 1,
        pageSize: 10
      };
  },
  
  // 组件挂载后调用接口
  mounted() {
    this.checkAndRefreshList()
  },
  
  // 方法定义
  methods: {
    // 导航到文章详情页
    navigateToArticle(id) {
      // 从收藏列表获取文章数据，传递收藏状态信息
      const article = this.list.find(item => item.id === id);
      const collectCount = article?.collectCount || 1; // 确保收藏数不为零
      
      // 记录来源为收藏列表并传递收藏状态
      this.$router.push({ 
        path: `/article/${id}`,
        query: { 
          fromCollect: 'true',
          isCollected: 'true',
          collectCount: collectCount.toString()
        }
      });
    },

    // 检查是否需要刷新列表
    checkAndRefreshList() {
      try {
        // 检查是否存在需要刷新的标志
        const needRefresh = localStorage.getItem('needRefreshCollectList')
        if (needRefresh === 'true') {
          // 清除标志
          localStorage.removeItem('needRefreshCollectList')
          // 重置页码并刷新列表
          this.page = 1
          this.fetchOrders()
        } else {
          // 正常加载列表
          this.fetchOrders()
        }
      } catch (error) {
        console.error('检查刷新标志失败:', error)
        // 出错时仍然尝试加载列表
        this.fetchOrders()
      }
    },
    
    // 调用接口：获取收藏文章列表
    async fetchOrders() {    
        try {
            const response = await request.get('/user/collections?page=' + this.page + '&pageSize=' + this.pageSize);
            
            // 直接使用API返回的数据（request实例已处理为response.data）
            if (response && response.data) {
                this.list = response.data.list || [];
                this.total = response.data.total || 0;
                this.success = true;
                console.log('获取收藏文章列表成功:', response);
            } else {
                // 如果API返回格式不符合预期，使用空数据
                this.list = [];
                this.total = 0;
                this.success = true;
            }
            
        } catch (error) {
            console.error('获取收藏文章列表失败:', error);
            this.success = false;
            this.message = '获取收藏文章列表失败';
            this.list = [];
            this.total = 0;
        }
    },
    

    // 新增：每页条数改变时触发
    handleSizeChange(val) {
      this.pageSize = val;
      this.page = 1; // 条数改变时重置为第一页
      this.fetchOrders(); // 重新请求数据
    },
    
    // 新增：当前页码改变时触发
    handleCurrentChange(val) {
      this.page = val;
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
          // 先将列表清空，提供更好的用户体验
          this.list = [];
          
          // 根据API文档，使用POST方法而不是DELETE方法
          const response = await request.post(`/article/collect/${id}`);

          this.$message.success('取消收藏成功');
          
       
          await this.fetchOrders();
        
          
        } catch (err) {
          
          this.$message.error('取消收藏失败，请稍后重试');
          // 即使失败也重新获取列表，确保数据一致性
          await this.fetchOrders();
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
  width: 100%;
  padding: 15px;
  box-sizing: border-box;
  min-height: calc(100vh - 120px);
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
  padding: 0 15px;
  background-color: #fafbfc;
  border-bottom: 1px solid #ebeef5;
  font-weight: 500;
  color: #606266;
  font-size: 14px;
}

/* 表格内容区域 */
.table-content {
  max-height: 600px;
  overflow-y: auto;
}

/* 自定义滚动条 */
.table-content::-webkit-scrollbar {
  width: 6px;
}

.table-content::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.table-content::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}

.table-content::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 表格行 */
.table-row {
  display: flex;
  align-items: center;
  height: 70px;
  padding: 0 15px;
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
  font-size: 14px;
  cursor: pointer;
  transition: color 0.2s;
}

.title-cell:hover {
  color: #409EFF;
}

.img-cell img {
  cursor: pointer;
  transition: transform 0.2s;
}

.img-cell img:hover {
  transform: scale(1.05);
}

.img-cell {
  flex: 1;
  justify-content: center;
  min-width: 60px;
}

.author-cell {
  flex: 1;
  justify-content: center;
  color: #606266;
  font-size: 13px;
}

.time-cell {
  flex: 1.5;
  justify-content: center;
  color: #909399;
  font-size: 12px;
}

.action-cell {
  flex: 1;
  justify-content: center;
  min-width: 80px;
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
  font-size: 12px;
  padding: 4px 10px;
}

.delete-btn:hover {
  opacity: 0.8;
  transform: scale(1.05);
}

/* 空数据状态 */
.empty-state {
  width: 100%;
  min-height: 50vh;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: clamp(14px, 2vw, 18px);
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

/* 新增分页样式 */
.pagination {
  padding: 15px;
  display: flex;
  justify-content: flex-end;
  align-items: center;
  border-top: 1px solid #f2f3f5;
  background-color: #fafbfc;
}

/* 响应式适配 - 大屏幕 */
@media (min-width: 1200px) {
  .title-cell {
    flex: 3.5;
  }
}

/* 响应式适配 - 中等屏幕 */
@media (max-width: 992px) {
  .time-cell {
    display: none;
  }
  
  .title-cell {
    flex: 2.5;
  }
  
  .img-cell, .author-cell, .action-cell {
    flex: 1;
  }
}

/* 响应式适配 - 小屏幕 */
@media (max-width: 768px) {
  .author-cell {
    display: none;
  }
  
  .container {
    padding: 10px;
  }
  
  .table-header, .table-row {
    padding: 0 10px;
    height: auto;
    min-height: 60px;
    flex-wrap: wrap;
  }
  
  .title-cell {
    flex: 1;
    min-width: 120px;
    order: 1;
  }
  
  .img-cell {
    order: 0;
    min-width: 50px;
    margin-right: 10px;
  }
  
  .action-cell {
    order: 2;
    flex: 1;
    justify-content: flex-end;
    min-width: 100px;
  }
  
  .cover-img {
    width: 45px;
    height: 30px;
  }
  
  .pagination {
    justify-content: center;
    padding: 12px 8px;
  }
  
  :deep(.el-pagination) {
    font-size: 12px;
  }
  
  :deep(.el-pagination__sizes .el-input__inner) {
    width: 80px;
  }
}

/* 响应式适配 - 超小屏幕 */
@media (max-width: 480px) {
  .container {
    padding: 5px;
  }
  
  .table-header, .table-row {
    padding: 8px 5px;
  }
  
  .title-cell {
    font-size: 13px;
    white-space: normal;
    word-break: break-word;
    line-height: 1.3;
  }
  
  .cover-img {
    width: 40px;
    height: 25px;
  }
  
  .delete-btn {
    font-size: 11px;
    padding: 3px 8px;
  }
  
  .empty-state {
    min-height: 50vh;
    font-size: 14px;
  }
}
</style>