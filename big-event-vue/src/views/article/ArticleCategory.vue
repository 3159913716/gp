<!--
     分类管理组件：
      实现文章分类页的增删改查功能
-->

<script setup>
/* 从 Vue 引入 ref 函数：用于创建响应式数据 */
import { ref } from 'vue'

/* 导入 Element Plus 的图标组件：
   Edit - 编辑图标
   Delete - 删除图标 */
import { Edit, Delete } from '@element-plus/icons-vue'

/* 导入 Element Plus 的消息组件：
   ElMessage - 用于显示全局提示
   ElMessageBox - 用于显示模态对话框 */
import { ElMessage, ElMessageBox } from 'element-plus'

/* 导入文章分类相关的 API 接口函数 */
import { 
  articleCategoryListService,   // 获取分类列表
  articleCategoryAddService,     // 添加分类
  articleCategoryUpdateService,  // 更新分类
  articleCategoryDeleteService   // 删除分类
} from '@/api/article.js'

/* 
  响应式数据定义：
  ref() 创建响应式引用对象，value 属性存储实际值 
*/
const categorys = ref([])       // 存储文章分类列表数据
const dialogVisible = ref(false) // 控制对话框显示/隐藏的状态
const title = ref('')            // 对话框标题

/* 
  表单引用与数据模型：
  ref(null) 创建模板引用，用于访问表单组件实例 
*/
const addCategoryFrom = ref(null)
/* 分类模型的响应式对象，用于表单数据绑定 */
const categoryModel = ref({
  categoryName: '',  // 分类名称
  categoryAlias: ''  // 分类别名
})

/*
  表单验证规则对象：
  使用 Element Plus 的表单验证规则结构
*/
const rules = {
  // 分类名称验证规则
  categoryName: [
    { 
      required: true,   // 必填验证
      message: '请输入分类名称', // 验证失败提示
      trigger: 'blur'   // 触发时机：失去焦点时验证
    },
  ],
  // 分类别名验证规则
  categoryAlias: [
    { 
      required: true, 
      message: '请输入分类别名',
      trigger: 'blur' 
    }
  ]
}

/* 
  获取文章分类列表的异步函数 
  async/await 语法处理异步操作
*/
const articleCategoryList = async () => {
  // 调用 API 获取数据
  let result = await articleCategoryListService();
  // 更新响应式数据
  categorys.value = result.data;
}
// 组件创建时立即调用获取数据
articleCategoryList();

/* 清空表单数据的函数 */
const clearCategoryModel = () => {
  categoryModel.value.categoryName = ''
  categoryModel.value.categoryAlias = ''
}

/* 
  显示编辑对话框的函数 
  @param {Object} row - 当前行的分类数据
*/
const showDialog = (row) => {
  // 重置表单验证状态（如果表单引用存在）
  if (addCategoryFrom.value) {
    addCategoryFrom.value.resetFields();
  }
  dialogVisible.value = true;      // 显示对话框
  title.value = '编辑分类'         // 设置对话框标题
  // 用当前行数据填充表单
  categoryModel.value.categoryName = row.categoryName
  categoryModel.value.categoryAlias = row.categoryAlias
  categoryModel.value.id = row.id   // 保存分类ID用于后续更新
}

/* 添加分类的异步函数 */
const addCategory = async () => {
  // 触发表单验证
  addCategoryFrom.value.validate(async (valid) => {
    if (valid) {  // 验证通过
      // 调用添加分类API
      await articleCategoryAddService(categoryModel.value);
      ElMessage.success('添加成功');  // 显示成功提示
      articleCategoryList();        // 刷新分类列表
      dialogVisible.value = false;   // 关闭对话框
    } else {      // 验证失败
      ElMessage.error('请填写完整表单内容');  // 显示错误提示
      return;
    }
  })
}

/* 更新分类的异步函数 */
const updateCategory = async () => {
  // 触发表单验证
  addCategoryFrom.value.validate(async (valid) => {
    if (valid) {  // 验证通过
      // 调用更新分类API
      await articleCategoryUpdateService(categoryModel.value);
      ElMessage.success('修改成功');  // 显示成功提示
      dialogVisible.value = false;    // 关闭对话框
      articleCategoryList();          // 刷新分类列表
    } else {      // 验证失败
      ElMessage.error('请填写完整表单内容');
      return;
    }
  });
}

/* 
  删除分类函数 
  @param {Object} row - 要删除的分类数据
*/
const deleteCategory = (row) => {
  /* 
    Element Plus 的确认对话框:
    ElMessageBox.confirm() 创建确认弹窗
    参数说明:
      '你确认删除该分类信息吗？' - 弹窗内容
      '温馨提示'                - 弹窗标题
      配置对象:
        confirmButtonText - 确认按钮文本
        cancelButtonText  - 取消按钮文本
        type             - 弹窗类型（warning 显示警告图标）
  */
  ElMessageBox.confirm(
    '你确认删除该分类信息吗？',
    '温馨提示',
    {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning',
    }
  )
    .then(async () => {  // 用户点击确认
      await articleCategoryDeleteService(row.id)  // 调用删除API
      ElMessage.success('删除成功')  // 显示成功提示
      articleCategoryList()          // 刷新分类列表
    })
    .catch(() => {  // 用户点击取消
      // 显示取消操作的提示
      ElMessage({
        type: 'info',
        message: '取消删除',
      })
    })
}
</script>

<template>
  <div class="container">
     <!-- 
    Element Plus 的卡片组件:
    class="page-container" - 自定义样式类名
  -->
  <el-card class="page-container">
    <!-- 
      卡片头部插槽:
      #header 是 Vue 的具名插槽语法
    -->
    <template #header>
      <div class="header">
        <span>文章分类</span>
        <div class="extra">
          <!-- 
            添加分类按钮:
            type="primary" - 主按钮样式
            @click - 点击事件处理程序
          -->
          <el-button type="primary" 
            @click="title = '添加分类'; dialogVisible = true; clearCategoryModel()">
            添加分类
          </el-button>
        </div>
      </div>
    </template>
    <!-- 
      Element Plus 的表格组件:
      :data="categorys" - 绑定表格数据源
      style="width: 100%" - 内联样式设置宽度
    -->
    <el-table :data="categorys" style="width: 100%">
      <!-- 
        序号列:
        type="index" - 自动生成索引列
        label="序号" - 列标题文本
        width="100" - 列宽度
      -->
      <el-table-column label="序号" width="100" type="index"> </el-table-column>
      <!-- 
        分类名称列:
        prop="categoryName" - 对应数据项的属性名
      -->
      <el-table-column label="分类名称" prop="categoryName"></el-table-column>
      <!-- 分类别名列 -->
      <el-table-column label="分类别名" prop="categoryAlias"></el-table-column>
      <!-- 操作列 -->
      <el-table-column label="操作" width="100">
        <!-- 
          作用域插槽:
          #default="{ row }" 获取当前行数据
        -->
        <template #default="{ row }">
          <!-- 
            编辑按钮:
            :icon="Edit" - 绑定编辑图标组件
            circle      - 圆形按钮
            plain       - 朴素按钮样式
            type="primary" - 按钮类型（主色调）
          -->
          <el-button :icon="Edit" circle plain type="primary" @click="showDialog(row)"></el-button>
          <!-- 
            删除按钮:
            type="danger" - 危险操作按钮（红色）
          -->
          <el-button :icon="Delete" circle plain type="danger" @click="deleteCategory(row)"></el-button>
        </template>
      </el-table-column>
      <!-- 当表格数据为空时显示的插槽 -->
      <template #empty>
        <!-- Element Plus 的空状态组件 -->
        <el-empty description="没有数据" />
      </template>
    </el-table>

    <!-- 
      添加/编辑分类对话框:
      v-model="dialogVisible" - 双向绑定对话框显示状态
      :title="title"         - 动态绑定对话框标题
      width="30%"            - 对话框宽度（百分比响应式）
    -->
    <el-dialog v-model="dialogVisible" :title="title" width="30%">
      <!-- 
        Element Plus 的表单组件:
        :model="categoryModel" - 绑定表单数据对象
        :rules="rules"         - 绑定验证规则
        label-width="100px"    - 标签宽度
        ref="addCategoryFrom"   - 表单引用标识
      -->
      <el-form 
        :model="categoryModel" 
        :rules="rules" 
        label-width="100px" 
        style="padding-right: 30px"
        ref="addCategoryFrom">
        <!-- 分类名称表单项 -->
        <el-form-item label="分类名称" prop="categoryName">
          <!-- 
            输入框组件:
            v-model="categoryModel.categoryName" - 双向绑定分类名称
            minlength="1" - 最小输入长度
            maxlength="10" - 最大输入长度
          -->
          <el-input v-model="categoryModel.categoryName" minlength="1" maxlength="10"></el-input>
        </el-form-item>
        <!-- 分类别名表单项 -->
        <el-form-item label="分类别名" prop="categoryAlias">
          <el-input v-model="categoryModel.categoryAlias" minlength="1" maxlength="15"></el-input>
        </el-form-item>
      </el-form>
      <!-- 对话框底部插槽 -->
      <template #footer>
        <span class="dialog-footer">
          <!-- 取消按钮 -->
          <el-button @click="dialogVisible = false">取消</el-button>
          <!-- 
            确认按钮:
            三元表达式根据标题区分添加/编辑操作
            type="primary" - 主按钮样式
          -->
          <el-button type="primary" @click="title === '添加分类' ? addCategory() : updateCategory()">
            确认
          </el-button>
        </span>
      </template>
    </el-dialog>
  </el-card>
  </div>
 
</template>

<!-- 
  Scoped CSS:
  scoped 属性使样式仅作用于当前组件
  lang="scss" 使用 SCSS 语法
-->
<style lang="scss" scoped>
/* 容器样式 */
.container {
  display: flex;
  flex-direction: column; /* 子元素垂直排列 */
  width: 100%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 20px;
  box-sizing: border-box;
  font-size: 16px;
}

.page-container {
  min-height: 100%;     /* 最小高度占满父容器 */
  box-sizing: border-box; /* 盒子模型计算方式 */
  border-radius: 8px;
  padding: 15px;
  /* 头部样式 */
  .header {
    display: flex;              /* 弹性布局 */
    align-items: center;         /* 垂直居中 */
    justify-content: space-between; /* 两端对齐 */
  }
}
</style>