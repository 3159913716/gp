<template>
  <div>
    <el-card>
      <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px">
        <div style="font-weight:600">系统设置</div>
        <div>
          <el-button type="text" @click="toggleView">{{ showJson ? '表单模式' : 'JSON 模式' }}</el-button>
        </div>
      </div>

      <div v-if="!showJson">
        <el-form :model="formModel" label-width="120px">
          <template v-for="(field, idx) in fields" :key="field.key">
            <el-form-item :label="field.key">
              <component
                :is="field.component"
                v-model="formModel[field.key]"
                v-bind="field.props"
              />
            </el-form-item>
          </template>

          <el-form-item>
            <el-button type="primary" @click="saveForm">保存</el-button>
            <el-button @click="reset">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <div v-else>
        <el-form>
          <el-form-item label="设置（JSON）">
            <el-input type="textarea" :rows="12" v-model="jsonText" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="saveJson">保存</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getSettings, updateSettings } from '@/api/admin.js'

const showJson = ref(false)
const jsonText = ref('')
const formModel = ref({})
const fields = ref([])

function toggleView() {
  showJson.value = !showJson.value
}

function buildFieldsFromObject(obj) {
  const f = []
  for (const [k, v] of Object.entries(obj || {})) {
    const t = typeof v
    if (t === 'boolean') {
      f.push({ key: k, component: 'el-switch', props: {} })
    } else if (t === 'number') {
      f.push({ key: k, component: 'el-input-number', props: { controlsPosition: 'right' } })
    } else if (t === 'string') {
      f.push({ key: k, component: 'el-input', props: {} })
    } else {
      // for objects/arrays - show stringified textarea in JSON mode only
      f.push({ key: k, component: 'el-input', props: {} })
      // ensure value is string
      formModel.value[k] = JSON.stringify(v)
      continue
    }
    formModel.value[k] = v
  }
  fields.value = f
}

async function load() {
  try {
    const res = await getSettings()
    const data = res.data || res || {}
    // initialize
    jsonText.value = JSON.stringify(data, null, 2)
    formModel.value = {}
    buildFieldsFromObject(data)
  } catch (e) {
    console.error('getSettings failed', e)
    ElMessage.error('加载设置失败')
  }
}

async function saveForm() {
  try {
    // prepare payload: parse any stringified JSON values if needed
    const payload = {}
    for (const key of Object.keys(formModel.value)) {
      const val = formModel.value[key]
      // try to detect JSON string
      if (typeof val === 'string') {
        try { payload[key] = JSON.parse(val) } catch { payload[key] = val }
      } else payload[key] = val
    }
    await updateSettings(payload)
    ElMessage.success('保存成功')
    // refresh
    await load()
  } catch (e) {
    console.error('save settings failed', e)
    ElMessage.error('保存失败')
  }
}

async function saveJson() {
  try {
    const body = JSON.parse(jsonText.value)
    await updateSettings(body)
    ElMessage.success('保存成功')
    await load()
  } catch (e) {
    console.error('save settings failed', e)
    ElMessage.error('保存失败：请检查 JSON 格式')
  }
}

function reset() {
  // reload settings
  load()
}

onMounted(load)
</script>
