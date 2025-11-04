<template>
  <div>
    <div style="display:flex; gap:12px; align-items:center; margin-bottom:16px">
      <el-select v-model="filters.status" placeholder="状态" clearable style="width:160px">
        <el-option :label="'待审核'" :value="0" />
        <el-option :label="'已通过'" :value="1" />
        <el-option :label="'已拒绝'" :value="2" />
      </el-select>
      <el-input 
        v-model="filters.username" 
        placeholder="按用户名或昵称搜索" 
        clearable 
        style="width:240px" 
        @keyup.enter.native="debouncedLoadApplies" 
      />
      <el-button type="primary" @click="debouncedLoadApplies">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-card>
      <el-table 
        :data="applies" 
        stripe 
        style="width:100%" 
        row-key="id"
      >
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="申请人" width="140">
          <template #default="{ row }">
            <div>{{ row.userInfo.username }}</div>
            <div style="color:#999; font-size:12px">{{ row.userInfo.email || '无邮箱' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="realName" label="真实姓名" width="80" />
        <el-table-column label="身份证号" width="180">
          <template #default="{ row }">{{ row.maskedIdCard }}</template>
        </el-table-column>
        <el-table-column prop="applyDesc" label="申请描述">
          <template #default="{ row }">{{ row.applyDesc || '无描述' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'warning' : (row.status === 1 ? 'success' : 'info')">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="160">
          <template #default="{ row }">{{ row.createTime }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-dropdown trigger="click" v-if="row.status === 0">
              <el-button size="small">更多操作<i class="el-icon-arrow-down el-icon--right"></i></el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="approve(row)">通过申请</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>

        <template #empty>
          <div>暂无申请数据</div>
        </template>
      </el-table>

      <div style="text-align:center; margin-top:16px">
        <el-pagination 
          v-model:current-page="page" 
          :page-size="pageSize" 
          :page-sizes="[10, 20, 50]"
          :total="total" 
          layout="sizes, prev, pager, next, jumper"
          @current-change="debouncedLoadApplies" 
          @size-change="handlePageSizeChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAuthorApplies, auditAuthorApply } from '@/api/admin.js'
import { debounce } from 'lodash'

const applies = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
let abortController = null

const filters = ref({ status: null, username: '' })

const debouncedLoadApplies = debounce(loadApplies, 300)

async function loadApplies() {
  if (abortController) abortController.abort()
  abortController = new AbortController()

  const params = { 
    page: page.value, 
    pageSize: pageSize.value, 
    status: filters.value.status 
  }
  if (filters.value.username) params.username = filters.value.username

  try {
    const res = await getAuthorApplies(params, { signal: abortController.signal })
    if (res?.data) {
      applies.value = res.data.list.map(item => {
        const frozenItem = Object.freeze(item)
        return {
          ...frozenItem,
          statusText: frozenItem.status === 0 ? '待审核' : (frozenItem.status === 1 ? '已通过' : '已拒绝'),
          maskedIdCard: maskIdCard(frozenItem.idCard)
        }
      }) || []
      total.value = res.data.total || 0
    } else {
      applies.value = []
      total.value = 0
    }
  } catch (err) {
    if (err.name !== 'AbortError') {
      console.error('获取申请列表失败:', err)
      ElMessage.error('获取申请列表失败')
    }
  } finally {
    abortController = null
  }
}

const handlePageSizeChange = (size) => {
  pageSize.value = size
  page.value = 1
  debouncedLoadApplies()
}

const resetFilters = () => {
  filters.value = { status: null, username: '' }
  page.value = 1
  debouncedLoadApplies()
}

const maskIdCard = (id) => {
  if (!id) return '无身份证信息'
  return id.slice(0, 6) + '******' + id.slice(-4)
}

const approve = async (row) => {
  try {
    await ElMessageBox.confirm('确认通过该申请？', '审核确认', { type: 'warning' })
    const res = await auditAuthorApply(row.id, { status: 1 })
    ElMessage.success(res?.message || res?.msg || '已通过')
    debouncedLoadApplies()
  } catch (err) {
    if (err !== 'cancel') {
      console.error('通过申请失败:', err)
      ElMessage.error('操作失败')
    }
  }
}

onMounted(debouncedLoadApplies)
</script>