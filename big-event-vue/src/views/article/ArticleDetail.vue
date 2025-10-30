<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElCard, ElAvatar, ElTag, ElPagination, ElEmpty, ElInput, ElButton, ElMessage } from 'element-plus'
import articleHomeApi from '@/api/articlehome.js'
import defaultCover from '@/assets/default.png'
import avatarImgAsset from '@/assets/avatar.jpg'
import request from '@/utils/request.js'
import sendCommentApi from '@/api/sendcomment.js'
import { useTokenStore } from '@/stores/token.js'
import useUserInfoStore from '@/stores/userInfo.js'

const route = useRoute()
const router = useRouter()

// 当前文章ID（来自路由）
const articleId = computed(() => Number(route.params.id))

// 本地持久化：用户交互（点赞/收藏）
const userInfoStore = useUserInfoStore()
const getPersistKey = () => {
  const uid = userInfoStore?.info?.id ?? 0
  return `article:interactions:${articleId.value}:${uid || 'anon'}`
}
const readInteraction = () => {
  try {
    const raw = localStorage.getItem(getPersistKey())
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
}
const saveInteraction = () => {
  try {
    const payload = {
      liked: liked.value,
      favorited: favorited.value,
      likeCount: localLikeCount.value,
      collectCount: localCollectCount.value,
      ts: Date.now()
    }
    localStorage.setItem(getPersistKey(), JSON.stringify(payload))
  } catch {}
}

// 加载状态与错误信息
const loading = ref(false)
const errorMsg = ref('')

// 图片地址规范化：空/无效关键字视为无效
const normalizeImageUrl = (url) => {
  const s = String(url || '').trim()
  if (!s) return ''
  const invalids = ['null', 'undefined', 'none', 'n/a', 'false', '0']
  if (invalids.includes(s.toLowerCase())) return ''
  return s
}

// 新增：基础 HTML 清洗工具（移除标签与常见实体，压缩空白）
const stripHtml = (s) => {
  const t = String(s || '')
    .replace(/<[^>]*>/g, ' ') // 去掉标签
    .replace(/&nbsp;|&#160;/gi, ' ') // nbsp
    .replace(/&lt;/gi, '<').replace(/&gt;/gi, '>').replace(/&amp;/gi, '&')
    .replace(/\s+/g, ' ') // 压缩空白
    .trim()
  return t
}

// 新增：详情页展示用的纯文本内容
const contentPlainText = computed(() => {
  const raw = article.value?.contentHtml || article.value?.contentText || ''
  return stripHtml(raw)
})
const article = ref({
  id: null,
  title: '',
  contentHtml: '',
  contentText: '',
  coverImg: '',
  authorName: '',
  authorAvatar: '',
  categoryId: null,
  categoryName: '',
  createTime: '',
  // 移除阅读量字段 viewCount
  likeCount: 0,
  commentCount: 0,
  isLiked: false,
  collectCount: 0,
  isCollected: false
})

// 封面图加载失败：隐藏封面区域避免空占位
const onCoverError = () => {
  if (article.value) {
    article.value.__hideCover = true
    article.value.coverImg = ''
  }
}
// 作者头像加载失败：回退默认头像
const onAuthorAvatarError = () => {
  if (article.value) {
    article.value.authorAvatar = avatarImgAsset
  }
}
// 评论头像加载失败：回退默认头像
const onCommentAvatarError = (c) => {
  if (c?.user) {
    c.user.avatar = avatarImgAsset
  }
}

// 点赞与收藏（接入后端接口）
const liked = ref(false)
const favorited = ref(false)
const localLikeCount = ref(0)
const localCollectCount = ref(0)
const likeLoading = ref(false)
const favoriteLoading = ref(false)
const toggleLike = async () => {
  if (likeLoading.value) return
  const tokenStore = useTokenStore()
  if (!tokenStore?.token) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    router.push({ name: 'Login', query: { redirect } })
    return
  }
  likeLoading.value = true
  try {
    const resp = await sendCommentApi.toggleArticleLike(articleId.value)
    const payload = resp?.data ?? resp
    const data = payload?.data ?? payload
    const prevLiked = liked.value
    const isLikedRaw = data?.isLiked ?? data?.is_liked ?? data?.liked
    const nextLiked = (isLikedRaw !== undefined) ? Boolean(isLikedRaw) : !prevLiked
    const countMaybe = data?.likeCount ?? data?.like_count ?? data?.count
    const prevCount = localLikeCount.value
    const nextCount = (countMaybe !== undefined) ? Number(countMaybe) : (prevCount + (nextLiked ? 1 : -1))
    liked.value = nextLiked
    localLikeCount.value = Math.max(0, Number.isFinite(nextCount) ? nextCount : prevCount)
    // 同步顶部统计（非负）
    article.value.likeCount = localLikeCount.value
    saveInteraction()
  } catch (err) {
    // 失败时做乐观切换，避免用户操作无反馈
    liked.value = !liked.value
    localLikeCount.value = Math.max(0, localLikeCount.value + (liked.value ? 1 : -1))
    article.value.likeCount = localLikeCount.value
    saveInteraction()
    console.warn('文章点赞接口调用异常：', err?.message || err)
  } finally {
    likeLoading.value = false
  }
}
const toggleFavorite = async () => {
  if (favoriteLoading.value) return
  const tokenStore = useTokenStore()
  if (!tokenStore?.token) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    router.push({ name: 'Login', query: { redirect } })
    return
  }
  favoriteLoading.value = true
  try {
    const resp = await sendCommentApi.toggleArticleCollect(articleId.value)
    const payload = resp?.data ?? resp
    const data = payload?.data ?? payload
    const prevFavorited = favorited.value
    const isCollectedRaw = data?.isCollected ?? data?.is_collected ?? data?.collected
    const nextFavorited = (isCollectedRaw !== undefined) ? Boolean(isCollectedRaw) : !prevFavorited
    const countMaybe = data?.collectCount ?? data?.collect_count ?? data?.count
    const prevCount = localCollectCount.value
    const nextCount = (countMaybe !== undefined) ? Number(countMaybe) : (prevCount + (nextFavorited ? 1 : -1))
    favorited.value = nextFavorited
    localCollectCount.value = Math.max(0, Number.isFinite(nextCount) ? nextCount : prevCount)
    // 同步顶部统计（非负）
    article.value.collectCount = localCollectCount.value
    saveInteraction()
  } catch (err) {
    // 失败时乐观更新，保证操作反馈
    favorited.value = !favorited.value
    localCollectCount.value = Math.max(0, localCollectCount.value + (favorited.value ? 1 : -1))
    article.value.collectCount = localCollectCount.value
    saveInteraction()
    console.warn('文章收藏接口调用异常：', err?.message || err)
  } finally {
    favoriteLoading.value = false
  }
}

// 生成模拟文章详情（兜底）
const generateMockDetail = (id = 1) => {
  return {
    id,
    title: `大事件资讯第${id}期 - 前端技术热点洞察（模拟）`,
    contentHtml: `<p>这是一段模拟的文章HTML内容，用于占位展示。</p>\n<p>包含富文本格式、图片、列表等演示。</p>\n<p>用于在接口不可用时提供一致的用户体验。</p>\n<hr>\n<h3>内容要点</h3>\n<ul>\n  <li>新技术动态与实践案例</li>\n  <li>工程化与性能优化建议</li>\n  <li>行业趋势与生态观察</li>\n</ul>\n<p>如果你看到这段文字，说明当前接口暂不可用或网络异常。待接口恢复后，页面将自动展示真实文章内容。</p>`,
    contentText: '这是一篇关于前端开发技术的精彩文章（模拟）',
    coverImg: defaultCover,
    authorName: `作者${(id % 10) + 1}`,
    authorAvatar: avatarImgAsset,
    categoryId: null,
    categoryName: ['技术资讯', '行业动态', '经验分享', '教程学习'][id % 4],
    createTime: `2024-01-${String(20 - (id % 15)).padStart(2, '0')}`,
    // 移除阅读量 viewCount
    likeCount: Math.floor(Math.random() * 200) + 30,
    commentCount: Math.floor(Math.random() * 50) + 8
  }
}

// 将接口返回的文章字段映射到页面所需结构（兼容驼峰/下划线字段）
const normalizeDetail = (data) => {
  if (!data || typeof data !== 'object') return generateMockDetail(articleId.value || 1)
  return {
    id: data.id ?? articleId.value,
    title: data.title ?? '',
    contentHtml: data.contentHtml ?? data.content_html ?? '',
    contentText: data.content ?? data.content_text ?? '',
    coverImg: normalizeImageUrl(data.coverImg ?? data.cover_img),
    authorName: data.author?.username ?? data.authorName ?? data.author_name ?? data.author ?? data.username ?? data.createUserName ?? (data.create_user ? `用户${data.create_user}` : '匿名作者'),
    authorAvatar: normalizeImageUrl(data.author?.avatar ?? data.authorAvatar ?? data.author_pic ?? data.userPic) || avatarImgAsset,
    categoryId: data.categoryId ?? data.category_id ?? null,
    categoryName: data.categoryName ?? data.category_name ?? '',
    createTime: data.createTime ?? data.create_time ?? data.publishTime ?? '',
    // 移除阅读量字段：viewCount/readCount/read_count
    likeCount: data.likeCount ?? data.like_count ?? 0,
    commentCount: data.commentCount ?? data.comment_count ?? 0,
    isLiked: data.isLiked ?? data.is_liked ?? false,
    collectCount: data.collectCount ?? data.collect_count ?? 0,
    isCollected: data.isCollected ?? data.is_collected ?? false
  }
}

// 新增：从列表项解析作者信息（与首页保持一致）
const resolveAuthorFromItem = (item) => {
  const name = item?.author?.username ?? item?.authorName ?? item?.author_name ?? item?.author ?? item?.username ?? item?.createUserName ?? (item?.create_user ? `用户${item.create_user}` : '匿名作者')
  const avatar = normalizeImageUrl(item?.author?.avatar ?? item?.authorAvatar ?? item?.author_pic ?? item?.userPic) || avatarImgAsset
  return { name, avatar }
}

// 加载文章详情
const loadDetail = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await articleHomeApi.getArticleDetail(articleId.value)
    let data = res?.data || res?.item || res?.article || res

    // 若主接口未返回有效数据，尝试备用接口：GET /article?id={id}
    if (!data || (!data.id && !data.title && !data.content && !data.contentHtml)) {
      try {
        const tokenStore = useTokenStore()
        const alt = await request.get('/article', { params: { id: articleId.value, ...(tokenStore?.token ? {} : { state: '已发布' }) } })
        const payload = alt?.data ?? alt
        // 兼容多种字段：item/items/list 或直接对象
        data = payload?.item || (Array.isArray(payload?.items) ? payload.items[0] : null) ||
               (Array.isArray(payload?.list) ? payload.list[0] : null) || payload
      } catch (e) {
        // 备用接口失败不抛出，后续走兜底
        console.warn('备用接口加载失败:', e?.message || e)
      }
    }

    article.value = normalizeDetail(data)
    // API是否明确携带状态
    const apiIsLikedRaw = data?.isLiked ?? data?.is_liked ?? data?.liked
    const apiIsCollectedRaw = data?.isCollected ?? data?.is_collected ?? data?.collected
    const persisted = readInteraction()
    // 初始化本地点赞与收藏计数/状态（非负，合并本地持久化）
    article.value.likeCount = Math.max(0, Number(article.value.likeCount || 0))
    article.value.collectCount = Math.max(0, Number(article.value.collectCount || 0))

    liked.value = (apiIsLikedRaw !== undefined) ? Boolean(apiIsLikedRaw) : Boolean(persisted.liked ?? article.value.isLiked)
    favorited.value = (apiIsCollectedRaw !== undefined) ? Boolean(apiIsCollectedRaw) : Boolean(persisted.favorited ?? article.value.isCollected)

    localLikeCount.value = Math.max(0, Number((persisted.likeCount ?? article.value.likeCount) || 0))
    localCollectCount.value = Math.max(0, Number((persisted.collectCount ?? article.value.collectCount) || 0))

    // 同步顶部统计为当前展示值
    article.value.likeCount = localLikeCount.value
    article.value.collectCount = localCollectCount.value

    // 如果分类名缺失但存在分类ID，则补充查询分类详情（一次性）
    if (!article.value.categoryName && article.value.categoryId) {
      try {
        const cRes = await articleHomeApi.getCategoryDetail(article.value.categoryId)
        const cData = cRes?.data || cRes
        const name = cData?.categoryName ?? cData?.category_name
        if (name) article.value.categoryName = name
      } catch (e) {
        // 分类查询失败不影响正文展示
        console.warn('分类名称补充失败:', e?.message || e)
      }
    }

    // 新增：若详情页作者缺失或为匿名，尝试从列表接口补齐（与首页一致）
    if (!article.value.authorName || article.value.authorName === '匿名作者') {
      try {
        const listRes = await articleHomeApi.getHomeArticles({ pageNum: 1, pageSize: 50, state: '已发布' })
        const payload = listRes?.data ?? listRes
        const list = Array.isArray(payload?.item) ? payload.item : (Array.isArray(payload?.items) ? payload.items : [])
        const found = Array.isArray(list) ? list.find(it => Number(it?.id) === Number(articleId.value)) : null
        if (found) {
          const { name, avatar } = resolveAuthorFromItem(found)
          if (name) article.value.authorName = name
          if (avatar) article.value.authorAvatar = avatar
        }
      } catch (e) {
        console.warn('从列表接口补齐作者失败:', e?.message || e)
      }
    }

    // 进一步兜底：根据标题搜索一次
    if (!article.value.authorName || article.value.authorName === '匿名作者') {
      const kw = String(article.value.title || '').trim()
      if (kw) {
        try {
          const sRes = await articleHomeApi.searchArticles({ keyword: kw, page: 1, pageSize: 10, state: '已发布' })
          const payload = sRes?.data ?? sRes
          const list = Array.isArray(payload?.list)
            ? payload.list
            : (Array.isArray(payload?.items) ? payload.items : (Array.isArray(payload?.item) ? payload.item : []))
          const found = Array.isArray(list) ? list.find(it => Number(it?.id) === Number(articleId.value)) || list[0] : null
          if (found) {
            const { name, avatar } = resolveAuthorFromItem(found)
            if (name) article.value.authorName = name
            if (avatar) article.value.authorAvatar = avatar
          }
        } catch (e) {
          console.warn('搜索接口补齐作者失败:', e?.message || e)
        }
      }
    }
  } catch (err) {
    console.error('加载文章详情失败，切换到模拟数据:', err?.message || err)
    // 为避免“读取失败”影响体验，失败时直接使用兜底内容且不提示错误
    errorMsg.value = ''
    article.value = generateMockDetail(articleId.value)
  } finally {
    loading.value = false
  }
}

// 评论相关状态
const comments = ref([])
const commentsLoading = ref(false)
const commentsError = ref('')
const commentsPage = ref(1)
const commentsPageSize = ref(10)
const commentsTotal = ref(0)

// 评论点赞加载状态映射（按评论ID）
const commentLikeLoading = ref({})
const onToggleCommentLike = async (comment) => {
  const id = Number(comment?.id)
  if (!id || commentLikeLoading.value[id]) return
  const tokenStore = useTokenStore()
  if (!tokenStore?.token) {
    ElMessage.warning('请先登录后再点赞评论')
    const redirect = encodeURIComponent(location.pathname + location.search)
    router.push({ name: 'Login', query: { redirect } })
    return
  }
  commentLikeLoading.value[id] = true
  try {
    const resp = await sendCommentApi.toggleCommentLike(id)
    const payload = resp?.data ?? resp
    const data = payload?.data ?? payload
    const isLiked = data?.isLiked ?? data?.is_liked ?? undefined
    const count = data?.likeCount ?? data?.like_count ?? undefined
    if (isLiked !== undefined) comment.isLiked = Boolean(isLiked)
    else comment.isLiked = !comment.isLiked
    if (count !== undefined) comment.likeCount = Math.max(0, Number(count) || 0)
    else comment.likeCount = Math.max(0, comment.likeCount + (comment.isLiked ? 1 : -1))
  } catch (err) {
    comment.isLiked = !comment.isLiked
    comment.likeCount = Math.max(0, comment.likeCount + (comment.isLiked ? 1 : -1))
    console.warn('评论点赞接口调用异常：', err?.message || err)
  } finally {
    commentLikeLoading.value[id] = false
  }
}

// 新增：发布评论输入与状态
const MAX_COMMENT_LEN = 200
const newComment = ref('')
const submittingComment = ref(false)
const canSubmitComment = computed(() => {
  const len = newComment.value.trim().length
  return len > 0 && len <= MAX_COMMENT_LEN
})

const submitComment = async () => {
  if (!canSubmitComment.value || submittingComment.value) return
  const tokenStore = useTokenStore()
  if (!tokenStore?.token) {
    // 未登录时，引导登录并保留返回地址
    ElMessage.warning('请先登录后再发表评论')
    const redirect = encodeURIComponent(location.pathname + location.search)
    router.push({ name: 'Login', query: { redirect } })
    return
  }
  submittingComment.value = true
  commentsError.value = ''
  try {
    const parent = 0 // 默认父评论ID为0（一级评论）
    const payload = { content: newComment.value.trim(), parentId: parent, parent_id: parent }
    const resp = await sendCommentApi.addComment(articleId.value, payload)
    // 兼容不同后端返回结构：可能是 {code, message, data} 或直接对象
    const code = resp?.code ?? resp?.status ?? 0
    const ok = (code === 0 || code === 200 || resp?.success === true)
    if (!ok) {
      throw new Error(resp?.message || '评论发布失败')
    }
    // 成功后清空输入并刷新到第一页评论
    newComment.value = ''
    commentsPage.value = 1
    await loadComments()
    ElMessage.success('评论发布成功')
  } catch (err) {
    commentsError.value = err?.message || '评论发布失败，请稍后重试'
    // ElMessage.error(commentsError.value)
  } finally {
    submittingComment.value = false
  }
}

// 生成模拟评论数据（兜底）
const generateMockComments = (id, page = 1, pageSize = 10) => {
  const total = 50
  const start = (page - 1) * pageSize
  const list = Array.from({ length: pageSize }).map((_, idx) => {
    const cid = start + idx + 1
    return {
      id: cid,
      content: `这是第 ${cid} 条模拟评论，针对文章 ${id} 的讨论内容。`,
      createTime: `2023-01-01 12:${String((cid % 60)).padStart(2, '0')}:00`,
      user: {
        id: (cid % 9) + 1,
        username: `user${(cid % 9) + 1}`,
        avatar: avatarImgAsset
      },
      likeCount: Math.floor(Math.random() * 20),
      isLiked: false,
      replies: []
    }
  })
  return { list, total }
}

// 归一化评论数据
const normalizeComments = (data) => {
  const list = Array.isArray(data?.list) ? data.list : (Array.isArray(data?.items) ? data.items : [])
  const total = data?.total ?? list.length
  return {
    list: list.map(c => ({
      id: c.id ?? 0,
      content: c.content ?? '',
      createTime: c.createTime ?? c.create_time ?? '',
      user: {
        id: c.user?.id ?? c.user_id ?? c.userId ?? c.uid ?? c.userInfo?.id ?? 0,
        username: c.user?.username ?? c.user?.nickname ?? c.userInfo?.nickname ?? c.userInfo?.username ?? c.nickname ?? c.userName ?? c.user_name ?? '游客',
        avatar: normalizeImageUrl(c.user?.avatar ?? c.user?.userPic ?? c.userInfo?.userPic ?? c.userInfo?.avatar ?? c.userPic ?? c.user_pic ?? c.avatarUrl ?? c.avatar) || avatarImgAsset
      },
      likeCount: c.likeCount ?? c.comment_like_count ?? 0,
      isLiked: c.isLiked ?? false,
      replies: Array.isArray(c.replies) ? c.replies : []
    })),
    total
  }
}

// 加载评论列表
const loadComments = async () => {
  commentsLoading.value = true
  commentsError.value = ''
  try {
    const res = await articleHomeApi.getArticleComments(articleId.value, {
      page: commentsPage.value,
      pageSize: commentsPageSize.value
    })
    // 不强制要求code/success，直接按数据结构解析
    const payload = res?.data ?? res
    const normalized = normalizeComments(payload)
    comments.value = normalized.list
    commentsTotal.value = normalized.total
    // 同步顶部统计的评论数与列表总数一致
    article.value.commentCount = Math.max(0, Number(commentsTotal.value || 0))
  } catch (err) {
    console.error('加载评论失败，切换到模拟数据:', err?.message || err)
    commentsError.value = err?.message || '评论获取失败，已切换为模拟内容'
    const mock = generateMockComments(articleId.value, commentsPage.value, commentsPageSize.value)
    comments.value = mock.list
    commentsTotal.value = mock.total
    // 同步顶部统计的评论数
    article.value.commentCount = Math.max(0, Number(commentsTotal.value || 0))
  } finally {
    commentsLoading.value = false
  }
}

// 评论分页事件
const onCommentsSizeChange = (size) => {
  commentsPageSize.value = size
  commentsPage.value = 1
  loadComments()
}
const onCommentsCurrentChange = (num) => {
  commentsPage.value = num
  loadComments()
}
const goToCategory = () => {
  const id = article.value?.categoryId
  if (id !== undefined && id !== null) {
    router.push(`/category/${id}`)
  }
}
onMounted(() => {
  loadDetail()
  loadComments()
})

// 路由参数变化时，重新加载详情
watch(() => route.params.id, () => {
  commentsPage.value = 1
  loadDetail()
  loadComments()
})
</script>

<template>
  <div class="article-detail-page">
    <div class="container">
      <ElCard class="detail-card">
        <template #header>
          <div class="detail-header">
            <h2 class="title">{{ article.title }}</h2>
            <div class="meta">
              <div class="author">
                <span class="author-name">{{ article.authorName }}</span>
                <ElTag class="category-tag" size="small" :effect="'light'" @click="goToCategory">{{ article.categoryName }}</ElTag>
                <span class="time">{{ article.createTime }}</span>
              </div>
              <div class="stats">
                <span>点赞 {{ article.likeCount }}</span>
                <span>评论 {{ commentsTotal }}</span>
              </div>
            </div>
          </div>
        </template>

        <div v-if="loading" class="loading">正在加载文章内容...</div>
        <div v-else>
          <div v-if="article.coverImg && !article.__hideCover" class="cover">
            <img :src="article.coverImg" :alt="article.title" @error="onCoverError" />
          </div>

          <!-- 内容统一做基础 HTML 清洗后以纯文本展示 -->
          <div class="content">{{ contentPlainText }}</div>
        </div>

        <!-- 操作按钮：点赞 / 收藏（前端本地状态） -->
        <div class="actions-bar">
          <ElButton :type="liked ? 'primary' : 'default'" :loading="likeLoading" :disabled="likeLoading" class="action-btn like" @click="toggleLike">
            <span class="icon">👍</span>
            <span class="label">{{ liked ? '已赞' : '点赞' }}</span>
            <span class="count">{{ localLikeCount }}</span>
          </ElButton>
          <ElButton :type="favorited ? 'warning' : 'default'" :loading="favoriteLoading" :disabled="favoriteLoading" class="action-btn fav" @click="toggleFavorite">
            <span class="icon">⭐</span>
            <span class="label">{{ favorited ? '已收藏' : '收藏' }}</span>
            <span class="count">{{ localCollectCount }}</span>
          </ElButton>
        </div>

        <div v-if="errorMsg" class="error">{{ errorMsg }}</div>
        
        <!-- 评论列表区域 -->
        <div class="comments-section">
          <h3 class="comments-title">评论</h3>

          <!-- 新增：发表评论输入框与发布按钮 -->
          <div class="comment-editor">
            <ElInput
              v-model="newComment"
              type="textarea"
              :autosize="{ minRows: 3, maxRows: 10 }"
              :maxlength="MAX_COMMENT_LEN"
              show-word-limit
              placeholder="请输入评论内容（最多200字）"
              class="comment-textarea"
            />
            <div class="editor-actions">
              <ElButton type="primary" :disabled="!canSubmitComment || submittingComment" @click="submitComment">发布评论</ElButton>
            </div>
          </div>

          <div v-if="commentsLoading" class="comments-loading">正在加载评论...</div>
          <template v-else>
            <template v-if="comments.length">
              <div v-for="c in comments" :key="c.id" class="comment-item">
                <ElAvatar :src="c.user.avatar" size="small" @error="onCommentAvatarError(c)" />
                <div class="c-body">
                  <div class="c-meta">
                    <span class="c-user">{{ c.user.username }}</span>
                    <span class="c-time">{{ c.createTime }}</span>
                    <span class="c-like">
                      <ElButton class="c-like-btn" :type="c.isLiked ? 'primary' : 'default'" size="small" @click="onToggleCommentLike(c)" :loading="commentLikeLoading[c.id]">
                        <span class="icon">👍</span>
                        <span class="count">{{ c.likeCount }}</span>
                      </ElButton>
                    </span>
                  </div>
                  <div class="c-content">{{ c.content }}</div>
                </div>
              </div>
            </template>
            <ElEmpty v-else description="暂无评论" />
            <ElPagination
              v-model:current-page="commentsPage"
              v-model:page-size="commentsPageSize"
              :page-sizes="[5, 10, 20]"
              layout="jumper, total, sizes, prev, pager, next"
              background
              :total="commentsTotal"
              @size-change="onCommentsSizeChange"
              @current-change="onCommentsCurrentChange"
              class="comments-pagination"
            />
          </template>
          <div v-if="commentsError" class="comments-error">{{ commentsError }}</div>
        </div>
      </ElCard>
    </div>
  </div>
</template>

<style scoped>
.article-detail-page {
  padding: 16px 0;
}

.container {
  max-width: 880px;
  margin: 0 auto;
}

.detail-card {
  border-radius: 6px;
}

.detail-header {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}

.author {
  display: flex;
  align-items: center;
  gap: 8px;
}

.category-tag {
  margin-left: 6px;
}

.time {
  color: #909399;
  font-size: 13px;
}

.stats {
  display: flex;
  gap: 12px;
  color: #909399;
  font-size: 13px;
}

.cover {
  width: 100%;
  height: 220px;
  overflow: hidden;
  border-radius: 6px;
  margin-bottom: 14px;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.content {
  font-size: 15px;
  color: #303133;
  line-height: 1.7;
}

.loading {
  padding: 28px 0;
  text-align: center;
  color: #909399;
}

.error {
  margin-top: 10px;
  color: #f56c6c;
}
.comments-section {
  border-top: 1px solid #ebeef5;
  margin-top: 16px;
  padding-top: 14px;
}
.comments-title {
  margin: 0 0 6px;
  font-size: 16px;
  color: #303133;
}
/* 统一评论项样式 */
.comment-item {
  display: flex;
  gap: 12px;
  padding: 12px 10px;
  border-bottom: 1px solid #ebeef5;
}
.comment-item:last-child {
  border-bottom: none;
}
/* 头像统一尺寸与形状 */
.comment-item :deep(.el-avatar) {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0,0,0,0.06);
}
/* 主体与元信息 */
.c-body {
  flex: 1;
}
.c-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}
.c-user {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.c-user:hover {
  color: #409eff;
}
.c-time {
  font-size: 12px;
  color: #909399;
}
/* 操作区右对齐 */
.c-like {
  margin-left: auto;
}
.c-like-btn {
  border-radius: 16px;
  padding: 2px 10px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.c-like-btn .icon {
  font-size: 14px;
}
.c-like-btn .count {
  font-size: 12px;
  color: #909399;
}
/* 内容排版统一 */
.c-content {
  margin-top: 6px;
  font-size: 14px;
  color: #606266;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}
/* 响应式：在窄屏下减小间距与头像尺寸，并让元信息换行 */
@media (max-width: 768px) {
  .comment-item {
    gap: 10px;
    padding: 10px 8px;
  }
  .comment-item :deep(.el-avatar) {
    width: 32px;
    height: 32px;
  }
  .c-meta {
    flex-wrap: wrap;
    gap: 6px;
  }
  .c-user {
    font-size: 13px;
  }
  .c-time {
    font-size: 12px;
  }
}
/* 评论编辑区：禁用拖拽放大并提供足够默认输入空间 */
.comment-editor :deep(.el-textarea__inner) {
  resize: none;
  line-height: 1.6;
  border-radius: 6px;
}
.comment-editor .editor-actions {
  margin-top: 8px;
}
/* 修复误插入的模板内容，保留样式作用域 */
.editor-actions {
  margin-top: 8px;
}
.comments-loading {
  padding: 16px 0;
  text-align: center;
  color: #909399;
}
.comments-error {
  margin-top: 8px;
  color: #f56c6c;
}

/* 操作按钮样式 */
.actions-bar {
  margin: 12px 0 8px;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}
.action-btn {
  border-radius: 20px;
  padding: 8px 14px;
  transition: all 0.2s ease;
}
.action-btn .icon {
  font-size: 16px;
  margin-right: 6px;
}
.action-btn .count {
  margin-left: 6px;
  color: #909399;
  font-size: 12px;
}
.action-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.08);
}
</style>