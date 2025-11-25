<script setup>
// 在导入部分添加缺少的组件导入
import { ElCard, ElAvatar, ElPagination } from 'element-plus'
// 引入Font Awesome样式
import '@fortawesome/fontawesome-free/css/all.css';
import articleHomeApi from '@/api/articlehome.js'
import defaultCover from '@/assets/default.png'
import { articleCategoryListService } from '@/api/article.js'
import { useTokenStore } from '@/stores/token.js'
import useUserInfoStore from '@/stores/userInfo.js'
import { ElMessage } from 'element-plus' // 添加ElMessage导入

// 添加avatarImgAsset变量定义，使用defaultCover作为默认头像
const avatarImgAsset = defaultCover

import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'


// 新增：登录状态判断（用于分类过滤逻辑）
const tokenStore = useTokenStore()
const isLoggedIn = computed(() => !!tokenStore.token)

// 文章列表数据
const articles = ref([])
// 热门文章数据
const hotArticles = ref([])
// 当前选择的分类ID（从路由读取）
const selectedCategoryId = ref(null)
// 新增：搜索关键词（从路由读取）
const selectedKeyword = ref('')

// 新增：本地交互持久化读取（与详情页键一致）
const userInfoStore = useUserInfoStore()
const getPersistKeyFor = (id) => {
  const uid = userInfoStore?.info?.id ?? 0
  return `article:interactions:${id}:${uid || 'anon'}`
}
const readInteractionFor = (id) => {
  try {
    const raw = localStorage.getItem(getPersistKeyFor(id))
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}
const mergePersistedCounts = (list) => {
  if (!Array.isArray(list)) return []
  return list.map(a => {
    const p = readInteractionFor(a.id)
    const like = p?.likeCount
    const collect = p?.collectCount
    // 仅当本地存在值时覆盖接口值，保持非负
    const next = { ...a }
    if (like !== undefined) next.likeCount = Math.max(0, Number(like) || 0)
    if (collect !== undefined) next.collectCount = Math.max(0, Number(collect) || 0)
    // 兜底：确保字段存在
    if (next.likeCount === undefined) next.likeCount = 0
    if (next.collectCount === undefined) next.collectCount = 0
    return next
  })
}

// 热门分类数据（模拟）
const hotCategories = ref([])

// 排序方式（目前只支持最新，可扩展）
const activeSort = ref('new')
// 默认分类兜底数据（请求失败时使用）
const defaultCategories = [
  { id: 1, categoryName: '技术资讯', categoryAlias: 'tech' },
  { id: 2, categoryName: '行业动态', categoryAlias: 'industry' },
  { id: 3, categoryName: '经验分享', categoryAlias: 'experience' },
  { id: 4, categoryName: '教程学习', categoryAlias: 'tutorial' }
]



// 分页数据
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// 图片地址规范化：为空、'null'、'undefined' 等视为无效
const normalizeImageUrl = (url) => {
  const s = String(url || '').trim()
  if (!s) return ''
  const invalids = ['null', 'undefined', 'none', 'n/a', 'false', '0']
  if (invalids.includes(s.toLowerCase())) return ''
  return s
}

// 封面图加载失败时隐藏封面区域，避免占位
const onCoverError = (article) => {
  if (article) {
    article.__hideCover = true
    article.coverImg = ''
  }
}

// 头像加载失败时回退默认头像
const onAvatarError = (article) => {
  if (article) {
    article.avatar = defaultCover
  }
}

// 生成模拟文章数据（兜底展示）

// 按createTime降序排序文章列表——确保最新文章排在前面
const sortArticles = (list) => {
  if (!Array.isArray(list)) return []
  const arr = [...list]
  // 按createTime降序排序，确保最新的文章排在前面
  return arr.sort((a, b) => {
    // 处理可能的时间格式差异
    const timeA = new Date(a.createTime || 0).getTime()
    const timeB = new Date(b.createTime || 0).getTime()
    // 降序排列（新的在前）
    return timeB - timeA
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
        coverImg: normalizeImageUrl(item.coverImg ?? item.cover_img),
        author: item.author?.username ?? item.authorName ?? item.author_name ?? item.author ?? item.username ?? item.createUserName ?? (item.create_user ? `用户${item.create_user}` : '匿名作者'),
        avatar: normalizeImageUrl(item.author?.avatar ?? item.authorAvatar ?? item.author_pic ?? item.userPic) || avatarImgAsset,
        createTime: item.createTime ?? item.create_time ?? '',
        // 移除阅读量：viewCount/read_count
        likeCount: item.likeCount ?? item.like_count ?? 0,
        commentCount: item.commentCount ?? item.comment_count ?? 0,
        collectCount: item.collectCount ?? item.collect_count ?? 0
      }))
      const merged = mergePersistedCounts(mapped)
      const sorted = sortArticles(merged)
      articles.value = sorted
      total.value = Number(payload?.total ?? sorted.length)
      console.log('搜索结果加载完成，共', articles.value.length, '条数据')
      await loadFullContentsFor(articles.value)
      return
    }

    // 2) 常规模式：按分类或默认列表
    console.log('加载文章列表，排序方式:', activeSort.value, '分类ID:', selectedCategoryId.value)
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      // 分类筛选（若未选择则不传）
      categoryId: selectedCategoryId.value ?? undefined,
      // 强制首页只显示已发布内容（与角色无关）
      state: '已发布',
      // 排序：使用 activeSort 响应式变量
      sort: activeSort.value
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
      coverImg: normalizeImageUrl(item.coverImg ?? item.cover_img),
      author: item.author?.username ?? item.authorName ?? item.author_name ?? item.author ?? item.username ?? item.createUserName ?? (item.create_user ? `用户${item.create_user}` : '匿名作者'),
      avatar: normalizeImageUrl(item.author?.avatar ?? item.authorAvatar ?? item.author_pic ?? item.userPic) || avatarImgAsset,
      createTime: item.createTime ?? item.create_time ?? '',
      // 移除阅读量字段
      likeCount: item.likeCount ?? item.like_count ?? 0,
      commentCount: item.commentCount ?? item.comment_count ?? 0,
      collectCount: item.collectCount ?? item.collect_count ?? 0
    }))

    // 本地兜底筛选（后端若未按分类过滤）
    const filtered = selectedCategoryId.value != null
      ? mapped.filter(a => a.categoryId === selectedCategoryId.value)
      : mapped

    const merged = mergePersistedCounts(filtered)
    const sorted = sortArticles(merged)
    articles.value = sorted
    total.value = Number(payload?.total ?? sorted.length)
    console.log('文章列表加载完成，共', articles.value.length, '条数据')
    await loadFullContentsFor(articles.value)
  } catch (error) {
    ElMessage.error('文章列表加载失败，请稍后重试')
  }
}
// 加载当前列表的全文内容（并发，错误不打断）
const loadFullContentsFor = async (list) => {
  if (!Array.isArray(list)) return
  await Promise.all(list.map(async (a) => {
    a.__fullLoading = true
    a.__fullError = ''
    a.__fullHtml = ''
    a.__fullText = ''
    try {
      const res = await articleHomeApi.getArticleDetail(a.id)
      const payload = res?.data ?? res
      const detail = payload?.data ?? payload?.item ?? payload?.article ?? payload
      a.__fullHtml = detail?.contentHtml ?? detail?.content_html ?? ''
      a.__fullText = detail?.content ?? detail?.content_text ?? ''
    } catch (err) {
      a.__fullError = err?.message || '加载全文失败'
    } finally {
      a.__fullLoading = false
    }
  }))
}

// 新增：去除简单HTML标签，压缩空白
const stripHtml = (s) => {
  const t = String(s || '')
    .replace(/<[^>]*>/g, ' ') // 去掉标签
    .replace(/&nbsp;|&#160;/gi, ' ') // nbsp
    .replace(/&lt;/gi, '<').replace(/&gt;/gi, '>').replace(/&amp;/gi, '&')
    .replace(/\s+/g, ' ') // 压缩空白
    .trim()
  return t
}

// 新增：获取用于摘要的原始文本（优先接口全文）
const getRawContent = (a) => {
  const src = a?.__fullHtml || a?.__fullText || a?.content || ''
  return stripHtml(src)
}

// 新增：生成最多65字的摘要文本
const getSummary = (a) => {
  const text = getRawContent(a)
  return text.length <= 65 ? text : (text.slice(0, 65) + '...')
}

// 新增：判断是否显示“阅读全文”
const shouldShowReadMore = (a) => getRawContent(a).length > 65

// 处理排序切换
const handleSortChange = () => {
  pageNum.value = 1 // 重置到第一页
  loadArticles()
  scrollToTop() // 滚动到顶部
}

// 分页事件分别处理
const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  loadArticles()
  scrollToTop() // 滚动到顶部
}

const handleCurrentChange = (num) => {
  pageNum.value = num
  loadArticles()
  scrollToTop() // 滚动到顶部
}

// 路由实例
const router = useRouter()
const route = useRoute()

// 同步路由中的分类ID到本地状态（增强版，支持多种参数形式）
const syncCategoryFromRouteEnhanced = () => {
  // 支持多种参数形式：
  // 1. route.params.categoryId (来自HomeLayout.vue的跳转)
  // 2. route.params.id (原有的参数形式)
  // 3. route.query.categoryId (查询参数形式)
  const raw = route.params?.categoryId ?? route.params?.id ?? route.query?.categoryId
  const num = Number(raw)
  selectedCategoryId.value = Number.isFinite(num) ? num : null
}

// 同步路由中的搜索关键词到本地状态
const syncKeywordFromRoute = () => {
  selectedKeyword.value = String(route.query?.keyword || '').trim()
}

// 监听路由参数变化，更新选中的分类和搜索关键词
watch(() => [route.params, route.query], () => {
  syncCategoryFromRouteEnhanced()
  syncKeywordFromRoute()

  // 重置页码并加载文章
  pageNum.value = 1
  loadArticles()

  // 路由参数变化时滚动到顶部
  setTimeout(() => {
    scrollToTop()
  }, 100)
}, { immediate: true, deep: true })

// 跳转到文章详情
const goToArticleDetail = (articleId) => {
  router.push(`/article/${articleId}`)
}

// 跳转到分类页面（无ID时忽略）
const goToCategory = (categoryId) => {
  if (!categoryId && categoryId !== 0) return
  // 在路由跳转前先滚动到顶部
  scrollToTop()
  // 使用setTimeout确保滚动操作先执行
  setTimeout(() => {
    router.push(`/category/${categoryId}`)
  }, 100)
}

// 右侧热门/最新文章逻辑已移除（仅展示主列表）

// 计算属性：选中分类与该分类的预览文章
const selectedCategory = computed(() => {
  return hotCategories.value.find(c => Number(c.id) === Number(selectedCategoryId.value)) || null
})
const selectedCategoryName = computed(() => selectedCategory.value?.categoryName || '')
const selectedCategoryAlias = computed(() => selectedCategory.value?.categoryAlias || '')

const categoryPreviewArticles = computed(() => {
  if (!selectedCategoryId.value) return []
  return articles.value.filter(a => Number(a.categoryId) === Number(selectedCategoryId.value)).slice(0, 5)
})
// 获取热门文章
const loadHotArticles = async () => {
  try {
    // 获取按点赞量排序的热门文章
    const res = await articleHomeApi.getHomeArticles({
      pageNum: 1,
      pageSize: 10,
      state: '已发布',
      sort: 'hot' // 按热度（点赞量）排序
    })
    const payload = res?.data ?? res
    const list = Array.isArray(payload?.item)
      ? payload.item
      : (Array.isArray(payload?.items) ? payload.items : [])

    // 映射文章数据
    const mapped = list.map(item => ({
      id: item.id,
      title: item.title,
      likeCount: item.likeCount ?? item.like_count ?? 0
    }))

    // 确保按点赞量降序排序
    hotArticles.value = mapped.sort((a, b) => b.likeCount - a.likeCount)
  } catch (error) {
    console.error('加载热门文章失败:', error)
    hotArticles.value = []
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



// 加载热门分类（补充真实接口，兜底默认）
const loadHotCategories = async () => {
  try {
    const res = await articleCategoryListService()
    hotCategories.value = Array.isArray(res?.data) ? res.data : defaultCategories
  } catch {
    hotCategories.value = defaultCategories
  }
}

onMounted(() => {
  syncCategoryFromRouteEnhanced()
  syncKeywordFromRoute()
  loadArticles()
  loadHotCategories()
  // 加载热门文章
  loadHotArticles()
  // 监听滚动以控制返回顶部按钮显示
  window.addEventListener('scroll', handleScroll, { passive: true })
  handleScroll()
})

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll)
})
</script>
<template>
  <!-- 首页主容器 -->
  <div class="home-page">
    <!-- 内容包装容器 -->
    <div class="content-wrapper">
      <!-- 内容主体区域：包含左侧文章列表和右侧边栏 -->
      <div class="content-main">
        <!-- 文章列表状态信息区域：始终显示当前文章列表的状态 -->
        <div class="category-detail-card">
          <!-- 状态信息头部：显示当前列表类型和数量 -->
          <div class="category-header">
            <div>
              <h3 class="category-title">
                <template v-if="selectedCategoryId">
                  {{ selectedCategoryName }}
                </template>
                <template v-else-if="selectedKeyword">
                  搜索结果："{{ selectedKeyword }}"
                </template>
                <template v-else>
                  最新文章
                </template>
              </h3>
              <div class="category-info">
                <span v-if="selectedCategoryId">分类 · </span>
                共 {{ total }} 篇文章
              </div>
            </div>
          </div>
        </div>
        <!-- 左侧文章列表区域 -->
        <div class="article-list-container">
          <!-- 主文章列表区域 -->
          <div class="articles">
          <!-- 文章卡片循环渲染 -->
          <ElCard v-for="article in articles" :key="article.id" class="article-card" role="link" tabindex="0"
            @keyup.enter="goToArticleDetail(article.id)" @click="goToArticleDetail(article.id)">
            <!-- 文章标题区域 -->
            <div class="article-header">
              <h3 class="article-title clickable" @click.stop="goToArticleDetail(article.id)" title="查看详情">{{
                article.title }}
              </h3>
            </div>

            <!-- 文章内容区域：包含封面图和摘要 -->
            <div class="article-content">
              <!-- 文章封面图 -->
              <div class="article-cover" v-if="article.coverImg && !article.__hideCover"
                @click.stop="goToArticleDetail(article.id)">
                <img :src="article.coverImg" :alt="article.title" class="cover-img" @error="onCoverError(article)">
              </div>
              <!-- 文章摘要 -->
              <div class="article-summary">
                {{ getSummary(article) }}
                <span v-if="shouldShowReadMore(article)" class="read-more"
                  @click.stop="goToArticleDetail(article.id)">阅读全文</span>
              </div>
            </div>

            <!-- 文章元信息区域：包含作者信息和互动统计 -->
            <div class="article-meta">
              <!-- 作者和发布时间信息 -->
              <div class="author-info">
                <span class="author-name">{{ article.author }}</span>
                <span class="publish-time">{{ article.createTime }}</span>
              </div>
              <!-- 互动统计按钮组：点赞、收藏、评论 -->
              <div class="list-actions">
                <!-- 点赞按钮 -->
                <div class="action-btn like display-only">
                  <i class="fa-solid fa-heart" style="color:#c0c4cc;"></i>
                  <span class="count" style="margin-left: 6px; font-weight: bold; font-size: 14px;">{{ article.likeCount
                    }}</span>
                </div>
                <!-- 收藏按钮 -->
                <div class="action-btn fav display-only">
                  <i class="fa-solid fa-star" style="color:#c0c4cc;"></i>
                  <span class="count" style="margin-left: 6px; font-weight: bold; font-size: 14px;">{{
                    article.collectCount
                    }}</span>
                </div>
                <!-- 评论数按钮 -->
                <div class="action-btn comment-count display-only">
                  <i class="fa-solid fa-comment" style="color:#c0c4cc;"></i>
                  <span class="count" style="margin-left: 6px; font-weight: bold; font-size: 14px;">{{
                    article.commentCount || 0
                    }}</span>
                </div>
              </div>
            </div>
          </ElCard>
        </div>
        <!-- 分页组件 -->
        <div class="pagination">
          <ElPagination v-model:current-page="pageNum" v-model:page-size="pageSize" :page-sizes="[5, 10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper" :total="total" @size-change="handleSizeChange"
            @current-change="handleCurrentChange" />
        </div>
        </div>
         <!-- 右侧边栏区域 -->
      <aside class="sidebar">
        <!-- 热门文章区块 -->
        <div class="sidebar-section">
          <h3 class="section-title">热门文章</h3>
          <div class="hot-articles-list">
            <div v-for="(article, index) in hotArticles" :key="article.id" class="hot-article-item"
              @click="goToArticleDetail(article.id)">
              <div class="hot-article-rank">{{ index + 1 }}</div>
              <div class="hot-article-title">{{ article.title }}</div>
            </div>
          </div>
        </div>
        <!-- 关于我们区块 -->
        <div class="sidebar-section">
          <h3 class="section-title">关于我们</h3>
          <div class="about-us">
            <img src="@/assets/1c2eec34a5c4585860422e9018c334e71710737406504.webp" alt="大事件" class="about-logo">
            <p class="about-desc">
              大事件资讯是一个专注于技术领域的内容分享平台，
              提供最新的技术动态、行业趋势和实战经验。
            </p>
          </div>
        </div>

        <!-- 侧边栏底部占位 -->
        <div class="sidebar-bottom"></div>
      </aside>
      </div>
        
      
      <!-- 返回顶部按钮 -->
      <div v-show="showBackToTop" class="back-to-top" @click="scrollToTop" aria-label="返回顶部" title="返回顶部">↑</div>
      
    </div>
  </div>
</template>

<style scoped>
.home-page {
  padding: 0 0 20px 0;
}

.content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* 内容主体区域：包含左侧状态信息、中间文章列表和右侧边栏 */
.content-main {
  display: flex;
  gap: 30px;
}

/* 文章列表状态信息区域 */
.category-detail-card {
  width: 100px;
  flex-shrink: 0;
  /* background: #fff; */
  padding: 10px;
  border-radius: 8px;
  /* box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06); */
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

.article-full-content {
  flex: 1;
  color: #606266;
  line-height: 1.7;
  font-size: 14px;
  display: -webkit-box;
  -webkit-line-clamp: 6;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.read-more {
  color: #1890ff;
  margin-left: 5px;
  font-weight: 500;
}

.full-loading {
  color: #909399;
}

.full-error {
  color: #f56c6c;
}

.full-html :deep(img) {
  max-width: 100%;
  height: auto;
}

.full-html :deep(p) {
  margin: 0 0 10px 0;
}

.full-text {
  display: -webkit-box;
  -webkit-line-clamp: 6;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 标准属性兼容性（补充 line-clamp） */
.full-text {
  line-clamp: 6;
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

/* 列表展示用的点赞/收藏按钮样式（参考详情页样式） */
.list-actions {
  display: flex;
  gap: 15px;
}

.list-actions .action-btn {
  cursor: pointer;
  pointer-events: none;
  /* 仅展示，不可点击 */
  display: flex;
  align-items: center;
  font-size: 16px;
}

.list-actions .action-btn .count {
  margin-left: 6px;
  color: #909399;
  font-weight: bold;
  font-size: 14px;
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
  /* 让右侧卡片顶部与左侧主文章列表区域对齐 */
  margin-top: 0;
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
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 中间分类详情卡片 */
.category-detail-card {
  margin-bottom: 20px;
}

.category-detail-card .category-header {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 12px;
  text-align: center;
}

.category-detail-card .category-header > div {
  text-align: center;
}

.category-title {
  font-size: 18px;
  margin: 0;
  color: #303133;
}

.category-alias {
  font-size: 12px;
  color: #909399;
}

.category-articles {
  display: flex;
  gap: 12px;
}

.category-article-card {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 10px;
  width: 100%;
  cursor: pointer;
}

.category-article-card .cat-thumb img {
  width: 100px;
  height: 64px;
  object-fit: cover;
  border-radius: 6px;
}

.category-article-card .cat-meta {
  flex: 1;
}

.cat-title {
  font-weight: 500;
  color: #303133;
}

.cat-info {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}



/* 右侧热门文章样式 */
.popular-section .popular-articles {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.popular-item {
  display: flex;
  gap: 10px;
  align-items: center;
  cursor: pointer;
}

.popular-thumb {
  width: 64px;
  height: 48px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
}

.popular-text {
  flex: 1;
}

.popular-title {
  font-size: 14px;
  color: #303133;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  overflow: hidden;
}

.popular-meta {
  font-size: 12px;
  color: #909399;
  margin-top: 6px;
}

.popular-meta .likes {
  color: #ff7a45;
  font-weight: 500;
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
  width: 100%;
  height: auto;
  max-height: 200px;
  object-fit: cover;
  border-radius: 8px;
  margin-bottom: 15px;
}

.about-desc {
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
  margin: 0;
}

/* 热门文章样式 */
.hot-articles-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.hot-article-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  cursor: pointer;
  transition: all 0.3s;
  padding: 8px 0;
}

.hot-article-item:hover {
  transform: translateX(5px);
}

.hot-article-rank {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
  font-size: 14px;
  font-weight: bold;
  color: #fff;
  background-color: #ff7a45;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 2px;
}

/* 排名数字颜色变化 */
.hot-article-item:nth-child(1) .hot-article-rank {
  background-color: #ff4d4f;
}

.hot-article-item:nth-child(2) .hot-article-rank {
  background-color: #fa8c16;
}

.hot-article-item:nth-child(3) .hot-article-rank {
  background-color: #faad14;
}

.hot-article-item:nth-child(n+4) .hot-article-rank {
  background-color: #1890ff;
}

.hot-article-title {
  flex: 1;
  font-size: 14px;
  color: #303133;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.3s;
}

.hot-article-item:hover .hot-article-title {
  color: #1890ff;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  .content-wrapper {
    flex-direction: column;
  }

  /* 三栏布局在中等屏幕下改为两栏，状态信息区域在上，文章和侧边栏在下 */
  .content-main {
    flex-direction: column;
  }

  .category-detail-card {
    width: 100%;
    margin-bottom: 20px;
  }

  .sidebar {
    width: 100%;
    display: flex;
    gap: 20px;
    margin-top: 20px;
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
  from {
    opacity: 0;
    transform: translateY(8px) scale(0.96);
  }

  to {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}
</style>