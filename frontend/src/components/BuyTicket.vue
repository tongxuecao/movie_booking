<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useShowtimeStore } from '../stores/showtimes.js'
import { useAuthStore } from '../stores/auth.js'

const props = defineProps({
  movie: { type: Object, required: true },
  cinemaId: { type: Number, default: null },
})

const emit = defineEmits(['close', 'needLogin'])
const router = useRouter()
const showtimeStore = useShowtimeStore()
const auth = useAuthStore()

const weekdays = ['日', '一', '二', '三', '四', '五', '六']

// Fetch showtimes on mount and when movie changes
async function loadShowtimes() {
  const params = { movieId: props.movie.id }
  if (props.cinemaId) params.cinemaId = props.cinemaId
  await showtimeStore.fetchShowtimes(params)
}

onMounted(loadShowtimes)
watch(() => props.movie.id, loadShowtimes)

// Flatten cinemaGroups into a list of showtimes with cinema info
const allShowtimes = computed(() => {
  const list = []
  for (const group of showtimeStore.cinemaGroups) {
    for (const s of (group.showtimes || [])) {
      list.push({ ...s, cinemaId: group.cinemaId, cinemaName: group.cinemaName })
    }
  }
  return list
})

// Group by date
const dateGroups = computed(() => {
  const groups = {}
  for (const s of allShowtimes.value) {
    const d = s.showDate
    if (!groups[d]) groups[d] = []
    groups[d].push(s)
  }
  return groups
})

const dates = computed(() => Object.keys(dateGroups.value).sort())
const selectedDate = ref('')

watch(dates, (d) => { if (d.length && !d.includes(selectedDate.value)) selectedDate.value = d[0] }, { immediate: true })

function selectDate(d) { selectedDate.value = d }
const showsForDate = computed(() => dateGroups.value[selectedDate.value] || [])

function goSeatSelect(showtime) {
  if (!auth.isLoggedIn) {
    emit('close')
    emit('needLogin')
    return
  }
  emit('close')
  router.push(`/seat-select/${showtime.id}`)
}
</script>

<template>
  <Teleport to="body">
    <div class="buyticket-overlay" @click.self="emit('close')">
      <div class="buyticket-modal">
        <div class="bt-header">
          <div class="bt-movie-info">
            <img v-if="movie.poster" :src="movie.poster" class="bt-poster" />
            <div v-else class="bt-no-poster">🎬</div>
            <div>
              <h3>{{ movie.title }}</h3>
              <p class="bt-meta">{{ movie.genre }} / {{ movie.duration }}分钟</p>
            </div>
          </div>
          <button class="bt-close" @click="emit('close')">&times;</button>
        </div>

        <div class="bt-body">
          <div class="bt-dates" v-if="dates.length">
            <button
              v-for="d in dates"
              :key="d"
              :class="{ active: selectedDate === d }"
              @click="selectDate(d)"
            >
              <span class="date-day">{{ d.slice(5) }}</span>
              <span class="date-week">{{ weekdays[new Date(d + 'T00:00:00').getDay()] }}</span>
            </button>
          </div>

          <div class="bt-table-wrap" v-if="showsForDate.length">
            <table class="bt-table">
              <thead>
                <tr>
                  <th>放映时间</th>
                  <th>影厅</th>
                  <th>影院</th>
                  <th>余座</th>
                  <th class="col-price">票价</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="s in showsForDate" :key="s.id">
                  <td class="col-time">{{ s.showTime }}</td>
                  <td>{{ s.hallName }}</td>
                  <td class="col-cinema">{{ s.cinemaName }}</td>
                  <td>{{ s.availableSeats }}/{{ s.totalSeats }}</td>
                  <td class="col-price"><span class="price">&yen;{{ s.price }}</span></td>
                  <td>
                    <button class="btn-buy-miku" @click="goSeatSelect(s)">选座购票</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="bt-empty" v-else>
            <p>{{ showtimeStore.loading ? '加载中...' : '暂无排片' }}</p>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.buyticket-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.6);
  z-index: 1100;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30px;
}

.buyticket-modal {
  background: #fff;
  border-radius: 14px;
  max-width: 860px;
  width: 100%;
  max-height: 85vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.bt-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid #eee;
}

.bt-movie-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.bt-poster {
  width: 52px;
  height: 70px;
  object-fit: cover;
  border-radius: 4px;
}

.bt-no-poster {
  width: 52px;
  height: 70px;
  background: #eee;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.bt-movie-info h3 { font-size: 17px; margin-bottom: 2px; }
.bt-meta { font-size: 12px; color: var(--text-light); }

.bt-close {
  font-size: 28px;
  background: none;
  color: #999;
}
.bt-close:hover { color: #333; }

.bt-body {
  padding: 20px 24px;
  overflow-y: auto;
  flex: 1;
}

.bt-dates {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.bt-dates button {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 8px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 10px;
  background: #fff;
  min-width: 56px;
  transition: all 0.2s;
}

.bt-dates button:hover { border-color: var(--accent); }

.bt-dates button.active {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
}

.date-day { font-size: 15px; font-weight: 600; }
.date-week { font-size: 11px; opacity: 0.7; margin-top: 2px; }

.bt-table-wrap {
  border: 1px solid #eee;
  border-radius: 8px;
  overflow: hidden;
}

.bt-table { width: 100%; border-collapse: collapse; }

.bt-table th {
  text-align: left;
  padding: 12px 14px;
  font-size: 12px;
  color: var(--text-light);
  background: #fafafa;
  border-bottom: 1px solid #eee;
}

.bt-table td {
  padding: 14px;
  font-size: 14px;
  border-bottom: 1px solid #f5f5f5;
}

.bt-table tr:last-child td { border-bottom: none; }

.col-time { font-weight: 600; font-size: 16px !important; color: #111; }
.col-price { white-space: nowrap; }
.col-cinema { font-size: 13px !important; color: var(--text-light); }

.price { color: var(--primary); font-weight: 700; font-size: 16px; }

.btn-buy-miku {
  padding: 8px 20px;
  background: var(--primary);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  border-radius: 20px;
  transition: all 0.2s;
  white-space: nowrap;
}

.btn-buy-miku:hover {
  background: var(--primary-hover);
  box-shadow: 0 2px 8px rgba(231, 76, 60, 0.4);
}

.bt-empty { text-align: center; padding: 48px 0; color: var(--text-light); }
</style>
