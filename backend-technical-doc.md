# 电影票预订系统 — 后端技术解释文档

> 本文档基于项目源码逐模块解读，帮助理解整个后端的架构设计、核心流程和技术细节。

---

## 目录

1. [技术栈总览](#1-技术栈总览)
2. [项目结构](#2-项目结构)
3. [分层架构详解](#3-分层架构详解)
4. [数据库设计](#4-数据库设计)
5. [统一响应与异常处理](#5-统一响应与异常处理)
6. [认证体系 (JWT)](#6-认证体系-jwt)
7. [核心业务模块](#7-核心业务模块)
8. [订单流程 — 最核心的链路](#8-订单流程--最核心的链路)
9. [Redis 在本项目中的应用](#9-redis-在本项目中的应用)
10. [并发安全设计](#10-并发安全设计)
11. [配置文件解读](#11-配置文件解读)
12. [API 接口清单](#12-api-接口清单)

---

## 1. 技术栈总览

| 层级 | 技术 | 版本 | 作用 |
|------|------|------|------|
| 框架 | Spring Boot | 3.2.5 | 提供 Web 服务器、依赖注入、自动配置 |
| ORM | Spring Data JPA (Hibernate) | — | 数据库访问，自动生成 SQL |
| 数据库 | MySQL | 8.0 | 持久化存储 |
| 缓存 | Redis | — | 座位锁、订单队列 |
| 认证 | JJWT | 0.12.5 | JWT 令牌的生成与验证 |
| 密码 | Spring Security Crypto | — | BCrypt 密码哈希 |
| 构建 | Maven | — | 依赖管理和构建 |
| JDK | Java | 17 | 语言版本 |

**为什么不用 Lombok？** 项目注释说明 Lombok 与 JDK 24 存在兼容性问题，因此所有实体类都手写 getter/setter。

---

## 2. 项目结构

```
backend/src/main/java/com/moviebooking/
├── MovieBookingApplication.java   ← 启动类
├── controller/    ← 9 个控制器，接收 HTTP 请求
├── service/       ← 9 个服务类，核心业务逻辑
├── repository/    ← 12 个 JPA 接口，数据库操作
├── entity/        ← 12 个实体类 + 12 个枚举类
├── dto/           ← 12 个请求数据传输对象（含 ChangePasswordRequest）
├── config/        ← 3 个配置类 (JWT拦截器、Web配置、Redis配置)
├── redis/         ← 3 个 Redis 相关类 (锁座服务、订单生产者/消费者)
├── common/        ← 4 个通用类 (统一响应、分页、异常、全局异常处理)
└── util/          ← 2 个工具类 (JWT工具、订单号生成器)

backend/src/main/resources/
├── application.yml                ← 配置文件
├── lua/lock_seats.lua             ← Redis Lua 原子锁座脚本
├── db/init.sql                    ← 完整建库脚本 (380行)
└── data.sql                       ← Spring Boot 自动执行的种子数据
```

**文件统计：** 共 78 个 Java 文件 (12 实体 + 12 枚举 + 12 仓库 + 11 DTO + 9 控制器 + 9 服务 + 4 通用 + 3 配置 + 3 Redis + 2 工具 + 1 启动类)

---

## 3. 分层架构详解

本项目采用经典的 **四层架构**：

```
HTTP 请求
    ↓
┌─────────────────────────────────────────┐
│  Controller 层 (接收请求、参数校验、返回响应)  │
│  例: UserController, OrderController       │
└─────────────────┬───────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│  Service 层 (核心业务逻辑)                  │
│  例: UserService, OrderService             │
└─────────────────┬───────────────────────┘
                  ↓
┌─────────────────────────────────────────┐
│  Repository 层 (数据库操作)                 │
│  例: UserRepository, OrderRepository       │
└─────────────────┬───────────────────────┘
                  ↓
              MySQL / Redis
```

### 各层职责

**Controller 层：**
- 用 `@RestController` 标记，返回 JSON
- 用 `@RequestMapping` 定义 URL 前缀
- 用 `@Valid` 触发 DTO 上的参数校验注解
- 从 `HttpServletRequest.getAttribute("userId")` 获取当前登录用户 ID（由 JWT 拦截器注入）
- 调用 Service 方法，将结果包装成 `ApiResult` 返回

**Service 层：**
- 用 `@Service` 标记
- 包含所有业务逻辑：校验、计算、事务管理
- 通过构造函数注入 Repository 依赖（`@Autowired` 构造器注入）
- 返回 `Map<String, Object>` 或 `PageResult`，而非直接返回实体

**Repository 层：**
- 继承 `JpaRepository<Entity, ID>`，获得 CRUD 能力
- 只需定义方法签名，Spring Data JPA 自动生成 SQL
- 复杂查询用 `@Query` 注解写 JPQL

**为什么不直接返回实体？** Service 返回 `Map<String, Object>` 而非实体对象，这样可以：
1. 精确控制返回给前端的字段（如隐藏密码、脱敏手机号）
2. 灵活组合多个表的数据（如订单详情需要关联电影、影院、场次信息）

---

## 4. 数据库设计

### ER 关系图（文字版）

```
users (用户)
  ├── 1:N → orders (订单)
  ├── 1:N → reviews (评价)
  └── 1:N → notifications (通知)

movies (电影)
  ├── 1:N → movie_images (电影图片)
  ├── 1:N → showtimes (场次)
  └── 1:N → reviews (评价)

cinemas (影院)
  └── 1:N → halls (影厅)
              └── 1:N → seats (座位)

showtimes (场次) ──关联→ movies + halls
  └── 1:N → orders (订单)

orders (订单) ──关联→ users + showtimes
  ├── 1:N → order_seats (订单座位)
  └── 1:N → payments (支付记录)

order_seats (订单座位) ──关联→ orders + seats
```

### 12 张表详解

#### 4.1 users — 用户表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT (自增) | 主键 |
| username | VARCHAR(50) UNIQUE | 用户名，不可重复 |
| password | VARCHAR | BCrypt 加密后的密码 |
| phone | VARCHAR(20) | 手机号 |
| role | ENUM('user','admin') | 角色 |
| wallet_balance | DECIMAL(10,2) | 虚拟钱包余额，默认 1000.00 |
| version | INT | 乐观锁版本号，初始为 0 |
| avatar | VARCHAR(255) | 头像 URL |
| created_at / updated_at | DATETIME | 创建/更新时间 |

**关键设计：** `version` 字段用于乐观锁——支付扣款时通过 `WHERE version = ?` 确保不会被并发重复扣款。

#### 4.2 movies — 电影表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键 |
| title | VARCHAR(200) | 电影名 |
| duration | INT | 时长（分钟） |
| release_date | DATE | 上映日期 |
| rating | DECIMAL(3,1) | 评分 (0.0-9.9) |
| description | TEXT | 简介 |
| genre | VARCHAR(200) | 类型 |
| director | VARCHAR(100) | 导演 |
| actors | VARCHAR(500) | 演员 |
| poster | VARCHAR(255) | 海报 URL |
| status | ENUM('upcoming','showing','ended') | 状态 |

#### 4.3 movie_images — 电影图片表

存储电影的多张图片（海报、剧照、横幅），通过 `movie_id` 关联电影。

#### 4.4 cinemas — 影院表

| 字段 | 说明 |
|------|------|
| name | 影院名称 |
| address | 地址 |
| phone | 电话 |
| status | 状态 (open/suspended/preparing/closed) |
| business_hours | 营业时间 |
| longitude/latitude | 经纬度 |

#### 4.5 halls — 影厅表

每个影厅属于一个影院，记录行列数和类型 (normal/imax/vip/threeD)。

#### 4.6 showtimes — 场次表

| 字段 | 说明 |
|------|------|
| movie_id | 关联电影 |
| hall_id | 关联影厅 |
| show_date | 放映日期 |
| show_time | 放映时间 |
| price | 基础票价 |
| status | 状态 (normal/cancelled/sold_out) |

#### 4.7 seats — 座位表

每个影厅自动生成 `seatRows × seatCols` 个座位，有唯一约束 `(hall_id, row_num, col_num)`。
座位类型：normal（普通）、vip（VIP，1.5倍票价）、couple（情侣座，2倍票价，必须成对选择）。

#### 4.8 orders — 订单表

| 字段 | 说明 |
|------|------|
| order_no | 订单号，格式 `ORDyyyyMMddHHmmssXXXX` |
| user_id | 关联用户 |
| showtime_id | 关联场次 |
| total_amount | 总金额 |
| status | pending → paid → refunded/cancelled |

#### 4.9 order_seats — 订单座位关联表

一个订单可以包含多个座位，记录每个座位的实际购买价格。

#### 4.10 payments — 支付记录表

记录每笔支付/退款，关联订单和用户。

#### 4.11 reviews — 评价表

用户对电影的评分(1-5)和文字评价。

#### 4.12 notifications — 通知表

系统通知和订单通知，有已读/未读状态。

---

## 5. 统一响应与异常处理

### ApiResult — 统一响应格式

```java
{
    "code": 200,        // 状态码
    "message": "success", // 提示信息
    "data": { ... }     // 业务数据（可为 null）
}
```

`ApiResult` 类提供静态工厂方法：
- `ApiResult.success(data)` — 成功
- `ApiResult.success("操作成功", data)` — 成功 + 自定义消息
- `ApiResult.badRequest("参数错误")` — 400
- `ApiResult.notFound("不存在")` — 404
- `ApiResult.conflict("冲突")` — 409

`@JsonInclude(NON_NULL)` 注解确保 `data` 为 null 时不输出到 JSON。

### PageResult — 分页响应

```java
{
    "list": [...],  // 当前页数据
    "total": 100,   // 总记录数
    "page": 1,      // 当前页码（从1开始）
    "size": 10      // 每页大小
}
```

### BusinessException — 业务异常

自定义运行时异常，携带 HTTP 状态码。Service 层通过 `throw BusinessException.badRequest("xxx")` 抛出。

### GlobalExceptionHandler — 全局异常处理

用 `@RestControllerAdvice` 捕获所有异常：

| 异常类型 | HTTP 状态 | 处理方式 |
|---------|----------|---------|
| BusinessException | 200 | 返回对应的业务错误码和消息 |
| MethodArgumentNotValidException | 200 | 收集所有校验错误消息，返回 400 |
| 其他 Exception | 500 | 返回"服务器内部错误"，记录日志 |

**为什么 BusinessException 返回 HTTP 200？** 这是前后端约定：HTTP 层面始终返回 200，通过 `code` 字段区分业务成功/失败。前端只需检查 `response.data.code === 200` 即可。

---

## 6. 认证体系 (JWT)

### 6.1 JWT 令牌结构

```
Header.Payload.Signature

Header:  { "alg": "HS256", "typ": "JWT" }
Payload: { "userId": 1, "username": "zhangsan", "role": "user", "iat": ..., "exp": ... }
Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secret)
```

- 使用 HMAC-SHA256 签名算法
- 密钥配置在 `application.yml`：`MovieTicketBookingSystemSecretKey2026MustBeLongEnoughForHS256Algorithm`
- 过期时间：7 天 (604800000ms)

### 6.2 JwtUtil — JWT 工具类

```java
@Component
public class JwtUtil {
    // 生成令牌：将 userId、username、role 写入 claims
    public String generateToken(Long userId, String username, String role)

    // 解析令牌：提取 claims
    public Claims parseToken(String token)

    // 便捷方法
    public Long getUserId(String token)
    public String getUsername(String token)
    public String getRole(String token)

    // 验证令牌是否有效（未过期）
    public boolean isTokenValid(String token)
}
```

### 6.3 JwtInterceptor — JWT 拦截器

实现 Spring 的 `HandlerInterceptor`，在每个请求的 `preHandle` 阶段执行：

```
请求进入 → 检查是否 OPTIONS（放行）→ 提取 Authorization 头 → 去掉 "Bearer " 前缀
    → 验证 token 是否有效 → 将 userId/username/role 写入 request 属性 → 放行
```

失败时直接写回 401 JSON 响应，不进入 Controller。

### 6.4 WebConfig — 拦截器配置

哪些路径**需要**登录：
- `/order/**`（下单、查订单）
- `/user/profile`（个人信息）
- `/admin/**`（管理接口，除了 `/admin/login`）
- `/review`（发表评价）
- `/notification/**`（通知）
- `/upload/**`（上传）

哪些路径**不需要**登录（公开接口）：
- `/user/register`, `/user/login`, `/admin/login`
- `/movie/list`, `/movie/**`
- `/cinema/list`
- `/showtime/list`, `/showtime/*/seats`
- `/uploads/**`（静态资源）
- `/review/list`

### 6.5 认证流程图

```
前端登录:
  POST /api/user/login  { username, password }
      ↓
  UserService.login()
      → 查数据库 → BCrypt 验证密码 → 生成 JWT
      → 返回 { token, userInfo }
      ↓
  前端存储 token 到 localStorage

前端后续请求:
  GET /api/user/profile
  Header: Authorization: Bearer eyJhbGciOiJI...
      ↓
  JwtInterceptor.preHandle()
      → 解析 token → 提取 userId → 写入 request.setAttribute("userId", 1)
      ↓
  UserController.getProfile()
      → Long userId = request.getAttribute("userId")
      → 调用 service
```

### 6.6 密码加密

使用 BCrypt 算法：
- 注册时：`passwordEncoder.encode(plainPassword)` → 生成带盐的哈希值
- 登录时：`passwordEncoder.matches(plainPassword, hashedPassword)` → 验证

BCrypt 的特点是每次加密结果不同（内置随机盐），但验证时能正确匹配。

---

## 7. 核心业务模块

### 7.1 用户模块 (UserService)

**注册流程：**
1. 检查用户名是否已存在
2. BCrypt 加密密码
3. 创建用户（默认角色 user，初始余额 1000）
4. 返回用户信息（手机号脱敏：`138****1234`）

**登录流程：**
1. 根据用户名查找用户
2. BCrypt 验证密码
3. 如果是管理员登录，额外检查 role 是否为 admin
4. 生成 JWT 令牌
5. 返回 token + 用户信息

**手机号脱敏：** `maskPhone("13812345678")` → `"138****5678"`

### 7.2 电影模块 (MovieService)

**查询电影列表：**
- 支持按状态筛选 (upcoming/showing/ended)
- 支持关键词搜索（模糊匹配电影名）
- 分页查询，按 ID 倒序

**Repository 的自定义查询：**
```java
@Query("SELECT m FROM Movie m WHERE (:status IS NULL OR m.status = :status) AND (:keyword IS NULL OR m.title LIKE %:keyword%)")
Page<Movie> findByStatusAndKeyword(@Param("status") MovieStatus status, @Param("keyword") String keyword, Pageable pageable);
```

当参数为 null 时条件自动跳过，实现可选筛选。

**管理员操作：** 增删改电影，使用 `applyMovieFields` 方法实现部分更新（只更新非 null 字段）。

**状态枚举注意：** `MovieStatus` 枚举值为 `upcoming`/`showing`/`ended`。`valueOf()` 对无效值会抛 `IllegalArgumentException`，已加 try-catch 包装为 `BusinessException.badRequest`。

### 7.3 影院模块 (CinemaService)

**创建影厅时自动生成座位：**
```java
@Transactional
public Map<String, Object> createHall(HallRequest request) {
    // 1. 保存影厅
    Hall hall = hallRepository.save(new Hall(...));

    // 2. 自动生成 seats 行×col 个座位
    for (int row = 1; row <= seatRows; row++) {
        for (int col = 1; col <= seatCols; col++) {
            seats.add(new Seat(hall.getId(), row, col, normal, active));
        }
    }
    seatRepository.saveAll(seats);
}
```

10×20 的 IMAX 厅会自动生成 200 个座位记录。

### 7.4 场次模块 (ShowtimeService)

**查询排片列表：**
- 按电影ID + 可选影院ID + 可选日期查询
- 结果按影院分组展示
- 每个场次附带剩余座位数 = 总座位数 - 已售座位数

**已售座位查询：**
```java
@Query("SELECT os.seatId FROM OrderSeat os WHERE os.orderId IN (SELECT o.id FROM Order o WHERE o.showtimeId = :showtimeId AND o.status = 'paid')")
List<Long> findSoldSeatIdsByShowtimeId(@Param("showtimeId") Long showtimeId);
```

通过子查询找到该场次所有已支付订单关联的座位ID。

**获取座位图：**
返回每个座位的状态：
- `available` — 可选
- `sold` — 已售（在已支付订单中）
- `locked` — 维护中（seat.status = maintenance）

**批量创建排片：** 一次提交多个时间段，循环创建场次记录。

### 7.5 评价模块 (ReviewService)

- 用户提交评分(1-5)和文字内容
- 查询评价列表时关联查出用户名

### 7.6 通知模块 (NotificationService)

- 按用户查询通知列表（按时间倒序）
- 标记已读（校验通知归属，防止越权）

### 7.7 文件上传模块 (UploadService)

1. 校验文件非空
2. 校验扩展名 (jpg/jpeg/png/gif/webp)
3. 生成 UUID 文件名避免冲突
4. 使用 `getAbsoluteFile()` 获取绝对路径后保存到 `./uploads/` 目录
5. 返回 URL：`/uploads/xxxxxxxx.jpg`

静态资源通过 `WebConfig.addResourceHandlers` 映射到文件系统，使用 `File.toURI().toString()` 生成正确的 `file:///` URI。

**注意：** 必须使用绝对路径，否则 `transferTo` 在 Windows 上可能因相对路径失败。

---

## 8. 订单流程 — 最核心的链路

这是整个系统最复杂、最有技术含量的部分，涉及 Redis 锁座、异步队列、乐观锁支付。

### 8.1 完整流程图

```
用户选座
    ↓
POST /api/order/lock  { showtimeId, seatIds }
    │
    ├─ 校验场次存在
    ├─ 校验座位存在
    ├─ 校验情侣座成对选择
    ├─ Redis Lua 脚本原子锁座 ←──── 防并发超卖的关键
    ├─ 计算价格（普通1倍、VIP1.5倍、情侣座2倍）
    └─ 返回 { lockToken, expireTime, seats, totalAmount }
    ↓
用户确认下单
    ↓
POST /api/order/create  { showtimeId, seatIds, lockToken }
    │
    ├─ 验证锁是否有效
    ├─ 生成订单号
    └─ 推入 Redis 队列，立即返回 { orderNo, status: "processing" }
    ↓
Redis 队列异步消费 (OrderStreamConsumer, 每500ms)
    │
    ├─ 验证锁是否仍有效
    ├─ 计算价格
    ├─ 检查余额是否充足
    ├─ 乐观锁扣减余额 ←──── 防并发重复扣款
    ├─ 创建订单 (status=paid)
    ├─ 创建订单座位关联
    ├─ 创建支付记录
    └─ 释放 Redis 锁
    ↓
前端轮询订单状态
    ↓
GET /api/order/status/{orderNo}
    └─ 返回 { orderNo, status, message }
```

### 8.2 为什么用两步提交？

**锁座 → 创建订单** 分为两步，中间有一个确认过程。这样设计的原因：

1. **锁座阶段**：用户还在犹豫选哪些座位，此时只需锁定，不创建订单
2. **创建阶段**：用户确认信息后提交，此时座位已被锁住，不会被抢
3. **15分钟超时**：如果用户锁了座但不付款，TTL 到期自动释放

### 8.3 价格计算

```java
private BigDecimal calculateSeatPrice(BigDecimal basePrice, SeatType seatType) {
    return switch (seatType) {
        case vip -> basePrice × 1.5    // VIP 座 1.5 倍
        case couple -> basePrice × 2   // 情侣座 2 倍
        default -> basePrice;          // 普通座 1 倍
    };
}
```

### 8.4 订单状态流转

```
pending (排队中)
    ↓ 支付成功
paid (已支付)
    ↓ 退款
refunded (已退款)

pending
    ↓ 支付失败/锁过期/余额不足
cancelled (已取消)
```

### 8.5 退票流程

```java
@Transactional
public Map<String, Object> cancelOrder(String orderNo, Long userId) {
    // 1. 校验订单存在且属于当前用户
    // 2. 校验订单状态必须是 paid
    // 3. 退款到钱包（乐观锁）
    userRepository.addBalance(userId, order.getTotalAmount());
    // 4. 更新订单状态为 refunded
    // 5. 创建退款记录（金额为负数）
    // 6. 释放 Redis 中的用户锁
    seatLockService.releaseUserLock(userId);
}
```

---

## 9. Redis 在本项目中的应用

### 9.1 座位锁 (SeatLockService)

**Redis Key 设计：**
```
seat:lock:{showtimeId}:{seatId}  →  值=userId, TTL=900秒
user:lock:{userId}               →  值=showtimeId:seatId1,seatId2, TTL=900秒
```

**为什么用 Redis 而不是数据库锁？**
1. **高性能**：Redis 操作是内存级别的，比数据库行锁快 10-100 倍
2. **自动过期**：Redis 的 TTL 机制天然支持"15分钟后自动释放"
3. **分布式**：如果将来部署多台服务器，Redis 锁天然跨进程共享

### 9.2 Lua 原子锁座脚本

```lua
-- lock_seats.lua
local userId = ARGV[1]
local ttl = tonumber(ARGV[2])

-- 第一步：检查所有座位是否可用
for i, key in ipairs(KEYS) do
    local val = redis.call('GET', key)
    if val and val ~= '' then
        return {0, key}  -- 有冲突，返回冲突的 key
    end
end

-- 第二步：全部锁定
for i, key in ipairs(KEYS) do
    redis.call('SETEX', key, ttl, userId)
end

return {1, ''}  -- 全部成功
```

**为什么需要 Lua 脚本？**

假设用户要锁 A1 和 A2 两个座位，不用 Lua 的话：

```
线程1: 检查A1→OK, 检查A2→OK, 锁A1, 锁A2   ← 成功
线程2: 检查A1→OK, 检查A2→OK, 锁A1(失败!), 锁A2  ← A1被线程1锁了，但A2已经被线程2检查过了
```

这就是经典的**检查-执行竞态条件 (check-then-act race condition)**。

Lua 脚本在 Redis 中是**原子执行**的——整个脚本作为一个不可中断的操作运行，中间不会有其他命令插入。

**执行流程：**
```
Java 代码:
  keys = ["seat:lock:1:101", "seat:lock:1:102"]
  args = ["userId=1", "ttl=900"]

Redis 执行 Lua:
  检查 seat:lock:1:101 是否存在 → 不存在
  检查 seat:lock:1:102 是否存在 → 不存在
  SETEX seat:lock:1:101 900 "1"
  SETEX seat:lock:1:102 900 "1"
  return {1, ""}

Java 接收结果:
  result[0] == 1 → 锁定成功
```

### 9.3 用户锁机制

除了座位锁，还有一个**用户锁** `user:lock:{userId}`：

- **目的：** 每个用户同时只能有一组未完成的锁座
- **防止：** 用户锁了 A1/A2，又去锁 B1/B2，导致 A1/A2 白白占用
- **实现：** 锁座前先检查 `user:lock:{userId}` 是否存在

### 9.4 订单队列 (OrderStreamProducer / OrderStreamConsumer)

**为什么用异步队列？**

创建订单涉及：验证锁 → 查价格 → 扣余额 → 创建订单 → 创建关联 → 创建支付记录 → 释放锁

如果在 HTTP 请求中同步执行，高并发时会：
1. 请求响应慢
2. 数据库连接池被占满
3. 用户体验差

**解决方案：** 用 Redis List 作为简单的消息队列。

**Producer（生产者）：**
```java
// 将订单请求推入队列，立即返回
String message = orderNo + "|" + userId + "|" + showtimeId + "|" + seatIds + "|" + lockToken;
redisTemplate.opsForList().rightPush("order:queue", message);
```

**Consumer（消费者）：**
```java
@Scheduled(fixedDelay = 500)  // 每500ms执行一次
public void consume() {
    String message = redisTemplate.opsForList().leftPop("order:queue", 200, TimeUnit.MILLISECONDS);
    if (message != null) {
        processOrder(message);  // 在事务中处理
    }
}
```

**为什么用 List 而不是 Redis Stream？** 项目文件名叫 `OrderStreamProducer/Consumer`，但实际用的是 `RedisTemplate.opsForList()`（List 操作），而非 Stream API。这是因为 List 操作更简单，对于这个课设场景足够了。命名中的 "Stream" 可能是最初设计时的想法。

**消息格式：** 用 `|` 分隔符拼接字段，避免 JSON 序列化开销：
```
ORD202606241430520001|1|1|101,102|abc123def456
```

---

## 10. 并发安全设计

### 10.1 乐观锁 — 余额扣减

```java
// UserRepository
@Modifying
@Query("UPDATE User u SET u.walletBalance = u.walletBalance - :amount, u.version = u.version + 1 " +
       "WHERE u.id = :userId AND u.version = :version AND u.walletBalance >= :amount")
int deductBalance(@Param("userId") Long userId, @Param("amount") BigDecimal amount, @Param("version") Integer version);
```

**工作原理：**
1. 读取用户时记录当前 `version = 0`
2. 执行 UPDATE 时带 `WHERE version = 0`
3. 如果期间有其他事务已经更新了该行（version 变成 1），WHERE 条件不匹配，更新 0 行
4. `int updated = deductBalance(...)` 返回 0 表示失败

**对比悲观锁：**
- 悲观锁：`SELECT ... FOR UPDATE`，阻塞其他事务（适合写多读少）
- 乐观锁：不加锁，提交时检查版本号（适合读多写少，如余额操作）

### 10.2 原子锁座 — Redis Lua

如上文所述，Lua 脚本保证"检查+锁定"的原子性。

### 10.3 异步串行化 — Redis 队列

将订单创建请求推入 Redis List，消费者每 500ms 取一条处理。虽然不是严格的串行（如果消费逻辑执行时间 > 500ms 可能并发），但大幅降低了并发冲突的概率。

---

## 11. 配置文件解读

### application.yml 关键配置

```yaml
server:
  port: 8080
  servlet:
    context-path: /api     # 所有接口前缀加 /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/movie_ticket?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: clh123456
  jpa:
    hibernate:
      ddl-auto: update     # 自动更新表结构（不删数据）
    open-in-view: false     # 关闭 OSIV，避免懒加载问题
  data:
    redis:
      host: localhost
      port: 6379
      password: clh123456

app:
  jwt:
    secret: MovieTicketBookingSystemSecretKey2026MustBeLongEnoughForHS256Algorithm
    expiration: 604800000   # 7天 = 7×24×60×60×1000 ms
  upload:
    path: ./uploads/
    url-prefix: /uploads/
  seat-lock:
    ttl-seconds: 900        # 15分钟 = 900秒
```

**`ddl-auto: update` 的含义：**
- `none`：不自动建表
- `create`：每次启动删表重建（数据丢失）
- `create-drop`：启动建表，关闭删表
- `update`：对比实体和表结构，只做增量更新（推荐开发用）
- `validate`：只验证不修改（推荐生产用）

**`open-in-view: false` 的含义：**
- `true`（默认）：在整个 HTTP 请求期间保持数据库 Session 打开，允许在 Controller 中访问懒加载属性
- `false`：Session 在 Service 层结束后关闭，更安全但需要在 Service 中提前加载所有需要的数据

### 启动类

```java
@SpringBootApplication(exclude = {RedisRepositoriesAutoConfiguration.class})
@EnableScheduling
```

- 排除 `RedisRepositoriesAutoConfiguration`：不使用 Spring Data Redis 的 Repository 自动配置（本项目直接用 `RedisTemplate` 操作 Redis）
- `@EnableScheduling`：启用定时任务（用于 `OrderStreamConsumer` 的 `@Scheduled`）

---

## 12. API 接口清单

### 公开接口（无需登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/register` | 用户注册 |
| POST | `/api/user/login` | 用户登录 |
| POST | `/api/admin/login` | 管理员登录 |
| GET | `/api/movie/list` | 电影列表 (支持 status/keyword/page/size) |
| GET | `/api/movie/{id}` | 电影详情 |
| GET | `/api/cinema/list` | 影院列表 |
| GET | `/api/showtime/list` | 排片列表 (需要 movieId, 可选 cinemaId/date) |
| GET | `/api/showtime/{id}/seats` | 座位图 |
| GET | `/api/review/list` | 评价列表 (需要 movieId) |

### 用户接口（需要登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/profile` | 获取个人信息 |
| PUT | `/api/user/profile` | 修改个人信息（支持 username/phone/avatar） |
| PUT | `/api/user/password` | 修改密码（需校验旧密码） |
| POST | `/api/order/lock` | 锁定座位 |
| POST | `/api/order/create` | 创建订单 |
| GET | `/api/order/status/{orderNo}` | 查询订单状态 |
| GET | `/api/order/{orderNo}` | 订单详情 |
| GET | `/api/order/list` | 订单列表 |
| POST | `/api/order/cancel/{orderNo}` | 退票 |
| POST | `/api/review` | 发表评价 |
| GET | `/api/notification/list` | 通知列表 |
| PUT | `/api/notification/{id}/read` | 标记通知已读 |
| POST | `/api/upload/image` | 上传图片 |

### 管理员接口（需要管理员登录）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/admin/movie` | 添加电影 |
| PUT | `/api/admin/movie/{id}` | 修改电影 |
| DELETE | `/api/admin/movie/{id}` | 删除电影 |
| POST | `/api/admin/cinema` | 添加影院 |
| POST | `/api/admin/hall` | 添加影厅 |
| POST | `/api/admin/showtime` | 添加排片 |
| POST | `/api/admin/showtime/batch` | 批量添加排片 |
| GET | `/api/admin/order/list` | 管理员订单列表 |
| GET | `/api/admin/order/{orderNo}` | 管理员订单详情 |
| GET | `/api/admin/statistics` | 数据统计 |

---

## 附录：关键技术点速查

| 技术点 | 实现位置 | 说明 |
|--------|---------|------|
| BCrypt 密码加密 | UserService | 注册加密，登录验证 |
| JWT 认证 | JwtUtil + JwtInterceptor | 令牌生成、验证、用户信息注入 |
| 分页查询 | 所有 list 接口 | `PageRequest.of(page-1, size)` 注意 page 从 1 开始 |
| 参数校验 | DTO + @Valid | Jakarta Validation 注解 |
| Redis 原子锁座 | SeatLockService + lock_seats.lua | Lua 脚本保证原子性 |
| 乐观锁扣款 | UserRepository.deductBalance | version 字段 + WHERE 条件 |
| 异步订单处理 | OrderStreamProducer/Consumer | Redis List 作为消息队列 |
| CORS 跨域 | WebConfig | 允许所有来源 |
| 静态资源 | WebConfig.addResourceHandlers | /uploads/** 映射到文件系统 |
| 自动生成座位 | CinemaService.createHall | 创建影厅时自动填充 seats 表 |
| 手机号脱敏 | UserService.maskPhone | 中间4位替换为 **** |
| 订单号生成 | OrderNoGenerator | 时间戳 + 4位自增序列 |
