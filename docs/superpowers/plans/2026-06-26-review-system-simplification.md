# 评论系统简化实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将电影评论系统从"只有购票用户可评论"改为"任何登录用户可评论"，同时保留每人每部电影只能评论一次（可修改）的限制。

**Architecture:** 最小修改方案 - 只移除购票检查逻辑，保留 `orderId` 字段以备后用。后端修改 `ReviewService.java`，前端修改 `RatingComponent.vue`。

**Tech Stack:** Spring Boot 3.2.5, Vue 3, Element Plus

## Global Constraints

- 后端 API 基础路径: `http://localhost:8080/api`
- 认证: `Authorization: Bearer {token}`
- 保留 `orderId` 字段，不做删除

---

### Task 1: 后端 - 简化 ReviewService.java

**Files:**
- Modify: `backend/src/main/java/com/moviebooking/service/ReviewService.java`

**Interfaces:**
- Consumes: `ReviewRepository.findByUserIdAndMovieId()`, `ReviewRepository.save()`
- Produces: 简化后的 `createReview()` 和 `getReviewStatus()` 方法

- [ ] **Step 1: 移除 createReview() 中的 hasWatched 检查**

打开 `backend/src/main/java/com/moviebooking/service/ReviewService.java`，找到 `createReview()` 方法（第 42-77 行）。

删除以下代码块（第 43-47 行）：
```java
// 验证用户是否看过这部电影（使用原生SQL，避免JPQL枚举问题）
boolean hasWatched = orderRepository.existsByUserIdAndMovieIdAndPaidNative(userId, request.getMovieId()) > 0;
if (!hasWatched) {
    throw new BusinessException("只有看过这部电影的用户才能评分");
}
```

保留其余代码不变。

- [ ] **Step 2: 简化 getReviewStatus() 方法**

找到 `getReviewStatus()` 方法（第 82-110 行）。

将方法体替换为：
```java
public Map<String, Object> getReviewStatus(Long userId, Long movieId) {
    Movie movie = movieRepository.findById(movieId)
            .orElseThrow(() -> new BusinessException("电影不存在"));

    boolean hasReviewed = false;
    Integer userRating = null;

    if (userId != null) {
        // 检查用户是否已评分
        Optional<Review> existingReview = reviewRepository.findByUserIdAndMovieId(userId, movieId);
        if (existingReview.isPresent()) {
            hasReviewed = true;
            userRating = existingReview.get().getRating();
        }
    }

    return Map.of(
            "canReview", userId != null,
            "hasReviewed", hasReviewed,
            "userRating", userRating,
            "averageRating", movie.getRating() != null ? movie.getRating() : null,
            "ratingCount", movie.getRatingCount() != null ? movie.getRatingCount() : 0
    );
}
```

- [ ] **Step 3: 验证编译**

运行后端编译命令验证代码无语法错误：
```bash
cd backend && mvn compile -q
```

Expected: 编译成功，无错误输出

- [ ] **Step 4: 提交后端改动**

```bash
git add backend/src/main/java/com/moviebooking/service/ReviewService.java
git commit -m "refactor: 移除评论购票检查，任何登录用户可评论"
```

---

### Task 2: 前端 - 简化 RatingComponent.vue

**Files:**
- Modify: `frontend/src/components/RatingComponent.vue`

**Interfaces:**
- Consumes: `apiGetReviewStatus()`, `apiCreateReview()`, `apiGetReviewList()`
- Produces: 简化后的评分组件，登录用户始终可评论

- [ ] **Step 1: 移除 canReview 响应式变量**

打开 `frontend/src/components/RatingComponent.vue`，找到第 15 行：
```javascript
const canReview = ref(false)
```

删除这一行。

- [ ] **Step 2: 简化 loadReviewStatus() 函数**

找到 `loadReviewStatus()` 函数（第 36-48 行），替换为：
```javascript
async function loadReviewStatus() {
  try {
    const status = await apiGetReviewStatus(props.movieId)
    hasReviewed.value = status.hasReviewed
    userRating.value = status.userRating || 0
    averageRating.value = status.averageRating || 0
    ratingCount.value = status.ratingCount || 0
    selectedRating.value = userRating.value
  } catch (error) {
    console.error('获取评分状态失败:', error)
  }
}
```

- [ ] **Step 3: 简化 openRatingDialog() 函数**

找到 `openRatingDialog()` 函数（第 63-80 行），替换为：
```javascript
function openRatingDialog() {
  if (!auth.isLoggedIn) {
    emit('need-login')
    return
  }
  selectedRating.value = userRating.value || 0
  commentText.value = ''
  // 如果已评分，加载已有评论内容
  const existing = reviews.value.find(r => r.userId === auth.currentUser?.id)
  if (existing) {
    commentText.value = existing.content || ''
  }
  showRatingDialog.value = true
}
```

- [ ] **Step 4: 更新模板 - 评分概览区域**

找到模板中的评分概览区域（第 142-166 行），替换为：
```html
<!-- 评分概览 -->
<div class="rating-overview">
  <div class="rating-display">
    <div class="average-rating">
      <span class="score">{{ averageRating != null ? Number(averageRating).toFixed(1) : '暂无' }}</span>
      <span class="label">平均评分</span>
    </div>
    <div class="rating-count">
      <span class="count">{{ ratingCount }}</span>
      <span class="label">人评分</span>
    </div>
  </div>

  <div v-if="auth.isLoggedIn" class="user-rating">
    <button class="rating-btn" @click="openRatingDialog">
      {{ hasReviewed ? '修改评分' : '立即评分' }}
    </button>
    <span v-if="hasReviewed" class="user-score">我的评分: {{ userRating }}分</span>
  </div>
  <div v-else class="cannot-review">
    <span>登录后即可评分和评论</span>
  </div>
</div>
```

- [ ] **Step 5: 更新模板 - 空评论提示**

找到空评论提示（第 206-210 行），替换为：
```html
<div v-else class="reviews-empty">
  <div class="empty-icon">&#128172;</div>
  <p>暂无评论</p>
  <p class="empty-hint">登录后即可评分和评论</p>
</div>
```

- [ ] **Step 6: 验证前端构建**

运行前端构建命令验证代码无语法错误：
```bash
cd frontend && npm run build
```

Expected: 构建成功，无错误输出

- [ ] **Step 7: 提交前端改动**

```bash
git add frontend/src/components/RatingComponent.vue
git commit -m "refactor: 简化评分组件，登录用户始终可评论"
```

---

## 验证清单

完成所有任务后，进行以下验证：

1. **后端编译**：`cd backend && mvn compile -q`
2. **前端构建**：`cd frontend && npm run build`
3. **功能测试**：
   - 启动后端和前端
   - 登录用户访问电影详情页
   - 验证"立即评分"按钮始终显示
   - 验证可以成功提交评论
   - 验证可以修改已提交的评论
   - 验证未登录用户显示"登录后即可评分和评论"
