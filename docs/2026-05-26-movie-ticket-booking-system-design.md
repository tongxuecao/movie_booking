# 电影票预订系统 — 软件工程设计流程

## 项目概述

本文档记录电影票预订系统的完整软件工程设计过程，遵循标准的软件开发生命周期（SDLC）。

### 已确认决策

| 决策项 | 结论 | 说明 |
|--------|------|------|
| **项目定位** | 课设/期末项目 | 目标：快速交付 + 技术亮点拿高分 |
| **项目周期** | 1 个月 | 4 周极限通关 |
| **前端方案** | Vue3 + Vite + Vant 4 | 浏览器端，先跑起来 |
| **后端技术栈** | Spring Boot 3 | 单体架构 |
| **数据库** | MySQL 8.0 | 本地安装 |
| **缓存** | Redis | 座位锁 + Lua 原子脚本（答辩核心亮点） |
| **异步削峰** | Redis Stream | 订单异步处理（技术亮点） |
| **支付方式** | 虚拟钱包 | 注册送 1000 元，完整交易闭环 |
| **部署** | 本地单机 | 零成本，单机伪集群 |
| **压测** | JMeter 1000 并发 | 压测报告截图作为答辩证据 |
| **角色分工** | 你负责后端 | 前端同学按 API 文档并行开发 |

---

## 第一阶段：问题定义与可行性分析

### 1.1 问题定义

明确系统要解决的核心问题：

- 用户需要在线浏览电影信息、选择场次、预订座位并完成支付
- 影院需要管理电影排片、座位库存和订单
- 系统需要处理并发预订，避免超卖

### 1.2 可行性分析

| 维度 | 分析内容 |
|------|----------|
| **技术可行性** | 前后端技术栈选型、第三方支付接口、座位锁机制 |
| **经济可行性** | 开发成本、运维成本、预期收益 |
| **操作可行性** | 用户使用习惯、操作界面友好度 |
| **时间可行性** | 项目周期、里程碑规划 |

**产出物：** 可行性分析报告

---

## 第二阶段：需求分析

### 2.1 功能需求

#### 用户端功能
- 用户注册与登录
- 浏览电影列表（正在上映、即将上映）
- 查看电影详情（简介、评分、演职员表）
- 选择影院、场次、座位
- 在线支付（模拟支付，后续可扩展支付宝/微信）
- 查看订单历史
- 退票/改签
- 评价电影

#### 管理端功能
- 电影信息管理（增删改查）
- 影院与影厅管理
- 排片管理（场次、时间、票价）
- 订单管理
- 用户管理
- 数据统计与报表

### 2.2 非功能需求

| 类别 | 要求 |
|------|------|
| **性能** | 页面加载 < 2秒，支持 1000+ 并发用户 |
| **可用性** | 系统可用性 99.9%，支持 7×24 小时 |
| **安全性** | HTTPS、支付加密、SQL注入防护、XSS防护 |
| **可扩展性** | 支持水平扩展，模块化架构 |
| **兼容性** | 支持主流浏览器（Chrome、Firefox、Safari、Edge） |

### 2.3 用例分析

**核心用例：**

```
UC01 - 用户注册
UC02 - 用户登录
UC03 - 浏览电影列表
UC04 - 查看电影详情
UC05 - 选择场次与座位
UC06 - 提交订单
UC07 - 在线支付
UC08 - 查看订单
UC09 - 退票申请
UC10 - 管理电影信息
UC11 - 管理排片
UC12 - 查看统计报表
```

**产出物：** 需求规格说明书（SRS）、用例图、用例描述文档

---

## 第三阶段：系统设计（概要设计）

### 3.1 系统架构设计

采用分层架构：

```
┌─────────────────────────────────────────┐
│              表现层 (Presentation)        │
│    Web前端 / 移动端 H5 / 管理后台         │
├─────────────────────────────────────────┤
│              接口层 (API Gateway)         │
│         RESTful API / WebSocket          │
├─────────────────────────────────────────┤
│              业务层 (Business Logic)      │
│  用户服务 │ 电影服务 │ 订单服务 │ 支付服务 │
├─────────────────────────────────────────┤
│              数据层 (Data Access)         │
│       MySQL 8.0 │ Redis                   │
├─────────────────────────────────────────┤
│              基础设施层 (Infrastructure)   │
│    服务器 │ 负载均衡 │ CDN │ 日志监控      │
└─────────────────────────────────────────┘
```

### 3.2 模块划分

| 模块 | 职责 | 关键技术 |
|------|------|----------|
| 用户模块 | 注册、登录、个人信息管理 | JWT、OAuth2 |
| 电影模块 | 电影信息展示、搜索 | 全文检索 |
| 影院模块 | 影院信息、排片管理 | 地理位置服务 |
| 订单模块 | 下单、锁座、订单状态管理 | 分布式锁、状态机 |
| 支付模块 | 支付对接、退款处理 | 第三方支付SDK |
| 管理模块 | 后台管理功能 | RBAC权限控制 |

### 3.3 接口设计

定义各模块间的接口契约：

```
API 示例：
GET    /api/movies              # 获取电影列表
GET    /api/movies/{id}         # 获取电影详情
GET    /api/cinemas             # 获取影院列表
GET    /api/showtimes/{movieId} # 获取场次信息
POST   /api/orders              # 创建订单
PUT    /api/orders/{id}/pay     # 支付订单
DELETE /api/orders/{id}         # 取消订单
```

### 3.4 数据库概要设计

**核心实体：**
- User（用户）
- Movie（电影）
- Cinema（影院）
- Hall（影厅）
- Showtime（场次）
- Seat（座位）
- Order（订单）
- Payment（支付记录）

**产出物：** 系统架构设计文档、接口文档、ER图

---

## 第四阶段：详细设计

### 4.1 数据库详细设计

#### 核心表结构（MySQL 8.0）

```sql
-- 用户表
CREATE TABLE users (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,
    phone       VARCHAR(20),
    email       VARCHAR(100),
    avatar      VARCHAR(255),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 电影表
CREATE TABLE movies (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    title       VARCHAR(200) NOT NULL,
    description TEXT,
    duration    INT COMMENT '时长(分钟)',
    release_date DATE,
    poster      VARCHAR(255),
    rating      DECIMAL(2,1),
    status      ENUM('upcoming', 'showing', 'ended'),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- 影院表
CREATE TABLE cinemas (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    name        VARCHAR(200) NOT NULL,
    address     VARCHAR(500),
    latitude    DECIMAL(10,7),
    longitude   DECIMAL(10,7),
    phone       VARCHAR(20)
);

-- 影厅表
CREATE TABLE halls (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    cinema_id   BIGINT NOT NULL,
    name        VARCHAR(50),
    seat_rows   INT,
    seat_cols   INT,
    hall_type   ENUM('normal', 'imax', '4dx'),
    FOREIGN KEY (cinema_id) REFERENCES cinemas(id)
);

-- 场次表
CREATE TABLE showtimes (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id    BIGINT NOT NULL,
    hall_id     BIGINT NOT NULL,
    show_date   DATE NOT NULL,
    show_time   TIME NOT NULL,
    price       DECIMAL(8,2) NOT NULL,
    status      ENUM('available', 'locked', 'sold_out'),
    FOREIGN KEY (movie_id) REFERENCES movies(id),
    FOREIGN KEY (hall_id) REFERENCES halls(id)
);

-- 座位表
CREATE TABLE seats (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    hall_id     BIGINT NOT NULL,
    row_num     INT NOT NULL,
    col_num     INT NOT NULL,
    seat_type   ENUM('normal', 'vip', 'couple'),
    status      ENUM('available', 'locked', 'sold'),
    FOREIGN KEY (hall_id) REFERENCES halls(id)
);

-- 订单表
CREATE TABLE orders (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(64) UNIQUE NOT NULL,
    user_id         BIGINT NOT NULL,
    showtime_id     BIGINT NOT NULL,
    seat_ids        JSON COMMENT '座位ID列表',
    total_amount    DECIMAL(10,2) NOT NULL,
    status          ENUM('pending', 'paid', 'cancelled', 'refunded'),
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    paid_at         DATETIME,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (showtime_id) REFERENCES showtimes(id)
);

-- 支付记录表
CREATE TABLE payments (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id        BIGINT NOT NULL,
    payment_method  ENUM('mock', 'alipay', 'wechat', 'card'),
    amount          DECIMAL(10,2) NOT NULL,
    transaction_id  VARCHAR(100),
    status          ENUM('pending', 'success', 'failed', 'refunded'),
    paid_at         DATETIME,
    FOREIGN KEY (order_id) REFERENCES orders(id)
);
```

### 4.2 核心流程详细设计

#### 订票流程（状态机）

```
[浏览电影] → [选择场次] → [选择座位] → [确认订单]
                                              │
                                    ┌─────────┴─────────┐
                                    ▼                   ▼
                              [锁定座位]           [座位已被占]
                              (15分钟超时)         返回重新选择
                                    │
                                    ▼
                              [发起支付]
                                    │
                        ┌───────────┼───────────┐
                        ▼           ▼           ▼
                    [支付成功]   [支付失败]   [支付超时]
                        │           │           │
                        ▼           ▼           ▼
                  [确认订单]   [释放座位]   [释放座位]
                  [出票完成]   [取消订单]   [取消订单]
```

#### 座位锁定机制

```
1. 用户选择座位 → Redis SETNX 加锁（TTL 15分钟）
2. 锁定成功 → 创建待支付订单
3. 支付成功 → 更新座位状态为 sold
4. 超时未支付 → 释放 Redis 锁，取消订单
5. 并发冲突 → 提示用户座位已被占用
```

### 4.3 接口详细设计

| 接口 | 方法 | 请求参数 | 响应 | 说明 |
|------|------|----------|------|------|
| /api/movies | GET | page, size, status | 电影列表 | 分页查询 |
| /api/movies/{id} | GET | - | 电影详情 | 含评分、演职员 |
| /api/showtimes | GET | movieId, cinemaId, date | 场次列表 | 按影院分组 |
| /api/seats/{showtimeId} | GET | - | 座位图 | 含锁定状态 |
| /api/orders | POST | showtimeId, seatIds | 订单信息 | 创建订单 |
| /api/orders/{id}/pay | POST | paymentMethod | 支付结果 | 发起支付 |
| /api/orders/{id} | DELETE | - | 取消结果 | 取消/退票 |

**产出物：** 数据库设计文档、接口文档（Swagger）、流程图、时序图

---

## 第五阶段：编码实现

### 5.1 技术栈选型（已确认）

| 层级 | 技术选型 | 说明 |
|------|----------|------|
| 前端 | Vue3 + Vite + Vant 4 | 用户端 + 管理后台 |
| 后端框架 | Spring Boot 3 | 单体 RESTful API |
| 数据库 | MySQL 8.0 | 主数据存储 |
| 缓存 + 锁座 | Redis + Lua 脚本 | 原子锁座，核心亮点 |
| 异步削峰 | Redis Stream | 订单异步处理，技术亮点 |
| 支付 | 虚拟钱包 | 完整交易闭环 |
| 部署 | 本地单机 | 零成本 |

#### Spring Boot 3 连接 MySQL 配置

```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/movie_ticket?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
    username: root
    password: your_password
    driver-class-name: com.mysql.cj.jdbc.Driver
  data:
    redis:
      host: localhost
      port: 6379
      database: 0
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: update
```

```xml
<!-- pom.xml 核心依赖 -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

### 5.2 开发规范

- 代码规范：遵循各语言官方编码规范
- 版本管理：Git Flow 分支策略
- 代码审查：每个 PR 至少一人 Review
- 提交规范：Conventional Commits

### 5.3 开发顺序（一个月极限通关计划）

```
Week 1（搭建）：Spring Boot 项目初始化 + MySQL 建表 + Redis 安装
                + 用户模块 + 电影/影院/场次 CRUD + 前端页面骨架

Week 2（核心）：Redis Lua 原子锁座 + 虚拟钱包 + 创建订单
                + 前端选座页面 + 订单页面

Week 3（亮点）：Redis Stream 异步下单 + 前端轮询订单状态
                + 管理后台 + 前端防抖

Week 4（交付）：JMeter 压测 1000 并发 + 截图零超卖证据
                + Bug 修复 + 答辩 PPT 准备
```

**产出物：** 源代码、单元测试、API文档

---

## 第六阶段：测试

### 6.1 测试策略

| 测试类型 | 范围 | 工具 |
|----------|------|------|
| 单元测试 | 各模块核心逻辑 | JUnit / Jest |
| 集成测试 | 模块间接口交互 | Postman / RestAssured |
| 系统测试 | 完整业务流程 | Selenium / Cypress |
| 性能测试 | 并发、响应时间 | JMeter / Locust |
| 安全测试 | SQL注入、XSS、支付安全 | OWASP ZAP |

### 6.2 关键测试场景

- 并发选座（同一座位多人同时选择）
- 支付超时与回滚
- 库存一致性（座位不超卖）
- 高并发起订（热门电影开售瞬间）

**产出物：** 测试计划、测试用例、测试报告、缺陷报告

---

## 第七阶段：部署与维护

### 7.1 部署架构

```
                    ┌──────────┐
                    │   CDN    │
                    └────┬─────┘
                         │
                    ┌────▼─────┐
                    │   Nginx  │  负载均衡 + 静态资源
                    └────┬─────┘
                         │
            ┌────────────┼────────────┐
            ▼            ▼            ▼
      ┌──────────┐ ┌──────────┐ ┌──────────┐
      │ App-1   │ │ App-2   │ │ App-3   │
      └────┬─────┘ └────┬─────┘ └────┬─────┘
           │            │            │
      ┌────▼────────────▼────────────▼─────┐
      │           MySQL 8.0                │
      │           Redis                    │
      └────────────────────────────────────┘
```

### 7.2 运维监控

- 应用监控：Prometheus + Grafana
- 日志管理：ELK Stack
- 告警机制：异常自动通知
- 备份策略：数据库每日全量 + 实时增量备份

**产出物：** 部署文档、运维手册、监控大盘

---

## 第八阶段：微信小程序扩展规划（第二个月）

### 8.1 迁移方案

采用 uni-app 将 Vue3 H5 版本迁移至微信小程序，后端 API 完全复用。

| 对比项 | Vue3 H5 | uni-app 微信小程序 |
|--------|---------|-------------------|
| 模板语法 | Vue3 SFC | 几乎相同 |
| 路由 | Vue Router | 页面文件即路由 |
| 网络请求 | axios | uni.request（封装后用法一致） |
| UI组件库 | Vant 4 | uni-ui / uview-plus |
| 状态管理 | Pinia | 直接可用 |

### 8.2 迁移步骤

1. 创建 uni-app Vue3 项目
2. 复用业务逻辑层（API封装、状态管理、工具函数）
3. 页面模板微调（`div→view`、`span→text`、`img→image`）
4. 替换 UI 组件库为小程序兼容版本
5. 处理小程序特有限制（无 DOM 操作、包体积限制 2MB）

### 8.3 微信小程序特有对接

| 功能 | 说明 |
|------|------|
| **登录** | wx.login 获取 code → 后端换 openid |
| **支付** | 必须走微信支付，需申请商户号 |
| **分包** | 主包限制 2MB，按功能分包加载 |
| **域名白名单** | 所有请求域名必须在小程序后台配置 |
| **审核** | 提交微信审核，通常 1-3 个工作日 |

### 8.4 时间规划

```
当前（Month 1）：Vue3 H5 版本开发上线
    ↓
Month 2：uni-app 迁移前端，后端 API 不变
    ↓
Month 3：提交微信审核，正式上线小程序
```

---

## 各阶段产出物汇总

| 阶段 | 主要产出物 |
|------|------------|
| 问题定义 | 可行性分析报告 |
| 需求分析 | 需求规格说明书（SRS）、用例图 |
| 概要设计 | 架构设计文档、接口文档、ER图 |
| 详细设计 | 数据库设计、流程图、时序图、API详细文档 |
| 编码实现 | 源代码、单元测试、API文档 |
| 测试 | 测试计划、测试用例、测试报告 |
| 部署维护 | 部署文档、运维手册、监控大盘 |

---

## 待讨论事项

### 已全部确认

核心决策已确认完毕，详见文档顶部"已确认决策"表。

### API 接口文档

完整的前后端接口规范见：`docs/api-spec.md`（27 个接口，前后端可并行开发）

---

*文档创建日期：2026-05-26*
*最后更新：2026-05-28*
*状态：设计完成 — 进入开发阶段*
