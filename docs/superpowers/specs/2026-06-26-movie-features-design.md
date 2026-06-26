# 电影功能增强设计文档

## 概述

本次功能增强包括：
1. 首页侧边栏显示今日票房和最受期待电影榜单
2. 电影详情页添加"想看"和"评分"功能
3. 评分机制改为用户评分，只有看过电影的用户才能评分

## 需求详情

### 1. 首页侧边栏

**位置**：首页电影列表右侧

**内容**：
- **今日票房**：显示今日已支付订单的总金额
  - 数据来源：本地订单统计
  - 更新频率：每5分钟通过Redis缓存更新
  - 显示格式：¥xxx,xxx.xx

- **最受期待电影**：显示即将上映电影中"想看"数量最多的前5名
  - 数据来源：Movie表的wishCount字段
  - 排序方式：按想看数量降序
  - 显示内容：电影名称、想看数量

### 2. 想看功能

**触发位置**：电影详情页

**交互逻辑**：
- 用户点击"想看"按钮，类似点赞功能
- 每个账号每部电影只能点击一次（已想看状态显示为已点赞）
- 显示总想看数量
- 只对即将上映的电影显示"想看"按钮

**数据存储**：
- Redis Set：`movie:wish:{movieId}` 存储用户ID（用于去重和快速查询）
- Movie表：`wish_count` 字段持久化存储想看数量
- 同步机制：每小时从Redis同步到Movie表

### 3. 评分功能

**触发位置**：电影详情页

**交互逻辑**：
- 用户可以对正在热映的电影进行评分
- 评分范围：1-10分
- 每个用户每部电影只能评分一次，但可以修改评分
- 只有看过电影的用户才能评分（通过查询已支付订单验证）

**数据存储**：
- Review表：存储每次评分记录（已有）
- Movie表：`rating` 字段存储平均分，`rating_count` 字段存储评分人数
- 更新机制：用户评分后实时更新Movie表的平均分

## 数据库设计

### Movie表新增字段

```sql
ALTER TABLE movies ADD COLUMN wish_count INT DEFAULT 0 COMMENT '想看数量';
ALTER TABLE movies ADD COLUMN rating_count INT DEFAULT 0 COMMENT '评分人数';
```

### Redis键设计

| 键名 | 类型 | 说明 |
|------|------|------|
| `movie:wish:{movieId}` | Set | 存储想看该电影的用户ID |
| `movie:wish:count:{movieId}` | String | 想看数量缓存 |
| `box:office:today` | String | 今日票房缓存 |
| `box:office:today:movies` | List | 今日票房电影列表缓存 |

## API设计

### 1. 今日票房API

```
GET /api/box-office/today
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "totalRevenue": 123456.78,
    "movies": [
      {
        "movieId": 1,
        "title": "电影名称",
        "revenue": 12345.67,
        "ticketCount": 123
      }
    ]
  }
}
```

### 2. 最受期待电影API

```
GET /api/movie/most-expected
```

**响应**：
```json
{
  "code": 200,
  "data": [
    {
      "movieId": 1,
      "title": "电影名称",
      "wishCount": 1234,
      "poster": "http://..."
    }
  ]
}
```

### 3. 想看操作API

```
POST /api/movie/{movieId}/wish
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "wishCount": 1235,
    "isWished": true
  }
}
```

### 4. 查询想看状态API

```
GET /api/movie/{movieId}/wish-status
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "wishCount": 1234,
    "isWished": false
  }
}
```

### 5. 评分API

```
POST /api/review
```

**请求体**：
```json
{
  "movieId": 1,
  "orderId": 123,
  "rating": 8
}
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "id": 1,
    "rating": 8
  }
}
```

### 6. 查询用户评分状态API

```
GET /api/review/status/{movieId}
```

**响应**：
```json
{
  "code": 200,
  "data": {
    "canReview": true,
    "hasReviewed": false,
    "userRating": null,
    "averageRating": 8.5,
    "ratingCount": 123
  }
}
```

## 前端设计

### 1. 首页侧边栏

**布局调整**：
- 将电影列表从5列改为4列
- 右侧添加侧边栏（宽度300px）

**侧边栏内容**：
- 今日票房卡片（显示总票房和前10名电影）
- 最受期待榜单（显示前5名即将上映电影）

### 2. 电影详情页增强

**新增元素**：
- 想看按钮（仅即将上映电影显示）
- 评分组件（仅正在热映电影显示，且用户已购票）
- 评分显示（平均分 + 评分人数）

**交互逻辑**：
- 想看按钮：点击后切换状态，更新数量
- 评分组件：滑动选择1-10分，提交后显示用户评分

## 定时任务

### 1. 今日票房同步

```java
@Scheduled(fixedRate = 300000) // 每5分钟
public void syncTodayBoxOffice() {
    // 查询今日已支付订单
    // 计算总票房和各电影票房
    // 更新Redis缓存
}
```

### 2. 想看数量同步

```java
@Scheduled(fixedRate = 3600000) // 每小时
public void syncWishCount() {
    // 遍历所有电影
    // 从Redis Set获取想看数量
    // 更新Movie表的wishCount字段
}
```

## 实现步骤

### 后端实现

1. **数据库变更**
   - Movie表添加wishCount和ratingCount字段
   - 创建数据库迁移脚本

2. **Repository层**
   - MovieRepository：添加按wishCount排序查询
   - OrderRepository：添加查询用户是否看过某电影的方法
   - ReviewRepository：添加查询用户评分状态的方法

3. **Service层**
   - BoxOfficeService：今日票房统计和缓存
   - MovieService：增强想看和评分功能
   - ReviewService：增强评分验证逻辑

4. **Controller层**
   - BoxOfficeController：今日票房API
   - MovieController：增强想看相关API
   - ReviewController：增强评分验证API

5. **定时任务**
   - BoxOfficeSyncTask：今日票房同步
   - WishCountSyncTask：想看数量同步

### 前端实现

1. **首页改造**
   - 修改布局为侧边栏结构
   - 创建SideBar组件
   - 创建BoxOffice组件
   - 创建MostExpected组件

2. **电影详情页增强**
   - 添加WishButton组件
   - 添加RatingComponent组件
   - 修改页面布局和样式

3. **API集成**
   - 添加想看相关API函数
   - 添加评分相关API函数
   - 添加今日票房API函数

## 数据存储位置

| 数据类型 | 存储位置 | 说明 |
|---------|---------|------|
| 想看用户ID | Redis Set `movie:wish:{movieId}` | 用于去重和快速查询 |
| 想看数量 | Movie表 `wish_count` 字段 | 持久化存储 |
| 用户评分 | Review表 | 存储每次评分记录 |
| 平均评分 | Movie表 `rating` 字段 | 实时更新 |
| 评分人数 | Movie表 `rating_count` 字段 | 实时更新 |
| 今日票房 | Redis `box:office:today` | 每5分钟更新 |
| 今日票房电影列表 | Redis `box:office:today:movies` | 每5分钟更新 |

## 注意事项

1. **并发控制**：想看操作使用Redis Set的原子性保证去重
2. **数据一致性**：想看数量每小时同步到Movie表，评分实时更新
3. **性能优化**：今日票房使用Redis缓存，减少数据库查询
4. **用户体验**：想看和评分操作提供即时反馈
