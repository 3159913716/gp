<template>
  <div class="author-center">
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
        
        <el-form-item label="联系电话" prop="phone">
          <el-input 
            v-model="formData.phone" 
            placeholder="请输入您的联系电话"
            maxlength="11"
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
  </div>
</template>

<script>
import request from '@/utils/request';
// 引入Element Plus组件
import { ElCarousel, ElCarouselItem, ElDialog, ElForm, ElFormItem, ElInput, ElButton } from 'element-plus';
// 单独引入ElMessage工具函数
import { ElMessage } from 'element-plus';
// 引入Vue Router
import { useRouter } from 'vue-router';
// 引入Token Store用于认证
import { useTokenStore } from '@/stores/token';

export default {
  components: {
    ElCarousel,
    ElCarouselItem,
    ElDialog,
    ElForm,
    ElFormItem,
    ElInput,
    ElButton
  },
  data() {
    return {
      // 表单数据
      formData: {
        realName: '',
        idCard: '',
        phone: '',
        applyDesc: ''
      },
      // 申请状态信息 - 默认值为undefined
      applyStatus: {status: undefined},
      // 加载状态
      loading: false,
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
        phone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号码', trigger: 'blur' }
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
        imgUrl: "https://article.biliimg.com/bfs/article/a458ab2123afad361c051906c8d465a27c40a3e3.jpg", 
        desc: "作者特权：专属内容推荐位"
        },
        {
          imgUrl: "https://i1.hdslb.com/bfs/article/a8972251db016eb0af256b5df6a071e33493090700560683.jpg@1192w.jpg", 
          desc: "流量扶持：优质内容优先曝光"
        },
        {
          imgUrl: "https://i1.hdslb.com/bfs/article/b9aea15865eea5c7e6fe56c14a669c4e286431045.jpg@1192w.jpg", 
          desc: "数据统计：详细的作品分析报告"
        },
        {
          imgUrl: "https://i1.hdslb.com/bfs/article/0e203aabdc81e9619708f29f3146f91b3493090700560683.jpg@1192w.jpg", 
          desc: "荣誉认证：官方认证作者身份标识"
        },
        {
         imgUrl: "https://i1.hdslb.com/bfs/new_dyn/5e34406cdcc8316cac73a72a8fb233f13493090700560683.jpg@1192w.jpg", 
          desc: "版权保护：原创作品版权维权支持"
        }        
      ],
      newsList:[
        {
            title: "抖音发布 \"砥砺计划\" 2025 版 专项扶持知识创作者",
            desc: "抖音在北京发布 \"知识创作砥砺计划\" 2025 升级版本，聚焦自然科普、前沿科技、人文社科、名校名课四大核心赛道强化激励。平台将投入专项流量资源，通过算法优化为优质内容提升曝光权重，并搭建作者成长、创作变现、出版计划三大服务体系，全年计划深度服务 1000 位优质创作者。"
        },
        {
            title: "腾讯新闻升级内容分成规则 优质创作与平台粘性成核心激励点",
            desc: "腾讯新闻近日宣布升级内容创作者收益激励规则，新方案将于 2025 年 5 月 7 日正式生效。此次调整明确将收益向 \"优质内容\" 与 \"高粘性作者\" 双重倾斜，重构了以有效流量为基础的分成计算体系。"
        },
        {
            title:"B 站推 \"SUPER UP\" 计划 百亿流量锚定美妆垂类创作者",
            desc:"B 站近日启动时尚及美妆 UP 主专项扶持计划 \"SUPER UP\"，计划投入百亿流量资源，重点培育 100 位优质时尚生活方式创作者。此次扶持精准瞄准垂类赛道，特别开通 \"头部作者绿色通道\"，面向小红书 10 万粉以上、抖音百万粉以上的博主及行业从业者提供 1V1 对接服务。"
        }
      ]
    };
  },
  mounted() {
    // 初始化默认状态
    this.applyStatus = { status: undefined };
    
    // 组件挂载时检查是否有申请状态，如果有则跳转到状态页面
    this.checkAndRedirectToStatus();
  },
  methods: {
    // 检查申请状态并跳转
    async checkAndRedirectToStatus() {
      const tokenStore = useTokenStore();
      try {
        const headers = {};
        if (tokenStore.token) {
          const raw = tokenStore.token;
          headers.Authorization = raw.startsWith('Bearer ') ? raw : `Bearer ${raw}`;
        }
        
        const response = await request.get('/user/author-apply/status', { headers });
        const data = response?.data || response;
        
        // 处理响应，检查是否有申请状态
        let statusValue;
        if (response?.code === 0 || response?.code === 200) {
          if (response.data && response.data !== null) {
            statusValue = response.data?.status;
          }
        } else if (data) {
          statusValue = data?.status;
        }
        
        statusValue = statusValue !== undefined ? Number(statusValue) : undefined;
        
        // 如果有有效的申请状态，直接跳转到状态页面
        if (statusValue !== undefined && !isNaN(statusValue) && [0, 1, 2].includes(statusValue)) {
          // 保存状态到localStorage供State.vue使用
          const statusData = {
            status: statusValue,
            rejectReason: response.data?.rejectReason || data?.rejectReason || data?.reason || data?.reject_reason
          };
          localStorage.setItem('authorApplyStatus', JSON.stringify(statusData));
          
          // 使用this.$router替代router
          this.$router.push('/user/author/status'); // 正确的状态页面路径
        }
      } catch (error) {
        console.error('检查申请状态失败:', error);
        // 错误情况下不进行跳转，保持在当前页面
      }
    },
    
    // 打开申请弹窗
    submitApplication() {
      this.dialogVisible = true;
    },
    
    // 提交申请
    async handleSubmit() {
      // 无论成功与否，用户提交后都应该跳转到状态页面查看状态
      // 状态页面会通过API获取最新的申请状态
      // 验证表单
      this.$refs.formRef.validate(async (valid) => {
        if (valid) {
          try {
            const submitData = {
              realName: String(this.formData.realName || ''),
              idCard: String(this.formData.idCard || ''),
              phone: String(this.formData.phone || ''),
              applyDesc: String(this.formData.applyDesc || '')
            };
            
            console.log('提交的字段:', Object.keys(submitData));
            console.log('提交的数据:', submitData);
            
            const response = await request.post('/user/author-apply', submitData);
            console.log('服务器返回的响应:', response);
            
            // 检查响应是否表示成功
            if (response?.code === 0 || response?.code === 200) {
              // 提交成功后设置状态为0，并保存到localStorage
              const statusData = { status: 0 };
              localStorage.setItem('authorApplyStatus', JSON.stringify(statusData));
              
              // 关闭弹窗
              this.dialogVisible = false;
              if (this.$refs.formRef) {
                try {
                  this.$refs.formRef.resetFields();
                } catch (err) {
                  console.error('重置表单失败:', err);
                }
              }
              
              // 跳转到状态页面
            this.$router.push('/user/author/status'); // 修改为正确的路径
            } else {
              console.log('服务器返回错误:', response?.message || '未知错误');
              ElMessage.error(response?.message || '提交申请失败，请稍后重试');
            }
          } catch (error) {
            console.error('提交申请失败:', error);
            
            if (error.response) {
              console.log('错误响应数据:', error.response.data);
              
              if (error.response.data?.code === 1 && 
                  error.response.data?.message && 
                  (error.response.data.message.includes('身份证') || error.response.data.message.includes('格式不正确'))) {
                ElMessage.error(error.response.data.message);
              } else {
                ElMessage.error(error.response.data?.message || '提交申请失败，请稍后重试');
              }
            } else {
              ElMessage.error('网络连接失败，请检查您的网络连接');
            }
          }
        } else {
          return false;
        }
      });
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
};
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

.becomeAuthor {
  text-align: center;
  margin-bottom: 30px;
  padding: 20px 0;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 10px;
  color: white;
}

.title {
  font-weight: bold;
  margin: 0;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
}

.pics {
  margin-bottom: 30px;
}

.carousel-item {
  position: relative;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
}

.carousel-img {
  width: 100%;
  height: 150px;
  object-fit: cover;
  border-radius: 8px;
  margin-bottom: 10px;
}

.carousel-desc {
  text-align: center;
  font-weight: 500;
  color: #333;
}

.footer {
  text-align: center;
  margin-bottom: 40px;
}

.btn {
  padding: 12px 40px;
  font-size: 18px;
}

.news-container {
  background: #f5f7fa;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.plan-title {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 10px;
  border-bottom: 2px solid #409eff;
}

.plan-icon {
  font-size: 24px;
  margin-right: 10px;
}

.plan-title h3 {
  margin: 0;
  color: #333;
}

.news-slider {
  display: flex;
  gap: 20px;
  flex-wrap: wrap;
}

.news-item {
  flex: 1;
  min-width: 300px;
  background: white;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.news_header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.news_tag {
  background: #409eff;
  color: white;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  margin-right: 10px;
}

.news_title {
  font-weight: 600;
  color: #333;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.news_desc {
  color: #606266;
  line-height: 1.6;
  margin-bottom: 15px;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.news-footer {
  text-align: right;
}

.learn-more-btn {
  padding: 6px 16px;
}

/* 表单样式 */
.author-form {
  margin-bottom: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .news-slider {
    flex-direction: column;
  }
  
  .news-item {
    min-width: 100%;
  }
}
</style>