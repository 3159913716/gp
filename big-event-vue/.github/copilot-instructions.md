## 项目速览（给 AI 编码助手）

这是一个基于 Vue 3 + Vite 的后台管理前端（大事件练习项目）。下面是立即可用的关键信息与约定，便于快速开始修改、调试或实现新功能。

- 目录别名：`@` 映射到 `src/`（见 `vite.config.js`）。
- 运行脚本（见 `package.json`）：
  - 开发：`npm run dev`
  - 打包：`npm run build`
  - 预览：`npm run preview`

## 架构与主要边界

- 路由：`src/router/index.js`，使用 `meta.requiresAuth` 标记需要认证的路由。全局守卫在 `beforeEach` 中使用 `useTokenStore()` 验证 `token`。
- 状态：Pinia（composition style），stores 在 `src/stores/*`，重要的有 `token.js`（保存/移除 token）和 `userInfo.js`（用户信息）。两者都启用了 persist（本地持久化）。
- API 层：`src/api/*.js` 封装具体接口，例如 `src/api/article.js` 提供文章/分类的 CRUD 函数。
- 网络层：`src/utils/request.js` 创建了一个 axios 实例，baseURL 为 `/api`（Vite dev 时通过代理到后端 `http://localhost:8080`），
  - 请求拦截器会从 `useTokenStore().token` 取值并写入 `config.headers.Authorization`，因此 `token` 值应包含完整格式（仓库注释示例是 `Bearer <token>`）。
  - 响应拦截器返回 `result.data` —— 上层 API 调用直接得到业务数据。

## 可修改/扩展时的具体规则

- 不要修改 `baseURL` 或 Vite 的 `/api` 前缀，除非同时调整 `vite.config.js` 的 proxy 配置。
- API 方法（如 `articleCategoryListService`）假设 axios 返回的是业务 data，函数签名中常用 `params` 或直接传对象（POST/PUT）。请保持同样风格。
- token 管理：移除 token 时要同时调用 `tokenStore.removeToken()` 并使用 router 跳转到 `Login`（项目中已有 `redirectToLogin`/router 逻辑），避免仅清储存而不跳转。
- Pinia store 风格：使用 composition API 风格（`defineStore('name', () => { const x = ref(); return { x } }, { persist: true })`）。外部使用时可直接访问 store 中的属性（不需要 `.value`）。

## 常见改动示例（用于 PR / 代码补全）
- 如果新增 API：在 `src/api/` 新建模块并使用 `import request from '@/utils/request.js'`，确保返回 promise 并使用 `request.get/post/put/delete`。
- 若修改路由守卫（鉴权流程），请参考 `src/router/index.js` 的 `beforeEach`：使用 `to.matched.some(record => record.meta.requiresAuth)` 判断并用 `useTokenStore()` 检查 `token`。

## 调试与本地开发提示

- 本地后端：Vite 开发代理将 `/api` 转发到 `http://localhost:8080`（见 `vite.config.js`）。若后端地址不同，请同时更新代理。
- 快速启动（PowerShell）：
```powershell
npm install
npm run dev
```

## 代码风格与注意点（从代码中可观察）
- 组件与视图放在 `src/views/`（如 `views/article/ArticleManage.vue`），页面级路由通常在 `router` 中直接 import 对应视图组件。
- UI 框架：Element Plus（`element-plus`），全局注册在 `src/main.js`，使用中文语言包。
- 富文本：使用 `@vueup/vue-quill`（已列为依赖）。

## 变更建议（AI 助手在提交补丁时应遵守）
- 保持 Pinia persist 配置；不要无意中删除 `persist: true`。
- 新增 token 的格式需要与后端一致：若后端期望 `Bearer <token>`，store 中的 token 应当包含 `Bearer ` 前缀（仓库注释已有提示）。
- 修改请求封装时，确保响应仍然返回 `data`（否则需同时更新所有 `src/api/*` 的调用点）。

如果这些说明中有不明确的地方或你需要我把某部分扩展成 PR 模板 / 自动修复脚本，请告诉我要补充的方向。谢谢！

补充说明：操作日志导出
-----
- 本项目曾在前端尝试实现“操作日志导出（CSV）”的临时功能，但由于没有稳定的后端导出接口，该前端实现已被移除。
- 推荐做法：后端提供导出接口（例如 POST `/admin/logs/export` 返回文件流或生成文件 URL），然后前端调用该接口实现可靠导出。若后端提供该接口，我可以把前端适配实现补回。
