import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'
import { fileURLToPath } from 'node:url'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  //配置代理
  server:{
    proxy:{
      // 优先匹配邮箱验证码相关接口，保持后端路径中的 /api 前缀不被重写
      '/api/email':{
        target:'http://localhost:8080',
        changeOrigin:true
        // 不设置 rewrite，确保请求为 http://localhost:8080/api/email/**
      },
      // 其他接口维持原有重写规则
      '/api':{
        target:'http://localhost:8080',
        changeOrigin:true,
        rewrite:(path)=>path.replace(/^\/api/,'')
      }
    }
  }
})