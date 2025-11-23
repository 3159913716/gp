<template>
  <div class="author-status-container">
    <!-- 紫色渐变背景标题栏 -->
    <div class="gradient-header">
      <h1>成为作者Ciallo~(✧ω✧)~☆</h1>
    </div>

    <!-- 状态显示卡片 -->
    <div class="status-card">
      <div v-if="loading" class="loading-container">
        <el-loading-spinner class="loading-spinner"></el-loading-spinner>
        <p>正在获取申请状态...</p>
      </div>

      <div v-else-if="error" class="error-container">
        <el-alert
          title="获取状态失败"
          :description="error"
          type="error"
          show-icon
        />
        <el-button type="primary" @click="fetchAuthorStatus">重试</el-button>
      </div>

      <div v-else class="status-content">
        <!-- 待审核状态 -->
        <div v-if="status === 0" class="pending-status">
          <div class="status-icon pending">
            <el-icon><Timer /></el-icon>
          </div>
          <h2>申请已提交</h2>
          <p>您的作者申请已提交成功，正在等待审核</p>
          <p class="tip">审核结果将通过站内信通知您，请耐心等待</p>
        </div>

        <!-- 审核通过状态 -->
        <div v-else-if="status === 1" class="approved-status">
          <div class="status-icon approved">
            <el-icon><Check /></el-icon>
          </div>
          <h2>恭喜！申请已通过</h2>
          <p>您已成功成为平台作者，可以开始发布文章了</p>
          <el-button type="primary" @click="toArticleCreate">去发布文章</el-button>
        </div>

        <!-- 审核拒绝状态 -->
        <div v-else-if="status === 2" class="rejected-status">
          <div class="status-icon rejected">
            <el-icon><Close /></el-icon>
          </div>
          <h2>申请未通过</h2>
          <p v-if="rejectReason" class="reject-reason">拒绝原因：{{ rejectReason }}</p>
          <p>很遗憾，您的作者申请未通过审核</p>
          <el-button type="primary" @click="toApplyAgain">重新申请</el-button>
        </div>

        <!-- 未申请状态 -->
        <div v-else class="not-applied-status">
          <div class="status-icon not-applied">
            <el-icon><Plus /></el-icon>
          </div>
          <h2>您还未申请成为作者</h2>
          <p>申请成为作者后，可以发布原创内容并获得更多权益</p>
          <el-button type="primary" @click="toApplyPage">立即申请</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request';
import { useTokenStore } from '@/stores/token';
import { ElMessage } from 'element-plus';
import { Timer, Check, Close, Plus } from '@element-plus/icons-vue';

export default {
  components: {
    Timer,
    Check,
    Close,
    Plus
  },
  data() {
    return {
      loading: false,
      error: null,
      status: undefined, // 0: 待审核, 1: 通过, 2: 拒绝
      rejectReason: ''
    };
  },
  mounted() {
    this.fetchAuthorStatus();
  },
  methods: {
    // 获取作者申请状态
    async fetchAuthorStatus() {
      this.loading = true;
      this.error = null;
      
      const tokenStore = useTokenStore();
      try {
        const headers = {};
        if (tokenStore.token) {
          const raw = tokenStore.token;
          headers.Authorization = raw.startsWith('Bearer ') ? raw : `Bearer ${raw}`;
        }
        
        const response = await request.get('/user/author-apply/status', { headers });
        const data = response?.data || response;
        console.log('作者申请状态响应:', response);

        // 处理响应
        let statusValue;
        if (response?.code === 0 || response?.code === 200) {
          if (response.data && response.data !== null) {
            statusValue = response.data?.status;
            this.rejectReason = response.data?.rejectReason || response.data?.reason || response.data?.reject_reason || '';
          }
        } else if (data) {
          statusValue = data?.status;
          this.rejectReason = data?.rejectReason || data?.reason || data?.reject_reason || '';
        }
        
        statusValue = statusValue !== undefined ? Number(statusValue) : undefined;
        this.status = statusValue;
        
        // 保存到localStorage
        const statusData = {
          status: this.status,
          rejectReason: this.rejectReason
        };
        localStorage.setItem('authorApplyStatus', JSON.stringify(statusData));
        
        // 如果申请审核通过，自动跳转到登录页面
        if (this.status === 1) {
          setTimeout(() => {
            this.$router.push('/login');
          }, 1000); // 延迟1秒跳转，让用户有时间看到成功提示
        }
        
      } catch (error) {
        console.error('获取作者申请状态失败:', error);
        this.error = error.message || '网络错误，请稍后重试';
        
        // 尝试从localStorage获取缓存的状态
        try {
          const cached = localStorage.getItem('authorApplyStatus');
          if (cached) {
            const cachedData = JSON.parse(cached);
            this.status = cachedData.status;
            this.rejectReason = cachedData.rejectReason || '';
          }
        } catch (e) {
          console.error('读取缓存状态失败:', e);
        }
      } finally {
        this.loading = false;
      }
    },
    
    // 跳转到文章发布页
    toArticleCreate() {
      this.$router.push('/article/create');
    },
    
    // 重新申请
    toApplyAgain() {
      this.$router.push('/user/author/apply');
    },
    
    // 去申请页面
    toApplyPage() {
      this.$router.push('/user/author/apply');
    }
  }
};
</script>

<style scoped>
.author-status-container {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.gradient-header {
  background: linear-gradient(135deg, #8e2de2, #4a00e0);
  color: white;
  padding: 40px 20px;
  border-radius: 12px;
  text-align: center;
  margin-bottom: 30px;
  box-shadow: 0 10px 30px rgba(142, 45, 226, 0.3);
}

.gradient-header h1 {
  font-size: 32px;
  font-weight: 700;
  margin: 0;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
}

.status-card {
  background: white;
  border-radius: 12px;
  padding: 40px;
  box-shadow: 0 15px 35px rgba(0, 0, 0, 0.1);
  text-align: center;
}

.loading-container,
.error-container,
.status-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.loading-spinner {
  width: 60px;
  height: 60px;
}

.status-icon {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  margin:0 auto 20px;
}

.status-icon.pending {
  background-color: #fef3c7;
  color: #d97706;
}

.status-icon.approved {
  background-color: #d1fae5;
  color: #059669;
}

.status-icon.rejected {
  background-color: #fee2e2;
  color: #dc2626;
}

.status-icon.not-applied {
  background-color: #dbeafe;
  color: #2563eb;
}

.status-content h2 {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
  color: #1e293b;
}

.status-content p {
  color: #64748b;
  margin: 8px 0;
  line-height: 1.6;
}

.status-content .tip {
  font-size: 14px;
  color: #94a3b8;
}

.reject-reason {
  background-color: #fee2e2;
  color: #dc2626;
  padding: 10px 20px;
  border-radius: 8px;
  margin: 10px 0;
  line-height: 1.5;
}

.el-button {
  margin-top: 20px;
  padding: 10px 30px;
  font-size: 16px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .author-status-container {
    padding: 10px;
  }
  
  .gradient-header h1 {
    font-size: 24px;
  }
  
  .status-card {
    padding: 30px 20px;
  }
  
  .status-icon {
    width: 80px;
    height: 80px;
    font-size: 36px;
  }
  
  .status-content h2 {
    font-size: 20px;
  }
}
</style>