# 电影票预订系统

Spring Boot 3 + Vue 3 全栈项目，支持在线选座、虚拟钱包支付、管理后台。

## 技术栈

| 层 | 技术 |
|----|------|
| 前端 | Vue 3 + Vite + Element Plus + Pinia |
| 后端 | Spring Boot 3.2.5 + JPA/Hibernate |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis（锁座 + 订单队列） |
| 认证 | JWT（7 天过期） |

## 环境准备

### 需要安装的软件

| 软件 | 版本要求 | 下载地址 | 说明 |
|------|----------|----------|------|
| JDK | 17+ | [Adoptium](https://adoptium.net/) | Java 开发运行环境 |
| Maven | 3.8+ | [maven.apache.org](https://maven.apache.org/) | 后端构建工具（或使用 IDE 内置 Maven） |
| MySQL | 8.0+ | [mysql.com](https://dev.mysql.com/downloads/) | 数据库，需创建 `movie_ticket` 库 |
| Redis | 6.0+ | [redis.io](https://redis.io/download/) | 缓存服务，用于锁座和订单队列 |
| Node.js | 18+ | [nodejs.org](https://nodejs.org/) | 前端运行环境（含 npm） |

### 数据库初始化

1. 启动 MySQL 服务
2. 创建数据库：

```sql
CREATE DATABASE IF NOT EXISTS movie_ticket DEFAULT CHARSET utf8mb4;
```

3. 导入建表和数据脚本（**必须**）：

```bash
mysql -u root -p movie_ticket < CDM_PDM/init.sql
```
先创建一个名为movie_ticket得数据库，将CDM_PDM/init.sql文件中的SQL代码复制，
在MySQLmovie_ticket数据库中新建查询表,粘贴运行

> 该 SQL 文件包含完整的表结构 + 初始数据（影院、影厅、座位、电影、管理员账号等）。后端 JPA 设为了 `ddl-auto: update` 会自动建表，但初始数据必须手动导入。

4. 如需测试数据，可额外导入：

```bash
mysql -u root -p movie_ticket < 测试/test-data.sql
```

### 配置文件修改

编辑 `backend/src/main/resources/application.yml`，将数据库和 Redis 密码改为你的实际密码：

```yaml
spring:
  datasource:
    password: 你的MySQL密码
  data:
    redis:
      password: 你的Redis密码
```

## 启动项目

### 1. 确保 MySQL 和 Redis 已启动

```bash
# Windows 下检查服务是否运行
mysqladmin -u root -p ping
redis-cli ping
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端启动后访问 `http://localhost:8080/api`，看到 JSON 响应即启动成功。首次启动 JPA 会自动建表。

### 3. 启动前端

```bash
cd frontend
npm install          # 首次运行需安装依赖
npx vite --host      # 启动并暴露局域网（手机可测试）
```

浏览器访问 `http://localhost:5173`

### 内置测试账号

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | `admin` | `123456` |
| 普通用户 | 注册即用 | —（注册送 1000 元余额） |

## 项目结构

```
├── backend/     # Spring Boot（78 个 Java 文件）
├── frontend/    # Vue 3（13 页面 + 6 组件 + 5 Store）
├── docs/        # 技术文档 + API 规范
└── CDM_PDM/     # 数据库设计
```

## 核心功能

- **Redis Lua 原子锁座**：15 分钟 TTL，防并发超卖
- **虚拟钱包支付**：乐观锁保证余额并发安全，注册送 1000
- **场次过期检测**：自动过滤已过期场次，下单时二次校验
- **票房排行**：全量票房统计 + Redis 缓存，每 5 分钟刷新
- **想看功能**：Redis Set 存储，心愿排行榜
- **管理后台**：电影/影院/影厅/排片/订单管理 + 数据统计仪表盘

## API 概览

Base URL: `http://localhost:8080/api`

| 模块 | 路径 | 接口数 |
|------|------|--------|
| 用户 | `/user` | 6（注册/登录/信息/密码/充值） |
| 电影 | `/movie` | 5（列表/详情/最受期待/想看） |
| 影院 | `/cinema` | 1（列表） |
| 场次 | `/showtime` | 3（列表/详情/座位图） |
| 订单 | `/order` | 7（锁座/下单/支付/退票/列表/状态/详情） |
| 评价 | `/review` | 3（创建/列表/状态） |
| 票房 | `/box-office` | 1（排行榜） |
| 上传 | `/upload` | 1（图片上传） |
| 通知 | `/notification` | 2（列表/已读） |
| 管理 | `/admin` | 14（CRUD + 统计） |

完整接口文档：`docs/api-spec.md`

## 文档

- [后端技术文档](docs/backend-technical-doc.md)
- [前端技术文档](docs/frontend-technical-doc.md)
- [API 接口规范](docs/api-spec.md)
- [系统设计文档](docs/superpowers/specs/2026-05-26-movie-ticket-booking-system-design.md)
