<template>
  <div>
    <NavBar />
    <el-container style="padding:20px">
      <h2>搜索：{{ q }}</h2>
      <el-row :gutter="20">
        <el-col :span="8" v-for="item in list" :key="item.id">
          <ArticleCard :article="item" @read="goDetail" />
        </el-col>
      </el-row>
      <div style="text-align:center; margin-top:20px;">
        <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="fetchList" />
      </div>
    </el-container>
    <FooterBar />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from '@/components/front/NavBar.vue'
import FooterBar from '@/components/front/FooterBar.vue'
import ArticleCard from '@/components/front/ArticleCard.vue'
import { frontArticleList } from '@/api/front.js'

const route = useRoute()
const router = useRouter()
const q = ref(route.query.q || '')
const list = ref([])
const pageNum = ref(1)
const pageSize = ref(9)
const total = ref(0)

const fetchList = async () => {
  const res = await frontArticleList({ q: q.value, pageNum: pageNum.value, pageSize: pageSize.value })
  total.value = res.total || 0
  list.value = res.items || res.list || []
}

const goDetail = (id) => router.push({ name: 'FrontArticle', params: { id } })

onMounted(fetchList)
</script>
