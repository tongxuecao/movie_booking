# CLAUDE.md

电影票预订系统 — 课设项目，目标快速交付高分。

## 技术栈

| 层级 | 选型 |
|------|------|
| 前端 | Vue 3 + Vite + Vant 4 |
| 后端 | Spring Boot 3.2.5 (单体) |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis (锁座 + 订单流) |
| 认证 | JWT |
| 支付 | 虚拟钱包 (注册送 1000) |

## 后端结构 (78 个 Java 文件)

```
backend/src/main/java/com/moviebooking/
├── controller/     # 9 个控制器 (User/Movie/Cinema/Showtime/Order/Admin/Upload/Review/Notification)
├── service/        # 9 个服务类
├── repository/     # 12 个 JPA Repository
├── entity/         # 9 个实体 + 11 个枚举
├── dto/            # 11 个请求 DTO
├── config/         # JWT拦截器、Redis配置、Web配置
├── redis/          # SeatLockService、OrderStreamProducer/Consumer
├── common/         # ApiResult、异常处理、分页
└── util/           # JwtUtil、订单号生成
```

## 核心流程

```
选座 → Redis Lua 原子锁座 (15分钟TTL)
    → 创建订单 → Redis Stream 异步处理
    → 虚拟钱包支付 (乐观锁)
    → 前端轮询订单状态
```

## API 约定

- 基础路径: `http://localhost:8080/api`
- 响应格式: `{ "code": 200, "message": "success", "data": {...} }`
- 认证: `Authorization: Bearer {token}`
- 分页: `page`(从1开始)、`size`(默认10)
- 完整接口文档: `docs/api-spec.md` (27 个接口)

## 关键配置

| 配置项 | 文件 | 值 |
|--------|------|-----|
| 数据库 | application.yml | movie_ticket, root/clh123456 |
| Redis | application.yml | localhost:6379,password:clh123456 |
| JWT | application.yml | 7天过期 |
| 座位锁 | application.yml | TTL 900秒 (15分钟) |

## 测试相关

- 测试报告: `docs/backend-test-report-2026-06-21.md`
- Redis 测试指南: `docs/redis-test-guide.md`

## 行为准则

- 中文回答
- 简单优先，最少代码解决问题
- 精准修改，只动必须动的
- 编码前先思考，不确定就问
