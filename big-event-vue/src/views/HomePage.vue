<script setup>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElTabs, ElTabPane, ElCard, ElAvatar, ElPagination, ElTag } from 'element-plus'
import articleHomeApi from '@/api/articlehome.js'
import defaultCover from '@/assets/default.png'
import coverImgAsset from '@/assets/cover.jpg'
import logoImgAsset from '@/assets/logo.png'
import avatarImgAsset from '@/assets/avatar.jpg'
import { articleCategoryListService } from '@/api/article.js'

console.log('HomePage.vue 组件加载')

// 排序方式：最新或热门
const activeSort = ref('latest')

// 文章列表数据
const articles = ref([])
// 当前选择的分类ID（从路由读取）
const selectedCategoryId = ref(null)
// 新增：搜索关键词（从路由读取）
const selectedKeyword = ref('')

// 热门分类数据（模拟）
const hotCategories = ref([])
// 默认分类兜底数据（请求失败时使用）
const defaultCategories = [
  { id: 1, categoryName: '技术资讯', categoryAlias: 'tech' },
  { id: 2, categoryName: '行业动态', categoryAlias: 'industry' },
  { id: 3, categoryName: '经验分享', categoryAlias: 'experience' },
  { id: 4, categoryName: '教程学习', categoryAlias: 'tutorial' }
]

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

// 生成模拟文章数据（兜底展示）
const generateMockArticles = (page, size) => {
  const mockArticles = []
  const startId = (page - 1) * size + 1
  for (let i = 0; i < size; i++) {
    const id = startId + i
    mockArticles.push({
      id,
      title: `大事件资讯第${id}期 - 前端开发技术前沿动态`,
      coverImg: id % 3 === 0 ? coverImgAsset : (id % 3 === 1 ? logoImgAsset : defaultCover),
      content: '这是一篇关于前端开发技术的精彩文章，包含了最新的技术动态、实战经验分享和行业趋势分析...',
      categoryId: (id % 4) + 1,
      categoryName: ['技术资讯', '行业动态', '经验分享', '教程学习'][id % 4],
      author: `作者${id % 10 + 1}`,
      avatar: avatarImgAsset,
      createTime: `2024-01-${String(20 - (id % 15)).padStart(2, '0')}`,
      // 移除阅读量相关字段
      likeCount: Math.floor(Math.random() * 200) + 10,
      commentCount: Math.floor(Math.random() * 50) + 5
    })
  }
  return mockArticles
}

// 根据当前排序选项对文章列表进行排序
const sortArticles = (list) => {
  if (!Array.isArray(list)) return []
  if (activeSort.value === 'hot') {
    // 热门：按点赞数从大到小
    return [...list].sort((a, b) => Number(b.likeCount || 0) - Number(a.likeCount || 0))
  }
  // 最新：按发布时间从新到旧
  return [...list].sort((a, b) => {
    const ta = new Date(a.createTime || 0).getTime()
    const tb = new Date(b.createTime || 0).getTime()
    return tb - ta
  })
}

// 加载文章列表（接入真实接口，失败兜底为模拟数据）
const loadArticles = async () => {
  try {
    const keyword = selectedKeyword.value?.trim()
    // 1) 搜索模式：优先根据关键词检索
    if (keyword) {
      console.log('搜索模式，关键词:', keyword)
      const res = await articleHomeApi.searchArticles({ keyword, page: pageNum.value, pageSize: pageSize.value, state: '已发布' })
      const payload = res?.data ?? res
      const list = Array.isArray(payload?.list)
        ? payload.list
        : (Array.isArray(payload?.items) ? payload.items : (Array.isArray(payload?.item) ? payload.item : []))
      const mapped = list.map(item => ({
        id: item.id,
        title: item.title,
        content: item.content,
        categoryName: item.categoryName ?? item.category_name ?? '',
        categoryId: item.categoryId ?? item.category_id ?? null,
        coverImg: item.coverImg ?? item.cover_img ?? defaultCover,
        author: item.author?.username ?? item.authorName ?? (item.create_user ? `作者${item.create_user}` : ''),
        avatar: item.author?.avatar ?? item.authorAvatar ?? '',
        createTime: item.createTime ?? item.create_time ?? '',
        // 移除阅读量：viewCount/read_count
        likeCount: item.likeCount ?? item.like_count ?? 0,
        commentCount: item.commentCount ?? item.comment_count ?? 0
      }))
      const sorted = sortArticles(mapped)
      articles.value = sorted
      total.value = Number(payload?.total ?? sorted.length)
      console.log('搜索结果加载完成，共', articles.value.length, '条数据')
      return
    }

    // 2) 常规模式：按分类或默认列表
    console.log('加载文章列表，排序方式:', activeSort.value, '分类ID:', selectedCategoryId.value)
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      // 分类筛选（若未选择则不传）
      categoryId: selectedCategoryId.value ?? undefined,
      // 强制首页最新文章只显示已发布内容（与角色无关）
      state: '已发布'
    }

    const res = await articleHomeApi.getHomeArticles(params)
    // 兼容不同返回结构：{ code, data: { item | items, total } }
    const payload = res?.data ?? res
    const list = Array.isArray(payload?.item)
      ? payload.item
      : (Array.isArray(payload?.items) ? payload.items : [])

    // 将接口字段映射到现有渲染结构（兼容驼峰/下划线）
    const mapped = list.map(item => ({
      id: item.id,
      title: item.title,
      content: item.content,
      categoryName: item.categoryName ?? item.category_name ?? '',
      categoryId: item.categoryId ?? item.category_id ?? null,
      coverImg: item.coverImg ?? item.cover_img ?? defaultCover,
      author: item.author?.username ?? item.authorName ?? (item.create_user ? `作者${item.create_user}` : ''),
      avatar: item.author?.avatar ?? item.authorAvatar ?? '',
      createTime: item.createTime ?? item.create_time ?? '',
      // 移除阅读量字段
      likeCount: item.likeCount ?? item.like_count ?? 0,
      commentCount: item.commentCount ?? item.comment_count ?? 0
    }))

    // 本地兜底筛选（后端若未按分类过滤）
    const filtered = selectedCategoryId.value != null
      ? mapped.filter(a => a.categoryId === selectedCategoryId.value)
      : mapped

    const sorted = sortArticles(filtered)
    articles.value = sorted
    total.value = Number(payload?.total ?? sorted.length)
    console.log('文章列表加载完成，共', articles.value.length, '条数据')
  } catch (error) {
    console.error('加载文章列表失败，切换到模拟数据:', error?.message || error)
    // 兜底：使用本地预设的模拟数据展示
    let mock = generateMockArticles(pageNum.value, pageSize.value)
    const keyword = selectedKeyword.value?.trim()
    if (keyword) {
      mock = mock.filter(a => String(a.title).includes(keyword) || String(a.content).includes(keyword))
    } else if (selectedCategoryId.value != null) {
      mock = mock.filter(a => a.categoryId === selectedCategoryId.value)
    }
    const sorted = sortArticles(mock)
    articles.value = sorted
    total.value = 120
  }
}

// 处理排序切换
const handleSortChange = () => {
  pageNum.value = 1 // 重置到第一页
  loadArticles()
}

// 分页事件分别处理
const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadArticles()
}

const handleCurrentChange = (num) => {
  pageNum.value = num
  loadArticles()
}

// 路由实例
const router = useRouter()
const route = useRoute()

// 同步路由中的分类ID到本地状态
const syncCategoryFromRoute = () => {
  const raw = route.params?.id ?? route.query?.categoryId
  const num = Number(raw)
  selectedCategoryId.value = Number.isFinite(num) ? num : null
}
// 新增：同步路由中的搜索关键词到本地状态
const syncKeywordFromRoute = () => {
  selectedKeyword.value = String(route.query?.keyword || '').trim()
}
// 监听路由中分类ID变化，保持首页最新文章模块按分类展示
watch(() => route.params.id, () => {
  syncCategoryFromRoute()
  // 分类切换时清除搜索状态
  selectedKeyword.value = ''
  activeSort.value = 'latest'
  pageNum.value = 1
  loadArticles()
})
// 同时监听查询参数中的categoryId（兼容从其他位置跳转）
watch(() => route.query.categoryId, () => {
  syncCategoryFromRoute()
  // 清除搜索状态
  selectedKeyword.value = ''
  activeSort.value = 'latest'
  pageNum.value = 1
  loadArticles()
})
// 新增：监听搜索关键词变化，按关键词检索并展示
watch(() => route.query.keyword, () => {
  syncKeywordFromRoute()
  activeSort.value = 'latest'
  pageNum.value = 1
  loadArticles()
})

// 跳转到文章详情
const goToArticleDetail = (articleId) => {
  router.push(`/article/${articleId}`)
}

// 跳转到分类页面（无ID时忽略）
const goToCategory = (categoryId) => {
  if (!categoryId && categoryId !== 0) return
  router.push(`/category/${categoryId}`)
}

// 加载右侧“最新文章”板块（仅标题与创建日期）
const loadLatestArticles = async () => {
  try {
    const res = await articleHomeApi.getHomeArticles({ pageNum: 1, pageSize: 5, state: '已发布' })
    const payload = res?.data ?? res
    const list = Array.isArray(payload?.item)
      ? payload.item
      : (Array.isArray(payload?.items) ? payload.items : (Array.isArray(payload?.list) ? payload.list : []))

    latestArticles.value = list.map(item => ({
      id: item.id,
      title: item.title,
      createTime: item.createTime ?? item.create_time ?? ''
    }))
  } catch (error) {
    console.error('加载最新文章失败:', error?.message || error)
    latestArticles.value = []
  }
}
// 加载右侧热门分类（只取前4个）
const loadHotCategories = async () => {
  try {
    const res = await articleCategoryListService()
    const payload = res?.data ?? res
    const list = Array.isArray(payload?.items)
      ? payload.items
      : (Array.isArray(payload?.list) ? payload.list : (Array.isArray(payload) ? payload : []))
    hotCategories.value = list.slice(0, 4).map(c => ({
      id: c.id,
      categoryName: c.categoryName ?? c.categoryName ?? '',
      categoryAlias: c.categoryAlias ?? c.category_alias ?? ''
      // articleCount 可选：若后端提供则展示
    }))
  } catch (e) {
    console.error('加载热门分类失败:', e?.message || e)
    // 使用默认分类兜底展示
    hotCategories.value = defaultCategories.slice(0,4)
  }
}
// 返回顶部按钮显示控制
const showBackToTop = ref(false)
const handleScroll = () => {
  const scrollTop = window.pageYOffset || document.documentElement.scrollTop || document.body.scrollTop || 0
  const windowHeight = window.innerHeight || document.documentElement.clientHeight
  const docHeight = Math.max(document.documentElement.scrollHeight, document.body.scrollHeight)
  // 接近页面底部时显示返回顶部按钮（阈值 10px）
  showBackToTop.value = scrollTop + windowHeight >= docHeight - 10
}
const scrollToTop = () => {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

onMounted(() => {
  syncCategoryFromRoute()
  syncKeywordFromRoute()
  loadArticles()
  loadLatestArticles()
  loadHotCategories()
  // 监听滚动以控制返回顶部按钮显示
  window.addEventListener('scroll', handleScroll, { passive: true })
  handleScroll()
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
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
            role="link"
            tabindex="0"
            @keyup.enter="goToArticleDetail(article.id)"
            @click="goToArticleDetail(article.id)"
          >
            <div class="article-header">
              <h3 class="article-title clickable" @click.stop="goToArticleDetail(article.id)" title="查看详情">{{ article.title }}</h3>
              <ElTag 
                :effect="'light'" 
                class="category-tag"
                @click.stop="article.categoryId !== undefined && article.categoryId !== null && goToCategory(article.categoryId)"
              >
                {{ article.categoryName }}
              </ElTag>
            </div>
            
            <div class="article-content">
              <div class="article-cover" v-if="article.coverImg" @click.stop="goToArticleDetail(article.id)">
                <img :src="article.coverImg" :alt="article.title" class="cover-img">
              </div>
              <div class="article-summary">
                {{ article.content }}
                <span class="read-more" @click.stop="goToArticleDetail(article.id)">阅读全文</span>
              </div>
            </div>
            
            <div class="article-meta">
              <div class="author-info">
                <ElAvatar :src="article.avatar" size="small" class="author-avatar"></ElAvatar>
                <span class="author-name">{{ article.author }}</span>
                <span class="publish-time">{{ article.createTime }}</span>
              </div>
              <div class="article-stats">
                <span class="stat-item" aria-label="点赞数">
                  <i class="el-icon-thumb"></i>
                  {{ article.likeCount }}
                </span>
                <span class="stat-item" aria-label="评论数">
                  <i class="el-icon-chat-dot-square"></i>
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
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
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
              <span v-if="category.articleCount !== undefined && category.articleCount !== null" class="article-count">{{ category.articleCount }} 篇</span>
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
      <div class="sidebar-bottom"></div>
      </aside>

      <div 
        v-show="showBackToTop" 
        class="back-to-top" 
        @click="scrollToTop" 
        aria-label="返回顶部"
        title="返回顶部"
      >↑</div>
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

/* 标题可点击样式 */
.article-title.clickable {
  cursor: pointer;
  color: #1890ff;
}
.article-title.clickable:hover {
  text-decoration: underline;
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

/* 确保内容区域有足够的最小高度 */
.content-wrapper {
  min-height: 500px;
}

/* 确保分页显示在底部 */
.pagination {
  margin-top: 40px;
  text-align: center;
  margin-bottom: 20px;
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
.back-to-top {
  position: fixed;
  right: 32px;
  bottom: 96px;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #1890ff 0%, #40a9ff 100%);
  color: #fff;
  font-size: 20px;
  cursor: pointer;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.15);
  animation: floatIn .35s ease-out;
  transition: transform .2s ease, box-shadow .2s ease;
  z-index: 1000;
}

.back-to-top:hover {
  transform: translateY(-2px) scale(1.06);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.18);
}

@keyframes floatIn {
  from { opacity: 0; transform: translateY(8px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
</style>