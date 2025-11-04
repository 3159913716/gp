<script setup>
import { ref, onMounted, onUnmounted, watch, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElTabs, ElTabPane, ElCard, ElAvatar, ElPagination } from 'element-plus'
import articleHomeApi from '@/api/articlehome.js'
import defaultCover from '@/assets/default.png'
import coverImgAsset from '@/assets/cover.jpg'
import logoImgAsset from '@/assets/logo.png'
import avatarImgAsset from '@/assets/avatar.jpg'
import { articleCategoryListService } from '@/api/article.js'
import { useTokenStore } from '@/stores/token.js'
import useUserInfoStore from '@/stores/userInfo.js'

console.log('HomePage.vue 组件加载')

// 新增：登录状态判断（用于分类过滤逻辑）
const tokenStore = useTokenStore()
const isLoggedIn = computed(() => !!tokenStore.token)
// 排序方式：最新或热门
const activeSort = ref('latest')

// 文章列表数据
const articles = ref([])
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
    article.avatar = avatarImgAsset
  }
}

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
      commentCount: Math.floor(Math.random() * 50) + 5,
      collectCount: Math.floor(Math.random() * 50)
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
    console.error('加载文章列表失败，切换到模拟数据:', error?.message || error)
    // 兜底：使用本地预设的模拟数据展示
    let mock = generateMockArticles(pageNum.value, pageSize.value)
    const keyword = selectedKeyword.value?.trim()
    if (keyword) {
      mock = mock.filter(a => String(a.title).includes(keyword) || String(a.content).includes(keyword))
    } else if (selectedCategoryId.value != null) {
      mock = mock.filter(a => a.categoryId === selectedCategoryId.value)
    }
    const merged = mergePersistedCounts(mock)
    const sorted = sortArticles(merged)
    articles.value = sorted
    total.value = 120
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
  // 路由参数变化时也滚动到顶部
  setTimeout(() => {
    scrollToTop()
  }, 100)
})
// 同时监听查询参数中的categoryId（兼容从其他位置跳转）
watch(() => route.query.categoryId, () => {
  syncCategoryFromRoute()
  // 清除搜索状态
  selectedKeyword.value = ''
  activeSort.value = 'latest'
  pageNum.value = 1
  loadArticles()
  // 路由参数变化时也滚动到顶部
  setTimeout(() => {
    scrollToTop()
  }, 100)
})
// 新增：监听搜索关键词变化，按关键词检索并展示
watch(() => route.query.keyword, () => {
  syncKeywordFromRoute()
  activeSort.value = 'latest'
  pageNum.value = 1
  loadArticles()
  // 搜索时也滚动到顶部
  setTimeout(() => {
    scrollToTop()
  }, 100)
})

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
// 加载右侧热门分类（只取前4个，登录用户显示自己创建的分类；未登录显示公开分类）
const loadHotCategories = async () => {
  try {
    const res = await articleCategoryListService()
    const payload = res?.data ?? res
    let list = Array.isArray(payload?.items)
      ? payload.items
      : (Array.isArray(payload?.list) ? payload.list : (Array.isArray(payload) ? payload : []))

    // 仅在已登录时按 userCreated 过滤为“当前用户创建的分类”
    if (isLoggedIn.value) {
      list = list.filter(c => {
        const flag = c.userCreated ?? c.isUserCreated ?? c.is_user_created
        return flag === true || flag === 'true' || flag === 1 || flag === '1'
      })
    }

    hotCategories.value = list.slice(0, 4).map(c => ({
      id: c.id,
      categoryName: c.categoryName ?? c.category_name ?? '',
      categoryAlias: c.categoryAlias ?? c.category_alias ?? ''
      // articleCount 可选：若后端提供则展示
    }))
  } catch (e) {
    console.error('加载热门分类失败:', e?.message || e)
    // 使用默认分类兜底展示（未登录场景提供基础可视内容）
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
              
            </div>
            
            <div class="article-content">
              <div class="article-cover" v-if="article.coverImg && !article.__hideCover" @click.stop="goToArticleDetail(article.id)">
                <img :src="article.coverImg" :alt="article.title" class="cover-img" @error="onCoverError(article)">
              </div>
              <div class="article-summary">
                 {{ getSummary(article) }}
                 <span v-if="shouldShowReadMore(article)" class="read-more" @click.stop="goToArticleDetail(article.id)">阅读全文</span>
               </div>
            </div>
            
            <div class="article-meta">
              <div class="author-info">
                <span class="author-name">{{ article.author }}</span>
                <span class="publish-time">{{ article.createTime }}</span>
              </div>
              <div class="list-actions">
                <ElButton class="action-btn like display-only" type="default">
                  <span class="icon">👍</span>
                  
                  <span class="count">{{ article.likeCount }}</span>
                </ElButton>
                <ElButton class="action-btn fav display-only" type="default">
                  <span class="icon">⭐</span>
        
                  <span class="count">{{ article.collectCount }}</span>
                </ElButton>
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

/* 新增：列表展示用的点赞/收藏胶囊按钮（纯展示，无交互） */
.list-actions {
  display: flex;
  gap: 12px;
}
.list-actions .action-btn {
  border-radius: 20px;
  padding: 6px 12px;
  height: 32px;
  pointer-events: none; /* 仅展示，不可点击 */
  border: none !important; /* 取消黑色边框 */
  background-color: #fff !important; /* 背景与卡片一致 */
}
.list-actions .action-btn .icon {
  font-size: 16px;
  margin-right: 6px;
}
.list-actions .action-btn .count {
  margin-left: 6px;
  color: #909399;
  font-size: 12px;
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
  /* 让右侧卡片顶部与左侧文章列表（排除排序标签）顶部对齐 */
  margin-top: 56px;
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
    /* 竖排布局时取消顶部对齐偏移 */
    margin-top: 0;
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
  from { opacity: 0; transform: translateY(8px) scale(0.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
</style>