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
<<<<<<< HEAD
                    <el-button type="primary" size="mini" @click="viewUserProfile(item.id)" class="view-btn">查看资料</el-button>
                    <el-button type="success" size="mini" @click="followBack(item.id)" class="follow-btn" v-if="!isFollowing(item.id)">回关</el-button>
                    <el-button type="info" size="mini" disabled class="followed-btn" v-else>已关注</el-button>
=======
                    <el-button type="primary" size="small" @click="viewUserProfile(item.id)" class="view-btn">查看资料</el-button>
                    <el-button type="success" size="small" @click="followBack(item.id)" class="follow-btn" v-if="!item.isFollow">回关</el-button>
                    <el-button type="success" size="small" disabled class="followed-btn" v-else>已回关</el-button>
>>>>>>> 5982fa3e6d993fc4849de60c1e4191a30da6dd68
                </div>
            </div>
        </div>
    </div>
    <!-- 空数据状态 -->
    <div class="empty-state" v-show="fansList.length === 0 && !loading">
        <div>暂无粉丝</div>
    </div>
    <!-- 加载状态 -->
    <div class="loading-state" v-if="loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <div class="loading-text">加载中...</div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request.js';
import { Loading } from '@element-plus/icons-vue'; // 引入Element Plus图标
import { useTokenStore } from '@/stores/token.js';

export default {
 components: { Loading }, // 注册图标组件
  // 数据定义
  data() {
    return {
      loading: false,
      error: false,
      fansList: [], // 粉丝列表数组，直接对应接口返回的data字段
      followingIds: []
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
        // 调用真实接口，使用request实例自动携带token
        const response = await request.get('/user/followers');
        
        // 处理接口返回数据
        if (response.data.success) {
          this.fansList = response.data.data; // 直接使用一个数组接收data字段
        } else {
          this.error = true;
          console.error('获取粉丝列表失败:', response.data.message);
        }
      } catch (error) {
        console.error('获取粉丝列表失败:', error);
        this.error = true;
      } finally {
        this.loading = false;
      }
    },
    
    // 查看用户资料
    viewUserProfile(Id) {
      this.$message.info(`查看用户ID ${Id} 的资料`);
      // 实际项目中可以跳转到用户详情页面
      this.$router.push(`/user/${Id}`);
    },
    
    // 回关用户
    followBack(Id) {
      this.$confirm('确定要关注这个用户吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(async () => {
        try {
<<<<<<< HEAD
          // 调用关注接口，使用request实例自动携带token
          await request.post(`/user/follow/${Id}`);
          
          // 更新已关注列表
          this.followingIds.push(Id);
          this.$message.success('关注成功');
=======
          console.log(`开始关注用户ID: ${Id}`);
          // 使用guanzhu API模块中的toggleFollow方法
          await guanzhu.toggleFollow(Id);
          
          // 更新粉丝列表中的isFollow状态
          const fanIndex = this.fansList.findIndex(item => item.id === String(Id));
          if (fanIndex !== -1) {
            this.fansList[fanIndex].isFollow = true;
            console.log(`已更新用户ID ${Id} 的关注状态为已关注`);
          }
          
          this.$message.success('关注成功');
          console.log('关注成功');
>>>>>>> 5982fa3e6d993fc4849de60c1e4191a30da6dd68
        } catch (err) {
          this.$message.error('关注失败，请稍后重试');
        }
      }).catch(() => {
        this.$message.info('已取消操作');
      });
    },
    
    // 判断是否已关注用户
    isFollowing(Id) {
      return this.followingIds.includes(Id);
    },
    
    // 由于接口未提供分页参数，这里移除分页相关方法
    
  }
};
</script>

<style scoped>
.container {
  width: 100%;
  max-width: 1200px;
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
.view-btn, .follow-btn, .followed-btn {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  transition: all 0.2s;
}

.view-btn:hover {
  opacity: 0.8;
  transform: scale(1.05);
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
  
  .view-btn, .follow-btn, .followed-btn {
    font-size: 11px;
    padding: 3px 6px;
  }
  
  .empty-state {
    min-height: 50vh;
    font-size: 14px;
  }
}
</style>