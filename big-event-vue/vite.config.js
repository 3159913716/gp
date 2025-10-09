import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

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
      //获取路径中包含了/api的请求
      '/api':{
        // 后台服务所在的源
        target:'http://localhost:8080',
        //修改源
        changeOrigin:true, 
        //把路径中的/api替换为''
        rewrite:(path)=>path.replace(/^\/api/,'') 

      }
    }
  }
})
