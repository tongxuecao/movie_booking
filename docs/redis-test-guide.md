# Redis 测试指南

## 目录

1. [前置准备](#前置准备)
2. [测试步骤](#测试步骤)
   - [步骤 1：用户登录获取 Token](#步骤-1用户登录获取-token)
   - [步骤 2：获取场次座位信息](#步骤-2获取场次座位信息)
   - [步骤 3：锁定座位](#步骤-3锁定座位)
   - [步骤 4：重复锁定测试](#步骤-4重复锁定测试)
   - [步骤 5：创建订单](#步骤-5创建订单)
   - [步骤 6：轮询订单状态](#步骤-6轮询订单状态)
   - [步骤 7：查看订单详情](#步骤-7查看订单详情)
3. [Redis 命令速查](#redis-命令速查)
4. [验证清单](#验证清单)
5. [常见问题](#常见问题)

---

## 前置准备

### 启动服务

```bash
# 1. 启动 MySQL（确保 movie_ticket 数据库已初始化）
mysql -u root -p < backend/src/main/resources/db/init.sql

# 2. 启动 Redis（密码: clh123456）
redis-server

# 3. 启动后端
cd backend
mvn spring-boot:run
```

### 开启 Redis 监控

```bash
# 另开终端，实时监控 Redis 命令
redis-cli -a clh123456 monitor
```

---

## 测试步骤

### 步骤 1：用户登录获取 Token

**请求：**
```
POST http://localhost:8080/api/user/login
Content-Type: application/json
```

**Body：**
```json
{
    "username": "testuser",
    "password": "user123"
}
```

**预期响应：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "token": "eyJhbGciOiJIUzI1NiJ9...",
        "userId": 2,
        "username": "testuser",
        "role": "user"
    }
}
```

**操作：** 保存返回的 `token`，后续请求在 Header 中设置：
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

---

### 步骤 2：获取场次座位信息

**请求：**
```
GET http://localhost:8080/api/showtime/1/seats
Authorization: Bearer {your_token}
```

**预期响应：**
```json
{
    "code": 200,
    "message": "success",
    "data": [
        {
            "id": 1,
            "hallId": 1,
            "rowNum": 1,
            "colNum": 1,
            "seatType": "couple",
            "status": "active"
        }
    ]
}
```

**操作：** 记录几个可用的 `seatId`（如 1, 2, 3）

---

### 步骤 3：锁定座位

> 核心测试：验证 Redis Lua 脚本原子锁座

**请求：**
```
POST http://localhost:8080/api/order/lock
Authorization: Bearer {your_token}
Content-Type: application/json
```

**Body：**
```json
{
    "showtimeId": 1,
    "seatIds": [1, 2]
}
```

**预期响应：**
```json
{
    "code": 200,
    "message": "锁定成功",
    "data": null
}
```

**验证 Redis：**
```bash
redis-cli -a clh123456

# 查看座位锁
> keys seat:lock:*
# 输出：1) "seat:lock:1:1"  2) "seat:lock:1:2"

# 查看锁详情
> GET seat:lock:1:1
# 输出：用户ID（如 "2"）

# 查看过期时间
> TTL seat:lock:1:1
# 输出：约 900（15分钟）

# 查看用户锁
> GET user:lock:2
# 输出："1:1,2"
```

---

### 步骤 4：重复锁定测试

> 验证冲突检测机制

**请求：**
```
POST http://localhost:8080/api/order/lock
Authorization: Bearer {another_user_token}
Content-Type: application/json
```

**Body：**
```json
{
    "showtimeId": 1,
    "seatIds": [1, 3]
}
```

**预期响应（409 冲突）：**
```json
{
    "code": 409,
    "message": "座位已被锁定",
    "data": "seat:lock:1:1"
}
```

---

### 步骤 5：创建订单

> 测试 Redis Stream 异步处理

**请求：**
```
POST http://localhost:8080/api/order/create
Authorization: Bearer {your_token}
Content-Type: application/json
```

**Body：**
```json
{
    "showtimeId": 1,
    "seatIds": [1, 2],
    "payMethod": "wallet"
}
```

**预期响应：**
```json
{
    "code": 200,
    "message": "订单提交成功，正在排队处理",
    "data": {
        "orderNo": "ORD20260622001"
    }
}
```

**验证 Redis Stream：**
```bash
# 查看订单流
> keys order:stream:*

# 读取 Stream 消息
> XRANGE order:stream - +

# 查看 Stream 长度
> XLEN order:stream
```

---

### 步骤 6：轮询订单状态

**请求：**
```
GET http://localhost:8080/api/order/status/ORD20260622001
Authorization: Bearer {your_token}
```

**预期响应（处理完成后）：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "orderNo": "ORD20260622001",
        "status": "paid",
        "totalAmount": 160.00
    }
}
```

**验证锁释放：**
```bash
# 座位锁应已释放
> keys seat:lock:*
# 输出：(empty array)

# 用户锁应已释放
> GET user:lock:2
# 输出：(nil)
```

---

### 步骤 7：查看订单详情

**请求：**
```
GET http://localhost:8080/api/order/ORD20260622001
Authorization: Bearer {your_token}
```

**预期响应：**
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "orderNo": "ORD20260622001",
        "movieTitle": "流浪地球3",
        "cinemaName": "万达影城(国贸店)",
        "hallName": "1号厅(IMAX)",
        "showDate": "2026-06-20",
        "showTime": "10:00:00",
        "seats": [
            {"rowNum": 1, "colNum": 1, "seatType": "couple", "price": 160.00},
            {"rowNum": 1, "colNum": 2, "seatType": "couple", "price": 160.00}
        ],
        "totalAmount": 320.00,
        "status": "paid"
    }
}
```

---

## Redis 命令速查

```bash
# 连接 Redis
redis-cli -a clh123456

# 监控实时命令
redis-cli -a clh123456 monitor

# 查看所有 key
> keys *

# 座位锁
> keys seat:lock:*
> GET seat:lock:{showtimeId}:{seatId}
> TTL seat:lock:{showtimeId}:{seatId}

# 用户锁
> keys user:lock:*
> GET user:lock:{userId}

# 订单流
> keys *stream*
> XRANGE {stream_key} - +
> XLEN {stream_key}

# 清空数据库（测试用）
> FLUSHDB

# 查看 Redis 信息
> INFO server
> INFO memory
> INFO clients
```

---

## 验证清单

| 测试项 | 操作 | 预期结果 | 验证命令 |
|--------|------|----------|----------|
| Redis 连接 | `redis-cli ping` | 返回 PONG | `redis-cli -a clh123456 ping` |
| 用户登录 | POST /user/login | 返回 token | - |
| 锁定座位 | POST /order/lock | 返回成功 | `keys seat:lock:*` |
| 查看锁详情 | - | Redis 有锁数据 | `GET seat:lock:1:1` |
| 查看锁TTL | - | 约 900 秒 | `TTL seat:lock:1:1` |
| 重复锁定 | POST /order/lock (另一用户) | 返回 409 | - |
| 创建订单 | POST /order/create | 返回 orderNo | `keys *stream*` |
| 订单入队 | - | Stream 有消息 | `XRANGE order:stream - +` |
| 轮询状态 | GET /order/status/{orderNo} | 状态变为 paid | - |
| 锁释放 | - | 座位锁消失 | `keys seat:lock:*` |
| 用户锁释放 | - | 用户锁消失 | `GET user:lock:2` |

---

## 常见问题

### 1. 连接失败

```
错误：Could not connect to Redis
解决：检查 redis-server 是否启动，密码是否为 clh123456
```

### 2. 401 未授权

```
错误：{"code":401,"message":"未登录"}
解决：检查 Authorization header 格式是否为 "Bearer {token}"
```

### 3. 409 冲突

```
错误：{"code":409,"message":"座位已被锁定"}
解决：这是正常行为，说明锁机制工作正常
```

### 4. 订单创建后状态不变

```
原因：Redis Stream Consumer 可能未启动
检查：查看后端日志是否有消费日志
```

---

## Postman Collection 结构

```
Redis 测试
├── 01-用户登录
├── 02-获取座位
├── 03-锁定座位
├── 04-重复锁定(冲突测试)
├── 05-创建订单
├── 06-轮询订单状态
└── 07-查看订单详情
```

**环境变量：**
- `{{baseUrl}}` = `http://localhost:8080/api`
- `{{token}}` = 登录后自动保存
