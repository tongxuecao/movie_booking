<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  movie: { type: Object, required: true },
})

const router = useRouter()

function goDetail() {
  router.push(`/movie/${props.movie.id}`)
}
</script>

<template>
  <div class="movie-card" @click="goDetail" role="link" tabindex="0" @keydown.enter="goDetail">
    <div class="poster">
      <img
        v-if="movie.poster"
        :src="movie.poster"
        :alt="movie.title"
        loading="lazy"
      />
      <div v-else class="no-poster">
        <span class="no-poster-icon">🎬</span>
        <span>{{ movie.title }}</span>
      </div>
      <div class="rating" v-if="movie.rating">★ {{ movie.rating }}</div>
    </div>
    <div class="info">
      <h3 class="title">{{ movie.title }}</h3>
      <p class="meta">{{ movie.director }} / {{ movie.genre }}</p>
      <button class="buy-btn" @click.stop="goDetail">购票</button>
    </div>
  </div>
</template>

<style scoped>
.movie-card {
  background: var(--card-bg);
  border-radius: var(--radius);
  overflow: hidden;
  box-shadow: var(--shadow);
  transition: transform 0.2s, box-shadow 0.2s;
  cursor: pointer;
}

.movie-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 6px 24px rgba(0,0,0,0.14);
}

.poster {
  position: relative;
  aspect-ratio: 3 / 4;
  overflow: hidden;
  background: #e8e8e8;
}

.poster img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.no-poster {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #bbb;
  font-size: 13px;
}

.no-poster-icon { font-size: 36px; }

.rating {
  position: absolute;
  top: 8px;
  right: 8px;
  background: rgba(0,0,0,0.68);
  color: #ffb400;
  font-size: 13px;
  font-weight: 700;
  padding: 3px 10px;
  border-radius: 4px;
}

.info {
  padding: 12px;
}

.title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.meta {
  font-size: 12px;
  color: var(--text-light);
  margin-bottom: 10px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.buy-btn {
  width: 100%;
  padding: 8px 0;
  background: var(--primary);
  color: #fff;
  font-size: 13px;
  border-radius: 4px;
  transition: background 0.2s;
}

.buy-btn:hover { background: var(--primary-hover); }
</style>
