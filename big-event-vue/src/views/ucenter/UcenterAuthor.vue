<template>
  <div class="author-center">
    <div class="becomeAuthor">
        <div class="title">
          成为作者Ciallo～(∠・ω&lt; )⌒☆
        </div>
    </div>
      <div class="pics">
        <!-- 轮播图组件 -->
        <el-carousel :interval="3000" type="card" height="200px">
          <el-carousel-item v-for="item in carouselList" :key="item.id">
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
            <div class="more" title="创作者激励计划" v-for="item in newsList" :key="item.id">
            <div class="news_title">{{ item.title }}</div>
            <div class="news">{{ item.desc }}</div>
            </div>
    </div>
</div>
</template>

<script>
import axios from 'axios';
import { WarningFilled } from '@element-plus/icons-vue';
// 引入Element Plus轮播组件
import { ElCarousel, ElCarouselItem } from 'element-plus';
// 引入Element Plus样式
import 'element-plus/dist/index.css';

export default {
  components: { 
    WarningFilled,
    ElCarousel,
    ElCarouselItem
  },
  data() {
    return {
      realName: "真实姓名",
      idCard: "身份证号",
      applyDesc: "申请描述",
      code: 200,
      message: "申请提交成功，等待审核",
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
        }
      ]
    };
  },
  methods: {
    async fetchUserInfo() {
      const response = await axios.post('/author/apply');
      this.code = response.code;
      this.message = response.message;
    }
  }
}
</script>

<style scoped>
.title {
  font-size: clamp(1.5rem, 5vw, 3rem); /* 响应式字体大小 */
  font-weight: bold;
  font-family: 'Montserrat', -apple-system, BlinkMacSystemFont, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
  font-weight: 700;
  margin: 0 auto; /* 水平居中 */
  max-width: 90%; /* 限制最大宽度，避免在大屏幕上过宽 */
  text-align: center; /* 文本内容本身也居中（可选） */

}

.pics {
  width: 100%;
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 0;
}

.carousel-item {
  position: relative;
  width: 100%;
  height: 100%;
}

.carousel-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
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

.footer {
  text-align: center;
  margin-top: 20px;
}

.btn {
    height: 48px;
    width: 400px;
  padding: 12px 24px;
  font-size: 16px;
  font-weight: 500;
  border-radius: 8px;
  transition: all 0.3s ease;
}

.btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 8px rgba(0,0,0,0.2);
}

/* 新闻列表容器，设置整体内边距和背景等 */
.news-container {
  padding: 20px;
  display: flex;
  gap: 20px; /* 控制两个新闻项之间的水平间距 */
  flex-wrap: wrap; /* 当屏幕宽度不足时，允许换行，保证响应式 */
}
/* 每个新闻项的容器，设置底部外边距，区分不同新闻 */
.more {
  margin-bottom: 30px;
  width: calc(50% - 10px); /* 每个新闻项宽度为50%减去间距 */
}
/* “创作者激励计划” 标题样式，设置字体大小、颜色等 */
.plan-title {
  font-size: 18px;
  color: #333;
  font-weight: bold;
  margin-bottom: 8px;
}
/* 新闻标题样式，突出显示 */
.news_title {
  font-size: 16px;
  color: #000;
  margin-bottom: 6px;
}
/* 新闻描述样式，设置行高，增强可读性 */
.news {
  font-size: 14px;
  color: #666;
  line-height: 1.6;

}
</style>