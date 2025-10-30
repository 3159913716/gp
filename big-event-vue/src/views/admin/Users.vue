<template>
  <div>
    <div style="display:flex; gap:12px; align-items:center; margin-bottom:16px">
      <el-input v-model="filters.username" placeholder="按用户名搜索" clearable style="width:240px" @keyup.enter.native="loadUsers" />
      <el-select v-model="filters.role" placeholder="角色" clearable style="width:140px">
        <el-option :label="'管理员'" :value="0" />
        <el-option :label="'作者'" :value="1" />
        <el-option :label="'普通用户'" :value="2" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable style="width:140px">
        <el-option :label="'启用'" :value="0" />
        <el-option :label="'禁用'" :value="1" />
      </el-select>
      <el-button type="primary" @click="loadUsers">查询</el-button>
      <el-button @click="resetFilters">重置</el-button>
    </div>

    <el-card>
      <el-table :data="users" stripe style="width:100%">
          <el-table-column prop="id" label="ID" width="80" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column prop="nickname" label="昵称" />
          <el-table-column prop="email" label="邮箱" />
          <el-table-column prop="role" label="角色" width="120">
            <template #default="{ row }">
              <span>{{ roleLabel(row.role) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 0 ? 'success' : 'danger'">{{ row.status === 0 ? '启用' : '禁用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="注册时间" width="180" />
          <el-table-column label="操作" width="260">
            <template #default="{ row }">
              <el-dropdown trigger="click">
                <el-button size="small">更多操作<i class="el-icon-arrow-down el-icon--right"></i></el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleChangeRole(row)">
                      修改角色
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleChangeStatus(row)" :disabled="isSelf(row)">
                      {{ row.status === 0 ? '禁用用户' : '启用用户' }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页 -->
        <div style="margin-top:20px; text-align:right">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
          />
        </div>
      </el-card>

    <!-- 修改角色对话框 -->
    <el-dialog title="修改用户角色" v-model="roleDialog.visible" width="500px">
      <div>为用户 <strong>{{ roleDialog.target?.username }}</strong> 选择新角色：</div>
      <el-select v-model="roleDialog.newRole" placeholder="选择角色" style="width:200px; margin-top:12px">
        <el-option :label="'作者'" :value="1" />
        <el-option :label="'普通用户'" :value="2" />
      </el-select>
      <template #footer>
        <el-button @click="roleDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="confirmChangeRole">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, updateUserRole, updateUserStatus } from '@/api/admin.js'
import useUserInfoStore from '@/stores/userInfo.js'

// 数据状态
const users = ref([])
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filters = ref({ username: '', role: null, status: null })

// 角色修改对话框状态
const roleDialog = ref({
  visible: false,
  target: null,
  newRole: null
})

// 加载用户列表
const loadUsers = async () => {
  const params = { 
    page: page.value, 
    pageSize: pageSize.value, 
    username: filters.value.username, 
    role: filters.value.role, 
    status: filters.value.status 
  }
  
  try {
    const res = await getUsers(params)
    // 响应数据格式处理
    if (res && res.data) {
      users.value = res.data.list || []
      total.value = res.data.total || 0
    } else {
      users.value = []
      total.value = 0
      ElMessage.warning('未返回用户数据')
    }
  } catch (err) {
    console.error('获取用户列表失败:', err)
    ElMessage.error('获取用户列表失败')
    users.value = []
    total.value = 0
  }
}

// 重置筛选条件
const resetFilters = () => {
  filters.value = { username: '', role: null, status: null }
  page.value = 1
  loadUsers()
}

// 角色标签映射
const roleLabel = (r) => {
  if (r === 0) return '管理员'
  if (r === 1) return '作者'
  return '普通用户'
}

// 检查是否为当前管理员自己
const isSelf = (row) => {
  const userInfoStore = useUserInfoStore()
  const currentAdminId = userInfoStore.info?.id
  return currentAdminId && row.id === currentAdminId
}

// 打开角色修改对话框
const handleChangeRole = (row) => {
  console.log('打开角色修改对话框:', row)
  if (isSelf(row)) {
    ElMessage.error('管理员不能修改自身角色')
    return
  }
  
  // 单独设置visible状态，确保响应式更新
  roleDialog.value.target = row
  roleDialog.value.newRole = row.role
  roleDialog.value.visible = true
  console.log('角色对话框状态:', roleDialog.value)
}

// 确认修改角色
const confirmChangeRole = async () => {
  const { target, newRole } = roleDialog.value
  
  if (!target || newRole === undefined) {
    ElMessage.error('请选择有效的角色')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认将用户 ${target.username} 的角色修改为 ${roleLabel(newRole)} 吗？`, 
      '二次确认', 
      { type: 'warning' }
    )
    
    const res = await updateUserRole(target.id, { role: newRole })
    const msg = (res && (res.msg || res.message)) || '修改成功'
    ElMessage.success(msg)
    roleDialog.value.visible = false
    loadUsers()
  } catch (err) {
    // 取消操作不显示错误
    if (err !== 'cancel') {
      console.error('修改角色失败:', err)
      ElMessage.error('操作失败')
    }
  }
}

// 修改用户状态
const handleChangeStatus = async (row) => {
  if (isSelf(row)) {
    ElMessage.error('管理员不能修改自身状态')
    return
  }
  
  const newStatus = row.status === 0 ? 1 : 0
  const actionText = newStatus === 0 ? '启用' : '禁用'
  
  try {
    await ElMessageBox.confirm(
      `确认${actionText}用户 ${row.username} 吗？`, 
      '二次确认', 
      { type: 'warning' }
    )
    
    const res = await updateUserStatus(row.id, { status: newStatus })
    const msg = (res && (res.msg || res.message)) || `${actionText}成功`
    ElMessage.success(msg)
    loadUsers()
  } catch (err) {
    if (err !== 'cancel') {
      console.error(`${actionText}用户失败:`, err)
      ElMessage.error('操作失败')
    }
  }
}

// 分页相关方法
const handleSizeChange = (newSize) => {
  pageSize.value = newSize
  loadUsers()
}

const handleCurrentChange = (newCurrent) => {
  page.value = newCurrent
  loadUsers()
}

// 初始加载
onMounted(loadUsers)
</script>
