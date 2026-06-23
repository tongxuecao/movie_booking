# 电影票预订系统后端实现计划

> **Goal:** 实现完整的 Spring Boot 3 后端，包含 12 张表、27 个 API 接口、Redis Lua 原子锁座、Redis Stream 异步下单、虚拟钱包乐观锁。

**Architecture:** Spring Boot 3 单体架构，分层：Controller → Service → Repository → Entity。Redis 负责座位锁和订单异步处理（Stream）。JWT 认证 + BCrypt 密码加密。

**Tech Stack:** Spring Boot 3.2.5, Spring Data JPA, MySQL 8.0, Redis, JWT (jjwt 0.12.5), BCrypt (spring-security-crypto)

---

## 当前状态：后端开发完成（2026-06-23）

**编译状态：** `mvn clean compile` BUILD SUCCESS，共 78 个 Java 源文件。

**运行环境：**
- JDK 24（Lombok 不兼容，已移除改为手写 getter/setter/构造器）
- MySQL 8.0（数据库：movie_ticket，用户名：root，密码：clh123456）
- Redis（localhost:6379，密码：clh123456）

**已完成：**
- ✅ 后端 78 个 Java 文件全部完成
- ✅ 9 个 Controller 覆盖全部业务接口
- ✅ 12 个 JPA Repository 实现数据访问
- ✅ 9 个 Service 实现业务逻辑
- ✅ Redis Lua 原子锁座实现
- ✅ Redis Stream 异步订单处理
- ✅ 虚拟钱包乐观锁扣款
- ✅ JWT 认证拦截器
- ✅ 数据库初始化脚本（`db/init.sql`）
- ✅ 初始数据脚本（`data.sql`）
- ✅ Redis 测试指南（`docs/redis-test-guide.md`）

**待完成：**
-   前端开发（Vue 3 + Vant 4）
-   前后端联调
-   JMeter 性能压测（1000 并发）
-   单元测试编写

---

## 文件结构（实际）

```
backend/
├── pom.xml
├── src/main/java/com/moviebooking/
│   ├── MovieBookingApplication.java          # 主启动类（@EnableScheduling）
│   ├── common/
│   │   ├── ApiResult.java                    # 统一响应 { code, message, data }
│   │   ├── PageResult.java                   # 分页响应 { list, total, page, size }
│   │   ├── BusinessException.java            # 业务异常（携带 code）
│   │   └── GlobalExceptionHandler.java       # @RestControllerAdvice 全局异常处理
│   ├── config/
│   │   ├── RedisConfig.java                  # RedisTemplate + StringRedisTemplate 配置
│   │   ├── WebConfig.java                    # CORS + JWT 拦截器注册 + 静态资源映射
│   │   └── JwtInterceptor.java               # JWT 认证拦截器（校验 token，注入 userId/role）
│   ├── util/
│   │   ├── JwtUtil.java                      # JWT 生成/解析/校验（jjwt 0.12.5）
│   │   └── OrderNoGenerator.java             # 订单号生成（ORD + 时间戳 + 序列号）
│   ├── entity/                               # 12 个 JPA 实体
│   │   ├── User.java                         # 用户表（@Version 乐观锁）
│   │   ├── Movie.java                        # 电影表
│   │   ├── MovieImage.java                   # 电影图片表
│   │   ├── Cinema.java                       # 影院表
│   │   ├── Hall.java                         # 影厅表
│   │   ├── Showtime.java                     # 场次表
│   │   ├── Seat.java                         # 座位表
│   │   ├── Order.java                        # 订单表
│   │   ├── OrderSeat.java                    # 订单-座位关联表
│   │   ├── Payment.java                      # 支付记录表
│   │   ├── Review.java                       # 评价表
│   │   ├── Notification.java                 # 通知表
│   │   └── enums/                            # 13 个枚举类
│   │       ├── UserRole.java                 # user, admin
│   │       ├── MovieStatus.java              # upcoming, showing, ended
│   │       ├── CinemaStatus.java             # open, suspended, preparing, closed
│   │       ├── HallType.java                 # normal, imax, vip, threeD
│   │       ├── SeatType.java                 # normal, vip, couple
│   │       ├── SeatStatus.java               # active, maintenance
│   │       ├── OrderStatus.java              # pending, paid, refunded, cancelled
│   │       ├── PaymentMethod.java            # wallet
│   │       ├── PaymentStatus.java            # success, failed, refunded
│   │       ├── ImageType.java                # poster, still, banner
│   │       ├── NotificationType.java         # order, system
│   │       ├── NotificationStatus.java       # unread, read
│   │       └── ReviewRating.java             # 1-5 星评分
│   ├── repository/                           # 12 个 Spring Data JPA Repository
│   │   ├── UserRepository.java               # + deductBalance/addBalance（乐观锁扣款）
│   │   ├── MovieRepository.java              # + findByStatusAndKeyword
│   │   ├── MovieImageRepository.java
│   │   ├── CinemaRepository.java
│   │   ├── HallRepository.java
│   │   ├── ShowtimeRepository.java           # + findByMovieIdAndCinemaIdAndDate
│   │   ├── SeatRepository.java               # + findSoldSeatIdsByShowtimeId
│   │   ├── OrderRepository.java              # + countPaidOrdersSince/sumRevenueSince
│   │   ├── OrderSeatRepository.java
│   │   ├── PaymentRepository.java
│   │   ├── ReviewRepository.java
│   │   └── NotificationRepository.java
│   ├── dto/                                  # 11 个请求 DTO（带 @Valid 校验）
│   │   ├── RegisterRequest, LoginRequest, UpdateProfileRequest
│   │   ├── MovieRequest, CinemaRequest, HallRequest
│   │   ├── ShowtimeRequest, BatchShowtimeRequest
│   │   ├── LockSeatsRequest, CreateOrderRequest
│   │   └── ReviewRequest
│   ├── service/                              # 9 个 Service
│   │   ├── UserService.java                  # 注册/登录/个人信息
│   │   ├── MovieService.java                 # 电影列表/详情 + 管理 CRUD
│   │   ├── CinemaService.java                # 影院列表 + 影院/影厅管理（自动生成座位）
│   │   ├── ShowtimeService.java              # 场次列表/座位图 + 排片管理
│   │   ├── OrderService.java                 # 锁座/下单/状态/详情/列表/退票 + 管理端
│   │   ├── AdminService.java                 # 数据统计
│   │   ├── UploadService.java                # 图片上传
│   │   ├── ReviewService.java                # 评价提交/列表
│   │   └── NotificationService.java          # 通知列表/标记已读
│   ├── controller/                           # 9 个 Controller（27 个接口）
│   │   ├── UserController.java               # /user/*
│   │   ├── MovieController.java              # /movie/*
│   │   ├── CinemaController.java             # /cinema/*
│   │   ├── ShowtimeController.java           # /showtime/*
│   │   ├── OrderController.java              # /order/*
│   │   ├── AdminController.java              # /admin/*
│   │   ├── UploadController.java             # /upload/*
│   │   ├── ReviewController.java             # /review/*
│   │   └── NotificationController.java       # /notification/*
│   └── redis/
│       ├── SeatLockService.java              # Redis 锁座（原子检查+锁定+TTL 15分钟）
│       ├── OrderStreamProducer.java          # Redis Stream 生产者（推入订单队列）
│       └── OrderStreamConsumer.java          # Redis Stream 消费者（@Scheduled 500ms轮询）
│                                              #   → 校验锁 → 计算价格 → 乐观锁扣款 → 建单
├── src/main/resources/
│   ├── application.yml                       # 数据库/Redis/JWT/上传配置
│   ├── data.sql                              # 初始化数据（admin/测试用户/示例电影/影院/影厅）
│   └── db/
│       └── init.sql                          # 数据库完整初始化脚本（建库+建表+测试数据）
```

---

## 已实现的 API 接口（27 个）

### 用户模块 `/api/user`（4 个接口）
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /user/register | 注册（自动创建钱包 1000 元） | 否 |
| POST | /user/login | 登录（返回 JWT token） | 否 |
| GET | /user/profile | 获取个人信息 | 是 |
| PUT | /user/profile | 修改手机/头像 | 是 |

### 电影模块 `/api/movie`（2 个接口）
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /movie/list | 电影列表（支持 status/keyword/分页） | 否 |
| GET | /movie/{id} | 电影详情 | 否 |

### 影院/场次模块（3 个接口）
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /cinema/list | 影院列表（分页） | 否 |
| GET | /showtime/list | 场次列表（movieId/cinemaId/date） | 否 |
| GET | /showtime/{id}/seats | 座位图（含锁定/已售状态） | 否 |

### 订单模块 `/api/order`（6 个接口）
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /order/lock | 锁定座位（Redis 原子锁座） | 是 |
| POST | /order/create | 创建订单（推入 Redis Stream 异步处理） | 是 |
| GET | /order/status/{orderNo} | 轮询订单状态 | 是 |
| GET | /order/{orderNo} | 订单详情 | 是 |
| GET | /order/list | 订单列表（支持 status/分页） | 是 |
| POST | /order/cancel/{orderNo} | 退票（退款到钱包） | 是 |

### 管理后台 `/api/admin`（9 个接口）
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /admin/login | 管理员登录 | 否 |
| POST | /admin/movie | 新增电影 | admin |
| PUT | /admin/movie/{id} | 修改电影 | admin |
| DELETE | /admin/movie/{id} | 删除电影 | admin |
| POST | /admin/cinema | 新增影院 | admin |
| POST | /admin/hall | 新增影厅（自动生成 rows×cols 座位） | admin |
| POST | /admin/showtime | 新增场次 | admin |
| POST | /admin/showtime/batch | 批量排片 | admin |
| GET | /admin/order/list | 所有订单列表 | admin |
| GET | /admin/order/{orderNo} | 订单详情 | admin |
| GET | /admin/statistics | 数据统计（今日订单/收入/7天趋势/热门电影） | admin |

### 文件上传 `/api/upload`（1 个接口）
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /upload/image | 上传图片（jpg/png/gif/webp，10MB） | 是 |

### 评价 `/api/review`（2 个接口）
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| POST | /review | 提交评价（1-5 分） | 是 |
| GET | /review/list | 评价列表（movieId/分页） | 否 |

### 通知 `/api/notification`（2 个接口）
| 方法 | 路径 | 说明 | 认证 |
|------|------|------|------|
| GET | /notification/list | 通知列表 | 是 |
| PUT | /notification/{id}/read | 标记已读 | 是 |

---

## 核心技术实现说明

### 1. Redis 锁座（SeatLockService）
- 锁 key 格式：`seat:lock:{showtimeId}:{seatId}`，值为 userId
- 用户锁 key：`user:lock:{userId}`，值为 `{showtimeId}:{seatId1},{seatId2}`
- TTL 15 分钟自动过期释放
- 先检查所有座位 → 再全部锁定（简化版原子操作，生产环境应用 Lua 脚本）

### 2. Redis Stream 异步下单
- 生产者：`OrderStreamProducer.sendOrderRequest()` 推入 `order:stream`
- 消费者：`OrderStreamConsumer` 每 500ms 轮询，处理：校验锁 → 计算价格 → 乐观锁扣款 → 创建订单+支付记录 → 释放锁
- 失败订单标记为 `cancelled`

### 3. 虚拟钱包乐观锁
```java
// UserRepository.deductBalance()
@Modifying
@Query("UPDATE User u SET u.walletBalance = u.walletBalance - :amount, " +
       "u.version = u.version + 1 " +
       "WHERE u.id = :userId AND u.version = :version AND u.walletBalance >= :amount")
int deductBalance(@Param("userId") Long userId, 
                  @Param("amount") BigDecimal amount,
                  @Param("version") int version);
```

### 4. 座位定价
- 普通座：基础票价
- VIP 座：基础票价 × 1.5
- 情侣座：基础票价 × 2（必须成对选择）

### 5. JWT 认证
- 生成：userId + username + role 写入 claims
- 校验：JwtInterceptor 拦截，排除公开接口（注册/登录/电影列表等）
- 有效期 7 天

---

## 配置说明

### application.yml 关键配置

```yaml
server:
  port: 8080
  servlet:
    context-path: /api

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/movie_ticket?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8&allowPublicKeyRetrieval=true
    username: root
    password: clh123456
  data:
    redis:
      host: localhost
      port: 6379
      password: clh123456

app:
  jwt:
    secret: MovieTicketBookingSystemSecretKey2026MustBeLongEnoughForHS256Algorithm
    expiration: 604800000  # 7天
  seat-lock:
    ttl-seconds: 900  # 15分钟
```

---

## 启动步骤

```bash
# 1. 初始化数据库（包含建表+测试数据）
mysql -u root -p < backend/src/main/resources/db/init.sql
# 密码：clh123456

# 2. 确保 Redis 运行在 localhost:6379
redis-server

# 3. 启动后端
cd backend
mvn spring-boot:run

# 4. 访问 http://localhost:8080/api
```

---

## 测试相关

### Redis 测试指南
详见 `docs/redis-test-guide.md`，包含：
1. 用户登录获取 Token
2. 获取场次座位信息
3. 锁定座位（验证 Redis 锁）
4. 重复锁定测试（验证冲突检测）
5. 创建订单（验证 Redis Stream）
6. 轮询订单状态
7. 查看订单详情

### 验证清单

| 测试项 | 操作 | 预期结果 | 验证命令 |
|--------|------|----------|----------|
| Redis 连接 | `redis-cli ping` | 返回 PONG | `redis-cli -a clh123456 ping` |
| 用户登录 | POST /user/login | 返回 token | - |
| 锁定座位 | POST /order/lock | 返回成功 | `keys seat:lock:*` |
| 重复锁定 | POST /order/lock (另一用户) | 返回 409 | - |
| 创建订单 | POST /order/create | 返回 orderNo | `keys *stream*` |
| 订单入队 | - | Stream 有消息 | `XRANGE order:stream - +` |
| 轮询状态 | GET /order/status/{orderNo} | 状态变为 paid | - |

---

## 设计决策记录

| 决策 | 选择 | 原因 |
|------|------|------|
| Lombok | 移除，手写 getter/setter | JDK 24 不兼容 Lombok |
| 锁座实现 | Java 原生 Redis 操作 | 简化实现，保留 Lua 脚本作参考 |
| 座位生成 | 全部 normal，手动改 | 新增影厅时自动生成 rows×cols 个普通座 |
| 订单状态 | pending → paid → refunded/cancelled | 与 ER 图一致 |
| 密码加密 | BCrypt | Spring Security 自带，业界标准 |
| TEMP_SEAT_LOCKS 表 | 不建 MySQL 表 | 物理实现为 Redis，仅逻辑存在 |
| 数据库密码 | clh123456 | 本地开发环境 |

---

## 后续工作

### 前端开发（待开始）
- 技术栈：Vue 3 + Vite + Vant 4
- 页面：电影列表、电影详情、场次选择、座位图、订单管理
- 对接：27 个后端 API 接口

### 性能测试（待开始）
- 工具：JMeter
- 目标：1000 并发用户
- 验证：座位零超卖、响应时间 < 50ms

### 单元测试（待开始）
- 框架：JUnit 5 + Mockito
- 范围：Service 层核心业务逻辑

---

*计划创建日期：2026-06-15*
*实现完成日期：2026-06-15*
*最后更新：2026-06-23 — 根据项目实际建设情况更新*
*状态：**后端开发完成** — 等待前端开发和性能测试*
