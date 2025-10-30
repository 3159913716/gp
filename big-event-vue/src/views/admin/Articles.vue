<template>
  <div>
    <el-container>
      <el-header style="display:flex; gap:12px; align-items:center">
        <el-input v-model="filters.q" placeholder="按标题/作者搜索" clearable style="width:300px" @keyup.enter.native="loadArticles" />
        <el-select v-model="filters.status" placeholder="状态" clearable style="width:160px">
          <el-option :label="'全部'" :value="''" />
          <el-option :label="'待审核'" :value="0" />
          <el-option :label="'已通过'" :value="1" />
          <el-option :label="'已删除'" :value="-1" />
        </el-select>
        <el-button type="primary" @click="loadArticles">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
        <div style="flex:1"></div>
        <el-button type="danger" @click="batchDelete" :disabled="!multipleSelection.length">批量删除</el-button>
        <el-button type="primary" @click="batchApprove" :disabled="!multipleSelection.length">批量通过</el-button>
      </el-header>

      <el-main>
        <el-table :data="articles" stripe @selection-change="handleSelection" style="width:100%">
          <el-table-column type="selection" width="55" />
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="title" label="标题" />
          <el-table-column prop="author.nickname" label="作者" width="160" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">{{ statusLabel(row.status) }}</template>
          </el-table-column>
          <el-table-column prop="createTime" label="发布时间" width="180" />
          <el-table-column label="操作" width="240">
            <template #default="{ row }">
              <el-button size="small" @click="viewDetail(row.id)">查看</el-button>
              <el-button size="small" type="primary" v-if="row.status===0" @click="approve(row)">通过</el-button>
              <el-button size="small" type="danger" @click="deleteOne(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div style="text-align:center; margin-top:16px">
          <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next, jumper" @current-change="loadArticles" />
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminArticles, approveArticle, deleteArticle } from '@/api/admin.js'
import { useRouter } from 'vue-router'

const router = useRouter()
const articles = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filters = ref({ q: '', status: '' })
const multipleSelection = ref([])

const loadArticles = async () => {
  const params = { page: page.value, pageSize: pageSize.value, q: filters.value.q, status: filters.value.status }
  try {
    const res = await getAdminArticles(params)
    if (res && res.data) {
      articles.value = res.data.list || []
      total.value = res.data.total || 0
    } else {
      articles.value = []
      total.value = 0
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('获取文章列表失败')
  }
}

const resetFilters = () => { filters.value = { q: '', status: '' }; page.value = 1; loadArticles() }

const statusLabel = (s) => s === 0 ? '待审核' : (s === 1 ? '已通过' : '已删除')

const viewDetail = (id) => { router.push({ name: 'FrontArticle', params: { id } }) }

const approve = async (row) => {
  try {
    await ElMessageBox.confirm('确认通过该文章吗？', '二次确认', { type: 'warning' })
    const res = await approveArticle(row.id)
  // 操作由后端记录
    ElMessage.success(res && res.message ? res.message : '已通过')
    loadArticles()
  } catch (err) {}
}

const deleteOne = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该文章？', '二次确认', { type: 'warning' })
    const res = await deleteArticle(row.id)
  // 操作由后端记录
    ElMessage.success(res && res.message ? res.message : '已删除')
    loadArticles()
  } catch (err) {}
}

const handleSelection = (arr) => { multipleSelection.value = arr }

const batchApprove = async () => {
  if (!multipleSelection.value.length) return
  try {
    await ElMessageBox.confirm(`确认通过选中 ${multipleSelection.value.length} 篇文章？`, '批量通过', { type: 'warning' })
    for (const item of multipleSelection.value) { await approveArticle(item.id) }
  // 操作由后端记录
    ElMessage.success('批量通过完成')
    loadArticles()
  } catch (err) {}
}

const batchDelete = async () => {
  if (!multipleSelection.value.length) return
  try {
    await ElMessageBox.confirm(`确认删除选中 ${multipleSelection.value.length} 篇文章？此操作不可逆`, '批量删除', { type: 'warning' })
    for (const item of multipleSelection.value) { await deleteArticle(item.id) }
  // 操作由后端记录
    ElMessage.success('批量删除完成')
    loadArticles()
  } catch (err) {}
}

onMounted(loadArticles)
</script>
