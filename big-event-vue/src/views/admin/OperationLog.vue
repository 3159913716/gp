<template>
  <div>
    <el-card>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px">
        <div style="font-weight:600">操作日志</div>
        <div style="display:flex; gap:8px; align-items:center">
          <el-autocomplete
            v-model="adminQuery"
            :fetch-suggestions="fetchAdminSuggestions"
            placeholder="按管理员搜索"
            size="small"
            clearable
            style="width:220px"
            @select="handleSelectAdmin"
          />
          <el-input v-model="filters.keyword" placeholder="按内容搜索" size="small" clearable />
          <el-date-picker v-model="filters.dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" size="small" />
          <el-button type="primary" size="small" @click="fetchLogs">查询</el-button>
        </div>
      </div>

      <el-table :data="logs" v-loading="loading" style="width:100%" row-key="id" :default-expand-all="false">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div style="padding:12px">
              <div style="margin-bottom:8px"><strong>操作内容：</strong>{{ row.operation_content }}</div>
              <div style="margin-bottom:8px"><strong>管理员：</strong>{{ resolveAdminName(row.admin_id) }}</div>
              <div style="margin-bottom:8px"><strong>IP：</strong>{{ row.ip_address }}</div>
                <div style="margin-bottom:8px" v-if="canNavigateToRelated(row)">
                  <el-button type="text" size="small" @click="navigateToRelated(row)">跳转关联</el-button>
                </div>
              <div><strong>原始数据：</strong>
                <pre style="background:#f5f7fa;padding:8px; border-radius:4px; overflow:auto">{{ pretty(row) }}</pre>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column prop="id" label="#" width="80" />
        <el-table-column prop="admin_id" label="管理员" width="180">
          <template #default="{ row }">
            {{ resolveAdminName(row.admin_id) }}
          </template>
        </el-table-column>
        <el-table-column prop="operation_content" label="操作内容" />
        <el-table-column prop="ip_address" label="IP" width="160" />
        <el-table-column prop="created_at" label="时间" width="200" />
      </el-table>

      <div style="margin-top:12px; display:flex; justify-content:space-between; align-items:center">
        <div>
          <el-select v-model="pageSize" size="small" @change="onPageSizeChange" style="width:120px">
            <el-option v-for="s in pageSizeOptions" :key="s" :label="s + ' / 页'" :value="s" />
          </el-select>
        </div>
        <div>
          <el-pagination
            background
            :page-size="pageSize"
            :current-page="page"
            :total="total"
            @current-change="onPageChange"
            layout="prev, pager, next, jumper, ->, total"
          />
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { getLogs, getUsers } from '@/api/admin.js'
import { useRouter } from 'vue-router'

const router = useRouter()

const logs = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const loading = ref(false)

const adminList = ref([])
const adminQuery = ref('')
const filters = ref({ adminId: null, keyword: '', dateRange: null })
const pageSizeOptions = ref([10, 20, 50, 100])
let _adminSearchTimer = null

async function fetchLogs() {
  loading.value = true
  try {
    const params = {
      page: page.value,
      pageSize: pageSize.value,
    }
    if (filters.value.keyword) params.q = filters.value.keyword
    if (filters.value.adminId) params.admin_id = filters.value.adminId
    if (filters.value.dateRange && filters.value.dateRange.length === 2) {
      params.start = filters.value.dateRange[0]
      params.end = filters.value.dateRange[1]
    }

    const res = await getLogs(params)
    // 兼容 res.data 或 res
    const data = res.data || res
    logs.value = data.list || []
    total.value = data.total || (Array.isArray(data) ? data.length : 0)
  } catch (e) {
    console.error('getLogs failed', e)
    ElMessage.error('加载日志失败')
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  page.value = p
  fetchLogs()
}

function onPageSizeChange(size) {
  page.value = 1
  fetchLogs()
}

// mount both
onMounted(() => { loadAdmins(); fetchLogs() })

// load admin list for filter dropdown
async function loadAdmins() {
  try {
    // 拉取前 200 名用户作为管理员选择项（后端可支持按 role=0 获取管理员）
    const res = await getUsers({ page: 1, pageSize: 200 })
    const data = res.data || res
    adminList.value = data.list || []
  } catch (e) {
    console.warn('loadAdmins failed', e)
  }
}

// removed duplicate onMounted above

function resolveAdminName(id) {
  const found = adminList.value.find(a => a.id === id)
  return found ? (found.username || found.nickname || found.id) : id
}

function pretty(value) {
  try { return JSON.stringify(value, null, 2) } catch (e) { return String(value) }
}

// autocomplete search for admins
async function searchAdmins(queryString, cb) {
  try {
    if (!queryString) {
      const list = adminList.value.map(a => ({ value: a.username || a.nickname || a.id, id: a.id }))
      cb(list.slice(0, 20))
      return
    }

    // 优先使用后端定义的 `username` 参数；如果没有返回结果，再尝试兼容 `q` 或 `name` 参数
    let res = await getUsers({ page: 1, pageSize: 50, username: queryString })
    let data = res.data || res
    let list = (data.list || [])

    if ((!list || list.length === 0) && queryString) {
      // 兼容 1: 使用 q
      try {
        res = await getUsers({ page: 1, pageSize: 50, q: queryString })
        data = res.data || res
        list = (data.list || [])
      } catch (e) {
        // ignore and try next
      }
    }

    if ((!list || list.length === 0) && queryString) {
      // 兼容 2: 使用 name
      try {
        res = await getUsers({ page: 1, pageSize: 50, name: queryString })
        data = res.data || res
        list = (data.list || [])
      } catch (e) {
        // ignore
      }
    }

    const out = (list || []).map(a => ({ value: a.username || a.nickname || a.id, id: a.id }))
    cb(out)
  } catch (e) {
    cb([])
  }
}

// debounced wrapper to reduce requests
function fetchAdminSuggestions(queryString, cb) {
  if (_adminSearchTimer) clearTimeout(_adminSearchTimer)
  _adminSearchTimer = setTimeout(() => {
    searchAdmins(queryString, cb)
  }, 300)
}

onBeforeUnmount(() => {
  if (_adminSearchTimer) clearTimeout(_adminSearchTimer)
})

function handleSelectAdmin(item) {
  filters.value.adminId = item.id
}

function navigateToRelated(row) {
  const id = row.related_id || row.resource_id || row.target_id || row.object_id || row.id
  const type = row.related_type || row.resource_type || row.target_type || row.object_type
  if (!type || !id) {
    ElMessage.info('无法识别的关联信息')
    return
  }
  const t = String(type).toLowerCase()
  if (t.includes('article') || t.includes('post')) {
    router.push({ path: `/article/${id}` })
  } else if (t.includes('user')) {
    router.push({ path: `/admin/users`, query: { id } })
  } else if (t.includes('comment')) {
    router.push({ path: `/admin/comments`, query: { id } })
  } else {
    ElMessage.info('无法识别的关联类型')
  }
}

function canNavigateToRelated(row) {
  const id = row.related_id || row.resource_id || row.target_id || row.object_id || row.id
  const type = row.related_type || row.resource_type || row.target_type || row.object_type
  if (!type || !id) return false
  const t = String(type).toLowerCase()
  return t.includes('article') || t.includes('post') || t.includes('user') || t.includes('comment')
}
</script>
