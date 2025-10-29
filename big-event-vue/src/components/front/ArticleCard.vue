<template>
  <el-card class="article-card" :body-style="{ padding: '12px' }">
    <h3>{{ article.title }}</h3>
    <p>{{ article.summary || (article.content && article.content.slice(0, 120) + '...') }}</p>
    <div style="display:flex; justify-content:space-between; align-items:center; margin-top:8px">
      <div style="color:#999">{{ article.author?.nickname || article.author?.username }}</div>
      <el-button type="text" @click="$emit('read', article.id)">阅读</el-button>
    </div>
  </el-card>
</template>

<script setup>
import { defineProps } from 'vue'
const props = defineProps({ article: { type: Object, required: true } })
</script>
