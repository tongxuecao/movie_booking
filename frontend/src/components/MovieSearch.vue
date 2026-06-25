<script setup>
import { ref } from 'vue'

const emit = defineEmits(['search'])

const keyword = ref('')

function doSearch() {
  emit('search', keyword.value.trim())
}

function clear() {
  keyword.value = ''
  emit('search', '')
}

defineExpose({ clear })
</script>

<template>
  <div class="movie-search">
    <div class="ms-input-row">
      <el-input
        v-model="keyword"
        placeholder="搜索片名 / 导演 / 主演 / 影片类型…"
        clearable
        @keyup.enter="doSearch"
        @clear="emit('search', '')"
        class="ms-input"
      >
        <template #prefix>
          <span style="font-size:14px">🔍</span>
        </template>
      </el-input>
      <el-button type="primary" @click="doSearch">搜索</el-button>
    </div>
  </div>
</template>

<style scoped>
.movie-search {
  margin-bottom: 18px;
}

.ms-input-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.ms-input {
  flex: 1;
  max-width: 440px;
}
</style>
