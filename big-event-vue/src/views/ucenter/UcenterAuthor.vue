<template>
  <div class="author-center">
    <div class="becomeAuthor">
        <div class="title">
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
            <div class="plan-title">创作者激励计划</div>
            <!-- 轮播图组件 -->
            <div class="news-slider">
              <div v-for="(item, index) in newsList" :key="index" class="news-item">
                <div class="news_title">{{ item.title }}</div>
                <div class="news_desc">{{ item.desc }}</div>
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
          <el-input 
            v-model="formData.applyDesc" 
            type="textarea" 
            :rows="4"
            placeholder="请简要描述您的创作经验和特长（10-200字）"
            maxlength="200"
            show-word-limit
          />
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
import { WarningFilled } from '@element-plus/icons-vue';
// 引入Element Plus组件
import { ElCarousel, ElCarouselItem, ElDialog, ElForm, ElFormItem, ElInput, ElButton } from 'element-plus';
// 引入Element Plus样式
import 'element-plus/dist/index.css';

export default {
  components: { 
    WarningFilled,
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
        applyDesc: ''
      },
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
        },{
            title:"京东上线 “长阶计划” 亿级资源构建达人成长体系",
            desc:"京东在首届商家内容大会上推出达人专项扶持 “长阶计划”，以 10 亿流量倾斜与亿级现金奖励，打造 Top100 达人矩阵。该计划构建全周期成长路径，针对不同阶段创作者设计分层激励：新人达人月完成 100 单即可获现金奖励，门槛仅为行业平均水平的五分之一。"
        }
      ]
    };
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
            
            // 模拟成功响应
            console.log('提交的申请数据:', this.formData);
            
            // 显示成功消息
            this.$message.success('申请提交成功，等待审核');

            // 关闭弹窗
            this.dialogVisible = false;
            
            // 重置表单
            this.$refs.formRef.resetFields();
          } catch (error) {
            console.error('提交申请失败:', error);
            this.$message.error('提交申请失败，请稍后重试');
          }
        } else {
          console.log('表单验证失败');
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
}
</script>

<style scoped>
/* 全局容器样式 */
.author-center {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
  box-sizing: border-box;
  overflow: hidden;
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
  padding: 10px 0;
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

/* 新闻滑块容器 */
.news-slider {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 新闻项样式 */
.news-item {
  padding: 15px;
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