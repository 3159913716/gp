<script setup>
import { ref, onMounted } from 'vue'
import { ElTabs, ElTabPane, ElCard, ElAvatar, ElPagination, ElTag } from 'element-plus'
import { articleListService } from '@/api/article.js'

console.log('HomePage.vue 组件加载')

// 排序方式：最新或热门
const activeSort = ref('latest')

// 文章列表数据
const articles = ref([])

// 热门分类数据（模拟）
const hotCategories = ref([
  { id: 1, categoryName: '技术资讯', articleCount: 128 },
  { id: 2, categoryName: '行业动态', articleCount: 96 },
  { id: 3, categoryName: '经验分享', articleCount: 85 },
  { id: 4, categoryName: '教程学习', articleCount: 72 }
])

// 最新文章数据（模拟）
const latestArticles = ref([
  { id: 101, title: 'Vue 3 Composition API 实战技巧', createTime: '2024-01-20' },
  { id: 102, title: 'React 18 新特性深度解析', createTime: '2024-01-19' },
  { id: 103, title: 'Node.js 性能优化指南', createTime: '2024-01-18' },
  { id: 104, title: '前端工程化最佳实践', createTime: '2024-01-17' },
  { id: 105, title: 'TypeScript 进阶教程', createTime: '2024-01-16' }
])

// 分页数据
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 加载文章列表
const loadArticles = async () => {
  try {
    console.log('加载文章列表，排序方式:', activeSort.value)
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      // 根据排序方式调整参数
      orderBy: activeSort.value === 'latest' ? 'createTime' : 'likeCount',
      orderType: activeSort.value === 'latest' ? 'desc' : 'desc'
    }
    
    // 由于是模拟环境，我们使用模拟数据
    // const response = await articleListService(params)
    // articles.value = response.data.items
    // total.value = response.data.total
    
    // 模拟数据
    articles.value = generateMockArticles(params.pageNum, params.pageSize)
    total.value = 120 // 模拟总条数
    console.log('文章列表加载完成，共', articles.value.length, '条数据')
  } catch (error) {
    console.error('加载文章列表失败:', error)
  }
}

// 生成模拟文章数据
const generateMockArticles = (pageNum, pageSize) => {
  const mockArticles = []
  const startId = (pageNum - 1) * pageSize + 1
  
  for (let i = 0; i < pageSize; i++) {
    const id = startId + i
    mockArticles.push({
      id,
      title: `大事件资讯第${id}期 - 前端开发技术前沿动态`,
      coverImg: id % 3 === 0 ? '@/assets/cover.jpg' : (id % 3 === 1 ? '@/assets/logo.png' : '@/assets/default.png'),
      content: '这是一篇关于前端开发技术的精彩文章，包含了最新的技术动态、实战经验分享和行业趋势分析...',
      categoryId: (id % 4) + 1,
      categoryName: ['技术资讯', '行业动态', '经验分享', '教程学习'][id % 4],
      author: `作者${id % 10 + 1}`,
      avatar: '@/assets/avatar.jpg',
      createTime: `2024-01-${20 - id % 15}`,
      readCount: Math.floor(Math.random() * 1000) + 100,
      likeCount: Math.floor(Math.random() * 200) + 10,
      commentCount: Math.floor(Math.random() * 50) + 5
    })
  }
  
  return mockArticles
}

// 处理排序切换
const handleSortChange = () => {
  pageNum.value = 1 // 重置到第一页
  loadArticles()
}

// 处理分页变化
const handlePageChange = (num, size) => {
  pageNum.value = num
  pageSize.value = size
  loadArticles()
}

// 跳转到文章详情
const goToArticleDetail = (articleId) => {
  window.location.href = `#/article/${articleId}`
}

// 跳转到分类页面
const goToCategory = (categoryId) => {
  window.location.href = `#/category/${categoryId}`
}

onMounted(() => {
  loadArticles()
})
</script>

<template>
  <div class="home-page">
    <div class="content-wrapper">
      <!-- 左侧文章列表 -->
      <div class="article-list-container">
        <!-- 排序选项卡 -->
        <ElTabs v-model="activeSort" @tab-click="handleSortChange" class="sort-tabs">
          <ElTabPane label="最新文章" name="latest"></ElTabPane>
          <ElTabPane label="热门文章" name="hot"></ElTabPane>
        </ElTabs>
        
        <!-- 文章列表 -->
        <div class="articles">
          <ElCard 
            v-for="article in articles" 
            :key="article.id" 
            class="article-card"
            @click="goToArticleDetail(article.id)"
          >
            <div class="article-header">
              <h3 class="article-title">{{ article.title }}</h3>
              <ElTag 
                :effect="'light'" 
                class="category-tag"
                @click.stop="goToCategory(article.categoryId)"
              >
                {{ article.categoryName }}
              </ElTag>
            </div>
            
            <div class="article-content">
              <div class="article-cover" v-if="article.coverImg">
                <img :src="article.coverImg" :alt="article.title" class="cover-img">
              </div>
              <div class="article-summary">
                {{ article.content }}
                <span class="read-more">阅读全文</span>
              </div>
            </div>
            
            <div class="article-meta">
              <div class="author-info">
                <ElAvatar :src="article.avatar" size="small" class="author-avatar"></ElAvatar>
                <span class="author-name">{{ article.author }}</span>
                <span class="publish-time">{{ article.createTime }}</span>
              </div>
              <div class="article-stats">
                <span class="stat-item">
                  <i class="el-icon-view"></i>
                  {{ article.readCount }}
                </span>
                <span class="stat-item">
                  <i class="el-icon-thumb-up"></i>
                  {{ article.likeCount }}
                </span>
                <span class="stat-item">
                  <i class="el-icon-comment"></i>
                  {{ article.commentCount }}
                </span>
              </div>
            </div>
          </ElCard>
        </div>
        
        <!-- 分页 -->
        <div class="pagination">
          <ElPagination
            v-model:current-page="pageNum"
            v-model:page-size="pageSize"
            :page-sizes="[5, 10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handlePageChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
      
      <!-- 右侧边栏 -->
      <aside class="sidebar">
        <!-- 热门分类 -->
        <div class="sidebar-section">
          <h3 class="section-title">热门分类</h3>
          <div class="category-list">
            <div 
              v-for="category in hotCategories" 
              :key="category.id"
              class="category-item"
              @click="goToCategory(category.id)"
            >
              <span class="category-name">{{ category.categoryName }}</span>
              <span class="article-count">{{ category.articleCount }} 篇</span>
            </div>
          </div>
        </div>
        
        <!-- 最新文章 -->
        <div class="sidebar-section">
          <h3 class="section-title">最新文章</h3>
          <div class="latest-articles">
            <div 
              v-for="article in latestArticles" 
              :key="article.id"
              class="latest-article-item"
              @click="goToArticleDetail(article.id)"
            >
              <span class="article-title">{{ article.title }}</span>
              <span class="publish-date">{{ article.createTime }}</span>
            </div>
          </div>
        </div>
        
        <!-- 关于我们 -->
        <div class="sidebar-section">
          <h3 class="section-title">关于我们</h3>
          <div class="about-us">
            <img src="@/assets/logo2.png" alt="大事件" class="about-logo">
            <p class="about-desc">
              大事件资讯是一个专注于技术领域的内容分享平台，
              提供最新的技术动态、行业趋势和实战经验。
            </p>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  padding: 20px 0;
}

.content-wrapper {
  display: flex;
  gap: 30px;
}

/* 文章列表容器 */
.article-list-container {
  flex: 1;
}

/* 排序选项卡 */
.sort-tabs {
  margin-bottom: 20px;
  background: none;
}

.sort-tabs .el-tabs__nav {
  padding-left: 0;
}

.sort-tabs .el-tabs__item {
  font-size: 18px;
  font-weight: 500;
  color: #606266;
}

.sort-tabs .el-tabs__item.is-active {
  color: #1890ff;
}

.sort-tabs .el-tabs__active-bar {
  background-color: #1890ff;
  height: 3px;
}

/* 文章卡片 */
.article-card {
  margin-bottom: 20px;
  transition: all 0.3s;
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
}

.article-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.1);
}

.article-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.article-title {
  font-size: 20px;
  font-weight: 500;
  color: #303133;
  margin: 0;
  flex: 1;
  line-height: 1.4;
}

.category-tag {
  margin-left: 15px;
}

.article-content {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

.article-cover {
  flex-shrink: 0;
  width: 200px;
  height: 130px;
  overflow: hidden;
  border-radius: 6px;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.article-card:hover .cover-img {
  transform: scale(1.05);
}

.article-summary {
  flex: 1;
  color: #606266;
  line-height: 1.7;
  font-size: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.read-more {
  color: #1890ff;
  margin-left: 5px;
  font-weight: 500;
}

.article-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
}

.author-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.author-name {
  font-size: 14px;
  color: #606266;
}

.publish-time {
  font-size: 14px;
  color: #909399;
}

.article-stats {
  display: flex;
  gap: 20px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 14px;
  color: #909399;
}

/* 分页 */
.pagination {
  margin-top: 40px;
  text-align: center;
}

/* 侧边栏 */
.sidebar {
  width: 320px;
  flex-shrink: 0;
}

.sidebar-section {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.section-title {
  font-size: 18px;
  font-weight: 500;
  color: #303133;
  margin: 0 0 20px 0;
  padding-bottom: 10px;
  border-bottom: 2px solid #1890ff;
}

/* 热门分类 */
.category-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.category-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background-color: #f5f7fa;
  border-radius: 6px;
  transition: all 0.3s;
  cursor: pointer;
}

.category-item:hover {
  background-color: #ecf5ff;
  color: #1890ff;
}

.category-name {
  font-size: 14px;
  color: #606266;
}

.article-count {
  font-size: 12px;
  color: #909399;
  background-color: rgba(0, 0, 0, 0.05);
  padding: 2px 8px;
  border-radius: 10px;
}

.category-item:hover .article-count {
  background-color: rgba(24, 144, 255, 0.1);
  color: #1890ff;
}

/* 最新文章 */
.latest-articles {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.latest-article-item {
  cursor: pointer;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0f0f0;
  transition: all 0.3s;
}

.latest-article-item:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.latest-article-item:hover {
  transform: translateX(5px);
}

.latest-article-item .article-title {
  font-size: 14px;
  color: #303133;
  margin: 0 0 8px 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.publish-date {
  font-size: 12px;
  color: #909399;
}

/* 关于我们 */
.about-us {
  text-align: center;
}

.about-logo {
  width: 80px;
  height: 80px;
  margin-bottom: 15px;
}

.about-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .content-wrapper {
    flex-direction: column;
  }
  
  .sidebar {
    width: 100%;
    display: flex;
    gap: 20px;
  }
  
  .sidebar-section {
    flex: 1;
    margin-bottom: 0;
  }
}

@media (max-width: 768px) {
  .article-content {
    flex-direction: column;
  }
  
  .article-cover {
    width: 100%;
    height: 200px;
  }
  
  .sidebar {
    flex-direction: column;
  }
  
  .article-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .category-tag {
    margin-left: 0;
  }
  
  .article-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>