<template>
  <div>
    <el-container>
      <el-header style="display:flex; gap:12px; align-items:center">
        <el-input v-model="filters.q" placeholder="按评论内容/用户名搜索" clearable style="width:300px" @keyup.enter.native="loadComments" />
        <el-button type="primary" @click="loadComments">查询</el-button>
        <el-button @click="resetFilters">重置</el-button>
        <div style="flex:1"></div>
        <el-button type="danger" @click="batchDelete" :disabled="!multipleSelection.length">批量删除</el-button>
      </el-header>

      <el-main>
        <el-table :data="comments" stripe @selection-change="handleSelection" style="width:100%">
          <el-table-column type="selection" width="55" />
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="articleTitle" label="文章" />
          <el-table-column prop="content" label="评论内容" />
          <el-table-column prop="user.username" label="用户" width="160" />
          <el-table-column prop="createTime" label="评论时间" width="180" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-button size="small" type="danger" @click="deleteOne(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>

        <div style="text-align:center; margin-top:16px">
          <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next, jumper" @current-change="loadComments" />
        </div>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getComments, deleteComment } from '@/api/admin.js'

const comments = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filters = ref({ q: '' })
const multipleSelection = ref([])

const loadComments = async () => {
  const params = { page: page.value, pageSize: pageSize.value, q: filters.value.q }
  try {
    const res = await getComments(params)
    if (res && res.data) {
      comments.value = res.data.list || []
      total.value = res.data.total || 0
    } else {
      comments.value = []
      total.value = 0
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('获取评论列表失败')
  }
}

const resetFilters = () => { filters.value = { q: '' }; page.value = 1; loadComments() }

const handleSelection = (arr) => { multipleSelection.value = arr }

const deleteOne = async (row) => {
  try {
    await ElMessageBox.confirm('确认删除该评论？', '二次确认', { type: 'warning' })
    const res = await deleteComment(row.id)
  // 操作由后端记录
    ElMessage.success(res && res.message ? res.message : '已删除')
    loadComments()
  } catch (err) {}
}

const batchDelete = async () => {
  if (!multipleSelection.value.length) return
  try {
    await ElMessageBox.confirm(`确认删除选中 ${multipleSelection.value.length} 条评论？`, '批量删除', { type: 'warning' })
    for (const item of multipleSelection.value) { await deleteComment(item.id) }
  // 操作由后端记录
    ElMessage.success('批量删除完成')
    loadComments()
  } catch (err) {}
}

onMounted(loadComments)
</script>
