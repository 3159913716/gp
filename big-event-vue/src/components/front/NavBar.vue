<template>
  <el-header class="front-navbar" style="display:flex; align-items:center; gap:12px;">
    <div class="logo" @click="toHome" style="font-weight:700; cursor:pointer">MySite</div>
    <el-select v-model="category" placeholder="分类" clearable style="width:180px">
      <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
    </el-select>
    <el-input v-model="q" placeholder="搜索文章" @keyup.enter.native="doSearch" style="flex:1"/>
    <el-button type="primary" @click="doSearch">搜索</el-button>

    <div style="margin-left:12px">
      <el-button v-if="!isAuthenticated" size="small" @click="toLogin">登录</el-button>
      <el-dropdown v-else>
        <span class="el-dropdown-link">
          <el-avatar :src="userInfo.userPic" size="small"/>
          <span style="margin-left:6px">{{ userInfo.nickname || userInfo.username }}</span>
        </span>
        <el-dropdown-menu>
          <el-dropdown-item @click.native="goProfile">个人中心</el-dropdown-item>
          <el-dropdown-item v-if="isAuthor" @click.native="goAuthor">作者中心</el-dropdown-item>
          <el-dropdown-item @click.native="logout">退出</el-dropdown-item>
        </el-dropdown-menu>
      </el-dropdown>
    </div>
  </el-header>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useTokenStore } from '@/stores/token.js'
import { useUserInfoStore } from '@/stores/userInfo.js'
import { frontCategoryList } from '@/api/front.js'

const router = useRouter()
const tokenStore = useTokenStore()
const userInfoStore = useUserInfoStore()

const categories = ref([])
const category = ref(null)
const q = ref('')

const isAuthenticated = computed(() => !!tokenStore.token)
const userInfo = computed(() => userInfoStore.info || {})
const isAuthor = computed(() => userInfo.value?.role === 'author')

const toHome = () => router.push({ path: '/home' })
const toLogin = () => router.push({ name: 'Login' })
const goProfile = () => router.push({ name: 'Profile' })
const goAuthor = () => router.push({ name: 'Author' })
const logout = () => { tokenStore.removeToken(); userInfoStore.removeInfo(); router.push({ name: 'Login' }) }
const doSearch = () => router.push({ name: 'FrontSearch', query: { q: q.value } })

const onCategoryChange = (id) => router.push({ name: 'FrontCategory', params: { id } })

onMounted(async () => {
  try {
    const res = await frontCategoryList()
    categories.value = res || []
  } catch (err) {
    console.error(err)
  }
})
</script>
