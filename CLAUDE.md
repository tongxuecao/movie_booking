# CLAUDE.md

电影票预订系统 — 课设项目，目标快速交付高分。

## 技术栈

| 层级 | 选型 |
|------|------|
| 前端 | Vue 3 + Vite + Vue Router + Pinia + Element Plus |
| 后端 | Spring Boot 3.2.5 (单体) |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis (锁座 + 订单流) |
| 认证 | JWT |
| 支付 | 虚拟钱包 (注册送 1000) |

## 前端结构

```
frontend/src/
├── config.js           ← API_BASE/UPLOAD_BASE/resolveImageUrl
├── main.js             ← 入口（Vue/Pinia/Router/ElementPlus）
├── App.vue             ← 根组件（导航栏 + router-view + footer）
├── style.css           ← 全局 CSS 变量（--primary/--accent 等）
├── router/index.js     ← 路由定义 + beforeEach 守卫
├── stores/
│   ├── auth.js         ← 用户认证（登录/注册/登出/refreshProfile）
│   ├── movies.js       ← 电影列表缓存
│   ├── cinemas.js      ← 影院列表缓存
│   └── orders.js       ← 订单管理（锁座/创建/轮询/取消 + 支付上下文持久化）
├── services/
│   └── api.js          ← 所有 API 接口（JWT 管理 + fetch + 图片上传）
├── views/
│   ├── Home.vue        ← 首页（轮播 + 电影列表 + 搜索 + 状态筛选）
│   ├── Login.vue       ← 登录
│   ├── Register.vue    ← 注册
│   ├── MovieDetail.vue ← 电影详情 + 排片
│   ├── Cinemas.vue     ← 影院列表
│   ├── CinemaDetail.vue← 影院详情 + 排片
│   ├── SeatSelect.vue  ← 选座（座位网格 + 价格计算 + 锁座）
│   ├── Payment.vue     ← 支付（倒计时 + 轮询订单状态）
│   ├── MyProfile.vue   ← 个人中心（侧边栏：资料/订单/钱包/修改密码/头像上传预览）
│   ├── Admin.vue       ← 管理后台（电影/排片/统计/订单 + el-upload 海报上传）
│   └── NotFound.vue    ← 404
└── components/
    ├── MovieCard.vue   ← 电影海报卡片
    └── BuyTicket.vue   ← 购票组件（场次选择）
```

完整前端文档: `frontend-technical-doc.md`

## 后端结构

```
backend/src/main/java/com/moviebooking/
├── controller/     # 9 个控制器 (User/Movie/Cinema/Showtime/Order/Admin/Upload/Review/Notification)
├── service/        # 9 个服务类
├── repository/     # 12 个 JPA Repository
├── entity/         # 9 个实体 + 11 个枚举
├── dto/            # 12 个请求 DTO（含 ChangePasswordRequest）
├── config/         # JWT拦截器、Redis配置、Web配置（含静态资源映射）
├── redis/          # SeatLockService、OrderStreamProducer/Consumer
├── common/         # ApiResult、异常处理、分页
└── util/           # JwtUtil、订单号生成
```

完整后端文档: `backend-technical-doc.md`

## 核心流程

```
选座 → Redis Lua 原子锁座 (15分钟TTL)
    → 创建订单 → Redis Stream 异步处理
    → 虚拟钱包支付 (乐观锁)
    → 前端轮询订单状态
```

## API 约定

- 基础路径: `http://localhost:8080/api`（context-path: `/api`）
- 响应格式: `{ "code": 200, "message": "success", "data": {...} }`
- 认证: `Authorization: Bearer {token}`
- 分页: `page`(从1开始)、`size`(默认10)
- 完整接口文档: `docs/api-spec.md`

## 关键配置

| 配置项 | 文件 | 值 |
|--------|------|-----|
| 数据库 | application.yml | movie_ticket, root/clh123456 |
| Redis | application.yml | localhost:6379,password:clh123456 |
| JWT | application.yml | 7天过期 |
| 座位锁 | application.yml | TTL 900秒 (15分钟) |
| 上传目录 | application.yml | `./uploads/`，URL前缀 `/uploads/` |
| context-path | application.yml | `/api`（所有接口和静态资源前缀） |
| 前端 API 基址 | config.js | `http://host:8080/api` |
| 前端图片基址 | config.js | `UPLOAD_BASE = http://host:8080/api` |

## 重要注意事项

1. **context-path 是 `/api`**：前端所有 URL（含图片）必须带 `/api` 前缀
2. **图片 URL**：后端返回 `/uploads/xxx.jpg`，前端通过 `resolveImageUrl` 拼接 `UPLOAD_BASE` 变为 `http://host:8080/api/uploads/xxx.jpg`
3. **状态枚举**：`MovieStatus` 枚举值为 `upcoming`/`showing`/`ended`，前端必须用相同名称
4. **密码加密**：BCrypt，修改密码需校验旧密码
5. **BigDecimal**：后端可能以字符串返回，前端用 `Number(v)` 转换
6. **支付上下文**：持久化到 localStorage（key: `mt-payment-ctx`）

## 测试相关

- 测试报告: `docs/backend-test-report-2026-06-21.md`
- Redis 测试指南: `docs/redis-test-guide.md`

## 行为准则

- 中文回答
- 简单优先，最少代码解决问题
- 精准修改，只动必须动的
- 编码前先思考，不确定就问
