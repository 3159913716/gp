<template>
  <div>
    <div style="display:flex; gap:12px; align-items:center; margin-bottom:16px">
      <el-select v-model="filters.status" placeholder="状态" clearable style="width:160px">
        <el-option :label="'待审核'" :value="0" />
        <el-option :label="'已通过'" :value="1" />
        <el-option :label="'已拒绝'" :value="2" />
      </el-select>
      <el-input v-model="filters.username" placeholder="按用户名或昵称搜索" clearable style="width:240px" @keyup.enter.native="loadApplies" />
      <el-button type="primary" @click="loadApplies">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-card>
      <el-table :data="applies" stripe style="width:100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column label="申请人" width="140">
            <template #default="{ row }">
              <div>{{ row.userInfo?.username }} / {{ row.userInfo?.nickname }}</div>
              <div style="color:#999; font-size:12px">{{ row.userInfo?.email }}</div>
            </template>
          </el-table-column>
          <el-table-column prop="realName" label="真实姓名" width="80" />
          <el-table-column prop="idCard" label="身份证号" width="180">
            <template #default="{ row }">{{ maskIdCard(row.idCard) }}</template>
          </el-table-column>
          <el-table-column prop="applyDesc" label="申请描述" />
          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'warning' : (row.status === 1 ? 'success' : 'info')">{{ statusLabel(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="160" />
          <el-table-column label="操作" width="130">
            <template #default="{ row }">
              <el-dropdown trigger="click" v-if="row.status === 0">
                <el-button size="small">更多操作<i class="el-icon-arrow-down el-icon--right"></i></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="approve(row)">
                      通过申请
                    </el-dropdown-item>
                    <el-dropdown-item @click="openReject(row)">
                      拒绝申请
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>

        </el-table>

        <div style="text-align:center; margin-top:16px">
          <el-pagination v-model:current-page="page" :page-size="pageSize" :total="total" layout="prev, pager, next, jumper" @current-change="loadApplies" />
        </div>
      </el-card>

    <!-- 拒绝理由对话框 -->
    <el-dialog title="拒绝申请" :visible.sync="rejectDialog.visible">
      <div>对申请人 <strong>{{ rejectDialog.target?.userInfo?.username }}</strong> 的拒绝理由：</div>
      <el-input type="textarea" v-model="rejectDialog.reason" placeholder="请输入拒绝原因" rows="6" style="margin-top:12px" />
      <template #footer>
        <el-button @click="rejectDialog.visible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">提交拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAuthorApplies, auditAuthorApply } from '@/api/admin.js'

const applies = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const filters = ref({ status: null, username: '' })

const rejectDialog = ref({ visible: false, target: null, reason: '' })

const loadApplies = async () => {
  const params = { page: page.value, pageSize: pageSize.value, status: filters.value.status }
  if (filters.value.username) params.username = filters.value.username
  try {
    const res = await getAuthorApplies(params)
    if (res && res.data) {
      applies.value = res.data.list || []
      total.value = res.data.total || 0
    } else {
      applies.value = []
      total.value = 0
    }
  } catch (err) {
    console.error(err)
    ElMessage.error('获取申请列表失败')
  }
}

const resetFilters = () => { filters.value = { status: null, username: '' }; page.value = 1; loadApplies() }

const statusLabel = (s) => s === 0 ? '待审核' : (s === 1 ? '已通过' : '已拒绝')

const maskIdCard = (id) => {
  if (!id) return ''
  return id.slice(0,6) + '******' + id.slice(-4)
}

const approve = async (row) => {
  try {
    await ElMessageBox.confirm('确认通过该申请？', '审核确认', { type: 'warning' })
    const res = await auditAuthorApply(row.id, { status: 1 })
    ElMessage.success(res && (res.message || res.msg) ? (res.message || res.msg) : '已通过')
    loadApplies()
  } catch (err) {
    // ignore
  }
}

const openReject = (row) => { rejectDialog.value = { visible: true, target: row, reason: '' } }

const confirmReject = async () => {
  const target = rejectDialog.value.target
  if (!target) return
  if (!rejectDialog.value.reason || rejectDialog.value.reason.trim().length < 3) {
    ElMessage.error('请填写至少3个字的拒绝原因')
    return
  }
  try {
    const res = await auditAuthorApply(target.id, { status: 2, rejectReason: rejectDialog.value.reason })
    ElMessage.success(res && (res.message || res.msg) ? (res.message || res.msg) : '已拒绝')
    rejectDialog.value.visible = false
    loadApplies()
  } catch (err) {
    console.error(err)
    ElMessage.error('操作失败')
  }
}

onMounted(loadApplies)
</script>
