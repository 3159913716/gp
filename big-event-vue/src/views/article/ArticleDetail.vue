<script setup>
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElCard, ElAvatar, ElTag, ElPagination, ElEmpty, ElInput, ElButton, ElMessage } from 'element-plus'
import articleHomeApi from '@/api/articlehome.js'
import defaultCover from '@/assets/default.png'
const defaultAvatar = 'https://n.sinaimg.cn/sinacn20122/80/w440h440/20181223/62bf-hqqzpku8165766.jpg';
import request from '@/utils/request.js'
import sendCommentApi from '@/api/sendcomment.js'
import guanzhu from '@/api/guanzhu.js'
import { useTokenStore } from '@/stores/token.js'
import useUserInfoStore from '@/stores/userInfo.js'
import CommentTree from '@/components/front/CommentTree.vue'
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

// 新增：评论交互本地持久化
const getUserCommentKey = (commentId) => {
  const uid = userInfoStore?.info?.id ?? 0
  return `comment:interactions:${commentId}:${uid || 'anon'}`
}

const saveCommentInteraction = (commentId, isLiked, likeCount) => {
  try {
    const key = getUserCommentKey(commentId)
    const payload = {
      isLiked,
      likeCount,
      ts: Date.now()
    }
    localStorage.setItem(key, JSON.stringify(payload))
  } catch { }
}

const loadCommentInteraction = (commentId) => {
  try {
    const key = getUserCommentKey(commentId)
    const raw = localStorage.getItem(key)
    return raw ? JSON.parse(raw) : {}
  } catch {
    return {}
  }
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
      following: following.value,
      likeCount: localLikeCount.value,
      collectCount: localCollectCount.value,
      ts: Date.now()
    }
    localStorage.setItem(getPersistKey(), JSON.stringify(payload))
  } catch { }
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
      article.value.authorAvatar = defaultAvatar
    }
  }
  
  // 评论头像加载失败：回退默认头像
  const onCommentAvatarError = (c) => {
    if (c?.user) {
      c.user.avatar = defaultAvatar
    }
  }
  
  // 点赞与收藏（接入后端接口）
const liked = ref(false)
const favorited = ref(route.query.isCollected === 'true') // 从URL参数设置初始收藏状态
const localLikeCount = ref(0)
const localCollectCount = ref(Number(route.query.collectCount) || 0) // 从URL参数设置初始收藏数
const likeLoading = ref(false)
const favoriteLoading = ref(false)

// 关注相关状态
const following = ref(false) // 关注状态
const followLoading = ref(false) // 关注加载状态

// 关注UI控制计算属性
const followingUi = computed(() => {
  // 只有在已登录且确实已关注的情况下才显示已关注状态
  return hasAuth.value && following.value
})

// 是否显示关注按钮的计算属性
const showFollowButton = computed(() => {
  // 未登录状态：始终显示关注按钮
  if (!hasAuth.value) {
    return true
  }

  // 已登录状态：检查文章作者名与当前登录用户名是否相同
  const currentUserName = userInfoStore?.info?.username || userInfoStore?.info?.nickname || ''
  const articleAuthorName = article.value?.authorName || ''

  // 如果作者名与当前用户名不同，则显示关注按钮
  return currentUserName !== articleAuthorName
})

// 新增：点赞UI控制计算属性
const tokenStoreTop = useTokenStore()
const hasAuth = computed(() => !!tokenStoreTop?.token)
const likedUi = computed(() => {
  // 只有在已登录且确实已点赞的情况下才显示已赞状态
  return hasAuth.value && liked.value
})
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

  // 记录操作前的状态
  const prevFavorited = favorited.value
  const prevCount = localCollectCount.value
  const isFromCollectList = route.query.fromCollect === 'true'

  // 先直接切换收藏状态，提供即时反馈
  favorited.value = !favorited.value

  // 根据收藏状态变化更新收藏数
  if (favorited.value) {
    // 收藏操作：收藏数+1
    localCollectCount.value = prevCount + 1
  } else {
    // 取消收藏：收藏数-1（但不小于0）
    localCollectCount.value = Math.max(0, prevCount - 1)
  }

  // 确保article对象中的收藏数也同步更新
  article.value.collectCount = localCollectCount.value

  // 保存状态到本地存储
  saveInteraction()

  favoriteLoading.value = true
  try {
    // 调用后端接口完成收藏/取消收藏操作
    const resp = await sendCommentApi.toggleArticleCollect(articleId.value)

    // 清除URL参数中的收藏状态，避免状态混乱
    const newQuery = { ...route.query }
    delete newQuery.isCollected
    delete newQuery.collectCount
    router.replace({ query: newQuery })

    // 如果从收藏列表进入，设置刷新标志以便返回收藏列表时刷新
    if (isFromCollectList) {
      localStorage.setItem('needRefreshCollectList', 'true')
    }

  } catch (err) {
    // 接口调用失败，恢复到操作前的状态
    console.error('文章收藏接口调用失败：', err?.message || err)
    favorited.value = prevFavorited
    localCollectCount.value = prevCount
    article.value.collectCount = prevCount
    saveInteraction()
    ElMessage.error('操作失败，请稍后重试')
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
    authorId: (id % 10) + 1, // 添加authorId，确保关注按钮可用
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

  // 确保authorId始终有值，优先使用已有字段，否则从作者名提取或使用默认值
  let authorId = data.author?.id ?? data.authorId ?? data.author_id ?? data.userId ?? data.user_id ?? data.create_user;
  if (!authorId) {
    const authorName = data.author?.username ?? data.authorName ?? data.author_name ?? data.author ?? data.username ?? data.createUserName ?? (data.create_user ? `用户${data.create_user}` : '匿名作者');
    const match = String(authorName).match(/\d+/);
    if (match) {
      authorId = Number(match[0]);
    } else {
      // 默认作者ID为1
      authorId = 1;
    }
  }

  return {
    id: data.id ?? articleId.value,
    title: data.title ?? '',
    contentHtml: data.contentHtml ?? data.content_html ?? '',
    contentText: data.content ?? data.content_text ?? '',
    coverImg: normalizeImageUrl(data.coverImg ?? data.cover_img),
    authorName: data.author?.username ?? data.authorName ?? data.author_name ?? data.author ?? data.username ?? data.createUserName ?? (data.create_user ? `用户${data.create_user}` : '匿名作者'),
    authorAvatar: normalizeImageUrl(data.author?.avatar ?? data.authorAvatar ?? data.author_pic ?? data.userPic) || defaultAvatar,
    authorId: authorId,
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
  const avatar = normalizeImageUrl(item?.author?.avatar ?? item?.authorAvatar ?? item?.author_pic ?? item?.userPic) || defaultAvatar
  const authorId = item?.author?.id ?? item?.authorId ?? item?.author_id ?? item?.userId ?? item?.user_id ?? item?.create_user ?? null
  return { name, avatar, authorId }
}

// 加载文章详情
const loadDetail = async () => {
  loading.value = true
  errorMsg.value = ''
  try {
    // 使用公共文章详情接口（不带显式 /api 前缀，避免命中需认证的网关）
    const res = await articleHomeApi.getArticleDetail(articleId.value)
    let data = res?.data || res?.item || res?.article || res

    // 若主接口未返回有效数据，尝试备用公开接口：GET /article/detail-page?id={id}
    if (!data || (!data.id && !data.title && !data.content && !data.contentHtml)) {
      try {
        const alt = await request.get('/article/detail-page', { params: { id: articleId.value } })
        const payload = alt?.data ?? alt
        data = payload?.data ?? payload?.item ?? payload?.article ?? payload
      } catch (e) {
        // 备用接口失败不抛出，后续走兜底
        console.warn('备用公开接口加载失败:', e?.message || e)
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

    // 优先使用URL参数中的收藏状态（如果有）
    const urlIsCollected = route.query.isCollected === 'true'
    const urlCollectCount = Number(route.query.collectCount)

    // 认证用户时优先采用接口返回的个性化状态；否则更信任本地持久化
    const tokenStoreTop = useTokenStore()
    const hasAuth = computed(() => !!tokenStoreTop?.token)
    if (persisted.liked !== undefined) {
      liked.value = Boolean(persisted.liked)
    } else if (hasAuth && apiIsLikedRaw !== undefined) {
      liked.value = Boolean(apiIsLikedRaw)
    } else {
      liked.value = Boolean(article.value.isLiked)
    }
    // 如果从URL参数知道是收藏状态，则直接设置为已收藏；否则与点赞一致的优先级
    if (urlIsCollected) {
      favorited.value = true
    } else if (persisted.favorited !== undefined) {
      favorited.value = Boolean(persisted.favorited)
    } else if (hasAuth && apiIsCollectedRaw !== undefined) {
      favorited.value = Boolean(apiIsCollectedRaw)
    } else {
      favorited.value = Boolean(article.value.isCollected)
    }

    // 关注状态初始化
    if (persisted.following !== undefined) {
      following.value = Boolean(persisted.following)
    }

    localLikeCount.value = Math.max(0, Number((persisted.likeCount ?? article.value.likeCount) || 0))
    // 如果URL参数中有收藏数，则优先使用
    localCollectCount.value = !isNaN(urlCollectCount) && urlCollectCount > 0 ? urlCollectCount :
      Math.max(0, Number((persisted.collectCount ?? article.value.collectCount) || 0))

    // 同步顶部统计为当前展示值
    article.value.likeCount = localLikeCount.value
    article.value.collectCount = localCollectCount.value

    // 如果分类名缺失但存在分类ID，则补充查询分类详情（一次性）
    if (!article.value.categoryName && article.value.categoryId) {
      try {
        const cRes = await articleHomeApi.getCategoryDetail(article.value.categoryId)
        const cPayload = cRes?.data ?? cRes
        const cData = cPayload?.data ?? cPayload?.item ?? cPayload
        const name = cData?.categoryName ?? cData?.category_name ?? cData?.name ?? cData?.title ?? cData?.alias ?? cData?.category?.name ?? cData?.category?.categoryName
        if (name) {
          article.value.categoryName = String(name)
        } else {
          // 兜底：读取分类列表查找
          try {
            const listRes = await request.get('/category/list')
            const lPayload = listRes?.data ?? listRes
            const list = Array.isArray(lPayload?.data) ? lPayload.data : (Array.isArray(lPayload?.list) ? lPayload.list : (Array.isArray(lPayload) ? lPayload : []))
            const found = list.find(it => Number(it?.id) === Number(article.value.categoryId))
            const n2 = found?.categoryName ?? found?.category_name ?? found?.name ?? found?.title ?? found?.alias
            if (n2) article.value.categoryName = String(n2)
          } catch (e2) {
            console.warn('分类列表兜底失败:', e2?.message || e2)
          }
        }
      } catch (e) {
        // 分类详情接口未授权或失败时，直接回退到公开的分类列表
        try {
          const listRes = await request.get('/category/list')
          const lPayload = listRes?.data ?? listRes
          const list = Array.isArray(lPayload?.data) ? lPayload.data : (Array.isArray(lPayload?.list) ? lPayload.list : (Array.isArray(lPayload) ? lPayload : []))
          const found = list.find(it => Number(it?.id) === Number(article.value.categoryId))
          const n2 = found?.categoryName ?? found?.category_name ?? found?.name ?? found?.title ?? found?.alias
          if (n2) {
            article.value.categoryName = String(n2)
          } else {
            console.warn('分类名称补齐失败：列表中未找到匹配ID', article.value.categoryId)
          }
        } catch (e2) {
          console.warn('分类名称补充失败且列表兜底也失败:', e2?.message || e2)
        }
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
          const { name, avatar, authorId } = resolveAuthorFromItem(found)
          if (name) article.value.authorName = name
          if (avatar) article.value.authorAvatar = avatar
          if (authorId) article.value.authorId = authorId
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

    // 保存到本地存储
    saveCommentInteraction(id, comment.isLiked, comment.likeCount)
  } catch (err) {
    comment.isLiked = !comment.isLiked
    comment.likeCount = Math.max(0, comment.likeCount + (comment.isLiked ? 1 : -1))
    console.warn('评论点赞接口调用异常：', err?.message || err)

    // 即使出错也要保存到本地存储，保证本地状态一致性
    saveCommentInteraction(id, comment.isLiked, comment.likeCount)
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

// 针对某条评论的嵌套回复提交
const onSubmitReply = async ({ parentId, content }) => {
  const tokenStore = useTokenStore()
  if (!tokenStore?.token) {
    ElMessage.warning('请先登录后再回复')
    const redirect = encodeURIComponent(location.pathname + location.search)
    router.push({ name: 'Login', query: { redirect } })
    return
  }
  const text = String(content || '').trim()
  if (!text) return
  submittingComment.value = true
  try {
    const payload = { content: text, parentId, parent_id: parentId }
    const resp = await sendCommentApi.addComment(articleId.value, payload)
    const code = resp?.code ?? resp?.status ?? 0
    const ok = (code === 0 || code === 200 || resp?.success === true)
    if (!ok) throw new Error(resp?.message || '回复失败')
    await loadComments()
    ElMessage.success('回复成功')
  } catch (err) {
    ElMessage.error(err?.message || '回复失败，请稍后重试')
  } finally {
    submittingComment.value = false
  }
}

// 生成模拟评论数据（兜底）
const generateMockComments = (id, page = 1, pageSize = 10) => {
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
  const total = list.length
  return { list, total }
}

// 归一化评论数据（递归处理 replies）
const normalizeCommentItem = (c) => {
  // 先加载本地存储的交互状态
  const persisted = loadCommentInteraction(c.id)
  // 默认头像链接
  const defaultAvatar = 'https://n.sinaimg.cn/sinacn20122/80/w440h440/20181223/62bf-hqqzpku8165766.jpg'
  // 获取当前用户信息
  const currentUserInfo = userInfoStore?.info || {}

  const comment = {
    id: c.id ?? 0,
    content: c.content ?? '',
    createTime: c.createTime ?? c.create_time ?? '',
    user: {
      id: c.user?.id ?? c.user_id ?? c.userId ?? c.uid ?? c.userInfo?.id ?? 0,
      // 先使用评论数据中的nickname，如果为空则使用userinfo接口返回的username
      nickname: c.user?.nickname ?? c.userInfo?.nickname ?? c.nickname ?? currentUserInfo.username ?? '',
      username: c.user?.username ?? c.userInfo?.username ?? c.userName ?? c.user_name ?? currentUserInfo.username ?? '游客',
      // 头像为空时使用默认头像
      avatar: normalizeImageUrl(c.user?.avatar ?? c.user?.userPic ?? c.userInfo?.userPic ?? c.userInfo?.avatar ?? c.userPic ?? c.user_pic ?? c.avatarUrl ?? c.avatar) || defaultAvatar
    },
    // 优先使用本地存储的点赞状态和数量
    isLiked: persisted.isLiked ?? c.isLiked ?? false,
    likeCount: persisted.likeCount ?? c.likeCount ?? c.comment_like_count ?? 0,
    parentId: c.parentId ?? c.parent_id ?? 0,
    replyCount: c.replyCount ?? c.reply_count ?? (Array.isArray(c.replies) ? c.replies.length : 0),
    replies: Array.isArray(c.replies) ? c.replies.map(normalizeCommentItem) : []
  }

  return comment
}

const computeDeepTotal = (arr) => {
  if (!Array.isArray(arr)) return 0
  let sum = 0
  for (const item of arr) {
    sum += 1 + computeDeepTotal(item?.replies || [])
  }
  return sum
}

const normalizeComments = (data) => {
  const rawList = Array.isArray(data?.list) ? data.list : (Array.isArray(data?.items) ? data.items : [])
  const normalizedList = Array.isArray(rawList) ? rawList.map(normalizeCommentItem) : []

  // 如果后端已返回嵌套 replies，则直接取顶层（parentId===0）的节点，并计算总数为所有层级的和
  const hasNested = normalizedList.some(n => Array.isArray(n.replies) && n.replies.length > 0)
  if (hasNested) {
    const total = computeDeepTotal(normalizedList)
    return { list: normalizedList.filter(n => Number(n.parentId || 0) === 0), total }
  }

  // 否则根据 parentId 构建树，总数为原始扁平列表长度（已包含子回复）
  const map = new Map()
  normalizedList.forEach(n => {
    n.replies = Array.isArray(n.replies) ? n.replies : []
    map.set(Number(n.id), n)
  })
  const roots = []
  normalizedList.forEach(n => {
    const pid = Number(n.parentId || 0)
    if (pid > 0 && map.has(pid)) {
      map.get(pid).replies.push(n)
    } else {
      roots.push(n)
    }
  })
  // 更新每个节点的回复数量
  normalizedList.forEach(n => {
    n.replyCount = n.replyCount ?? (Array.isArray(n.replies) ? n.replies.length : 0)
  })
  const total = normalizedList.length
  return { list: roots, total }
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

// 平滑滚动到顶部
const scrollToTop = () => {
  window.scrollTo({
    top: 0,
    behavior: 'smooth'
  })
}

onMounted(() => {
  // 进入页面时平滑滚动到顶部
  scrollToTop()
  loadDetail()
  loadComments()
})

// 返回上一页或收藏列表
const handleBack = () => {
  // 检查是否从收藏列表进入
  const fromCollect = route.query.fromCollect === 'true'
  if (fromCollect) {
    // 直接返回到收藏列表页面
    router.push('/admin/ucenter/collect')
  } else {
    // 否则使用浏览器的返回功能
    window.history.back()
  }
}

// 关注/取消关注作者
const toggleFollow = async () => {
  // 检查是否已登录
  const tokenStore = useTokenStore()
  if (!tokenStore?.token) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    router.push({ name: 'Login', query: { redirect } })
    return
  }

  // 修复作者ID解析，确保能从多种格式获取
  let authorId = article.value?.authorId;
  // 如果没有authorId，尝试从作者名中提取数字ID
  if (!authorId && article.value?.authorName) {
    const match = String(article.value.authorName).match(/\d+/);
    if (match) {
      authorId = Number(match[0]);
    } else {
      // 如果无法提取数字，使用默认值1
      authorId = 1;
    }
  }

  if (!authorId) {
    ElMessage.error('作者信息不完整，无法操作')
    return
  }

  // 检查是否是自己的文章，防止关注自己
  const currentUserId = userInfoStore?.info?.id;
  if (currentUserId && Number(currentUserId) === Number(authorId)) {
    ElMessage.warning('不能关注自己')
    return
  }

  // 记录操作前的状态
  const prevFollowing = following.value

  // 先直接切换关注状态，提供即时反馈
  following.value = !following.value

  // 保存状态到本地存储
  saveInteraction()

  followLoading.value = true
  try {
    // 调用guanzhu.js中的toggleFollow方法完成关注/取消关注操作
    const data = await guanzhu.toggleFollow(authorId);

    // 从响应中获取后端返回的实际关注状态
    const backendFollowing = data?.following;

    // 如果后端返回了明确的关注状态，则使用后端状态更新前端
    if (backendFollowing !== undefined) {
      following.value = Boolean(backendFollowing);
    }

    // 保存最新状态到本地存储
    saveInteraction();

  } catch (err) {
    // 接口调用失败，恢复到操作前的状态
    console.error('关注操作失败：', err?.message || err)
    following.value = prevFollowing
    saveInteraction()
    ElMessage.error('操作失败，请稍后重试')
  } finally {
    followLoading.value = false
  }
}

// 路由参数变化时，重新加载详情
watch(() => route.params.id, () => {
  commentsPage.value = 1
  // 路由参数变化时也平滑滚动到顶部
  scrollToTop()
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
            <div class="header-top">
              <h2 class="title">{{ article.title }}</h2>
            </div>
            <div class="meta">
              <div class="author">
                <span class="author-name">{{ article.authorName }}</span>
                <ElButton v-if="showFollowButton" :type="followingUi ? 'success' : 'primary'" :loading="followLoading"
                  :disabled="followLoading" size="small" class="follow-btn" @click="toggleFollow"
                  style="cursor: pointer;">
                  {{ followingUi ? '已关注' : '关注' }}
                </ElButton>
                <ElTag class="category-tag" size="small" :effect="'light'" @click="goToCategory">{{ article.categoryName
                }}</ElTag>
                <span class="time">{{ article.createTime }}</span>
              </div>
              <div class="stats">
                <div class="stat-item">
                  <!-- <span class="stat-icon fal fa-heart">👍</span> -->
                  <i class="fa-solid fa-heart" style="color:#c0c4cc;"></i>
                  <!-- <i class="fa-regular fa-heart text-red-500" style="color: #ef4444;"></i> -->

                  <span class="stat-value">{{ localLikeCount }}</span>
                </div>
                <div class="stat-item">
                  <!-- <span class="stat-icon">💬</span> -->
                  <i class="fa-solid fa-comment" style="color:#c0c4cc;"></i>

                  <span class="stat-value">{{ commentsTotal }}</span>
                </div>
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
          <div :loading="likeLoading" :disabled="likeLoading"
            class="action-btn" @click="toggleLike">
            <!-- <span class="icon">👍</span> -->
            <i  class="fa-solid fa-heart"
              :style="{ color: likedUi ?'#ef4444' :  '#c0c4cc' }"></i>
            <!-- <span class="label">{{ likedUi ? '已赞' : '点赞' }}</span> -->
            <span class="count" style="margin-left: 6px; font-weight: bold; font-size: 14px;">{{ localLikeCount
            }}</span>
          </div>
          <div :loading="favoriteLoading" :disabled="favoriteLoading"
            class="action-btn fav" @click="toggleFavorite">
            <!-- <span class="icon">⭐</span> -->
              <i  class="fa-solid fa-star"
              :style="{ color: favorited ?'#ffb800' :  '#c0c4cc' }"></i>
            <!-- <span class="label">{{ favorited ? '已收藏' : '收藏' }}</span> -->
            <span class="count" style="margin-left: 6px; font-weight: bold; font-size: 14px;">{{ localCollectCount
            }}</span>
          </div>
        </div>

        <div v-if="errorMsg" class="error">{{ errorMsg }}</div>

        <!-- 评论列表区域 -->
        <div class="comments-section">
          <h3 class="comments-title">评论</h3>

          <!-- 新增：发布评论输入框与发布按钮 -->
          <div class="comment-editor">
            <ElInput v-model="newComment" type="textarea" :autosize="{ minRows: 3, maxRows: 10 }"
              :maxlength="MAX_COMMENT_LEN" show-word-limit placeholder="请输入评论内容（最多200字）" class="comment-textarea" />
            <div class="editor-actions">
              <ElButton type="primary" :disabled="!canSubmitComment || submittingComment" @click="submitComment">发布评论
              </ElButton>
            </div>
          </div>

          <div v-if="commentsLoading" class="comments-loading">正在加载评论...</div>
          <template v-else>
            <template v-if="comments.length">
              <CommentTree v-for="c in comments" :key="c.id" :node="c" :depth="0" @submit-reply="onSubmitReply"
                @toggle-like="onToggleCommentLike" />
            </template>
            <ElEmpty v-else description="暂无评论" />
            <ElPagination v-model:current-page="commentsPage" v-model:page-size="commentsPageSize"
              :page-sizes="[5, 10, 20]" layout="jumper, total, sizes, prev, pager, next" background
              :total="commentsTotal" @size-change="onCommentsSizeChange" @current-change="onCommentsCurrentChange"
              class="comments-pagination" />
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

.header-top {
  display: flex;
  align-items: center;
  gap: 12px;
}


title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
}

.title {
  margin: 0;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  flex: 1;
  word-break: break-word;
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
  flex-wrap: wrap;
}

/* 关注按钮样式 */
.follow-btn {
  border-radius: 20px;
  padding: 6px 12px;
  font-size: 13px;
  transition: all 0.2s ease;
  margin: 0 4px;
}

.follow-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.08);
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
  align-items: center;
  /* gap: 12px; */
  color: #606266;
  font-size: 13px;
}

.stat-item {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 2px 10px;
  /* background: #f5f7fa; */
  /* border: 1px solid #ebeef5;
  border-radius: 16px; */
}

.stat-icon {
  font-size: 14px;
}

.stat-value {
  color: #909399;
  font-weight: 600;
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
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.06);
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
  margin: 12px 15px 8px;
  display: flex;
  gap: 15px;
  justify-content: flex-end;
}

.action-btn {
    cursor: pointer; 
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

/* .action-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 12px rgba(0, 0, 0, 0.08);
} */
</style>