# 后端技术文档

## 技术栈

| 技术 | 版本/说明 |
|------|-----------|
| Java | 17 |
| Spring Boot | 3.2.5 |
| 持久层 | JPA / Hibernate，`ddl-auto: update` 自动建表 |
| 数据库 | MySQL 8.0，库名 `movie_ticket` |
| 缓存 | Redis 6.0+，用于锁座（Lua 原子操作）和订单队列 |
| 认证 | JWT（jjwt 0.12.5，HMAC-SHA256），7 天过期 |
| 密码加密 | BCrypt |
| JSON | Jackson，日期格式 `yyyy-MM-dd HH:mm:ss`，时区 `Asia/Shanghai`，序列化排除 null |

## 配置一览

文件：`backend/src/main/resources/application.yml`

| 配置项 | 值 | 说明 |
|--------|-----|------|
| `server.port` | 8080 | |
| `server.servlet.context-path` | `/api` | 所有接口前缀 |
| `spring.jpa.hibernate.ddl-auto` | update | 开发阶段自动建表 |
| `spring.jpa.open-in-view` | false | 避免懒加载在视图层意外触发 |
| `spring.servlet.multipart.max-file-size` | 10MB | 上传文件大小限制 |
| `app.jwt.secret` | （64 字符密钥） | HS256 签名 |
| `app.jwt.expiration` | 604800000 | 7 天（毫秒） |
| `app.upload.path` | `./uploads/` | 上传文件存储目录（相对路径） |
| `app.upload.url-prefix` | `/uploads/` | 返回给前端的 URL 前缀 |
| `app.seat-lock.ttl-seconds` | 900 | 锁座 TTL 15 分钟 |

---

## 完整包结构

```
backend/src/main/java/com/moviebooking/
│
├── controller/                    # 控制器层（10 个）
│   ├── UserController.java        # 用户注册、登录、个人信息、密码、充值
│   ├── MovieController.java       # 电影列表、详情、最受期待、想看
│   ├── CinemaController.java      # 影院列表
│   ├── ShowtimeController.java    # 场次列表、详情、座位图
│   ├── OrderController.java       # 锁座、下单、支付、退票、订单查询
│   ├── ReviewController.java      # 创建评价、评价列表、评价状态
│   ├── BoxOfficeController.java   # 票房排行
│   ├── UploadController.java      # 图片上传
│   ├── NotificationController.java # 通知列表、标记已读
│   └── AdminController.java       # 管理后台（电影/影院/排片/订单/统计）
│
├── service/                       # 业务服务层（11 个）
│   ├── UserService.java           # 注册、登录(JWT签发)、个人资料、密码修改、钱包充值
│   ├── MovieService.java          # 电影查询、电影 CRUD
│   ├── CinemaService.java         # 影院查询、影院 CRUD、影厅创建
│   ├── ShowtimeService.java       # 场次查询、座位图、场次 CRUD、批量排片
│   ├── OrderService.java          # 锁座、下单、支付、退票、订单状态、订单查询
│   ├── ReviewService.java         # 评价创建/更新、电影均分重算、评价查询
│   ├── BoxOfficeService.java      # 全量票房统计 + Redis 缓存 + 定时刷新
│   ├── WishService.java           # 想看（Redis Set）、心愿排行、定时同步
│   ├── AdminService.java          # 仪表盘统计（今日数据、7天趋势、排行榜）
│   ├── UploadService.java         # 文件校验与存储
│   └── NotificationService.java   # 通知列表、已读标记
│
├── repository/                    # 数据访问层（12 个 JPA Repository）
│   ├── UserRepository.java        # 用户名查重、原子扣款、原子加款
│   ├── MovieRepository.java       # 状态筛选、关键词搜索、心愿排行
│   ├── CinemaRepository.java      # 标准 JPA
│   ├── HallRepository.java        # 按影院查影厅
│   ├── ShowtimeRepository.java    # 按电影+影院+日期查场次（只查未过期）
│   ├── SeatRepository.java        # 按影厅查座位、查已售座位
│   ├── OrderRepository.java       # 按用户/状态查询、统计收入、电影票房排行
│   ├── OrderSeatRepository.java   # 按订单查座位关联
│   ├── PaymentRepository.java     # 按订单查支付记录
│   ├── ReviewRepository.java      # 按电影/用户查询、均分统计
│   ├── MovieImageRepository.java  # 电影图片查询
│   └── NotificationRepository.java # 通知列表、未读计数
│
├── redis/                         # Redis 操作（3 个）
│   ├── SeatLockService.java       # Lua 脚本原子锁座、锁验证、锁释放
│   ├── OrderStreamProducer.java   # 订单消息入队（Redis List）
│   └── OrderStreamConsumer.java   # 队列消费（500ms 轮询）+ 过期订单清理
│
├── entity/                        # 实体类（12 个）
│   ├── User.java                  # 用户（钱包余额、乐观锁版本号）
│   ├── Movie.java                 # 电影（状态、海报、评分、想看数）
│   ├── Cinema.java                # 影院（名称、地址、电话、状态）
│   ├── Hall.java                  # 影厅（行列数、类型）
│   ├── Seat.java                  # 座位（行列号、座位类型、状态）
│   ├── Showtime.java              # 场次（日期、时间、票价、过期判断）
│   ├── Order.java                 # 订单（订单号、金额、状态）
│   ├── OrderSeat.java             # 订单-座位关联
│   ├── Payment.java               # 支付记录
│   ├── Review.java                # 评价（评分 1-10、内容）
│   ├── Notification.java          # 通知
│   └── MovieImage.java            # 电影图片（海报/剧照/横幅）
│
├── dto/                           # 请求 DTO（13 个）
│   ├── RegisterRequest.java       # 注册（@NotBlank 校验）
│   ├── LoginRequest.java          # 登录
│   ├── ChangePasswordRequest.java # 修改密码
│   ├── RechargeRequest.java       # 充值（金额+密码）
│   ├── UpdateProfileRequest.java  # 更新个人资料
│   ├── MovieRequest.java          # 电影创建/更新
│   ├── CinemaRequest.java         # 影院创建
│   ├── HallRequest.java           # 影厅创建
│   ├── ShowtimeRequest.java       # 场次创建
│   ├── BatchShowtimeRequest.java  # 批量排片
│   ├── LockSeatsRequest.java      # 锁座请求
│   ├── CreateOrderRequest.java    # 下单请求（含 lockToken）
│   ├── PayOrderRequest.java       # 支付请求（密码）
│   └── ReviewRequest.java        # 评价请求（评分+内容）
│
├── config/                        # 配置类（3 个）
│   ├── WebConfig.java             # CORS、JWT 拦截器、静态资源映射
│   ├── RedisConfig.java           # RedisTemplate 序列化配置
│   └── SchedulingConfig.java      # @EnableScheduling
│
└── common/                        # 公共组件（4 个）
    ├── ApiResult.java             # 统一响应格式 code/message/data
    ├── BusinessException.java     # 业务异常（返回 200 + 错误 code）
    ├── GlobalExceptionHandler.java # @RestControllerAdvice 统一异常处理
    └── JwtUtil.java              # JWT 生成/解析/校验工具
```

---

## 数据模型

### ER 关系

```
User (1) ──< (N) Order (1) ──< (N) OrderSeat >── (1) Seat (N) >── (1) Hall (N) >── (1) Cinema
                │
                └── (N) Payment    （一个订单可能有多条支付记录：支付+退款）

User (1) ──< (N) Review >── (1) Movie

Movie (1) ──< (N) Showtime >── (1) Hall

Movie (1) ──< (N) MovieImage

User (1) ──< (N) Notification
```

### 各实体关键字段

**User** — `users`
- `username` VARCHAR(50) 唯一，`password` BCrypt 加密
- `walletBalance` DECIMAL(10,2)，默认 1000.00
- `version` INT，@Version 乐观锁
- `role` 枚举：`user` / `admin`
- `avatar` VARCHAR(255)，头像 URL

**Movie** — `movies`
- `status` 枚举：`upcoming` / `showing` / `ended`
- `duration` INT，片长（分钟）
- `rating` DECIMAL(2,1)，平均评分
- `ratingCount` INT，评分人数
- `wishCount` INT，想看人数
- `poster` VARCHAR(255)，海报 URL
- `description` TEXT，剧情简介

**Cinema** — `cinemas`
- `status` 枚举：`open` / `suspended` / `preparing` / `closed`

**Hall** — `halls`
- `hallType` 枚举：`normal` / `imax` / `vip` / `threeD`
- `seatRows` / `seatCols` INT，行列数

**Seat** — `seats`
- `seatType` 枚举：`normal` / `vip` / `couple`
- `status` 枚举：`active` / `maintenance`

**Showtime** — `showtimes`
- `showDate` LocalDate + `showTime` LocalTime
- `price` DECIMAL(10,2)
- `isExpired()` 方法：当前时间超过场次开始时间则视为过期

**Order** — `orders`
- `orderNo` VARCHAR(30) 唯一（`ORD` + 时间戳 + 随机数）
- `status` 枚举：`pending` / `paid` / `refunded` / `cancelled`
- `totalAmount` DECIMAL(10,2)

**Review** — `reviews`
- `rating` INT 评分（1-10 分制）
- `content` TEXT 评语

### 枚举类型

| 枚举 | 值 | 说明 |
|------|-----|------|
| `MovieStatus` | upcoming, showing, ended | 电影上映状态 |
| `OrderStatus` | pending, paid, refunded, cancelled | 订单状态 |
| `CinemaStatus` | open, suspended, preparing, closed | 影院运营状态 |
| `HallType` | normal, imax, vip, threeD | 影厅类型 |
| `SeatType` | normal, vip, couple | 座位类型（情侣座须成对购买） |
| `SeatStatus` | active, maintenance | 座位可用性 |
| `UserRole` | user, admin | 用户角色 |
| `PaymentMethod` | wallet | 支付方式（目前仅虚拟钱包） |
| `PaymentStatus` | success, failed, refunded | 支付状态 |
| `NotificationType` | order, system | 通知类型 |
| `NotificationStatus` | unread, read | 通知阅读状态 |
| `ImageType` | poster, still, banner | 电影图片类型 |

---

## 安全架构

### 认证拦截器链

```
请求 → WebConfig
       ├── SoftAuthInterceptor（order: 0）
       │     仅拦截 /review/status/*
       │     有 token → 解析 userId/username/role
       │     无 token → 放行（userId 为空）
       │
       └── JwtInterceptor（order: 1）
             拦截 /**，排除以下公开路径：
             /user/register, /user/login, /admin/login,
             /movie/list, /movie/most-expected, /movie/*/wish-status,
             /box-office/today, /cinema/list, /cinema/*,
             /showtime/list, /showtime/*, /showtime/*/seats,
             /uploads/**, /review/list, /review/status/*, /error
             无有效 token → 返回 401 JSON
```

### JWT 生成与存储

- 密钥：`application.yml` → `app.jwt.secret`（HS256）
- 有效期：7 天
- 载荷：`userId`, `username`, `role`
- 前端存储于 `localStorage`，每次请求放入 `Authorization: Bearer <token>` Header

### CORS

`WebConfig.addCorsMappings()` 允许所有源、所有方法、所有 Header，允许携带 Cookie（`allowCredentials(true)`）。

---

## API 接口

Base URL：`http://localhost:8080/api`
统一响应格式：`{ "code": 200, "message": "success", "data": {...} }`

### 一、用户模块 `/user`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/user/register` | 无 | 注册。校验用户名重复，BCrypt 加密密码，初始钱包余额 1000 |
| POST | `/user/login` | 无 | 登录。BCrypt 验证，生成 JWT，校验 role 必须为 user |
| GET | `/user/profile` | JWT | 获取个人信息（余额、手机号脱敏） |
| PUT | `/user/profile` | JWT | 修改用户名、手机号、头像 |
| PUT | `/user/password` | JWT | 修改密码。须校验旧密码，新密码 BCrypt 加密 |
| POST | `/user/recharge` | JWT | 钱包充值。须密码验证，使用乐观锁扣款 |

### 二、电影模块 `/movie`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/movie/list` | 无 | 分页列表。可选 `status`（showing/upcoming）和 `keyword`（模糊搜索标题/导演/演员/类型） |
| GET | `/movie/{id}` | 无 | 电影详情（含导演、演员、简介） |
| GET | `/movie/most-expected` | 无 | 最受期待榜。参数 `limit`，基于 wishCount 降序 |
| POST | `/movie/{id}/wish` | JWT | 想看/取消想看。Redis Set 中切换 |
| GET | `/movie/{id}/wish-status` | 软认证 | 当前用户是否已想看 + 累计想看数 |

### 三、影院模块 `/cinema`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/cinema/list` | 无 | 分页影院列表 |

### 四、场次模块 `/showtime`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/showtime/list` | 无 | 按电影查询场次，按影院分组返回。可选 `cinemaId` 和 `date`。只返回未过期场次，每个场次计算可用座位数 |
| GET | `/showtime/{id}` | 无 | 场次详情（含电影名、影院名、影厅名、价格、状态） |
| GET | `/showtime/{id}/seats` | 无 | 座位图。按行列网格排列，每个座位标记 available/sold/locked 状态 |

### 五、订单模块 `/order`（核心流程）

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/order/lock` | JWT | **锁座**。Redis Lua 原子操作，校验场次未过期、座位可用、情侣座成对，锁定 15 分钟，返回 lockToken + 过期时间 + 座位明细 + 总价 |
| POST | `/order/create` | JWT | **下单**。校验 lockToken 有效性，创建 pending 订单，推入 Redis List 异步队列，返回 orderNo |
| GET | `/order/status/{orderNo}` | JWT | 轮询订单状态（pending/paid/refunded/cancelled） |
| GET | `/order/{orderNo}` | JWT | 订单详情（电影、影院、影厅、日期时间、座位列表、金额） |
| GET | `/order/list` | JWT | 用户订单列表。可选 `status` 过滤，分页 |
| POST | `/order/pay/{orderNo}` | JWT | **支付**。密码验证 → 乐观锁扣余额 → 标记 paid → 创建支付记录 |
| POST | `/order/cancel/{orderNo}` | JWT | **退票**。仅 paid 状态可退，全额退款到钱包，创建退款支付记录 |

### 六、评价模块 `/review`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/review` | JWT | 创建或更新评价。评分 1-10，提交后自动重算电影均分 |
| GET | `/review/list` | 无 | 某电影的评价列表。含用户名、头像、评分、评语，分页 |
| GET | `/review/status/{movieId}` | 软认证 | 当前用户的评价状态 + 电影均分 + 评价总数 |

### 七、票房模块 `/box-office`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/box-office/today` | 无 | 全量票房总额 + 各电影票房排名（Top 10）。Redis 缓存 5 分钟 |

### 八、上传模块 `/upload`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| POST | `/upload/image` | JWT | 上传图片。校验扩展名（jpg/jpeg/png/gif/webp），UUID 命名存入 `./uploads/` |

### 九、通知模块 `/notification`

| 方法 | 路径 | 认证 | 说明 |
|------|------|------|------|
| GET | `/notification/list` | JWT | 用户通知列表，按创建时间倒序 |
| PUT | `/notification/{id}/read` | JWT | 标记某条通知为已读 |

### 十、管理后台 `/admin`

**认证要求：JWT + role 必须为 admin**

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/admin/login` | 管理员登录（校验 role 为 admin） |
| POST | `/admin/movie` | 新增电影 |
| PUT | `/admin/movie/{id}` | 修改电影 |
| DELETE | `/admin/movie/{id}` | 删除电影 |
| POST | `/admin/cinema` | 新增影院 |
| GET | `/admin/cinema/list` | 影院列表（管理端） |
| GET | `/admin/cinema/{id}/halls` | 查看某影院下所有影厅 |
| POST | `/admin/hall` | 新增影厅（自动生成 rows×cols 个 seat） |
| POST | `/admin/showtime` | 新增单条场次 |
| POST | `/admin/showtime/batch` | 批量排片（同一电影+影厅+日期，多个时间点） |
| GET | `/admin/showtime/list` | 场次列表（可过滤 movieId、date） |
| DELETE | `/admin/showtime/{id}` | 删除场次 |
| GET | `/admin/order/list` | 所有订单（可过滤 status） |
| GET | `/admin/order/{orderNo}` | 任意订单详情 |
| GET | `/admin/statistics` | 仪表盘：今日订单数/收入、总用户/电影/票房、7天趋势图数据、票房 Top10、心愿 Top10 |

---

## 核心流程详解

### 1. 购票流程

```
Client                        Server
  │                              │
  ├── POST /order/lock ──────────┤
  │   {showtimeId, seatIds}      │→ Redis Lua 原子检查+设置（TTL 15min）
  │                              │→ 校验场次未过期
  │                              │→ 情侣座必须双数
  │                              │← {lockToken, expireTime, seats[{price}], totalAmount}
  │                              │
  ├── POST /order/create ───────┤
  │   {showtimeId, seatIds,      │→ 校验 lockToken 仍有效
  │    lockToken}                │→ 创建 Order（status=pending）
  │                              │→ 创建 OrderSeat 关联
  │                              │→ 推入 Redis List（order:queue）
  │                              │← {orderNo, status: "pending"}
  │                              │
  ├── POST /order/pay/{orderNo} ┤
  │   {password}                 │→ 校验密码（BCrypt）
  │                              │→ 乐观锁扣款：UPDATE users SET walletBalance = balance - amount,
  │                              │              version = version + 1
  │                              │              WHERE id = ? AND version = ?
  │                              │→ 更新 Order status = paid
  │                              │→ 创建 Payment（status=success）
  │                              │← {orderNo, newBalance}
```

### 2. 锁座 Lua 脚本

```
Redis Lua 原子脚本 (lock_seats.lua)：
  遍历 seatIds：
    if EXISTS seat_lock:{seatId} → 返回冲突座位列表
  遍历 seatIds：
    SETEX seat_lock:{seatId} 900 {lockToken}
  记录用户锁：SADD user_locks:{userId} {lockToken}
  返回成功
```

### 3. 订单异步队列

```
OrderStreamProducer.push(orderNo)
    → RPUSH order:queue "orderNo|userId|showtimeId|timestamp"

OrderStreamConsumer (@Scheduled 500ms)
    → LPOP order:queue
    → 处理订单逻辑（当前记录日志，实际扣款由 payOrder 手动触发）

OrderStreamConsumer (@Scheduled 60s)
    → 清理 expiresAt > 15min 的 pending 订单
    → 标记为 cancelled，释放锁
```

### 4. 并发安全

| 场景 | 方案 | 实现 |
|------|------|------|
| 同时选座 | Redis Lua 原子操作 | `SeatLockService.tryLockSeats()` 内执行 Lua 脚本 |
| 钱包扣款 | 乐观锁 | `UserRepository.deductBalance()`：`UPDATE users SET walletBalance = balance - ?, version = version + 1 WHERE id = ? AND version = ? AND walletBalance >= ?` |
| 场次过期 | 三层防线 | (1) Repository SQL 只查未过期 (2) `lockSeats()` 校验 (3) `createOrder()` 校验 |

### 5. 场次过期判断

```java
// Showtime.java
public boolean isExpired() {
    return LocalDateTime.of(showDate, showTime).isBefore(LocalDateTime.now());
}
```

- Repository JPQL 使用 Java 传入的 `LocalDate.now()` / `LocalTime.now()` 而非 MySQL 的 `CURRENT_DATE`/`CURRENT_TIME`，避免时区差异
- `getShowtimeList()` 的 date 参数默认为 null（返回所有未过期场次），而非 "今天"

### 6. 票房统计

```
BoxOfficeService
  ├── getTodayBoxOffice()
  │     先查 Redis → 命中则直接返回
  │     未命中 → calculateAndCacheBoxOffice()
  │             遍历所有 paid 订单 → 按 movieId 聚合 → 排序 → Top 10
  │             缓存到 Redis (String 序列化, 无过期时间, 通过定时任务刷新)
  │
  └── @Scheduled(5min) syncTodayBoxOffice()
        定时刷新缓存
```

### 7. 想看功能

```
WishService
  ├── toggleWish(userId, movieId)
  │     SISMEMBER "wish:movie:{movieId}" userId
  │     存在 → SREM 移除
  │     不存在 → SADD 添加
  │
  ├── getWishCount(movieId) → SCARD
  │
  └── @Scheduled(60min) syncWishCounts()
        遍历所有 movie → SCARD → 更新 Movie.wishCount
```

---

## Redis Key 设计

| Key 模式 | 类型 | 用途 | TTL |
|----------|------|------|-----|
| `seat_lock:{seatId}` | String | 座位锁，值为 lockToken | 900s |
| `user_locks:{userId}` | Set | 当前用户的所有活跃锁 | — |
| `order:queue` | List | 订单处理队列 | — |
| `wish:movie:{movieId}` | Set | 想看该电影的用户 ID | — |
| `review:movie:{movieId}` | List | 电影评价缓存 | — |
| `box:office:today` | String | 全量总票房（JSON 数值） | — |
| `box:office:today:movies` | String | 电影票房排名（JSON 数组） | — |

---

## 静态资源

- 上传目录：`./uploads/`（相对于 backend 工作目录）
- 资源映射：`WebConfig.addResourceHandlers("/uploads/**")` → `file:./uploads/`
- JWT 拦截器排除 `/uploads/**`，图片无需认证即可访问
- 返回给前端的 URL：`/uploads/{uuid}.{ext}`

---

## 异常处理

`GlobalExceptionHandler`（@RestControllerAdvice）：

| 异常类型 | 返回 code | 说明 |
|----------|-----------|------|
| `BusinessException` | 抛出时指定的 code | 业务异常，如 400 参数错误、409 冲突 |
| `MethodArgumentNotValidException` | 400 | JSR-303 校验失败 |
| 其他 `Exception` | 500 | 未知异常，日志记录 |

注意：所有异常都返回 HTTP 200，通过响应体 `code` 字段区分状态（前端不用处理 HTTP 错误码）。

---

## 启动与部署

```bash
cd backend
mvn spring-boot:run        # 开发启动
mvn clean package -DskipTests  # 打包 JAR
java -jar target/*.jar     # 生产运行
```

依赖环境：MySQL 8.0 + Redis 6.0+，密码配置见 `application.yml`。
