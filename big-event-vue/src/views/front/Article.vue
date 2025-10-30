<template>
  <div>
    <NavBar />
    <el-container style="padding:24px">
      <el-card>
        <h1>{{ article.title }}</h1>
        <img v-if="article.coverImg" :src="article.coverImg" style="max-width:100%; margin:12px 0" />
        <div v-html="article.content"></div>
        <div style="margin-top:12px">
          <el-button type="primary" @click="doLike">👍 点赞</el-button>
          <el-button @click="doFavorite">☆ 收藏</el-button>
        </div>
      </el-card>
    </el-container>
    <FooterBar />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from '@/components/front/NavBar.vue'
import FooterBar from '@/components/front/FooterBar.vue'
import { frontArticleDetail, likeArticle, favoriteArticle } from '@/api/front.js'
import { useTokenStore } from '@/stores/token.js'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const article = ref({})
const tokenStore = useTokenStore()

const load = async () => {
  const id = route.params.id
  const res = await frontArticleDetail(id)
  article.value = res || {}
}

const doLike = async () => {
  if (!tokenStore.token) { ElMessage.warning('请先登录'); router.push({ name: 'Login' }); return }
  await likeArticle(article.value.id)
  ElMessage.success('已点赞')
}

const doFavorite = async () => {
  if (!tokenStore.token) { ElMessage.warning('请先登录'); router.push({ name: 'Login' }); return }
  await favoriteArticle(article.value.id)
  ElMessage.success('已收藏')
}

onMounted(load)
</script>
