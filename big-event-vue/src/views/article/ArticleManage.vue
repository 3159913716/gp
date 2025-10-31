<!--
  文章管理组件：
  实现文章的分类管理、增删改查及富文本编辑功能
-->

<script setup>
/* 从Vue引入ref函数：用于创建响应式数据 */
import { ref } from 'vue'

/* 
  导入Element Plus图标组件：
  Edit - 编辑图标
  Delete - 删除图标
  Plus - 添加图标
*/
import { Edit, Delete, Plus } from '@element-plus/icons-vue'

/* 
  导入Element Plus消息组件：
  ElMessage - 全局提示消息
  ElMessageBox - 模态对话框
*/
import { ElMessage, ElMessageBox } from 'element-plus'

/* 导入文章相关API接口函数 */
import { 
  articleCategoryListService,  // 获取分类列表
  articleAddService,           // 添加文章
  articleDeleteService,        // 删除文章
  articleUpdateService         // 更新文章
} from '@/api/article.js'

/* 导入自定义的Axios实例（用于直接调用API） */
import request from '@/utils/request.js'

/* 导入富文本编辑器组件 */
import { QuillEditor } from '@vueup/vue-quill'
import '@vueup/vue-quill/dist/vue-quill.snow.css'  // 编辑器样式

/* 导入存储用户token的Pinia store */
import { useTokenStore } from '@/stores/token.js'

// 初始化Pinia token store
const tokenStore = useTokenStore()

/*
  响应式数据定义：
  ref() 创建响应式引用对象
*/
// 文章分类数据
const categorys = ref([
  {
    "id": "",
    "categoryName": "",
    "categoryAlias": "",
    "updateTime": ""
  }
])

// 文章列表数据
const articles = ref([
  {
    "id": "",
    "title": "",
    "content": "",
    "coverImg": "",
    "state": "",
    "categoryId": "",
    "createTime": "",
    "updateTime": "",
  },
])

// 筛选条件：分类ID和文章状态
const categoryId = ref('')  // 当前选中的分类ID
const state = ref('')       // 当前选中的文章状态

/*
  分页状态管理：
  pageNum - 当前页码
  pageSize - 每页显示数量
  total - 总数据条数
*/
const pageNum = ref(1)      // 默认第1页
const total = ref(20)       // 默认总条数20
const pageSize = ref(3)     // 默认每页3条

/*
  UI状态：
  visibleDrawer - 控制右侧抽屉显示/隐藏
  title - 抽屉标题（用于区分添加/编辑）
*/
const visibleDrawer = ref(false)  // 默认隐藏抽屉
const title = ref('')             // 抽屉标题

/*
  表单相关：
  articleForm - 表单引用（用于调用表单方法）
  articleModel - 文章表单数据模型
*/
const articleForm = ref(null)   // 表单实例引用
const articleModel = ref({      // 文章表单数据
  title: '',       // 文章标题
  categoryId: null,  // 分类ID - 使用null避免默认选中第一个选项
  coverImg: '',    // 封面图URL
  content: '',     // 文章内容(HTML)
  state: '',       // 文章状态
})

/*
  表单验证规则对象：
  使用Element Plus的表单验证规则结构
  包含必填验证和触发条件设置
*/
const rules = ref({
  // 标题验证规则：必填，触发条件为blur（失去焦点）
  title: [
    {
      required: true,
      message: '文章标题不能为空',
      trigger: 'blur'
    }
  ],
  // 分类ID验证规则：必填，触发条件为change（值改变）
  categoryId: [
    {
      required: true,
      message: '文章分类不能为空',
      trigger: 'change'
    }
  ],
  // 内容验证规则：必填，触发条件为blur
  content: [
    {
      required: true,
      message: '文章内容不能为空',
      trigger: 'blur'
    }
  ],
  // 封面图验证规则：必填，触发条件为blur
  coverImg: [
    {
      required: true,
      message: '文章封面不能为空',
      trigger: 'blur'
    }
  ],
})

/*
  生命周期钩子函数：
  在组件初始化时加载分类和文章列表
*/

// 加载文章分类列表
const articleCategoryList = async () => {
  const result = await articleCategoryListService()
  categorys.value = result.data
}

// 加载文章列表（带分页和筛选条件）
const articleList = async () => {
  try {
    // 构建查询参数字符串，直接拼接URL
    let queryStr = '?pageNum=' + pageNum.value + '&pageSize=' + pageSize.value;
    
    // 添加可选参数
    if (categoryId.value) {
      queryStr += '&categoryId=' + categoryId.value;
    }
    if (state.value) {
      queryStr += '&state=' + state.value;
    }
    
    // 直接使用request.get调用API，手动添加Authorization头
    const tokenStore = useTokenStore();
    const headers = {};
    if (tokenStore.token) {
      const raw = tokenStore.token;
      headers.Authorization = raw.startsWith('Bearer ') ? raw : `Bearer ${raw}`;
    }
    
    console.log('发送请求:', '/article' + queryStr);
    const response = await request.get('/article' + queryStr, { headers });
    
    // 处理响应数据
    console.log('=== 完整API响应 ===', JSON.stringify(response));
    
    try {
      // 根据接口文档格式处理响应
      console.log('响应数据:', response?.data);
      
      if (response && response.code === 0 && response.data) {
        // 按照接口文档的格式解析: response.data.item
        if (response.data.item && Array.isArray(response.data.item)) {
          articles.value = response.data.item;
          total.value = response.data.total || articles.value.length;
          console.log('使用response.data.item作为文章数组');
        } else {
          console.log('API返回数据格式不符合接口文档规范');
          articles.value = [];
          total.value = 0;
        }
        
        console.log('最终articles数组长度:', articles.value.length);
        console.log('最终total值:', total.value);
        
        // 手动触发视图更新
        articles.value = [...articles.value];
      } else {
        console.log('API返回失败或格式不正确:', response);
        articles.value = [];
        total.value = 0;
        ElMessage.error(response?.message || '获取文章列表失败');
      }
    } catch (e) {
      console.error('处理响应数据时出错:', e);
      articles.value = [];
      total.value = 0;
    }
  } catch (error) {
    console.error('获取文章列表失败:', error);
    ElMessage.error('获取文章列表失败，请稍后重试');
    articles.value = [];
    total.value = 0;
  }

  // 为每篇文章添加分类名称（通过匹配categoryId）
  for (let i = 0; i < articles.value.length; i++) {
    const article = articles.value[i]
    for (let j = 0; j < categorys.value.length; j++) {
      if (article.categoryId === categorys.value[j].id) {
        article.categoryName = categorys.value[j].categoryName
      }
    }
  }
}

/* 
  分页控制函数：
  处理分页大小和当前页变化
*/

// 每页显示数量变化时的回调
const onSizeChange = (size) => {
  pageSize.value = size  // 更新每页显示数量
  articleList()         // 重新加载数据
}

// 当前页码变化时的回调
const onCurrentChange = (num) => {
  pageNum.value = num  // 更新当前页码
  articleList()       // 重新加载数据
}

/*
  表单操作函数：
  清空表单数据和重置表单状态
*/

// 清空表单并初始化添加文章的UI状态
const clearArticelForm = () => {
  title.value = '新增文章'  // 设置抽屉标题
  
  // 强制重置表单验证状态
  if (articleForm.value) {
    articleForm.value.resetFields()
  }
  
  // 重置表单数据 - 使用undefined确保空状态
  articleModel.value = {
    title: '',
    categoryId: null,  // 使用null避免默认选中第一个选项
    coverImg: '',
    content: '',
    state: '',
    id: undefined
  }
  
  // 清空富文本编辑器内容（如果已渲染）
  if (document.querySelector('.ql-editor')) {
    document.querySelector('.ql-editor').innerHTML = ''
  }
  
  // 强制更新视图
  articleModel.value = { ...articleModel.value }
}

/*
  上传功能回调：
  处理图片上传成功后的响应
*/

// 图片上传成功回调函数
const uploadSuccess = (result) => {
  articleModel.value.coverImg = result.data  // 更新封面图URL
}

/*
  编辑功能：
  显示编辑表单并填充数据
*/

// 显示编辑抽屉（行数据回填）
const showDialog = (row) => {
  // 重置表单验证状态
  if (articleForm.value) {
    articleForm.value.resetFields()
  }
  visibleDrawer.value = true  // 显示抽屉
  title.value = '编辑文章'     // 设置抽屉标题
  
  // 先重置模型为空状态
  articleModel.value = {
    title: '',
    categoryId: '',
    coverImg: '',
    content: '',
    state: '',
    id: undefined
  }
  
  // 延迟填充数据，确保Vue响应式系统更新DOM
  setTimeout(() => {
    // 使用行数据填充表单模型
    articleModel.value = {
      title: row.title || '',          // 填充标题
      categoryId: row.categoryId || '', // 填充分类ID，使用空字符串避免默认选中第一个选项
      coverImg: row.coverImg || '',     // 填充封面图URL
      content: row.content || '',       // 填充内容
      state: row.state || '',           // 填充状态
      id: row.id                        // 保存文章ID（用于后续更新）
    }
  }, 0)
}

/*
  文章CRUD操作：
  包含添加、更新和删除功能
*/

// 添加文章（状态作为参数传入）
const addArticle = (state) => {
  // 表单验证
  articleForm.value.validate(async (valid) => {
    if (valid) {  // 验证通过
      articleModel.value.state = state  // 设置状态（已发布/草稿）
      await articleAddService(articleModel.value) // 调用添加API
      ElMessage.success('添加成功')   // 显示成功提示
      visibleDrawer.value = false    // 关闭抽屉
      articleList()                  // 刷新列表
    } else {      // 验证失败
      ElMessage.error('请填写完整表单内容')
    }
  })
}

// 更新文章
const articleUpdate = async (state) => {
  // 表单验证
  articleForm.value.validate(async (valid) => {
    if (valid) {  // 验证通过
      articleModel.value.state = state   // 设置状态
      await articleUpdateService(articleModel.value) // 调用更新API
      ElMessage.success('修改成功')      // 显示成功提示
      visibleDrawer.value = false       // 关闭抽屉
      articleList()                     // 刷新列表
    } else {      // 验证失败
      ElMessage.error('请填写完整表单内容')
    }
  })
}

// 删除文章
const deleteArticle = (row) => {
  // 显示确认对话框
  ElMessageBox.confirm(
    '你确认删除该文章吗？',  // 提示内容
    '温馨提示',             // 标题
    {                      // 配置选项
      confirmButtonText: '确认',   // 确认按钮文本
      cancelButtonText: '取消',    // 取消按钮文本
      type: 'warning',             // 警告类型（显示警告图标）
    }
  )
    .then(async () => {  // 用户点击确认
      await articleDeleteService(row.id)  // 调用删除API
      ElMessage.success('删除成功')        // 显示成功提示
      articleList()                       // 刷新列表
    })
    .catch(() => {       // 用户点击取消
      // 显示取消操作提示
      ElMessage({
        type: 'info',
        message: '取消删除',
      })
    })
}

/*
  初始化数据加载：
  组件创建时自动执行
*/
articleCategoryList()  // 加载分类列表
articleList()          // 加载文章列表
</script>

<template>
  <div class="container">
     <!-- 
    Element Plus 卡片组件：
    class="page-container" - 自定义样式类名
  -->
  <el-card class="page-container">
    <!-- 卡片头部插槽 -->
    <template #header>
      <div class="header">
        <span>文章管理</span>
        <div class="extra">
          <!-- 
            添加文章按钮：
            @click - 点击事件：显示抽屉并清空表单
          -->
          <el-button type="primary" @click="visibleDrawer = true; clearArticelForm()">
            添加文章
          </el-button>
          <!-- 
            刷新数据按钮：
            @click - 点击事件：重新加载文章列表
          -->
          <el-button @click="articleList">
            刷新数据
          </el-button>
        </div>
      </div>
    </template>
    
    <!-- 
      搜索表单：
      inline属性设置表单内联显示
    -->
    <el-form inline>
      <!-- 文章分类筛选 -->
      <el-form-item label=" 文章分类：">
        <!-- 
          分类选择器：
          v-model绑定categoryId（响应式变量）
          placeholder - 选择框占位符
          style - 设置宽度为200px
        -->
        <el-select placeholder="请选择" v-model="categoryId" style="width: 200px;">
          <!-- 
            选项渲染：
            v-for遍历categorys数组
            :key - 唯一标识符（使用分类id）
            :label - 显示文本（分类名称）
            :value - 选项值（分类id）
          -->
          <el-option v-for="c in categorys" :key="c.id" :label="c.categoryName" :value="c.id">
          </el-option>
        </el-select>
      </el-form-item>

      <!-- 发布状态筛选 -->
      <el-form-item label="发布状态：">
        <el-select placeholder="请选择" v-model="state" style="width: 200px;">
          <!-- 固定选项 -->
          <el-option label=" 已发布" value="已发布"></el-option>
          <el-option label="草稿" value="草稿"></el-option>
        </el-select>
      </el-form-item>
      
      <!-- 操作按钮 -->
      <el-form-item>
        <!-- 
          搜索按钮：
          @click触发articleList函数（带筛选条件）
        -->
        <el-button type="primary" @click="articleList">搜索</el-button>
        <!-- 
          重置按钮：
          @click清除筛选条件并重新加载数据
        -->
        <el-button @click="categoryId = ''; state = ''; articleList()">重置</el-button>
      </el-form-item>
    </el-form>
    
    <!-- 
      文章表格：
      :data="articles" - 绑定数据源
      style - 设置宽度100%
    -->
    <el-table :data="articles" style="width: 100%">
      <!-- 文章标题列（宽度400px，显示prop="title"） -->
      <el-table-column label="文章标题" width="400" prop="title"></el-table-column>
      <!-- 分类列（显示prop="categoryName"） -->
      <el-table-column label="分类" prop="categoryName"></el-table-column>
      <!-- 发表时间列 -->
      <el-table-column label="发表时间" prop="createTime"> </el-table-column>
      <!-- 状态列 -->
      <el-table-column label="状态" prop="state"></el-table-column>
      <!-- 操作列（固定宽度100px） -->
      <el-table-column label="操作" width="100">
        <!-- 作用域插槽：获取当前行数据 -->
        <template #default="{ row }">
          <!-- 
            编辑按钮：
            :icon - 绑定编辑图标
            circle - 圆形按钮
            plain - 朴素样式
            type - 主色调按钮
            @click - 点击事件：显示抽屉并回填数据
          -->
          <el-button :icon="Edit" circle plain type="primary"
            @click="visibleDrawer = true; showDialog(row)"></el-button>
          <!-- 
            删除按钮：
            type - 危险操作样式（红色）
            @click - 点击事件：触发删除确认
          -->
          <el-button :icon="Delete" circle plain type="danger" @click="deleteArticle(row)"></el-button>
        </template>
      </el-table-column>
      
      <!-- 空数据状态模板 -->
      <template #empty>
        <el-empty description="没有数据" />
      </template>
    </el-table>
    
    <!-- 
      分页组件：
      v-model:current-page - 双向绑定当前页码
      v-model:page-size - 双向绑定每页数量
      :page-sizes - 每页显示条数选项
      layout - 分页组件布局配置
      background - 添加背景色
      :total - 总数据条数
      @size-change - 每页条数变化事件
      @current-change - 当前页变化事件
    -->
    <el-pagination 
      v-model:current-page="pageNum" 
      v-model:page-size="pageSize" 
      :page-sizes="[3, 5, 10, 15]"
      layout="jumper, total, sizes, prev, pager, next" 
      background 
      :total="total" 
      @size-change="onSizeChange"
      @current-change="onCurrentChange" 
      style="margin-top: 20px; justify-content: flex-end" 
    />
    
    <!-- 
      右侧抽屉（用于添加/编辑文章）：
      v-model - 双向绑定抽屉显示状态
      :title - 动态标题
      direction="rtl" - 从右侧滑出
      size="50%" - 宽度占50%
      :show-close="true" - 显示关闭按钮
    -->
    <el-drawer v-model="visibleDrawer" :title="title" direction="rtl" size="50%" :show-close="true">
      <!-- 
        文章表单：
        :model - 表单数据绑定
        :rules - 表单验证规则
        label-width - 标签宽度
        ref - 表单引用标识
      -->
      <el-form 
        :model="articleModel" 
        label-width="100px" 
        :rules="rules" 
        ref="articleForm"
      >
        <!-- 标题输入框 -->
        <el-form-item label="文章标题" prop="title">
          <el-input v-model="articleModel.title" placeholder="请输入标题"></el-input>
        </el-form-item>
        
        <!-- 分类选择器 -->
        <el-form-item label="文章分类" prop="categoryId">
          <el-select placeholder="请选择" v-model="articleModel.categoryId">
            <!-- 遍历分类选项 -->
            <el-option v-for="c in categorys" :key="c.id" :label="c.categoryName" :value="c.id">
            </el-option>
          </el-select>
        </el-form-item>
        
        <!-- 封面图上传 -->
        <el-form-item label="文章封面" prop="coverImg">
          <!-- 
            Element Plus上传组件：
            class - 自定义样式类名
            :auto-upload="true" - 自动上传
            :show-file-list="false" - 不显示文件列表
            action - 上传API地址
            name="file" - 文件字段名
            :on-success - 上传成功回调
            :headers - 上传请求头（带认证token）
          -->
          <el-upload 
            class="avatar-uploader" 
            :auto-upload="true" 
            :show-file-list="false" 
            action="/api/upload"
            name="file" 
            :on-success="uploadSuccess" 
            :headers="{ 'Authorization': tokenStore.token }"
          >
            <!-- 图片预览 -->
            <img v-if="articleModel.coverImg" :src="articleModel.coverImg" class="avatar" />
            <!-- 上传占位图标 -->
            <el-icon v-else class="avatar-uploader-icon">
              <Plus />  <!-- 使用Plus图标 -->
            </el-icon>
          </el-upload>
        </el-form-item>
        
        <!-- 文章内容编辑器 -->
        <el-form-item label="文章内容" prop="content">
          <div class="editor">
            <!-- 
              Quill富文本编辑器：
              theme="snow" - 使用snow主题
              v-model:content - 双向绑定内容
              contentType="html" - 数据类型为HTML
            -->
            <quill-editor 
              theme="snow" 
              v-model:content="articleModel.content" 
              contentType="html"
            >
            </quill-editor>
          </div>
        </el-form-item>
        
        <!-- 表单操作按钮 -->
        <el-form-item>
          <!-- 
            发布按钮：
            根据title判断执行添加或更新操作，传入"已发布"状态
          -->
          <el-button type="primary" @click="title === '新增文章' ? addArticle('已发布') : articleUpdate('已发布')">
            发布
          </el-button>
          <!-- 
            草稿按钮：
            传入"草稿"状态
          -->
          <el-button type="info" @click="title === '新增文章' ? addArticle('草稿') : articleUpdate('草稿')">
            草稿
          </el-button>
        </el-form-item>
      </el-form>
    </el-drawer>
  </el-card>
  </div>
 
</template>

<!-- 
  Scoped CSS:
  使用SCSS预处理器，scoped属性限制样式作用域
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
}
/* 页面容器样式 */
.page-container {
  min-height: 100%;     /* 最小高度100% */
  box-sizing: border-box; /* 边框盒模型 */
  border-radius: 8px;
  padding: 15px;
  /* 头部样式 */
  .header {
    display: flex;              /* 弹性布局 */
    align-items: center;         /* 垂直居中 */
    justify-content: space-between; /* 两端对齐 */
  }
}

/* 上传组件样式 */
.avatar-uploader {
  /* 深度选择器（穿透scoped） */
  :deep() {
    /* 预览图样式 */
    .avatar {
      width: 178px;
      height: 178px;
      display: block;
    }

    /* 上传区域样式 */
    .el-upload {
      border: 1px dashed var(--el-border-color); /* 虚线边框 */
      border-radius: 6px; /* 圆角 */
      cursor: pointer;    /* 手型指针 */
      position: relative; 
      overflow: hidden;
      transition: var(--el-transition-duration-fast); /* 过渡效果 */
    }

    /* 鼠标悬停效果 */
    .el-upload:hover {
      border-color: var(--el-color-primary); /* 主色调边框 */
    }

    /* 上传图标样式 */
    .el-icon.avatar-uploader-icon {
      font-size: 28px;
      color: #8c939d;   /* 图标颜色 */
      width: 178px;
      height: 178px;
      text-align: center;
    }
  }
}

/* 富文本编辑器样式 */
.editor {
  width: 100%;

  /* 深度选择器修改编辑器样式 */
  :deep(.ql-editor) {
    min-height: 200px;  /* 最小高度200px */
  }
}
</style>