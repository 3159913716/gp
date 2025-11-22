<!--
     我的关注管理组件：
      实现对我的关注的用户的管理
-->

<template>
  <div class="container"> 
    <!-- 加载状态 -->
    <div class="loading-state" v-if="loading">
      <el-icon class="is-loading"><Loading /></el-icon>
      <div class="loading-text">加载中...</div>
    </div>
    
    <!-- 错误状态 -->
    <div class="error-state" v-else-if="error">
      <el-icon class="error-icon"><WarningFilled /></el-icon>
      <div class="error-text">{{ errorMessage }}</div>
      <el-button type="primary" size="small" class="reload-btn" @click="refreshList">
        <el-icon><Refresh /></el-icon> 重新加载
      </el-button>
    </div>
    
    <!-- 关注用户接口数据展示 -->
    <div class="follow-table" v-else-if="success && list.length > 0">
        <div class="table-header">
            <div class="title-cell">用户昵称</div>
            <div class="img-cell">用户头像</div>
            <div class="author-cell">关注时间</div>
            <div class="time-cell">操作</div>
        </div>
        <div class="table-content">
            <div class="table-row" v-for="item in list" :key="item.userId">
                <div class="title-cell">{{ item.nickname || item.username }}</div>
                <div class="img-cell"><img :src="item.userPic" alt="用户头像" class="user-img"></div>
                <div class="author-cell">{{ item.followTime }}</div>
                <div class="time-cell">
                    <el-button type="danger" size="small" @click="removeFollow(item.userId)" class="delete-btn">取消关注</el-button>
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
    <div class="empty-state" v-else>
        <div>{{ isFansPage ? '暂无粉丝' : '暂无关注的用户' }}</div>
        <div class="empty-tip">{{ isFansPage ? '当其他用户关注您时，会显示在这里' : '您可以在文章详情页关注感兴趣的作者' }}</div>
    </div>
  </div>
</template>

<script>
import guanzhu from '@/api/guanzhu.js';
import { WarningFilled, Refresh, Loading } from '@element-plus/icons-vue'; // 引入Element Plus图标
import defaultAvatar from '@/assets/default.png'; // 导入默认头像图片

export default {
 components: { WarningFilled, Refresh, Loading }, // 注册图标组件
  // 数据定义
  data() {
    return {
        success: false,
        loading: false,
        error: false,
        errorMessage: '',
        message: "获取成功",
        list: [],
        total: 0,
        page: 1,
        pageSize: 10,
        isFansPage: false // 标识当前是否为粉丝列表页面
    };
  },
  
  // 组件挂载后调用接口
  mounted() {
    // 根据路由判断是显示关注列表还是粉丝列表
    this.isFansPage = this.$route.path.includes('/fans');
    this.fetchFollowList()
  },
  
  // 监听路由变化
  watch: {
    '$route'(to) {
      this.isFansPage = to.path.includes('/fans');
      this.fetchFollowList();
    }
  },
  
  // 方法定义
  methods: {
    
    // 获取关注/粉丝列表
    async fetchFollowList() {    
        this.loading = true;
        this.error = false;
        
        try {
            // 根据当前页面类型调用对应的API方法
            const apiMethod = this.isFansPage ? guanzhu.getFollowersList : guanzhu.getFollowingList;
            const response = await apiMethod();
        
            console.log('获取关注/粉丝列表响应:', response);
            // 根据API文档，正确处理响应格式
            // 考虑多种可能的响应格式，增加健壮性
            let dataList = [];
            
            // 情况1: 响应是{code: 0, message: "操作成功", data: [...]}
            if (response && response.code === 0 && Array.isArray(response.data)) {
                dataList = response.data;
             
            }
            // 情况2: 响应是{success: true, data: [...]}
            else if (response && response.success && Array.isArray(response.data)) {
                dataList = response.data;
                
            }
            // 情况3: 响应直接是数组
            else if (Array.isArray(response)) {
                dataList = response;
             
            }
            // 情况4: 响应包含list字段
            else if (response && Array.isArray(response.list)) {
                dataList = response.list;
               
            }
            
            // 标准化数据格式，确保每个项目都有必要的字段
            this.list = dataList.map(item => ({
                userId: item.id || item.userId || item.followed_id || '',
                nickname: item.nickname || item.username || '未知用户',
                username: item.username || '未知',
                userPic: item.userPic || item.avatar || defaultAvatar,
                followTime: item.followTime || item.create_time || new Date().toLocaleString(),
                isFollow: item.isFollow !== undefined ? item.isFollow : true
            }));
            
            this.total = this.list.length;
            this.success = this.list.length > 0;
            
          
        } catch (error) {
            const listType = this.isFansPage ? '粉丝' : '关注';
            console.error(`获取${listType}列表失败:`, error);
            this.error = true;
            this.errorMessage = error?.message || `获取${listType}列表失败`;
            this.$message.error(`获取${listType}列表失败，请稍后重试`);
            // 清空列表数据，确保显示空状态
            this.list = [];
            this.total = 0;
            this.success = false;
        } finally {
            this.loading = false;
        }
    },
    
    // 刷新关注列表
    refreshList() {
      this.fetchFollowList();
    },
    
    // 取消关注方法
    removeFollow(id) {
      this.$confirm('确定要取消关注这个作者吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          // 使用guanzhu.js中的toggleFollow方法取消关注
          await guanzhu.toggleFollow(id);
          // 更新列表，移除取消关注的用户
          this.list = this.list.filter(item => item.userId !== id);
          this.total = Math.max(0, this.total - 1);
          this.$message.success('取消关注成功');
          // 如果列表变空，更新success状态
          if (this.list.length === 0) {
            this.success = false;
          }
        } catch (err) {
          console.error('取消关注失败:', err);
          this.$message.error('取消关注失败，请稍后重试');
        }
      }).catch(() => {
        this.$message.info('已取消操作');
      });
    },
    
    // 分页处理
    handleSizeChange(size) {
      this.pageSize = size;
      this.page = 1;
      this.fetchFollowList();
    },
    
    handleCurrentChange(current) {
      this.page = current;
      this.fetchFollowList();
    }
  }
};
</script>

<style scoped>
.container {
  width: 100%;
  padding: 15px;
  box-sizing: border-box;
  min-height: calc(100vh - 120px);
  margin: 0 auto;
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
  background: #fafbfc;
  border-bottom: 1px solid #ebeef5;
  font-weight: 500;
  color: #606266;
  padding: 0 15px;
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

/* 表格行样式 */
.table-row {
  display: flex;
  align-items: center;
  height: 70px;
  border-bottom: 1px solid #ebeef5;
  transition: background-color 0.2s;
  padding: 0 15px;
}

/* 行hover效果 */
.table-row:hover {
  background: #fafafa;
}

/* 单元格通用样式 */
.title-cell, .img-cell, .author-cell, .time-cell {
  padding: 0 8px;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  overflow: hidden;
}

/* 单元格宽度分配 */
.title-cell {
  flex: 2;
  color: #303133;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 14px;
}
.img-cell {
  flex: 1.5;
  display: flex;
  align-items: center;
}
.author-cell {
  flex: 2.5;
  color: #909399;
  white-space: nowrap;
  text-overflow: ellipsis;
  font-size: 12px;
}
.time-cell {
  flex: 1.5;
  display: flex;
  justify-content: center;
  min-width: 80px;
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
  font-size: 12px;
  transition: all 0.2s;
}
.delete-btn:hover {
  opacity: 0.8;
  transform: scale(1.05);
  background: #fef0f0;
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
    flex: 1.75;
  }
}

/* 响应式适配 - 中等屏幕 */
@media (max-width: 992px) {
  .author-cell {
    display: none;
  }
  
  .title-cell {
    flex: 2;
  }
  
  .img-cell {
    flex: 1.5;
  }
}

/* 响应式适配 - 小屏幕 */
@media (max-width: 768px) {
  .container {
    padding: 10px;
    max-width: none;
  }
  
  .table-header, .table-row {
    padding: 0 10px;
    height: auto;
    min-height: 60px;
    flex-wrap: wrap;
  }
  
  .title-cell {
    flex: 1;
    min-width: 80px;
    order: 0;
  }
  
  .img-cell {
    flex: 1;
    min-width: 80px;
    order: 1;
  }
  
  .time-cell {
    order: 2;
    flex: 1;
    justify-content: flex-end;
    min-width: 100px;
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
    min-width: 60px;
  }
  
  .img-cell {
    min-width: 60px;
  }
  
  .user-img {
    width: 32px;
    height: 32px;
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