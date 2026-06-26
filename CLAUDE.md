# CLAUDE.md

电影票预订系统 — 课设项目，目标快速交付高分。

## 技术栈

- 前端: Vue 3 + Vite + Element Plus
- 后端: Spring Boot 3.2.5 (单体)
- 数据库: MySQL 8.0
- 缓存: Redis (锁座 + 订单流)
- 认证: JWT (7天过期)
- 支付: 虚拟钱包 (注册送 1000)

## 核心流程

选座 → Redis Lua 原子锁座 (15分钟TTL)
    → 创建订单 → Redis Stream 异步处理
    → 虚拟钱包支付 (乐观锁)
    → 前端轮询订单状态

## API 约定

- 基础路径: `http://localhost:8080/api` (context-path: `/api`)
- 响应格式: `{ "code": 200, "message": "success", "data": {...} }`
- 认证: `Authorization: Bearer {token}`
- 分页: `page`(从1开始)、`size`(默认10)

## 关键配置

| 配置项 | 值 |
|--------|-----|
| 数据库 | movie_ticket, root/clh123456 |
| Redis | localhost:6379, password:clh123456 |
| 上传目录 | `./uploads/`，URL前缀 `/uploads/` |
| context-path | `/api` (所有接口和静态资源前缀) |
| 前端 API 基址 | `http://host:8080/api` |

## 重要注意事项

1. **context-path 是 `/api`**：前端所有 URL (含图片) 必须带 `/api` 前缀
2. **图片 URL**：后端返回 `/uploads/xxx.jpg`，前端通过 `resolveImageUrl` 拼接 `UPLOAD_BASE`
3. **状态枚举**：`MovieStatus` 枚举值为 `upcoming`/`showing`/`ended`
4. **密码加密**：BCrypt，修改密码需校验旧密码
5. **BigDecimal**：后端可能以字符串返回，前端用 `Number(v)` 转换

## 行为准则

- 中文回答
- 简单优先，最少代码解决问题
- 精准修改，只动必须动的
- 编码前先思考，不确定就问
