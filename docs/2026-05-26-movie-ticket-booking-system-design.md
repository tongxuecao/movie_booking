# 电影票预订系统 — 软件工程课程设计文档

> 本文档是电影票预订系统的完整软件工程设计文档，涵盖从需求分析到编码实现的全生命周期，可直接作为课程设计报告的参考框架。

---

## 项目信息

| 项目 | 说明 |
|------|------|
| **项目名称** | 电影票预订系统 (Movie Ticket Booking System) |
| **项目类型** | 软件工程课程设计 |
| **项目周期** | 2026年5月26日 - 2026年6月30日（5周） |
| **技术栈** | Vue 3 + Spring Boot 3.2.5 + MySQL 8.0 + Redis |
| **项目目标** | 完成可运行的电影票预订系统，具备高并发处理能力 |

---

## 第一章 项目概述与可行性分析

### 1.1 项目背景

随着互联网技术的发展，在线电影票预订已成为人们日常娱乐生活的重要组成部分。本项目旨在设计并实现一个功能完整的电影票预订系统，支持用户在线浏览电影、选择场次、预订座位并完成支付。

### 1.2 系统目标

1. **功能完整性**：实现用户注册登录、电影浏览、场次查询、在线选座、订单管理、虚拟支付等核心功能
2. **高并发处理**：采用 Redis Lua 脚本实现原子锁座，支持 1000+ 并发用户
3. **数据一致性**：通过乐观锁机制保证钱包余额并发操作的安全性
4. **用户体验**：响应式界面设计，页面加载时间 < 2秒

### 1.3 可行性分析

#### 1.3.1 技术可行性

| 技术领域 | 选型方案 | 可行性说明 |
|----------|----------|------------|
| 前端框架 | Vue 3 + Vant 4 | 成熟的移动端 UI 框架，组件丰富 |
| 后端框架 | Spring Boot 3.2.5 | 企业级 Java 框架，生态完善 |
| 数据库 | MySQL 8.0 | 开源关系型数据库，性能稳定 |
| 缓存中间件 | Redis | 内存数据库，支持 Lua 脚本原子操作 |
| 认证方案 | JWT | 无状态认证，适合前后端分离架构 |

#### 1.3.2 经济可行性

| 成本项 | 说明 |
|--------|------|
| 开发工具 | 全部使用开源免费工具（VS Code、IntelliJ IDEA Community） |
| 服务器 | 本地开发环境，零成本 |
| 第三方服务 | 虚拟钱包替代真实支付，无需企业资质 |

#### 1.3.3 操作可行性

- 用户界面采用移动端优先设计，符合用户使用习惯
- 管理后台提供直观的数据管理界面
- 系统部署简单，本地即可运行

---

## 第二章 需求分析

### 2.1 功能需求

#### 2.1.1 用户端功能

| 功能模块 | 功能点 | 优先级 |
|----------|--------|--------|
| 用户管理 | 注册、登录、个人信息修改 | P0 |
| 电影浏览 | 电影列表、电影详情、搜索 | P0 |
| 场次查询 | 按电影/影院/日期筛选场次 | P0 |
| 在线选座 | 座位图展示、座位选择、锁座 | P0 |
| 订单管理 | 创建订单、支付、查看订单、退票 | P0 |
| 电影评价 | 发表评价、查看评价 | P1 |
| 系统通知 | 订单状态通知、系统公告 | P2 |

#### 2.1.2 管理端功能

| 功能模块 | 功能点 | 优先级 |
|----------|--------|--------|
| 电影管理 | 增删改查电影信息 | P0 |
| 影院管理 | 影院、影厅管理 | P0 |
| 排片管理 | 创建场次、批量排片 | P0 |
| 订单管理 | 查看订单、订单详情 | P1 |
| 数据统计 | 今日统计、趋势分析、热门电影 | P2 |

### 2.2 非功能需求

| 需求类型 | 具体要求 |
|----------|----------|
| **性能** | 页面加载 < 2秒，API 响应 < 200ms |
| **并发** | 支持 1000+ 并发用户，座位零超卖 |
| **安全** | JWT 认证、SQL 注入防护、XSS 防护 |
| **可用性** | 系统可用性 99.9% |
| **兼容性** | 支持主流浏览器（Chrome、Firefox、Safari、Edge） |

### 2.3 用例分析

#### 2.3.1 核心用例列表

| 用例编号 | 用例名称 | 参与者 | 简要描述 |
|----------|----------|--------|----------|
| UC01 | 用户注册 | 普通用户 | 用户填写信息完成注册，系统自动创建虚拟钱包 |
| UC02 | 用户登录 | 普通用户/管理员 | 用户名密码登录，获取 JWT Token |
| UC03 | 浏览电影列表 | 普通用户 | 查看正在上映、即将上映的电影 |
| UC04 | 查看电影详情 | 普通用户 | 查看电影简介、评分、演职员表 |
| UC05 | 选择场次与座位 | 普通用户 | 选择影院、场次，查看座位图并选座 |
| UC06 | 提交订单 | 普通用户 | 确认选座，提交订单到异步队列 |
| UC07 | 在线支付 | 普通用户 | 使用虚拟钱包完成支付 |
| UC08 | 查看订单 | 普通用户 | 查看订单列表和订单详情 |
| UC09 | 退票申请 | 普通用户 | 申请退票，系统退款至钱包 |
| UC10 | 管理电影信息 | 管理员 | 增删改查电影信息 |
| UC11 | 管理排片 | 管理员 | 创建场次、批量排片 |
| UC12 | 查看统计报表 | 管理员 | 查看今日统计、趋势分析 |

#### 2.3.2 用例图

```
                          ┌─────────────────────────────────────┐
                          │          电影票预订系统              │
                          │                                     │
┌─────────┐               │  ┌─────────┐  ┌─────────┐         │
│         │──────────────▶│  │ 用户注册 │  │ 用户登录 │         │
│         │──────────────▶│  └─────────┘  └─────────┘         │
│         │               │                                     │
│         │──────────────▶│  ┌─────────┐  ┌─────────┐         │
│  普通   │               │  │浏览电影 │  │电影详情 │         │
│  用户   │──────────────▶│  └─────────┘  └─────────┘         │
│         │               │                                     │
│         │──────────────▶│  ┌─────────┐  ┌─────────┐         │
│         │               │  │选择场次 │  │在线选座 │         │
│         │──────────────▶│  └─────────┘  └─────────┘         │
│         │               │                                     │
│         │──────────────▶│  ┌─────────┐  ┌─────────┐         │
│         │               │  │提交订单 │  │在线支付 │         │
│         │──────────────▶│  └─────────┘  └─────────┘         │
│         │               │                                     │
└─────────┘               │  ┌─────────┐  ┌─────────┐         │
                          │  │查看订单 │  │退票申请 │         │
                          │  └─────────┘  └─────────┘         │
                          │                                     │
┌─────────┐               │  ┌─────────┐  ┌─────────┐         │
│         │──────────────▶│  │电影管理 │  │影院管理 │         │
│  管理员  │──────────────▶│  └─────────┘  └─────────┘         │
│         │               │                                     │
│         │──────────────▶│  ┌─────────┐  ┌─────────┐         │
│         │               │  │排片管理 │  │数据统计 │         │
└─────────┘               │  └─────────┘  └─────────┘         │
                          │                                     │
                          └─────────────────────────────────────┘
```

---

## 第三章 系统设计

### 3.1 系统架构设计

#### 3.1.1 整体架构

采用前后端分离的单体架构：

```
┌─────────────────────────────────────────────────────────────┐
│                        前端层                                │
│              Vue 3 + Vite + Vant 4 (移动端H5)                │
└─────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP/REST API
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                       后端层                                 │
│                 Spring Boot 3.2.5 (单体)                     │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Controller → Service → Repository → Database        │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Redis: Lua脚本(锁座) + Stream(异步订单)              │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                              │
            ┌─────────────────┼─────────────────┐
            ▼                 ▼                 ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│   MySQL 8.0   │   │    Redis      │   │   文件存储    │
│   (持久化)    │   │   (缓存+锁)   │   │  (上传文件)   │
└───────────────┘   └───────────────┘   └───────────────┘
```

#### 3.1.2 后端分层架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Controller 层                             │
│    UserController, MovieController, OrderController...      │
│    (接收请求，参数校验，返回响应)                            │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     Service 层                               │
│    UserService, MovieService, OrderService...               │
│    (业务逻辑处理，事务管理)                                  │
└─────────────────────────────────────────────────────────────┘
                              │
            ┌─────────────────┼─────────────────┐
            ▼                 ▼                 ▼
┌───────────────┐   ┌───────────────┐   ┌───────────────┐
│  Repository   │   │  Redis        │   │  其他服务     │
│  (数据访问)   │   │  (缓存/锁)    │   │  (文件上传)   │
└───────────────┘   └───────────────┘   └───────────────┘
```

### 3.2 模块划分

| 模块 | 对应包名 | 职责 | 核心类 |
|------|----------|------|--------|
| 用户模块 | controller.user, service | 注册、登录、个人信息管理 | UserController, UserService |
| 电影模块 | controller.movie, service | 电影信息展示、搜索 | MovieController, MovieService |
| 影院模块 | controller.cinema, service | 影院信息、影厅管理 | CinemaController, CinemaService |
| 场次模块 | controller.showtime, service | 排片查询、座位图 | ShowtimeController, ShowtimeService |
| 订单模块 | controller.order, service | 锁座、下单、支付、退票 | OrderController, OrderService |
| 管理模块 | controller.admin, service | 后台管理功能 | AdminController, AdminService |
| 上传模块 | controller.upload, service | 文件上传 | UploadController, UploadService |
| 评价模块 | controller.review, service | 电影评价 | ReviewController, ReviewService |
| 通知模块 | controller.notification, service | 系统通知 | NotificationController, NotificationService |

### 3.3 接口设计

#### 3.3.1 API 规范

| 项目 | 值 |
|------|-----|
| Base URL | `http://localhost:8080/api` |
| 数据格式 | JSON |
| 字符编码 | UTF-8 |
| 认证方式 | JWT Token (`Authorization: Bearer {token}`) |

#### 3.3.2 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

#### 3.3.3 核心接口列表

| 模块 | 接口 | 方法 | 说明 |
|------|------|------|------|
| 用户 | /api/user/register | POST | 用户注册 |
| 用户 | /api/user/login | POST | 用户登录 |
| 用户 | /api/user/profile | GET | 获取用户信息 |
| 电影 | /api/movie/list | GET | 电影列表 |
| 电影 | /api/movie/{id} | GET | 电影详情 |
| 影院 | /api/cinema/list | GET | 影院列表 |
| 场次 | /api/showtime/list | GET | 场次列表 |
| 场次 | /api/showtime/{id}/seats | GET | 座位图 |
| 订单 | /api/order/lock | POST | 锁定座位 |
| 订单 | /api/order/create | POST | 创建订单 |
| 订单 | /api/order/status/{orderNo} | GET | 订单状态 |
| 订单 | /api/order/{orderNo} | GET | 订单详情 |
| 订单 | /api/order/cancel/{orderNo} | POST | 取消订单 |
| 管理 | /api/admin/movie | POST | 新增电影 |
| 管理 | /api/admin/showtime | POST | 新增场次 |
| 管理 | /api/admin/statistics | GET | 数据统计 |

> 完整接口文档见 `docs/api-spec.md`（27个接口）

---

## 第四章 数据库设计

### 4.1 概念结构设计（E-R图）

#### 4.1.1 实体关系图

```
                              ┌─────────────┐
                              │   CINEMAS   │
                              │   影院表     │
                              ├─────────────┤
                              │ id (PK)     │
                              │ name        │
                              │ address     │
                              │ phone       │
                              │ status      │
                              └──────┬──────┘
                                     │
                                     │ 1:N
                                     ▼
┌─────────────┐              ┌─────────────┐              ┌─────────────┐
│   USERS     │              │   HALLS     │              │   MOVIES    │
│   用户表     │              │   影厅表     │              │   电影表     │
├─────────────┤              ├─────────────┤              ├─────────────┤
│ id (PK)     │              │ id (PK)     │              │ id (PK)     │
│ username    │              │ cinema_id   │              │ title       │
│ password    │              │ name        │              │ duration    │
│ phone       │              │ seat_rows   │              │ release_date│
│ role        │              │ seat_cols   │              │ rating      │
│ wallet_bal  │              │ hall_type   │              │ description │
│ version     │              └──────┬──────┘              │ status      │
└──────┬──────┘                     │                     └──────┬──────┘
       │                            │                            │
       │                            │ 1:N                        │ 1:N
       │                            ▼                            ▼
       │                     ┌─────────────┐              ┌─────────────┐
       │                     │   SEATS     │              │  SHOWTIMES  │
       │                     │   座位表     │              │  场次表      │
       │                     ├─────────────┤              ├─────────────┤
       │                     │ id (PK)     │              │ id (PK)     │
       │                     │ hall_id     │              │ movie_id    │
       │                     │ row_num     │              │ hall_id     │
       │                     │ col_num     │              │ show_date   │
       │                     │ seat_type   │              │ show_time   │
       │                     │ status      │              │ price       │
       │                     └──────┬──────┘              └──────┬──────┘
       │                            │                            │
       │ 1:N                        │ 1:N                        │ 1:N
       ▼                            ▼                            ▼
┌─────────────┐              ┌─────────────┐              ┌─────────────┐
│   ORDERS    │              │ ORDER_SEATS │              │  PAYMENTS   │
│   订单表     │              │ 订单座位关联  │              │  支付记录    │
├─────────────┤              ├─────────────┤              ├─────────────┤
│ id (PK)     │◀─────────────│ order_id    │              │ id (PK)     │
│ order_no    │              │ seat_id     │              │ order_id    │
│ user_id     │              │ price       │              │ amount      │
│ showtime_id │              └─────────────┘              │ status      │
│ total_amount│                                          └─────────────┘
│ status      │
└─────────────┘
```

#### 4.1.2 实体关系汇总

| 关系 | 类型 | 说明 |
|------|------|------|
| CINEMAS → HALLS | 1:N | 一个影院拥有多个影厅 |
| HALLS → SEATS | 1:N | 一个影厅包含多个座位 |
| MOVIES → SHOWTIMES | 1:N | 一部电影可以有多个排片场次 |
| HALLS → SHOWTIMES | 1:N | 一个影厅可以放映多个场次 |
| USERS → ORDERS | 1:N | 一个用户可以创建多个订单 |
| SHOWTIMES → ORDERS | 1:N | 一个场次可以产生多个订单 |
| ORDERS → ORDER_SEATS | 1:N | 一个订单包含多个座位 |
| SEATS → ORDER_SEATS | 1:N | 一个座位可以出现在多个历史订单中 |
| ORDERS → PAYMENTS | 1:N | 一个订单可以有多条支付记录 |

### 4.2 逻辑结构设计（关系模式）

#### 4.2.1 核心表结构

**用户表 (users)**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 用户唯一标识 |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 用户名 |
| password | VARCHAR(255) | NOT NULL | 密码(BCrypt加密) |
| phone | VARCHAR(20) | | 手机号 |
| role | ENUM | DEFAULT 'user' | 角色: user/admin |
| wallet_balance | DECIMAL(10,2) | DEFAULT 1000.00 | 虚拟钱包余额 |
| version | INT | DEFAULT 0 | 乐观锁版本号 |
| created_at | DATETIME | DEFAULT NOW() | 创建时间 |
| updated_at | DATETIME | ON UPDATE NOW() | 更新时间 |

**电影表 (movies)**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 电影唯一标识 |
| title | VARCHAR(200) | NOT NULL | 电影名称 |
| duration | INT | | 时长(分钟) |
| release_date | DATE | | 上映日期 |
| rating | DECIMAL(2,1) | | 评分 |
| description | TEXT | | 简介 |
| status | ENUM | | 状态: upcoming/showing/ended |
| created_at | DATETIME | DEFAULT NOW() | 创建时间 |
| updated_at | DATETIME | ON UPDATE NOW() | 更新时间 |

**场次表 (showtimes)**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 场次唯一标识 |
| movie_id | BIGINT | FK → movies.id | 电影ID |
| hall_id | BIGINT | FK → halls.id | 影厅ID |
| show_date | DATE | NOT NULL | 放映日期 |
| show_time | TIME | NOT NULL | 放映时间 |
| price | DECIMAL(8,2) | NOT NULL | 基础票价 |
| status | ENUM | DEFAULT 'normal' | 状态: normal/cancelled |
| created_at | DATETIME | DEFAULT NOW() | 创建时间 |

**订单表 (orders)**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 订单唯一标识 |
| order_no | VARCHAR(64) | UNIQUE, NOT NULL | 订单号 |
| user_id | BIGINT | FK → users.id | 用户ID |
| showtime_id | BIGINT | FK → showtimes.id | 场次ID |
| total_amount | DECIMAL(10,2) | NOT NULL | 订单总金额 |
| status | ENUM | | 状态: pending/paid/refunded/cancelled |
| created_at | DATETIME | DEFAULT NOW() | 创建时间 |
| updated_at | DATETIME | ON UPDATE NOW() | 更新时间 |

**订单座位关联表 (order_seats)**

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGINT | PK, AUTO_INCREMENT | 记录唯一标识 |
| order_id | BIGINT | FK → orders.id | 订单ID |
| seat_id | BIGINT | FK → seats.id | 座位ID |
| price | DECIMAL(8,2) | | 成交价格(含类型上浮) |

### 4.3 物理结构设计

#### 4.3.1 数据库配置

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/movie_ticket?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=UTF-8
    username: root
    password: clh123456
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update  # 自动同步实体到数据库
    show-sql: false
```

#### 4.3.2 索引设计

| 表名 | 索引字段 | 索引类型 | 说明 |
|------|----------|----------|------|
| users | username | UNIQUE | 登录查询 |
| orders | order_no | UNIQUE | 订单号查询 |
| orders | user_id | INDEX | 用户订单查询 |
| orders | showtime_id | INDEX | 场次订单查询 |
| showtimes | movie_id, show_date | INDEX | 电影场次查询 |
| seats | hall_id | INDEX | 影厅座位查询 |

---

## 第五章 详细设计

### 5.1 核心流程设计

#### 5.1.1 选座购票流程

```
┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐
│  浏览电影 │───▶│ 选择场次 │───▶│ 选择座位 │───▶│ 确认订单 │
└──────────┘    └──────────┘    └──────────┘    └──────────┘
                                                      │
                                                      ▼
                                              ┌──────────────┐
                                              │ Redis Lua锁座│
                                              │ (原子操作)   │
                                              └──────┬───────┘
                                                     │
                                    ┌────────────────┴────────────────┐
                                    ▼                                 ▼
                            ┌──────────────┐                 ┌──────────────┐
                            │   锁座成功   │                 │   锁座失败   │
                            └──────┬───────┘                 │  (座位已被占)│
                                   │                         └──────────────┘
                                   ▼
                            ┌──────────────┐
                            │ Redis Stream │
                            │ 异步处理订单 │
                            └──────┬───────┘
                                   │
                                   ▼
                            ┌──────────────┐
                            │ 扣减钱包余额 │
                            │ (乐观锁)     │
                            └──────┬───────┘
                                   │
                                   ▼
                            ┌──────────────┐
                            │ 生成订单记录 │
                            │ 更新座位状态 │
                            └──────┬───────┘
                                   │
                                   ▼
                            ┌──────────────┐
                            │ 前端轮询状态 │
                            │ 显示支付结果 │
                            └──────────────┘
```

#### 5.1.2 座位锁定机制（Redis Lua脚本）

```lua
-- Redis Lua 脚本：原子锁座
-- KEYS[1]: 座位锁 key (seat:lock:{showtimeId}:{seatId})
-- KEYS[2]: 用户锁 key (user:lock:{userId})
-- ARGV[1]: 用户ID
-- ARGV[2]: 过期时间(秒)

-- 检查座位是否已被锁定
local seatLock = redis.call('GET', KEYS[1])
if seatLock then
    -- 座位已被锁定，返回冲突
    return -1
end

-- 检查用户是否已有未完成的锁
local userLock = redis.call('GET', KEYS[2])
if userLock then
    -- 用户已有锁，返回冲突
    return -2
end

-- 执行锁定
redis.call('SETEX', KEYS[1], ARGV[2], ARGV[1])
redis.call('SETEX', KEYS[2], ARGV[2], KEYS[1])

return 1  -- 锁定成功
```

#### 5.1.3 订单异步处理（Redis Stream）

```
生产者 (OrderStreamProducer)          消费者 (OrderStreamConsumer)
        │                                       │
        │  XADD order:stream *                  │
        │  {orderData}                          │
        │──────────────────────────────────────▶│
        │                                       │
        │                                       │  处理订单:
        │                                       │  1. 校验lockToken
        │                                       │  2. 扣减余额(乐观锁)
        │                                       │  3. 创建订单记录
        │                                       │  4. 更新座位状态
        │                                       │  5. 释放Redis锁
        │                                       │
        │◀──────────────────────────────────────│
        │  订单状态更新                          │
```

### 5.2 关键算法设计

#### 5.2.1 钱包余额乐观锁

```java
@Transactional
public void deductBalance(Long userId, BigDecimal amount) {
    // 1. 查询当前版本号
    User user = userRepository.findById(userId).orElseThrow();
    int currentVersion = user.getVersion();
    
    // 2. 带版本号更新
    int affected = userRepository.deductBalance(
        userId, amount, currentVersion
    );
    
    // 3. 判断更新结果
    if (affected == 0) {
        throw new BusinessException("余额扣减失败，请重试");
    }
}

// Repository 方法
@Modifying
@Query("UPDATE User u SET u.walletBalance = u.walletBalance - :amount, " +
       "u.version = u.version + 1 " +
       "WHERE u.id = :userId AND u.version = :version AND u.walletBalance >= :amount")
int deductBalance(@Param("userId") Long userId, 
                  @Param("amount") BigDecimal amount,
                  @Param("version") int version);
```

#### 5.2.2 订单号生成算法

```java
public class OrderNoGenerator {
    private static final AtomicLong SEQUENCE = new AtomicLong(0);
    
    public static String generate() {
        String date = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        long seq = SEQUENCE.incrementAndGet() % 100000;
        return String.format("ORD%s%05d", date, seq);
    }
}
```

### 5.3 数据流图

#### 5.3.1 顶层数据流图（Context Diagram）

```
                          ┌─────────────────────┐
                          │    电影票预订系统     │
     ┌──────────┐         │                      │         ┌──────────┐
     │          │────────▶│                      │────────▶│          │
     │  普通用户 │  注册信息│                      │操作反馈  │  管理员   │
     │          │  登录请求│                      │         │          │
     │          │  订票申请│                      │◀────────│          │
     │          │◀────────│                      │管理指令  │          │
     └──────────┘ 认证结果 │                      │         └──────────┘
                  电影列表 │                      │
                  座位图   │                      │
                  订单结果 │                      │
                          └─────────────────────┘
```

#### 5.3.2 订单处理数据流图

```
用户 ──▶ [获取座位图] ──▶ [锁定座位] ──▶ [提交订单] ──▶ [异步处理] ──▶ [查询状态]
              │               │              │              │              │
              ▼               ▼              ▼              ▼              ▼
         座位数据         临时锁座        待处理队列      订单数据       订单状态
         (MySQL)         (Redis)        (Redis Stream)   (MySQL)       (MySQL)
```

---

## 第六章 编码实现

### 6.1 项目结构

```
backend/
├── src/main/java/com/moviebooking/
│   ├── MovieBookingApplication.java    # 启动类
│   ├── controller/                     # 控制器层 (9个)
│   │   ├── UserController.java
│   │   ├── MovieController.java
│   │   ├── CinemaController.java
│   │   ├── ShowtimeController.java
│   │   ├── OrderController.java
│   │   ├── AdminController.java
│   │   ├── UploadController.java
│   │   ├── ReviewController.java
│   │   └── NotificationController.java
│   ├── service/                        # 服务层 (9个)
│   │   ├── UserService.java
│   │   ├── MovieService.java
│   │   ├── CinemaService.java
│   │   ├── ShowtimeService.java
│   │   ├── OrderService.java
│   │   ├── AdminService.java
│   │   ├── UploadService.java
│   │   ├── ReviewService.java
│   │   └── NotificationService.java
│   ├── repository/                     # 数据访问层 (12个)
│   │   ├── UserRepository.java
│   │   ├── MovieRepository.java
│   │   ├── MovieImageRepository.java
│   │   ├── CinemaRepository.java
│   │   ├── HallRepository.java
│   │   ├── ShowtimeRepository.java
│   │   ├── SeatRepository.java
│   │   ├── OrderRepository.java
│   │   ├── OrderSeatRepository.java
│   │   ├── PaymentRepository.java
│   │   ├── ReviewRepository.java
│   │   └── NotificationRepository.java
│   ├── entity/                         # 实体类 (11个 + 13个枚举)
│   │   ├── User.java
│   │   ├── Movie.java
│   │   ├── MovieImage.java
│   │   ├── Cinema.java
│   │   ├── Hall.java
│   │   ├── Showtime.java
│   │   ├── Seat.java
│   │   ├── Order.java
│   │   ├── OrderSeat.java
│   │   ├── Payment.java
│   │   ├── Review.java
│   │   ├── Notification.java
│   │   └── enums/                      # 枚举类
│   ├── dto/                            # 数据传输对象 (11个)
│   ├── config/                         # 配置类
│   │   ├── JwtInterceptor.java
│   │   ├── WebConfig.java
│   │   └── RedisConfig.java
│   ├── redis/                          # Redis相关
│   │   ├── SeatLockService.java
│   │   ├── OrderStreamProducer.java
│   │   └── OrderStreamConsumer.java
│   ├── common/                         # 通用类
│   │   ├── ApiResult.java
│   │   ├── PageResult.java
│   │   ├── BusinessException.java
│   │   └── GlobalExceptionHandler.java
│   └── util/                           # 工具类
│       ├── JwtUtil.java
│       └── OrderNoGenerator.java
└── src/main/resources/
    ├── application.yml                 # 配置文件
    ├── data.sql                        # 初始数据
    └── db/init.sql                     # 数据库初始化
```

### 6.2 核心代码实现

#### 6.2.1 座位锁定服务

```java
@Service
public class SeatLockService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    // Lua脚本：原子锁座
    private static final String LOCK_SEAT_LUA = 
        "local seatLock = redis.call('GET', KEYS[1]) " +
        "if seatLock then return -1 end " +
        "local userLock = redis.call('GET', KEYS[2]) " +
        "if userLock then return -2 end " +
        "redis.call('SETEX', KEYS[1], ARGV[2], ARGV[1]) " +
        "redis.call('SETEX', KEYS[2], ARGV[2], KEYS[1]) " +
        "return 1";
    
    public boolean lockSeats(Long showtimeId, List<Long> seatIds, Long userId) {
        // 批量锁定座位
        for (Long seatId : seatIds) {
            String seatKey = "seat:lock:" + showtimeId + ":" + seatId;
            String userKey = "user:lock:" + userId;
            
            Long result = redisTemplate.execute(
                new DefaultRedisScript<>(LOCK_SEAT_LUA, Long.class),
                List.of(seatKey, userKey),
                userId.toString(),
                "900"  // 15分钟过期
            );
            
            if (result == null || result < 0) {
                return false;  // 锁定失败
            }
        }
        return true;
    }
}
```

#### 6.2.2 订单服务

```java
@Service
public class OrderService {
    
    @Autowired
    private SeatLockService seatLockService;
    
    @Autowired
    private OrderStreamProducer streamProducer;
    
    public String createOrder(CreateOrderRequest request, Long userId) {
        // 1. 校验座位是否已锁定
        // 2. 生成订单号
        String orderNo = OrderNoGenerator.generate();
        
        // 3. 发送到Redis Stream异步处理
        streamProducer.sendOrder(orderNo, userId, request);
        
        return orderNo;
    }
}
```

### 6.3 配置说明

| 配置项 | 文件 | 值 | 说明 |
|--------|------|-----|------|
| 数据库连接 | application.yml | movie_ticket, root/clh123456 | MySQL配置 |
| Redis连接 | application.yml | localhost:6379, password:clh123456 | Redis配置 |
| JWT密钥 | application.yml | MovieTicketBookingSystemSecret... | JWT签名密钥 |
| JWT过期 | application.yml | 604800000 (7天) | Token有效期 |
| 座位锁TTL | application.yml | 900秒 (15分钟) | 锁座超时时间 |
| 文件上传 | application.yml | 10MB | 最大上传大小 |

---

## 第七章 测试

### 7.1 测试策略

| 测试类型 | 范围 | 工具 | 状态 |
|----------|------|------|------|
| 单元测试 | Service层业务逻辑 | JUnit 5 | 待完成 |
| 集成测试 | API接口测试 | Postman | 已完成 |
| 功能测试 | 完整业务流程 | 手动测试 | 已完成 |
| 性能测试 | 并发、响应时间 | JMeter | 待完成 |

### 7.2 测试用例

#### 7.2.1 用户注册测试

| 测试项 | 输入 | 预期结果 | 实际结果 |
|--------|------|----------|----------|
| 正常注册 | username=test, password=123456 | 注册成功，返回用户信息 | ✓ |
| 重复用户名 | 已存在的用户名 | 返回400，用户名已存在 | ✓ |
| 缺少必填项 | 空用户名 | 返回400，参数错误 | ✓ |

#### 7.2.2 座位锁定测试

| 测试项 | 操作 | 预期结果 | 实际结果 |
|--------|------|----------|----------|
| 正常锁座 | 选择可用座位 | 返回200，锁定成功 | ✓ |
| 重复锁座 | 其他用户锁定同一座位 | 返回409，座位已被占用 | ✓ |
| 超时释放 | 等待15分钟 | 座位自动释放 | ✓ |

#### 7.2.3 并发测试场景

| 场景 | 并发数 | 预期结果 | 验证方法 |
|------|--------|----------|----------|
| 同一座位并发抢票 | 100 | 只有1人成功，99人失败 | 检查订单数量 |
| 同一用户并发支付 | 50 | 只扣一次款 | 检查余额变化 |
| 高并发起订 | 1000 | 系统正常响应 | JMeter报告 |

### 7.3 Redis测试指南

> 详见 `docs/redis-test-guide.md`

测试步骤：
1. 用户登录获取Token
2. 获取场次座位信息
3. 锁定座位（验证Redis锁）
4. 重复锁定测试（验证冲突检测）
5. 创建订单（验证Redis Stream）
6. 轮询订单状态
7. 查看订单详情

---

## 第八章 部署与维护

### 8.1 环境要求

| 组件 | 版本要求 | 说明 |
|------|----------|------|
| JDK | 17+ | Spring Boot 3.x 要求 |
| Maven | 3.8+ | 项目构建 |
| MySQL | 8.0+ | 数据库 |
| Redis | 6.0+ | 缓存中间件 |
| Node.js | 16+ | 前端构建 |

### 8.2 部署步骤

#### 8.2.1 后端部署

```bash
# 1. 初始化数据库
mysql -u root -p < backend/src/main/resources/db/init.sql

# 2. 启动Redis
redis-server

# 3. 启动后端
cd backend
mvn spring-boot:run
```

#### 8.2.2 前端部署

```bash
# 1. 安装依赖
cd frontend
npm install

# 2. 启动开发服务器
npm run dev

# 3. 构建生产版本
npm run build
```

### 8.3 监控与日志

| 监控项 | 工具 | 说明 |
|--------|------|------|
| 应用日志 | Logback | 控制台 + 文件输出 |
| Redis监控 | redis-cli monitor | 实时命令监控 |
| 数据库监控 | MySQL Workbench | 查询性能分析 |

---

## 第九章 项目总结

### 9.1 技术亮点

1. **Redis Lua原子锁座**：通过Lua脚本实现座位锁定的原子性，避免并发冲突
2. **Redis Stream异步处理**：订单处理采用消息队列模式，实现流量削峰
3. **乐观锁机制**：钱包余额操作采用版本号控制，防止并发超扣
4. **前后端分离**：RESTful API设计，支持前后端并行开发

### 9.2 项目成果

| 指标 | 目标 | 实际 |
|------|------|------|
| 后端代码量 | - | 78个Java文件 |
| API接口数 | - | 27个RESTful接口 |
| 数据库表 | - | 12张业务表 |
| 并发支持 | 1000+ | 待压测验证 |
| 座位超卖 | 0 | 通过Lua脚本保证 |

### 9.3 后续优化方向

1. **前端开发**：完成Vue 3 + Vant 4移动端界面
2. **性能压测**：使用JMeter进行1000并发压测
3. **单元测试**：补充Service层单元测试
4. **安全加固**：接口限流、参数校验增强
5. **监控告警**：集成Prometheus + Grafana

---

## 附录

### A. 团队分工建议

| 角色 | 职责 | 工作内容 |
|------|------|----------|
| 后端开发 | API实现 | Controller、Service、Repository层开发 |
| 前端开发 | 页面实现 | Vue组件、页面路由、API对接 |
| 测试 | 质量保证 | 测试用例编写、功能测试、性能测试 |
| 文档 | 项目文档 | 需求文档、设计文档、用户手册 |

### B. 开发进度表

| 阶段 | 时间 | 任务 | 状态 |
|------|------|------|------|
| 需求分析 | 第1周 | 需求调研、用例分析 | ✅ 已完成 |
| 系统设计 | 第1周 | 架构设计、数据库设计 | ✅ 已完成 |
| 后端开发 | 第2-3周 | API实现、业务逻辑 | ✅ 已完成 |
| 前端开发 | 第3-4周 | 页面开发、接口对接 |   进行中 |
| 测试 | 第4周 | 功能测试、性能测试 |   待开始 |
| 部署 | 第5周 | 系统部署、文档完善 |   待开始 |

### C. 参考文献

1. Spring Boot官方文档: https://spring.io/projects/spring-boot
2. Redis官方文档: https://redis.io/documentation
3. Vue.js官方文档: https://vuejs.org/
4. MySQL 8.0参考手册: https://dev.mysql.com/doc/refman/8.0/en/

---

*文档创建日期：2026-05-26*
*最后更新：2026-06-23*
*版本：v2.0*
*状态：后端开发完成，前端开发进行中*
