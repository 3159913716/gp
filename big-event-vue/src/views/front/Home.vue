<template>
  <div>
    <NavBar />
    <el-container style="padding:20px;">
      <el-row :gutter="20">
        <el-col :span="18">
          <div style="display:flex; gap:12px; margin-bottom:12px; align-items:center">
            <el-radio-group v-model="sort" @change="fetchList">
              <el-radio-button label="latest">最新</el-radio-button>
              <el-radio-button label="hot">热门</el-radio-button>
            </el-radio-group>
          </div>

          <el-row :gutter="20">
            <el-col :span="8" v-for="item in list" :key="item.id">
              <ArticleCard :article="item" @read="goDetail" />
            </el-col>
          </el-row>

          <div style="text-align:center; margin-top:20px;">
            <el-pagination v-model:current-page="pageNum" :page-size="pageSize" :total="total" layout="prev, pager, next" @current-change="fetchList" />
          </div>
        </el-col>

        <el-col :span="6">
          <el-card>
            <h4>热门分类</h4>
            <div style="margin-top:8px">
              <el-tag v-for="c in categories" :key="c.id" style="margin:6px; cursor:pointer" @click="toCategory(c.id)">{{ c.name }}</el-tag>
            </div>
          </el-card>
          <el-card style="margin-top:12px">
            <h4>最新文章</h4>
            <ul style="padding:0; list-style:none;">
              <li v-for="a in newest" :key="a.id" style="padding:8px 0; border-bottom:1px solid #f0f0f0; cursor:pointer" @click="goDetail(a.id)">{{ a.title }}</li>
            </ul>
          </el-card>
        </el-col>
      </el-row>
    </el-container>
    <FooterBar />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import NavBar from '@/components/front/NavBar.vue'
import FooterBar from '@/components/front/FooterBar.vue'
import ArticleCard from '@/components/front/ArticleCard.vue'
import { frontArticleList, frontCategoryList } from '@/api/front.js'
import { useRouter } from 'vue-router'

const router = useRouter()
const list = ref([])
const pageNum = ref(1)
const pageSize = ref(9)
const total = ref(0)
const sort = ref('latest')
const categories = ref([])
const newest = ref([])

const fetchList = async () => {
  const params = { pageNum: pageNum.value, pageSize: pageSize.value, sort: sort.value }
  const res = await frontArticleList(params)
  total.value = res.total || 0
  list.value = res.items || res.list || []
}

const fetchSide = async () => {
  const res = await frontCategoryList()
  categories.value = res || []
  // newest: fetch first page latest
  const newestRes = await frontArticleList({ pageNum:1, pageSize:5, sort:'latest' })
  newest.value = newestRes.items || newestRes.list || []
}

const goDetail = (id) => router.push({ name: 'FrontArticle', params: { id } })
const toCategory = (id) => router.push({ name: 'FrontCategory', params: { id } })

onMounted(() => { fetchList(); fetchSide() })
</script>
