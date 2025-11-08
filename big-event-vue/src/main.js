import { createApp } from 'vue'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

import './assets/main.scss'

import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import locale from 'element-plus/dist/locale/zh-cn.js'

import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'

// 创建应用实例
const app = createApp(App)
const pinia = createPinia()

// 配置Pinia持久化插件
pinia.use(piniaPluginPersistedstate)

// 安装插件和路由
app.use(pinia)
app.use(router)
app.use(ElementPlus, { locale })

// 挂载应用
app.mount('#app')