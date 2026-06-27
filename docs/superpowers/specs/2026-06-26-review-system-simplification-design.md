# 评论系统简化设计

## 目标

将电影评论系统从"只有购票用户可评论"改为"任何登录用户可评论"，同时保留每人每部电影只能评论一次（可修改）的限制。删除多余的代码。

## 当前状态

### 后端
- `ReviewService.createReview()`: 检查 `hasWatched`（用户是否有已支付订单）
- `ReviewService.getReviewStatus()`: 返回 `canReview`（基于购票状态）
- `Review` 实体有 `orderId` 字段
- `ReviewRequest` DTO 有 `orderId` 字段

### 前端
- `RatingComponent.vue`: 根据 `canReview` 控制评分按钮显示
- 显示"购票观影后即可评分和评论"提示

## 设计方案

### 方案：最小修改

**原则**：只移除购票检查逻辑，保留 `orderId` 字段以备后用。

### 后端改动

#### 1. `ReviewService.java`

**`createReview()` 方法**：
- 删除 `hasWatched` 检查（第 43-47 行）
- 保留创建/更新逻辑不变

**`getReviewStatus()` 方法**：
- 移除 `canReview` 逻辑
- 简化为：登录用户 `canReview = true`

#### 2. 其他后端文件
- `Review.java`: 保留 `orderId` 字段
- `ReviewRequest.java`: 保留 `orderId` 字段
- `ReviewController.java`: 无需改动
- `ReviewRepository.java`: 无需改动

### 前端改动

#### `RatingComponent.vue`

**状态变量**：
- 移除 `canReview` ref
- 保留 `hasReviewed`、`userRating` 等

**`loadReviewStatus()` 函数**：
- 移除 `canReview` 相关逻辑

**`openRatingDialog()` 函数**：
- 移除 `canReview` 检查
- 只检查登录状态（未登录则触发 `need-login`）

**模板改动**：
- 移除"购票观影后即可评分和评论"提示
- 登录用户始终显示评分按钮
- 更新"暂无评论"提示文案（移除购票相关）

## 影响范围

- 后端：1 个文件 (`ReviewService.java`)
- 前端：1 个文件 (`RatingComponent.vue`)

## 风险评估

- **低风险**：改动范围小，逻辑清晰
- **保留 `orderId`**：不影响现有功能，未来可扩展
