# API 接口规范

## 通用约定

### 基础信息

| 项目 | 值 |
|------|-----|
| Base URL | `http://localhost:8080/api` |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |
| 认证方式 | `Authorization: Bearer {token}` |

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 错误码

| code | 含义 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 / 业务逻辑拒绝（余额不足、用户名重复等） |
| 401 | 未登录（Token 过期或无效） |
| 403 | 无权限（非 admin 访问管理接口） |
| 404 | 资源不存在 |
| 409 | 冲突（座位已被锁定） |
| 500 | 服务器内部错误 |

### 分页

列表接口统一使用 `page`（从 1 开始）和 `size`（默认 10）参数。

分页响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [...],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

---

## 一、用户模块 `/user`

### 1.1 注册

```
POST /api/user/register
```

请求体：
```json
{
  "username": "zhangsan",
  "password": "123456",
  "phone": "13800138000"
}
```

响应 `data`：
```json
{
  "id": 1,
  "username": "zhangsan",
  "phone": "138****8000",
  "walletBalance": 1000.00,
  "createdAt": "2026-06-27 10:00:00"
}
```

规则：username 唯一，重复返回 400。注册成功自动赠送 1000 元余额。

### 1.2 登录

```
POST /api/user/login
```

请求体：
```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

响应 `data`：
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "userInfo": {
    "id": 1,
    "username": "zhangsan",
    "role": "user",
    "phone": "138****8000",
    "walletBalance": 1000.00,
    "avatar": null
  }
}
```

规则：校验 role 必须为 `user`，admin 账号需用 `/admin/login`。Token 有效期 7 天。

### 1.3 获取个人信息

```
GET /api/user/profile
Authorization: Bearer {token}
```

响应 `data`：
```json
{
  "id": 1,
  "username": "zhangsan",
  "phone": "138****8000",
  "walletBalance": 850.00,
  "avatar": null,
  "createdAt": "2026-06-27 10:00:00"
}
```

### 1.4 修改个人信息

```
PUT /api/user/profile
Authorization: Bearer {token}
```

请求体（所有字段可选）：
```json
{
  "username": "newName",
  "phone": "13900139000",
  "avatar": "/uploads/avatar.jpg"
}
```

响应：`code: 200, message: "修改成功"`。用户名重复返回 400。

### 1.5 修改密码

```
PUT /api/user/password
Authorization: Bearer {token}
```

请求体：
```json
{
  "oldPassword": "123456",
  "newPassword": "654321"
}
```

规则：BCrypt 校验旧密码，不匹配返回 400。

### 1.6 钱包充值

```
POST /api/user/recharge
Authorization: Bearer {token}
```

请求体：
```json
{
  "amount": 100.00,
  "password": "123456"
}
```

响应 `data`：
```json
{
  "walletBalance": 950.00
}
```

规则：先校验密码，再乐观锁加款。

---

## 二、电影模块 `/movie`

### 2.1 电影列表

```
GET /api/movie/list?status=showing&keyword=流浪&page=1&size=10
```

| 参数 | 必填 | 说明 |
|------|------|------|
| status | 否 | `showing` / `upcoming` / `ended`，不传返回全部 |
| keyword | 否 | 模糊搜索标题、导演、演员、类型 |

响应 `data.list` 每项：
```json
{
  "id": 1,
  "title": "流浪地球3",
  "poster": "/uploads/poster1.jpg",
  "duration": 150,
  "releaseDate": "2026-06-01",
  "rating": 9.2,
  "status": "showing",
  "genre": "科幻"
}
```

### 2.2 电影详情

```
GET /api/movie/{id}
```

响应 `data`：
```json
{
  "id": 1,
  "title": "流浪地球3",
  "poster": "/uploads/poster1.jpg",
  "duration": 150,
  "releaseDate": "2026-06-01",
  "rating": 9.2,
  "status": "showing",
  "genre": "科幻",
  "director": "郭帆",
  "actors": "吴京 / 刘德华 / 李雪健",
  "description": "太阳即将毁灭，人类在地球表面建造出巨大的推进器..."
}
```

### 2.3 最受期待

```
GET /api/movie/most-expected?limit=5
```

| 参数 | 必填 | 说明 |
|------|------|------|
| limit | 否 | 返回条数，默认 5 |

响应 `data` 数组，每项含 `id`, `title`, `poster`, `wishCount` 等。

### 2.4 想看/取消想看

```
POST /api/movie/{id}/wish
Authorization: Bearer {token}
```

响应 `data`：
```json
{
  "isWished": true,
  "wishCount": 128
}
```

规则：已想看则取消，未想看则添加。Redis Set 存储。

### 2.5 想看状态

```
GET /api/movie/{id}/wish-status
Authorization: Bearer {token}  （可选）
```

响应 `data`：
```json
{
  "isWished": false,
  "wishCount": 128
}
```

规则：未登录也返回（`isWished` 为 false）。

---

## 三、影院模块 `/cinema`

### 3.1 影院列表

```
GET /api/cinema/list?page=1&size=10
```

响应 `data.list` 每项：
```json
{
  "id": 1,
  "name": "万达影城（国贸店）",
  "address": "北京市朝阳区建国路93号",
  "phone": "010-12345678"
}
```

---

## 四、场次模块 `/showtime`

### 4.1 场次列表（按影院分组）

```
GET /api/showtime/list?movieId=1&cinemaId=1&date=2026-06-27
```

| 参数 | 必填 | 说明 |
|------|------|------|
| movieId | 是 | 电影 ID |
| cinemaId | 否 | 影院 ID，不传返回所有影院 |
| date | 否 | 日期 YYYY-MM-DD，不传返回所有未过期场次 |

响应 `data` 数组（按影院分组）：
```json
[
  {
    "cinemaId": 1,
    "cinemaName": "万达影城（国贸店）",
    "showtimes": [
      {
        "id": 101,
        "hallId": 1,
        "hallName": "1号厅（IMAX）",
        "hallType": "imax",
        "showDate": "2026-06-27",
        "showTime": "14:30",
        "price": 68.00,
        "availableSeats": 120,
        "totalSeats": 200
      }
    ]
  }
]
```

规则：只返回未过期场次。

### 4.2 场次详情

```
GET /api/showtime/{id}
```

响应 `data`：
```json
{
  "id": 101,
  "movieId": 1,
  "movieTitle": "流浪地球3",
  "hallId": 1,
  "hallName": "1号厅（IMAX）",
  "cinemaId": 1,
  "cinemaName": "万达影城（国贸店）",
  "showDate": "2026-06-27",
  "showTime": "14:30",
  "price": 68.00,
  "status": "available"
}
```

状态值：`available`（可售）/ `expired`（已过期）。

### 4.3 座位图

```
GET /api/showtime/{id}/seats
```

响应 `data`：
```json
{
  "showtimeId": 101,
  "hallName": "1号厅（IMAX）",
  "hallType": "imax",
  "rows": 10,
  "cols": 20,
  "seats": [
    { "id": 1,  "row": 1, "col": 1,  "type": "normal", "status": "available" },
    { "id": 2,  "row": 1, "col": 2,  "type": "normal", "status": "available" },
    { "id": 3,  "row": 1, "col": 3,  "type": "normal", "status": "sold" },
    { "id": 10, "row": 5, "col": 9,  "type": "vip",    "status": "available" },
    { "id": 20, "row": 10,"col": 10, "type": "couple", "status": "available" },
    { "id": 21, "row": 10,"col": 11, "type": "couple", "status": "available" }
  ]
}
```

座位类型 `type`：`normal` / `vip` / `couple`
座位状态 `status`：`available`（可选）/ `sold`（已售）/ `locked`（被锁定）

---

## 五、订单模块 `/order`

所有接口需要 JWT。

### 5.1 锁定座位

```
POST /api/order/lock
Authorization: Bearer {token}
```

请求体：
```json
{
  "showtimeId": 101,
  "seatIds": [1, 2]
}
```

成功响应 `data`：
```json
{
  "lockToken": "a1b2c3d4e5f6",
  "expireTime": "2026-06-27 14:45:00",
  "seats": [
    { "id": 1, "row": 1, "col": 1, "type": "normal", "price": 45.00 },
    { "id": 2, "row": 1, "col": 2, "type": "normal", "price": 45.00 }
  ],
  "totalAmount": 90.00
}
```

失败响应 (409)：
```json
{
  "code": 409,
  "message": "座位已被锁定",
  "data": { "conflictSeatIds": [1] }
}
```

规则：
- 使用 Redis Lua 脚本原子执行"检查全部可用 → 全部锁定"
- 锁定 TTL 15 分钟，超时自动释放
- 校验场次未过期、情侣座成对选择
- 一个用户同时只能有一组活跃锁定

### 5.2 创建订单

```
POST /api/order/create
Authorization: Bearer {token}
```

请求体：
```json
{
  "showtimeId": 101,
  "seatIds": [1, 2],
  "lockToken": "a1b2c3d4e5f6"
}
```

成功响应 `data`：
```json
{
  "orderNo": "ORD20260627143000001",
  "status": "pending"
}
```

规则：校验 lockToken 有效 → 创建 pending 状态订单 → 推入 Redis List 队列。

### 5.3 查询订单状态

```
GET /api/order/status/{orderNo}
Authorization: Bearer {token}
```

响应 `data`：
```json
{
  "orderNo": "ORD20260627143000001",
  "status": "pending"
}
```

status 值：`pending` / `paid` / `refunded` / `cancelled`

### 5.4 订单详情

```
GET /api/order/{orderNo}
Authorization: Bearer {token}
```

响应 `data`：
```json
{
  "orderNo": "ORD20260627143000001",
  "status": "paid",
  "movieTitle": "流浪地球3",
  "moviePoster": "/uploads/poster1.jpg",
  "cinemaName": "万达影城（国贸店）",
  "hallName": "1号厅（IMAX）",
  "showDate": "2026-06-27",
  "showTime": "14:30",
  "seats": [
    { "row": 1, "col": 1, "type": "normal" },
    { "row": 1, "col": 2, "type": "normal" }
  ],
  "totalAmount": 90.00,
  "createdAt": "2026-06-27 14:30:05",
  "paidAt": "2026-06-27 14:30:30"
}
```

### 5.5 订单列表

```
GET /api/order/list?status=paid&page=1&size=10
Authorization: Bearer {token}
```

| 参数 | 必填 | 说明 |
|------|------|------|
| status | 否 | `pending` / `paid` / `refunded` / `cancelled` |

响应 `data.list` 每项：
```json
{
  "orderNo": "ORD20260627143000001",
  "status": "paid",
  "movieTitle": "流浪地球3",
  "moviePoster": "/uploads/poster1.jpg",
  "cinemaName": "万达影城（国贸店）",
  "showDate": "2026-06-27",
  "showTime": "14:30",
  "seatCount": 2,
  "totalAmount": 90.00,
  "createdAt": "2026-06-27 14:30:05"
}
```

### 5.6 支付订单

```
POST /api/order/pay/{orderNo}
Authorization: Bearer {token}
```

请求体：
```json
{
  "password": "123456"
}
```

响应 `data`：
```json
{
  "orderNo": "ORD20260627143000001",
  "status": "paid",
  "walletBalance": 910.00
}
```

规则：BCrypt 校验密码 → 乐观锁扣余额 → 标记 paid。余额不足返回 400。

### 5.7 退票

```
POST /api/order/cancel/{orderNo}
Authorization: Bearer {token}
```

响应 `data`：
```json
{
  "orderNo": "ORD20260627143000001",
  "refundAmount": 90.00,
  "walletBalance": 1000.00
}
```

规则：仅 `paid` 状态可退，全额退款到钱包，状态变更为 `refunded`。

---

## 六、评价模块 `/review`

### 6.1 创建/更新评价

```
POST /api/review
Authorization: Bearer {token}
```

请求体：
```json
{
  "movieId": 1,
  "orderId": null,
  "rating": 8,
  "content": "特效很棒，剧情紧凑"
}
```

| 字段 | 必填 | 说明 |
|------|------|------|
| movieId | 是 | 电影 ID |
| orderId | 否 | 关联订单 ID |
| rating | 是 | 评分，1-10 整数 |
| content | 否 | 评语文字 |

规则：同一用户对同一电影多次提交会更新评分和评语。提交后自动重算该电影均分。

### 6.2 评价列表

```
GET /api/review/list?movieId=1&page=1&size=10
```

| 参数 | 必填 | 说明 |
|------|------|------|
| movieId | 是 | 电影 ID |

响应 `data.list` 每项：
```json
{
  "id": 1,
  "userId": 3,
  "username": "zhangsan",
  "avatar": "/uploads/avatar.jpg",
  "rating": 8,
  "content": "特效很棒，剧情紧凑",
  "createdAt": "2026-06-27 15:00:00"
}
```

### 6.3 评价状态

```
GET /api/review/status/{movieId}
Authorization: Bearer {token}  （可选）
```

响应 `data`：
```json
{
  "canReview": true,
  "myRating": 8,
  "myReviewId": 1,
  "averageRating": 8.5,
  "ratingCount": 42
}
```

规则：未登录可调用，`canReview` 为 false，评分相关字段为 null。

---

## 七、票房模块 `/box-office`

### 7.1 票房排行

```
GET /api/box-office/today
```

响应 `data`：
```json
{
  "totalRevenue": 156800.00,
  "movies": [
    {
      "movieId": 1,
      "title": "流浪地球3",
      "revenue": 52800.00,
      "ticketCount": 780
    },
    {
      "movieId": 2,
      "title": "封神第二部",
      "revenue": 37200.00,
      "ticketCount": 620
    }
  ]
}
```

规则：统计所有 paid 订单的历史总票房。Redis 缓存 5 分钟，定时刷新。返回 Top 10。

---

## 八、上传模块 `/upload`

### 8.1 上传图片

```
POST /api/upload/image
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

参数：`file`（表单文件字段）

响应 `data`：
```json
{
  "url": "/uploads/a1b2c3d4e5f6.jpg"
}
```

规则：仅允许 jpg / jpeg / png / gif / webp 格式，最大 10MB。UUID 重命名存入 `./uploads/`。

---

## 九、通知模块 `/notification`

所有接口需要 JWT。

### 9.1 通知列表

```
GET /api/notification/list?page=1&size=10
Authorization: Bearer {token}
```

响应 `data.list` 每项：
```json
{
  "id": 1,
  "title": "购票成功",
  "content": "您已成功购买《流浪地球3》2张票",
  "type": "order",
  "status": "unread",
  "createdAt": "2026-06-27 14:30:30"
}
```

### 9.2 标记已读

```
PUT /api/notification/{id}/read
Authorization: Bearer {token}
```

响应：`code: 200, message: "success"`。校验通知属于当前用户。

---

## 十、管理后台 `/admin`

所有接口需要 JWT + admin 角色。

### 10.1 管理员登录

```
POST /api/admin/login
```

请求体和响应格式同 `/user/login`，但校验 role 必须为 `admin`。

### 10.2 电影管理

#### 新增电影

```
POST /api/admin/movie
Authorization: Bearer {admin_token}
```

请求体（rating 已废弃，可不传）：
```json
{
  "title": "流浪地球3",
  "duration": 150,
  "releaseDate": "2026-06-01",
  "genre": "科幻",
  "director": "郭帆",
  "actors": "吴京 / 刘德华 / 李雪健",
  "description": "太阳即将毁灭...",
  "poster": "/uploads/poster1.jpg",
  "status": "showing"
}
```

#### 修改电影

```
PUT /api/admin/movie/{id}
Authorization: Bearer {admin_token}
```

请求体同新增，只传需要修改的字段。

#### 删除电影

```
DELETE /api/admin/movie/{id}
Authorization: Bearer {admin_token}
```

### 10.3 影院管理

#### 影院列表（管理端）

```
GET /api/admin/cinema/list?page=1&size=10
Authorization: Bearer {admin_token}
```

#### 新增影院

```
POST /api/admin/cinema
Authorization: Bearer {admin_token}
```

请求体：
```json
{
  "name": "万达影城（国贸店）",
  "address": "北京市朝阳区建国路93号",
  "phone": "010-12345678",
  "businessHours": "09:00-23:00"
}
```

#### 查看影院影厅

```
GET /api/admin/cinema/{id}/halls
Authorization: Bearer {admin_token}
```

响应 `data` 数组，每项含 `id`, `name`, `seatRows`, `seatCols`, `hallType`, `seatCount`。

#### 新增影厅

```
POST /api/admin/hall
Authorization: Bearer {admin_token}
```

请求体：
```json
{
  "cinemaId": 1,
  "name": "1号厅（IMAX）",
  "seatRows": 10,
  "seatCols": 20,
  "hallType": "imax"
}
```

规则：自动生成 `rows × cols` 个座位。

### 10.4 排片管理

#### 场次列表（管理端）

```
GET /api/admin/showtime/list?movieId=1&date=2026-06-27&page=1&size=10
Authorization: Bearer {admin_token}
```

| 参数 | 必填 | 说明 |
|------|------|------|
| movieId | 否 | 电影 ID |
| date | 否 | 日期 YYYY-MM-DD |

#### 新增场次

```
POST /api/admin/showtime
Authorization: Bearer {admin_token}
```

请求体：
```json
{
  "movieId": 1,
  "hallId": 1,
  "showDate": "2026-06-27",
  "showTime": "14:30",
  "price": 68.00
}
```

#### 批量排片

```
POST /api/admin/showtime/batch
Authorization: Bearer {admin_token}
```

请求体：
```json
{
  "movieId": 1,
  "hallId": 1,
  "showDate": "2026-06-27",
  "times": ["10:00", "14:30", "17:00", "19:30"],
  "price": 68.00
}
```

#### 删除场次

```
DELETE /api/admin/showtime/{id}
Authorization: Bearer {admin_token}
```

### 10.5 订单管理

#### 所有订单

```
GET /api/admin/order/list?status=paid&page=1&size=10
Authorization: Bearer {admin_token}
```

| 参数 | 必填 | 说明 |
|------|------|------|
| status | 否 | 过滤状态 |

#### 订单详情

```
GET /api/admin/order/{orderNo}
Authorization: Bearer {admin_token}
```

### 10.6 数据统计

```
GET /api/admin/statistics
Authorization: Bearer {admin_token}
```

响应 `data`：
```json
{
  "todayOrderCount": 156,
  "todayRevenue": 8920.00,
  "totalUsers": 523,
  "totalMovies": 12,
  "totalBoxOffice": 156800.00,
  "recent7Days": [
    { "date": "2026-06-21", "orderCount": 120, "revenue": 6800.00 },
    { "date": "2026-06-22", "orderCount": 145, "revenue": 7200.00 }
  ],
  "topMovies": [
    { "movieId": 1, "title": "流浪地球3", "orderCount": 89, "revenue": 5200.00 }
  ],
  "topWishMovies": [
    { "movieId": 3, "title": "封神第二部", "wishCount": 256 }
  ]
}
```

---

## 接口汇总

| 模块 | 路径 | 数量 | 认证 |
|------|------|------|------|
| 用户 | `/user` | 6 | 注册/登录无，其余 JWT |
| 电影 | `/movie` | 5 | 想看需 JWT，其余无 |
| 影院 | `/cinema` | 1 | 无 |
| 场次 | `/showtime` | 3 | 无 |
| 订单 | `/order` | 7 | JWT |
| 评价 | `/review` | 3 | 创建需 JWT，其余可选 |
| 票房 | `/box-office` | 1 | 无 |
| 上传 | `/upload` | 1 | JWT |
| 通知 | `/notification` | 2 | JWT |
| 管理 | `/admin` | 14 | JWT + admin |
| **合计** | | **43** | |

---

*文档更新日期：2026-06-27*
