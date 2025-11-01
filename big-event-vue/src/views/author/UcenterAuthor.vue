<!-- 创作者激励计划样式已移至文件底部的style标签中 -->

<template>
  <div class="author-center">
    <!-- 与弹窗同步显示的遮罩层 -->
    <div v-if="statusDialogVisible" class="status-overlay"></div>
    
    <div class="becomeAuthor">
        <div class="title" style="font-size: clamp(1.5rem, 5vw, 3rem); white-space: nowrap; overflow: hidden; text-overflow: ellipsis;">
          成为作者Ciallo～(∠・ω&lt; )⌒☆
        </div>
    </div>
      <div class="pics">
        <!-- 轮播图组件 -->
        <el-carousel :interval="3000" type="card" height="220px">
          <el-carousel-item v-for="(item, index) in carouselList" :key="index">
            <div class="carousel-item">
              <img :src="item.imgUrl" alt="轮播图" class="carousel-img" referrerpolicy="no-referrer">
              <div class="carousel-desc">{{ item.desc }}</div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>
      <div class="footer">
        <el-button type="primary" @click="submitApplication" class="btn">提交申请</el-button>
      </div>
     <div class="news-container">
            <div class="plan-title">
              <span class="plan-icon">✨</span>
              <h3>创作者激励计划</h3>
            </div>
            <div class="news-slider">
              <div v-for="(item, index) in newsList" :key="index" class="news-item">
                <div class="news_header">
                  <span class="news_tag">新闻</span>
                  <div class="news_title">{{ item.title }}</div>
                </div>
                <div class="news_desc">{{ item.desc }}</div>
                <div class="news-footer">
                  <el-button type="primary" size="small" plain class="learn-more-btn">
                    了解详情
                  </el-button>
                </div>
              </div>
            </div>
    </div>
    <!-- 作者申请弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      title="申请成为作者"
      width="500px"
      :close-on-click-modal="false"
      @close="handleClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="100px"
        class="author-form"
      >
        <el-form-item label="真实姓名" prop="realName">
          <el-input 
            v-model="formData.realName" 
            placeholder="请输入您的真实姓名"
            maxlength="10"
          />
        </el-form-item>
        
        <el-form-item label="身份证号" prop="idCard">
          <el-input 
            v-model="formData.idCard" 
            placeholder="请输入您的身份证号"
            maxlength="18"
          />
        </el-form-item>
        
        <el-form-item label="申请描述" prop="applyDesc">
          <el-input v-model="formData.applyDesc" type="textarea" :rows="4" placeholder="请描述创作经验和特长" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" @click="handleSubmit">提交申请</el-button>
      </template>
    </el-dialog>
    
    <!-- 申请状态显示模态框 -->
    <div v-if="statusDialogVisible" class="status-modal-overlay" @click.self="closeStatusDialog">
      <div class="status-modal-content">
        <div class="status-header">
          <h3>作者申请状态</h3>
          <button class="close-btn" @click="closeStatusDialog" v-if="applyStatus?.status !== 1">
            <Close />
          </button>
        </div>
        
        <div v-if="loading" class="loading-state">
          <div class="loading-spinner"></div>
          <p>正在获取申请状态...</p>
        </div>
        
        <div v-else-if="applyStatus" class="status-content">
          <!-- 等待审核状态 -->
          <div v-if="applyStatus.status === 0" class="status-pending">
            <div class="status-icon">
              <InfoFilled class="pending-icon" />
            </div>
            <h2>等待审核中</h2>
            <p class="status-desc">您的申请正在审核中，请耐心等待</p>
            <button class="cancel-btn" @click="cancelApply">取消申请</button>
          </div>
          
          <!-- 申请成功状态 -->
          <div v-else-if="applyStatus.status === 1" class="status-success">
            <div class="status-icon">
              <Check class="success-icon" />
            </div>
            <h2>申请作者成功</h2>
            <p class="status-desc">恭喜您已成功成为作者！</p>
            <p class="redirect-tip">系统将在 {{ redirectCountdown }} 秒后自动跳转到登录页面，请重新登录</p>
          </div>
          
          <!-- 申请失败状态 -->
          <div v-else-if="applyStatus.status === 2" class="status-rejected">
            <div class="status-icon">
              <Close class="rejected-icon" />
            </div>
            <h2>申请未通过</h2>
            <p class="status-desc">很遗憾，您的申请未能通过审核</p>
            <div class="reject-reason">
              <h4>拒绝原因：</h4>
              <p>{{ applyStatus.rejectReason || '暂无具体原因' }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import request from '@/utils/request';
import { Check, InfoFilled, Close } from '@element-plus/icons-vue';
// 引入Element Plus组件
import { ElCarousel, ElCarouselItem, ElDialog, ElForm, ElFormItem, ElInput, ElButton, ElMain } from 'element-plus';
// 单独引入ElMessage工具函数
import { ElMessage } from 'element-plus';
// 引入API服务
import { getAuthorApplyStatusService, cancelAuthorApplyService } from '@/api/user';
// 引入Vue Router
import { useRouter } from 'vue-router';
// 引入Element Plus样式
import 'element-plus/dist/index.css';

export default {
  components: {
    Check,
    InfoFilled,
    Close,
    ElCarousel,
    ElCarouselItem,
    ElDialog,
    ElForm,
    ElFormItem,
    ElInput,
    ElButton,
    ElMain
  },
  data() {
    return {
      // 表单数据
      formData: {
        realName: '',
        idCard: '',
        applyDesc: ''
      },
      // 状态显示模态框 - 默认不显示
      statusDialogVisible: false,
      // 申请状态信息 - 默认值为undefined
      applyStatus: {status: undefined},
      // 加载状态
      loading: false,
      // 倒计时（用于申请成功后跳转）
      redirectCountdown: 3,
      // 表单验证规则
      rules: {
        realName: [
          { required: true, message: '请输入真实姓名', trigger: 'blur' },
          { min: 2, max: 10, message: '姓名长度在 2 到 10 个字符', trigger: 'blur' }
        ],
        idCard: [
          { required: true, message: '请输入身份证号', trigger: 'blur' },
          { pattern: /^\d{17}[\dXx]$/, message: '请输入有效的身份证号', trigger: 'blur' }
        ],
        applyDesc: [
          { required: true, message: '请输入申请描述', trigger: 'blur' },
          { min: 10, max: 200, message: '申请描述长度在 10 到 200 个字符', trigger: 'blur' }
        ]
      },
      // 弹窗状态
      dialogVisible: false,
      // 表单引用
      formRef: null,
      // 轮播图数据
      carouselList: [
        {
        imgUrl: "https://i1.hdslb.com/bfs/new_dyn/8a891e8e7a8cd5731c2e0ad6e21b6f1b448738328.png@1192w.jpg", // 占位图：作者特权相关
        desc: "作者特权：专属内容推荐位"
        },
        {
          imgUrl: "https://i1.hdslb.com/bfs/article/a8972251db016eb0af256b5df6a071e33493090700560683.jpg@1192w.jpg", // 占位图：流量扶持相关（注：当前链接可能存在访问权限问题，建议后续校验服务器配置）
          desc: "流量扶持：优质内容优先曝光"
        },
        {
          imgUrl: "https://i1.hdslb.com/bfs/article/b9aea15865eea5c7e6fe56c14a669c4e286431045.jpg@1192w.jpg", // 占位图：数据统计相关
          desc: "数据统计：详细的作品分析报告"
        },
        {
          imgUrl: "https://i1.hdslb.com/bfs/article/0e203aabdc81e9619708f29f3146f91b3493090700560683.jpg@1192w.jpg", // 占位图：荣誉认证相关
          desc: "荣誉认证：官方认证作者身份标识"
        },
        {
         imgUrl: "https://i1.hdslb.com/bfs/new_dyn/5e34406cdcc8316cac73a72a8fb233f13493090700560683.jpg@1192w.jpg", // 占位图：版权保护相关
          desc: "版权保护：原创作品版权维权支持"
        }       
      ],
      newsList:[
        {
            title: "抖音发布 “砥砺计划” 2025 版 专项扶持知识创作者",
            desc: "抖音在北京发布 “知识创作砥砺计划” 2025 升级版本，聚焦自然科普、前沿科技、人文社科、名校名课四大核心赛道强化激励。平台将投入专项流量资源，通过算法优化为优质内容提升曝光权重，并搭建作者成长、创作变现、出版计划三大服务体系，全年计划深度服务 1000 位优质创作者。"
        },
        {
            title: "腾讯新闻升级内容分成规则 优质创作与平台粘性成核心激励点",
            desc: "腾讯新闻近日宣布升级内容创作者收益激励规则，新方案将于 2025 年 5 月 7 日正式生效。此次调整明确将收益向 “优质内容” 与 “高粘性作者” 双重倾斜，重构了以有效流量为基础的分成计算体系。"
        },
        {
            title:"B 站推 “SUPER UP” 计划 百亿流量锚定美妆垂类创作者",
            desc:"B 站近日正式启动时尚及美妆 UP 主专项扶持计划 “SUPER UP”，计划投入百亿流量资源，重点培育 100 位优质时尚生活方式创作者。此次扶持精准瞄准垂类赛道，特别开通 “头部作者绿色通道”，面向小红书 10 万粉以上、抖音百万粉以上的博主及行业从业者提供 1V1 对接服务。"
        }
      ]
    };
  },
  mounted() {
    // 组件挂载时先从localStorage读取状态
    const savedStatus = localStorage.getItem('authorApplyStatus');
    if (savedStatus) {
      try {
        this.applyStatus = JSON.parse(savedStatus);
        // 如果本地保存的状态是0，则显示状态对话框
        if (this.applyStatus?.status === 0) {
          this.statusDialogVisible = true;
        }
      } catch (e) {
        console.error('解析本地存储的状态失败:', e);
      }
    }
    
    // 然后从服务器获取最新状态
    this.getApplyStatus();
  },
  methods: {
    // 打开申请弹窗
    submitApplication() {
      this.dialogVisible = true;
    },
    
    // 提交申请
    async handleSubmit() {
      // 验证表单
      this.$refs.formRef.validate(async (valid) => {
        if (valid) {
          try {
            // 提交数据到服务器，使用request实例自动携带token
            const response = await request.post('/user/author-apply', this.formData);
            
            console.log('提交的申请数据:', this.formData);
            
            // 提交成功后立即设置状态为0，并保存到localStorage
            this.applyStatus = { status: 0 };
            localStorage.setItem('authorApplyStatus', JSON.stringify(this.applyStatus));
            this.statusDialogVisible = true;
            
            // 关闭弹窗并重置表单
            this.dialogVisible = false;
            if (this.$refs.formRef) {
              try {
                this.$refs.formRef.resetFields();
              } catch (err) {
                console.error('重置表单失败:', err);
              }
            }
            
            // 提交后本地已设置状态为0，无需立即同步服务器状态
            // 避免服务器响应延迟导致状态覆盖
            // setTimeout(() => {
            //   this.getApplyStatus();
            // }, 2000);

          } catch (error) {
            console.error('提交申请失败:', error);
            ElMessage.error('提交申请失败，请稍后重试');
          }
        } else {
          console.log('表单验证失败');
          return false;
        }
      });
    },
    
    // 获取申请状态
    async getApplyStatus() {
      this.loading = true;
      try {
        const response = await getAuthorApplyStatusService();
        // 由于响应拦截器已直接返回data，所以直接使用response
        // 只有当response存在且status明确为0、1或2时才使用
        // 明确检查response.status是否为有效值
        if (response && response.status !== undefined && [0, 1, 2].includes(response.status)) {
          this.applyStatus = response;
          // 将服务器返回的有效状态保存到localStorage
          localStorage.setItem('authorApplyStatus', JSON.stringify(this.applyStatus));
        } else {
          // 服务器返回无效状态时，如果当前状态是0则保持不变，否则视为未申请
          if (this.applyStatus?.status !== 0) {
            this.applyStatus = { status: undefined };
            // 清除本地存储
            localStorage.removeItem('authorApplyStatus');
          }
        }
        
        console.log('申请状态:', this.applyStatus);
        
        // 根据状态进行不同处理
        if (this.applyStatus) {
          switch (this.applyStatus.status) {
            case 0:
              // 状态为0时，必须显示对话框
              this.statusDialogVisible = true;
              break;
            case 1:
              // 申请成功，启动倒计时
              this.startRedirectCountdown();
              this.statusDialogVisible = true;
              break;
            case 2:
              // 申请失败，也显示对话框
              this.statusDialogVisible = true;
              break;
          }
        }
      } catch (error) {
        console.error('获取申请状态失败:', error);
        ElMessage.error('获取申请状态失败，请稍后重试');
      } finally {
        this.loading = false;
      }
    },
    // 启动跳转倒计时
    startRedirectCountdown() {
      const that = this;
      that.redirectCountdown = 3;
      const timer = setInterval(() => {
        that.redirectCountdown--;
        if (that.redirectCountdown <= 0) {
          clearInterval(timer);
          that.$router.push('/login');
        }
      }, 1000);
    },
    
    // 取消申请
    async cancelApply() {
      try {
        await cancelAuthorApplyService();
        ElMessage.success('申请已取消');
        // 隐藏状态对话框
        this.statusDialogVisible = false;
        // 重置状态为undefined，表示未申请，并从localStorage中清除
        this.applyStatus = { status: undefined };
        localStorage.removeItem('authorApplyStatus');
        console.log('取消申请后状态:', this.applyStatus);
        
        // 为了确保状态同步，再次获取最新的申请状态
        // 这样可以保证与服务器状态一致
        setTimeout(() => {
          this.getApplyStatus();
        }, 500);
      } catch (error) {
        console.error('取消申请失败:', error);
        ElMessage.error('取消申请失败，请稍后重试');
      }
    },
    
    // 关闭状态模态框
    closeStatusDialog() {
      // 只关闭对话框，保持当前状态和localStorage中保存的状态不变
      // 即使关闭了对话框，当刷新页面或重新进入时，状态为0的弹窗仍会重新显示
      this.statusDialogVisible = false;
    },
    
    // 关闭弹窗并重置表单
    handleClose() {
      this.dialogVisible = false;
      // 在下一个事件循环重置表单，避免关闭动画问题
      setTimeout(() => {
        if (this.$refs.formRef) {
          this.$refs.formRef.resetFields();
        }
      }, 300);
    }
  }
}
</script>

<style scoped>
/* 全局容器样式 */
.author-center {
  width: 100%;
  position: relative;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  box-sizing: border-box;
  overflow: hidden;
}

/* 始终显示的遮罩层 */
  .status-overlay {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    z-index: 90;
    pointer-events: none; /* 确保点击能穿透到下面的内容 */
  }
  
  /* 动画定义 */
  @keyframes fadeIn {
    from {
      opacity: 0;
      transform: translateY(-20px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

/* 标题样式 */
.title {
  font-size: clamp(1.5rem, 5vw, 3rem);
  font-weight: bold;
  font-family: 'Montserrat', -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  font-weight: 700;
  margin: 0 auto 20px auto;
  text-align: center;
}

/* 轮播图容器 */
  .pics {
    width: 100%;
    max-width: 800px;
    margin: 0 auto;
    padding: 20px 0;
  }

  .carousel-item {
    position: relative;
    width: 100%;
    height: 100%;
    overflow: hidden;
  }

  .carousel-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    border-radius: 8px;
    /* 调整图片位置，实现更精确的裁剪效果 */
    object-position: center 30%;
  }

  .carousel-desc {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 12px;
    background: linear-gradient(transparent, rgba(0,0,0,0.7));
    color: #fff;
    font-size: 14px;
    border-radius: 0 0 8px 8px;
  }

/* 按钮区域 */
.footer {
  text-align: center;
  margin: 20px 0;
}

.btn {
  height: 48px;
  min-width: 200px;
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.3s ease;
  margin: 0 auto;
}

.btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.2);
}

/* 新闻列表容器 */
.news-container {
  padding: 20px 0;
  width: 100%;
  margin: 0 auto;
}

/* 计划标题 */
.plan-title {
  font-size: 18px;
  color: #333;
  font-weight: bold;
  margin-bottom: 16px;
  text-align: center;
}

/* 新闻滑块容器：使用弹性布局让子元素横向排列 */
.news-slider {
  display: flex; /* 弹性布局，子元素默认横向排列 */
  gap: 20px;     /* 子元素之间的间距为20px */
  /* 可选：添加溢出处理（滑块通常需要横向滚动） */
  overflow-x: auto; /* 当内容超出容器宽度时，显示水平滚动条 */
  padding: 10px 0;  /* 上下内边距，避免内容贴边 */
}

/* 新闻项样式 */
.news-item {
  padding: 5px;
  background: #f9f9f9;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
}

/* 新闻标题 */
.news_title {
  font-size: 16px;
  color: #000;
  margin-bottom: 8px;
  font-weight: 600;
}

/* 新闻描述 */
.news_desc {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}

/* 表单样式 */
.author-form {
  margin-top: 10px;
}

/* 响应式设计 */
@media (max-width: 768px) {
    .author-center {
      padding: 10px;
    }
    
    .pics {
      padding: 10px 0;
    }
    
    :deep(.el-carousel) {
      height: 250px;
    }
    
    .btn {
      width: 100%;
      max-width: 300px;
    }
    
    .news-item {
      padding: 12px;
    }
    
    .news_title {
      font-size: 15px;
    }
    
    .news_desc {
      font-size: 13px;
    }
    
    .status-modal-content {
      width: 90%;
      max-width: none;
      margin: 10vh auto;
    }
  }

/* 申请状态模态框样式 - 浮动定位 */
.status-modal-overlay {
  position: fixed;
  top: 50%;
  left: 55%;
  transform: translate(-50%, -50%);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100; /* 确保在遮罩层之上 */
  background: transparent;
  width: 100%;
  max-width: 500px;
}

.status-modal-content {
  background-color: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  width: 90%;
  max-width: 800px;
  /* 移除最大高度限制和滚动条 */
  position: relative;
  z-index: 101;
  animation: fadeIn 0.3s ease-out;
  padding-bottom: 20px;
}

.status-header {
  padding: 20px 24px 0;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 20px;
}

.status-header h3 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.close-btn {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #909399;
  padding: 4px;
  transition: color 0.3s;
}

.close-btn:hover {
  color: #606266;
}

.loading-state {
  padding: 40px 24px;
  text-align: center;
}

.loading-spinner {
  width: 40px;
  height: 40px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #409eff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 16px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.status-content {
  padding: 20px 24px 30px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.status-icon {
  text-align: center;
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.status-icon .el-icon {
  font-size: 48px;
}

.status-pending,
.status-success,
.status-rejected {
  text-align: center;
  width: 100%;
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.status-pending h2,
.status-success h2,
.status-rejected h2 {
  margin: 20px 0 10px 0;
  font-size: 20px;
  font-weight: 600;
}

.status-desc {
  margin-bottom: 20px;
  color: #606266;
}

.cancel-btn {
  margin-top: 20px;
  padding: 8px 24px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  background: #fff;
  color: #606266;
  cursor: pointer;
  transition: all 0.3s;
}

.status-icon {
  text-align: center;
  margin-bottom: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  flex: 1;
  left: 50%;
  width: 50%;
}

.status-icon .pending-icon {
  color: #409eff;
  font-size: 48px !important;
  animation: pulse 2s infinite;
  display: inline-block;
}

/* 直接针对Element Plus图标组件 */
.status-icon .el-icon {
  font-size: 48px !important;
}

.pending-icon {
  color: #409eff;
  font-size: 48px !important;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.success-icon {
  color: #67c23a;
}

.rejected-icon {
  color: #f56c6c;
}

.status-pending h2,
.status-success h2,
.status-rejected h2 {
  text-align: center;
  margin: 0 0 12px;
  font-size: 20px;
  font-weight: 600;
}

.status-desc {
  text-align: center;
  color: #606266;
  margin-bottom: 24px;
}

.cancel-btn {
  display: block;
  width: 100%;
  padding: 10px;
  background-color: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 4px;
  color: #409eff;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
}

.cancel-btn:hover {
  background-color: #409eff;
  color: white;
  border-color: #409eff;
}

.redirect-tip {
  text-align: center;
  color: #909399;
  font-size: 14px;
  margin-top: 20px;
}

.reject-reason {
  background-color: #fef0f0;
  padding: 16px;
  border-radius: 4px;
  border-left: 4px solid #f56c6c;
}

.reject-reason h4 {
  margin: 0 0 8px;
  color: #f56c6c;
  font-size: 14px;
  font-weight: 500;
}

.reject-reason p {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
}

/* 创作者激励计划样式 */
.news-container {
  margin: 2rem auto;
  padding: 1.5rem;
  max-width: 100%;
  background: linear-gradient(135deg, #fff 0%, #f9f9f9 100%);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  border: 1px solid #eaeaea;
}

.plan-title {
  display: flex;
  align-items: center;
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 2px solid #f0f0f0;
}

.plan-icon {
  font-size: 1.5rem;
  margin-right: 0.8rem;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.1); }
  100% { transform: scale(1); }
}

.plan-title h3 {
  margin: 0;
  font-size: 1.5rem;
  font-weight: 600;
  color: #333;
  background: linear-gradient(90deg, #4f46e5, #7c3aed);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

/* 优化容器样式 */
.news-container {
  margin: 2rem auto;
  padding: 2rem; /* 统一内边距 */
  max-width: 1200px;
  background: linear-gradient(135deg, #fff, #f8f9ff);
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.05);
  overflow: visible;
  border: 1px solid #eaeaea;
  position: relative;
  box-sizing: border-box; /* 确保内边距不会导致容器超出 */
}

/* 三列网格布局，更合理展示内容 */
.news-slider {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1.5rem; /* 调整间距大小 */
  justify-content: space-between; /* 确保项目均匀分布 */
  width: 100%;
  box-sizing: border-box;
  padding: 0 0.5rem; /* 为左右两侧添加额外的内边距 */
}

/* 优化新闻项样式 */
.news-item {
  background: #fff;
  padding: 2rem;
  border-radius: 10px;
  transition: all 0.3s ease;
  border: 1px solid #e0e5ff;
  box-shadow: 0 4px 12px rgba(79, 70, 229, 0.05);
  min-height: 280px;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  position: relative;
}

/* 标题行样式 */
.news_header {
  display: flex;
  align-items: center;
  margin-bottom: 1rem;
  gap: 0.6rem;
}

/* 标签样式 */
.news_tag {
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  color: white;
  padding: 0.25rem 0.75rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: 500;
  white-space: nowrap;
}

/* 标题样式 */
.news_title {
  font-size: 1.1rem;
  font-weight: 600;
  color: #333;
  margin: 0;
  transition: color 0.3s ease;
  flex: 1;
}

/* 描述样式 */
.news_desc {
  color: #666;
  font-size: 0.95rem;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 1rem;
}

/* 底部样式 */
.news-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: auto;
}

/* 按钮样式 */
.learn-more-btn {
  color: #4f46e5;
  border-color: #4f46e5;
  font-size: 0.875rem;
  padding: 0.25rem 1rem;
}

.learn-more-btn:hover {
  background-color: #4f46e5;
  color: white;
}

/* 卡片悬停效果 */
.news-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 12px 24px rgba(79, 70, 229, 0.12);
  border-color: #4f46e5;
}

.news-item:hover .news_title {
  color: #4f46e5;
}

/* 优化响应式设计 */
@media (max-width: 1024px) {
  .news-slider {
    grid-template-columns: repeat(2, 1fr);
    gap: 1.2rem;
  }
  .news-container {
    margin: 1.5rem auto;
    padding: 0 1rem;
  }
}

@media (max-width: 768px) {
  .news-container {
    margin: 1rem;
    padding: 1.2rem;
  }
  
  .plan-title h3 {
    font-size: 1.3rem;
  }
  
  .news-slider {
    grid-template-columns: 1fr;
    gap: 1.2rem;
  }
  
  .news-item {
    min-height: 220px;
    padding: 1.2rem;
  }
}

/* 确保整个页面没有滚动条 */
:deep(.el-carousel) {
  overflow: visible;
}

:deep(.el-carousel__container) {
  overflow: visible;
}

/* 轮播图样式调整 */
:deep(.el-carousel__item) {
  height: 100%;
}
:deep(.el-carousel__item--card) {
  width: 60%;
}
:deep(.el-carousel__item--card.is-active) {
  width: 80%;
}
</style>