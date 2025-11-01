<script setup>
import { ref, computed, defineEmits, defineProps, defineOptions } from 'vue'
import { ElAvatar, ElButton, ElInput } from 'element-plus'

defineOptions({ name: 'CommentTree' })

const props = defineProps({
  node: { type: Object, required: true },
  depth: { type: Number, default: 0 }
})
const emit = defineEmits(['submit-reply', 'toggle-like'])

const showReplyEditor = ref(false)
const replyContent = ref('')
const MAX_LEN = 200
const canSubmit = computed(() => {
  const len = replyContent.value.trim().length
  return len > 0 && len <= MAX_LEN
})

const toggleReply = () => {
  showReplyEditor.value = !showReplyEditor.value
}

const submit = () => {
  if (!canSubmit.value) return
  emit('submit-reply', { parentId: props.node.id, content: replyContent.value.trim() })
  replyContent.value = ''
  showReplyEditor.value = false
}

// 深层默认折叠，避免一次渲染过多节点
const expanded = ref(props.depth < 2)
const toggleExpand = () => {
  expanded.value = !expanded.value
}
</script>

<template>
  <div class="comment-node" :style="{ marginLeft: depth * 16 + 'px' }">
    <div class="node-header">
      <ElAvatar :src="node.user?.avatar" size="small" />
      <div class="meta">
        <span class="user">{{ node.user?.username || '游客' }}</span>
        <span class="time">{{ node.createTime }}</span>
        <span class="reply-count" v-if="(node.replyCount ?? (node.replies?.length || 0)) > 0">
          · 回复 {{ node.replyCount ?? (node.replies?.length || 0) }}
        </span>
      </div>
      <div class="ops">
        <ElButton class="like" :type="node.isLiked ? 'primary' : 'default'" size="small" @click="emit('toggle-like', node)">
          👍 {{ node.likeCount || 0 }}
        </ElButton>
        <ElButton class="reply" size="small" v-if="depth === 0" @click="toggleReply">回复</ElButton>
      </div>
    </div>
    <div class="content">{{ node.content }}</div>

    <div v-if="showReplyEditor && depth === 0" class="reply-editor">
      <ElInput
        v-model="replyContent"
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 6 }"
        :maxlength="MAX_LEN"
        show-word-limit
        placeholder="回复内容（最多200字）"
      />
      <div class="actions">
        <ElButton type="primary" size="small" :disabled="!canSubmit" @click="submit">提交回复</ElButton>
        <ElButton size="small" @click="toggleReply">取消</ElButton>
      </div>
    </div>

    <div class="children" v-if="node.replies && node.replies.length">
      <div v-if="!expanded" class="expand-line">
        <ElButton link type="primary" @click="toggleExpand">展开 {{ node.replies.length }} 条回复</ElButton>
      </div>
      <div v-else>
        <CommentTree
          v-for="child in node.replies"
          :key="child.id"
          :node="child"
          :depth="depth + 1"
          @submit-reply="emit('submit-reply', $event)"
          @toggle-like="emit('toggle-like', $event)"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.comment-node {
  padding: 10px 8px;
  border-bottom: 1px solid #ebeef5;
}
.node-header {
  display: flex;
  align-items: center;
  gap: 10px;
}
.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
  font-size: 13px;
  flex: 1;
}
.user { color: #303133; font-weight: 500; }
.time { color: #909399; }
.reply-count { color: #909399; }
.ops { display: flex; gap: 8px; }
.content { margin-top: 6px; font-size: 14px; color: #303133; line-height: 1.6; }
.reply-editor { margin-top: 8px; }
.reply-editor .actions { margin-top: 6px; display: flex; gap: 8px; }
.children { margin-top: 6px; }
.expand-line { margin-top: 4px; color: #409eff; }
</style>