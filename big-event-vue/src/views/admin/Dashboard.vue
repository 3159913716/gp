<template>
  <div>
    <el-row :gutter="20">
      <el-col :span="6" v-for="(v, k) in statsMap" :key="k">
        <el-card shadow="hover">
          <div style="display:flex; justify-content:space-between; align-items:center">
            <div>
              <div style="font-size:14px; color:#666">{{ v.label }}</div>
              <div style="font-size:28px; font-weight:700">{{ stats[k] ?? 0 }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:16px">
      <el-col :span="16">
        <el-card>
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px">
            <div style="font-weight:600">关键图表（占位）</div>
            <div style="color:#999; font-size:12px">数据可接后端统计或前端图表组件</div>
          </div>
          <div style="height:240px; display:flex; align-items:center; justify-content:center; background:linear-gradient(90deg,#fff,#f7f9fc); border-radius:6px">
            <div style="text-align:center; color:#999">图表占位（这里可以接入 ECharts / Chart.js）</div>
          </div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px">
            <div style="font-weight:600">快速操作</div>
            <div style="color:#999; font-size:12px">管理员常用功能</div>
          </div>
          <div style="display:flex; flex-direction:column; gap:12px; padding:16px 0">
            <el-button type="primary" @click="$router.push('/admin/users')">用户管理</el-button>
            <el-button type="primary" @click="$router.push('/admin/applications')">作者申请</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getStatistics, getUsers } from '@/api/admin.js'

const stats = ref({})
const adminList = ref([])

const statsMap = {
  totalUsers: { label: '总用户数' },
  totalArticles: { label: '总文章数' },
  totalComments: { label: '总评论数' },
  todayNewUsers: { label: '今日新增用户' },
  todayNewArticles: { label: '今日新增文章' },
  todayNewComments: { label: '今日新增评论' }
}

async function fetchStats() {
  try {
    const res = await getStatistics()
    stats.value = res.data || res
  } catch (e) { console.error('fetch stats failed', e) }
}

async function loadAdmins() {
  try {
    const res = await getUsers({ page: 1, pageSize: 200 })
    const data = res.data || res
    adminList.value = data.list || []
  } catch (e) { console.warn('load admins failed', e) }
}

onMounted(() => {
  fetchStats()
  loadAdmins()
})
</script>
