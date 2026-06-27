# 电影票预订系统 — 前后端 API 接口规范

> 前后端并行开发的契约文档。前端按此文档 Mock 数据开发，后端按此文档实现接口。

---

## 通用约定

### 基础信息

| 项目 | 值 |
|------|-----|
| Base URL | `http://localhost:8080/api` |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |
| 认证方式 | JWT Token（放在 Header `Authorization: Bearer {token}`） |

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

### 错误码约定

| code | 含义 | 说明 |
|------|------|------|
| 200 | 成功 | |
| 400 | 参数错误 | 前端传参有误 |
| 401 | 未登录 | Token 过期或无效 |
| 403 | 无权限 | 管理员接口被普通用户访问 |
| 404 | 资源不存在 | |
| 409 | 冲突 | 座位已被他人锁定 |
| 500 | 服务器错误 | 后端异常 |

### 分页参数约定

所有列表接口支持分页：

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | int | 1 | 页码，从 1 开始 |
| size | int | 10 | 每页条数 |

分页响应格式：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

---

## 一、用户模块 `/api/user`

### 1.1 用户注册

```
POST /api/user/register
```

**请求体：**
```json
{
  "username": "zhangsan",
  "password": "123456",
  "phone": "13800138000"
}
```

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "注册成功",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "phone": "138****8000",
    "walletBalance": 1000.00,
    "createdAt": "2026-05-28 10:00:00"
  }
}
```

**业务规则：**
- 注册成功自动创建虚拟钱包，初始余额 1000 元
- username 唯一，重复返回 `code: 400, message: "用户名已存在"`

---

### 1.2 用户登录

```
POST /api/user/login
```

**请求体：**
```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userInfo": {
      "id": 1,
      "username": "zhangsan",
      "phone": "138****8000",
      "walletBalance": 1000.00,
      "avatar": null
    }
  }
}
```

**业务规则：**
- 前端拿到 token 后存入 localStorage，后续请求放入 Header
- token 有效期 7 天

---

### 1.3 获取用户信息

```
GET /api/user/profile
```

**请求头：** `Authorization: Bearer {token}`

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 1,
    "username": "zhangsan",
    "phone": "138****8000",
    "walletBalance": 850.00,
    "avatar": null,
    "createdAt": "2026-05-28 10:00:00"
  }
}
```

---

### 1.4 修改用户信息

```
PUT /api/user/profile
```

**请求头：** `Authorization: Bearer {token}`

**请求体：**
```json
{
  "phone": "13900139000",
  "avatar": "https://xxx.com/avatar.jpg"
}
```

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "修改成功",
  "data": null
}
```

---

## 二、电影模块 `/api/movie`

### 2.1 获取电影列表

```
GET /api/movie/list?status=showing&page=1&size=10
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 否 | `showing`上映中 / `upcoming`即将上映 / `ended`已下映，不传返回全部 |
| keyword | string | 否 | 搜索关键词（模糊匹配标题） |

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
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
    ],
    "total": 25,
    "page": 1,
    "size": 10
  }
}
```

---

### 2.2 获取电影详情

```
GET /api/movie/{id}
```

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
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
}
```

---

## 三、影院与场次模块 `/api/cinema` & `/api/showtime`

### 3.1 获取影院列表

```
GET /api/cinema/list?page=1&size=10
```

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "name": "万达影城（国贸店）",
        "address": "北京市朝阳区建国路93号",
        "phone": "010-12345678"
      }
    ],
    "total": 10,
    "page": 1,
    "size": 10
  }
}
```

---

### 3.2 获取某电影在某影院的场次列表

```
GET /api/showtime/list?movieId=1&cinemaId=1&date=2026-05-28
```

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| movieId | long | 是 | 电影ID |
| cinemaId | long | 否 | 影院ID，不传返回所有影院的场次 |
| date | string | 否 | 日期 YYYY-MM-DD，不传默认今天 |

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "cinemaId": 1,
      "cinemaName": "万达影城（国贸店）",
      "showtimes": [
        {
          "id": 101,
          "hallId": 1,
          "hallName": "1号厅（IMAX）",
          "hallType": "imax",
          "showDate": "2026-05-28",
          "showTime": "14:30",
          "price": 68.00,
          "availableSeats": 120,
          "totalSeats": 200,
          "status": "available"
        },
        {
          "id": 102,
          "hallId": 2,
          "hallName": "2号厅",
          "hallType": "normal",
          "showDate": "2026-05-28",
          "showTime": "17:00",
          "price": 45.00,
          "availableSeats": 80,
          "totalSeats": 150,
          "status": "available"
        }
      ]
    }
  ]
}
```

---

### 3.3 获取场次座位图

```
GET /api/showtime/{showtimeId}/seats
```

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "showtimeId": 101,
    "hallName": "1号厅（IMAX）",
    "hallType": "imax",
    "rows": 10,
    "cols": 20,
    "seats": [
      { "id": 1,  "row": 1, "col": 1,  "type": "normal", "status": "available" },
      { "id": 2,  "row": 1, "col": 2,  "type": "normal", "status": "available" },
      { "id": 3,  "row": 1, "col": 3,  "type": "normal", "status": "locked" },
      { "id": 4,  "row": 1, "col": 4,  "type": "normal", "status": "sold" },
      { "id": 5,  "row": 1, "col": 5,  "type": "normal", "status": "available" },
      { "id": 10, "row": 5, "col": 9,  "type": "vip",    "status": "available" },
      { "id": 11, "row": 5, "col": 10, "type": "vip",    "status": "available" },
      { "id": 20, "row": 10,"col": 10, "type": "couple", "status": "available" },
      { "id": 21, "row": 10,"col": 11, "type": "couple", "status": "available" }
    ]
  }
}
```

**座位状态说明：**
| status | 说明 | 前端展示 |
|--------|------|----------|
| available | 可选 | 绿色/可点击 |
| locked | 已被锁定（别人在选） | 灰色/不可点击 |
| sold | 已售出 | 红色/不可点击 |

**座位类型说明：**
| type | 说明 |
|------|------|
| normal | 普通座 |
| vip | VIP座（价格上浮 50%） |
| couple | 情侣座（价格上浮 100%，必须成对选） |

---

## 四、订单模块 `/api/order`

### 4.1 锁定座位（核心接口 — Redis Lua 原子操作）

```
POST /api/order/lock
```

**请求头：** `Authorization: Bearer {token}`

**请求体：**
```json
{
  "showtimeId": 101,
  "seatIds": [1, 2]
}
```

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "锁定成功",
  "data": {
    "lockToken": "a1b2c3d4e5f6",
    "expireTime": "2026-05-28 14:45:00",
    "seats": [
      { "id": 1, "row": 1, "col": 1, "type": "normal", "price": 45.00 },
      { "id": 2, "row": 1, "col": 2, "type": "normal", "price": 45.00 }
    ],
    "totalAmount": 90.00
  }
}
```

**失败响应 (409)：**
```json
{
  "code": 409,
  "message": "座位已被其他用户锁定，请重新选择",
  "data": {
    "conflictSeatIds": [2]
  }
}
```

**业务规则：**
- 后端使用 Redis Lua 脚本原子执行：检查所有座位是否可用 → 全部锁定 → 返回 lockToken
- 锁定 TTL = 15 分钟，超时自动释放
- 一个用户同时只能有一组未完成的锁定
- lockToken 用于后续创建订单时校验，防止篡改

---

### 4.2 创建订单（提交到 Redis Stream 异步处理）

```
POST /api/order/create
```

**请求头：** `Authorization: Bearer {token}`

**请求体：**
```json
{
  "showtimeId": 101,
  "seatIds": [1, 2],
  "lockToken": "a1b2c3d4e5f6"
}
```

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "订单提交成功，正在排队处理",
  "data": {
    "orderNo": "ORD20260528143000001",
    "status": "processing"
  }
}
```

**业务规则：**
- 请求进入 Redis Stream 队列，同步返回"排队中"
- 后端消费者异步处理：校验 lockToken → 扣减余额 → 生成订单 → 更新座位状态
- 前端拿到 orderNo 后轮询订单状态

---

### 4.3 查询订单状态（轮询用）

```
GET /api/order/status/{orderNo}
```

**请求头：** `Authorization: Bearer {token}`

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderNo": "ORD20260528143000001",
    "status": "paid",
    "message": "支付成功"
  }
}
```

**订单状态：**
| status | 说明 | 前端处理 |
|--------|------|----------|
| processing | 排队中 | 继续轮询（每 2 秒一次） |
| paid | 支付成功 | 跳转订单详情 |
| failed | 支付失败（余额不足等） | 提示用户 |
| cancelled | 已取消（超时） | 提示用户重新选座 |

---

### 4.4 获取订单详情

```
GET /api/order/{orderNo}
```

**请求头：** `Authorization: Bearer {token}`

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "orderNo": "ORD20260528143000001",
    "status": "paid",
    "movieTitle": "流浪地球3",
    "moviePoster": "/uploads/poster1.jpg",
    "cinemaName": "万达影城（国贸店）",
    "hallName": "1号厅（IMAX）",
    "showDate": "2026-05-28",
    "showTime": "14:30",
    "seats": [
      { "row": 1, "col": 1, "type": "normal" },
      { "row": 1, "col": 2, "type": "normal" }
    ],
    "totalAmount": 90.00,
    "createdAt": "2026-05-28 14:30:05",
    "paidAt": "2026-05-28 14:30:08"
  }
}
```

---

### 4.5 获取订单列表

```
GET /api/order/list?status=paid&page=1&size=10
```

**请求头：** `Authorization: Bearer {token}`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| status | string | 否 | `paid`/`cancelled`/`refunded`，不传返回全部 |

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "orderNo": "ORD20260528143000001",
        "status": "paid",
        "movieTitle": "流浪地球3",
        "moviePoster": "/uploads/poster1.jpg",
        "cinemaName": "万达影城（国贸店）",
        "showDate": "2026-05-28",
        "showTime": "14:30",
        "seatCount": 2,
        "totalAmount": 90.00,
        "createdAt": "2026-05-28 14:30:05"
      }
    ],
    "total": 5,
    "page": 1,
    "size": 10
  }
}
```

---

### 4.6 取消订单 / 退票

```
POST /api/order/cancel/{orderNo}
```

**请求头：** `Authorization: Bearer {token}`

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "退票成功，退款已到账",
  "data": {
    "orderNo": "ORD20260528143000001",
    "refundAmount": 90.00,
    "walletBalance": 910.00
  }
}
```

**业务规则：**
- 只有 `paid` 状态的订单可以退票
- 退款金额返还虚拟钱包
- 座位状态恢复为 `available`

---

## 五、管理后台模块 `/api/admin`

> 管理员接口，需要 admin 角色的 Token

### 5.1 管理员登录

```
POST /api/admin/login
```

**请求体：**
```json
{
  "username": "admin",
  "password": "admin123"
}
```

**成功响应：** 同用户登录

---

### 5.2 电影管理

#### 新增电影

```
POST /api/admin/movie
```

**请求头：** `Authorization: Bearer {admin_token}`

**请求体：**
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

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "添加成功",
  "data": { "id": 1 }
}
```

#### 修改电影

```
PUT /api/admin/movie/{id}
```

请求体同新增，只传需要修改的字段。

#### 删除电影

```
DELETE /api/admin/movie/{id}
```

---

### 5.3 影院管理

#### 新增影院

```
POST /api/admin/cinema
```

**请求体：**
```json
{
  "name": "万达影城（国贸店）",
  "address": "北京市朝阳区建国路93号",
  "phone": "010-12345678"
}
```

#### 新增影厅

```
POST /api/admin/hall
```

**请求体：**
```json
{
  "cinemaId": 1,
  "name": "1号厅（IMAX）",
  "seatRows": 10,
  "seatCols": 20,
  "hallType": "imax"
}
```

**业务规则：**
- 新增影厅后自动生成座位数据（rows × cols）

---

### 5.4 排片管理

#### 新增场次

```
POST /api/admin/showtime
```

**请求体：**
```json
{
  "movieId": 1,
  "hallId": 1,
  "showDate": "2026-05-28",
  "showTime": "14:30",
  "price": 68.00
}
```

#### 批量排片

```
POST /api/admin/showtime/batch
```

**请求体：**
```json
{
  "movieId": 1,
  "hallId": 1,
  "showDate": "2026-05-28",
  "times": ["10:00", "14:30", "17:00", "19:30"],
  "price": 68.00
}
```

---

### 5.5 订单管理

#### 获取所有订单

```
GET /api/admin/order/list?status=paid&page=1&size=10
```

#### 查看订单详情

```
GET /api/admin/order/{orderNo}
```

---

### 5.6 数据统计

```
GET /api/admin/statistics
```

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "todayOrderCount": 156,
    "todayRevenue": 8920.00,
    "totalUsers": 523,
    "totalMovies": 12,
    "recent7Days": [
      { "date": "2026-05-22", "orderCount": 120, "revenue": 6800.00 },
      { "date": "2026-05-23", "orderCount": 145, "revenue": 7200.00 }
    ],
    "topMovies": [
      { "movieId": 1, "title": "流浪地球3", "orderCount": 89, "revenue": 5200.00 },
      { "movieId": 2, "title": "封神第二部", "orderCount": 67, "revenue": 3720.00 }
    ]
  }
}
```

---

## 六、文件上传 `/api/upload`

### 上传图片（海报、头像）

```
POST /api/upload/image
Content-Type: multipart/form-data
```

**参数：** `file` (文件)

**成功响应 (200)：**
```json
{
  "code": 200,
  "message": "上传成功",
  "data": {
    "url": "/uploads/poster1.jpg"
  }
}
```

---

*文档创建日期：2026-05-28*
*状态：前后端并行开发契约*
