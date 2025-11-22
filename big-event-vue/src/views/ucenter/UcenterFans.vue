<!--
     我的粉丝管理组件：
      实现对我的粉丝用户的管理
-->

<template>
  <div class="container"> 
    <!-- 粉丝用户列表数据展示 -->
    <div class="follow-table" v-show="fansList.length > 0">
        <div class="table-header">
            <div class="title-cell">用户昵称</div>
            <div class="img-cell">用户头像</div>
            <div class="author-cell">关注时间</div>
            <div class="time-cell">操作</div>
        </div>
        <div class="table-content">
            <div class="table-row" v-for="item in fansList" :key="item.id">
                <div class="title-cell">{{ item.nickname || item.username }}</div>
                <div class="img-cell"><img :src="item.userPic" alt="用户头像" class="user-img"></div>
                <div class="author-cell">{{ item.followTime }}</div>
                <div class="time-cell">
                    <el-button type="success" size="small" @click="toggleFollow(item.id, item.isFollow)" class="follow-btn" v-if="!item.isFollow">回关</el-button>
                    <el-button type="warning" size="small" @click="toggleFollow(item.id, item.isFollow)" class="followed-btn" v-else>取消回关</el-button>
                </div>
            </div>
        </div>
    </div>
    <!-- 空数据状态 -->
    <!-- 错误状态 -->
    <div class="error-state" v-if="error && !loading">
        <el-icon class="error-icon"><WarningFilled /></el-icon>
        <div class="error-text">获取粉丝列表失败</div>
        <el-button type="primary" size="small" class="reload-btn" @click="fetchFans">
            <el-icon><Refresh /></el-icon> 重新加载
        </el-button>
    </div>
    
    <!-- 空数据状态 -->
    <div class="empty-state" v-show="fansList.length === 0 && !loading && !error">
        <div>暂无粉丝</div>
        <div class="empty-tip">当其他用户关注您时，会显示在这里</div>
    </div>
    <!-- 加载状态 -->
    <div class="loading-state" v-if="loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <div class="loading-text">加载中...</div>
    </div>
  </div>
</template>

<script>
import guanzhu from '@/api/guanzhu.js';
import { Loading, Refresh, WarningFilled } from '@element-plus/icons-vue'; // 引入Element Plus图标
import defaultAvatar from '@/assets/default.png'; // 导入默认头像图片

export default {
 components: { Loading, Refresh, WarningFilled }, // 注册图标组件
  // 数据定义
  data() {
    return {
      loading: false,
      error: false,
      fansList: [] // 粉丝列表数组，直接对应接口返回的data字段
    }
  },
  
  // 组件挂载后调用接口
  mounted() {
    this.fetchFans();
  },
  
  // 方法定义
  methods: {
    // 调用接口：获取粉丝列表
    async fetchFans() {
      this.loading = true;
      this.error = false;
      
      try {
    
        
        // 并行请求粉丝列表和关注列表
        const [followersResponse, followingResponse] = await Promise.all([
          guanzhu.getFollowersList(),
          guanzhu.getFollowingList()
        ]);
        console.log('粉丝列表API响应:', followersResponse);
        console.log('关注列表API响应:', followingResponse);
        
        // 处理粉丝列表数据
        let fansDataList = [];
        
        // 处理粉丝列表响应格式
        if (followersResponse && followersResponse.code === 0 && Array.isArray(followersResponse.data)) {
          fansDataList = followersResponse.data;
        } else if (followersResponse && Array.isArray(followersResponse.data)) {
          fansDataList = followersResponse.data;
        } else if (Array.isArray(followersResponse)) {
          fansDataList = followersResponse;
        } else if (followersResponse && Array.isArray(followersResponse.list)) {
          fansDataList = followersResponse.list;
        }
        
        // 处理关注列表数据
        let followingIds = new Set();
        if (followingResponse && followingResponse.code === 0 && Array.isArray(followingResponse.data)) {
          followingIds = new Set(followingResponse.data.map(item => 
            String(item.id || item.userId || item.userid || '')
          ));
        } else if (followingResponse && Array.isArray(followingResponse.data)) {
          followingIds = new Set(followingResponse.data.map(item => 
            String(item.id || item.userId || item.userid || '')
          ));
        } else if (Array.isArray(followingResponse)) {
          followingIds = new Set(followingResponse.map(item => 
            String(item.id || item.userId || item.userid || '')
          ));
        } else if (followingResponse && Array.isArray(followingResponse.list)) {
          followingIds = new Set(followingResponse.list.map(item => 
            String(item.id || item.userId || item.userid || '')
          ));
        }
        
        
        
        // 标准化粉丝列表数据格式
        this.fansList = fansDataList.map(item => {
          const userId = String(item.id || item.userId || item.userid || '');
         
          // 优先使用后端返回的isFollow状态，如果没有则通过关注列表判断
          let isFollowed = Boolean(item.isFollow);
          
          // 如果后端没有明确标记为已关注，则通过关注列表检查
          if (!isFollowed && followingIds.has(userId)) {
            isFollowed = true;
        
          }
          
    
          
          return {
            id: userId,
            nickname: item.nickname || item.username || '未知用户',
            username: item.username || '未知',
            userPic: item.userPic || item.avatar || defaultAvatar,
            followTime: item.followTime || item.create_time || new Date().toLocaleString(),
            isFollow: isFollowed
          };
        });
        
      
      } catch (error) {
        console.error('获取粉丝列表失败:', error);
        this.error = true;
        // 更详细的错误提示
        const errorMsg = error.response?.data?.message || error.message || '获取粉丝列表失败，请稍后重试';
        this.$message.error(errorMsg);
        // 清空列表数据
        this.fansList = [];
      } finally {
        this.loading = false;
      }
    },
    
   
    // 切换关注/取消关注状态
    toggleFollow(Id, isFollowing) {
      const operationText = isFollowing ? '取消关注' : '关注';
      const confirmText = isFollowing ? '确定要取消关注这个用户吗？' : '确定要关注这个用户吗？';
      
      this.$confirm(confirmText, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: isFollowing ? 'warning' : 'info'
      }).then(async () => {
        try {
         
          // 使用guanzhu API模块中的toggleFollow方法
          await guanzhu.toggleFollow(Id);
          
          // 更新粉丝列表中的isFollow状态
          const fanIndex = this.fansList.findIndex(item => item.id === String(Id));
          if (fanIndex !== -1) {
            this.fansList[fanIndex].isFollow = !isFollowing;
          
          }
          
          this.$message.success(`${operationText}成功`);
         
        } catch (err) {
          console.error(`${operationText}失败:`, err);
          const errorMsg = err.response?.data?.message || err.message || `${operationText}失败，请稍后重试`;
          this.$message.error(errorMsg);
        }
      }).catch(() => {
        this.$message.info('已取消操作');
      });
    },
    
    // 判断是否已关注用户（备用方法）
    isFollowing(Id) {
      const stringId = String(Id);
      const fanItem = this.fansList.find(item => item.id === stringId);
      return fanItem ? Boolean(fanItem.isFollow) : false;
    },
    
    // 由于接口未提供分页参数，这里移除分页相关方法
    
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

/* 错误状态样式 */
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

/* 空状态提示文本 */
.empty-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
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
  flex: 2;
  display: flex;
  justify-content: center;
  min-width: 150px;
  gap: 8px;
}

/* 头像样式 */
.user-img {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  object-fit: cover;
  border: 1px solid #ebeef5;
}

/* 按钮样式 */
 .follow-btn, .followed-btn {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  transition: all 0.2s;
}



.follow-btn:hover {
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

/* 分页样式 */
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
  
  .time-cell {
    flex: 2;
    justify-content: flex-end;
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
    min-height: 80px;
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
    flex: 100%;
    justify-content: flex-end;
    min-width: 100px;
    margin-top: 8px;
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
  
   .follow-btn, .followed-btn {
    font-size: 11px;
    padding: 3px 6px;
  }
  
  .empty-state {
    min-height: 50vh;
    font-size: 14px;
  }
}
</style>