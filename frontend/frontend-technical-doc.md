# 前端技术文档

## 1. 项目概述

电影票预订系统前端，基于 Vue 3 + Vite 构建的纯 SPA 应用，直接对接 Spring Boot 后端 API（`http://localhost:8080/api`）。

## 2. 技术栈

| 依赖 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5 | 响应式 UI 框架 |
| Vite | 8.0 | 构建工具 + 开发服务器 |
| Vue Router | 5.1 | 客户端路由 |
| Pinia | 3.0 | 状态管理 |
| Element Plus | 2.14 | UI 组件库 |

## 3. 目录结构

```
frontend/src/
├── config.js            # 动态域名配置（API_BASE / UPLOAD_BASE）
├── main.js              # 应用入口
├── App.vue              # 根组件（导航栏 + 路由出口）
├── style.css            # 全局样式
├── services/
│   └── api.js           # HTTP 客户端 + JWT 管理 + 全部 API 函数
├── stores/
│   ├── auth.js          # 认证状态（登录/注册/登出/会话恢复）
│   ├── movies.js        # 电影列表 + 详情
│   ├── cinemas.js       # 影院列表
│   ├── showtimes.js     # 排片查询
│   └── orders.js        # 订单流程（锁座/下单/轮询/取消）
├── views/
│   ├── Home.vue         # 首页（电影列表 + 搜索 + 预览）
│   ├── MovieDetail.vue  # 电影详情 + 排片选择
│   ├── Cinemas.vue      # 影院列表
│   ├── SeatSelect.vue   # 选座页面
│   ├── Payment.vue      # 支付页面
│   ├── Login.vue        # 登录
│   ├── Register.vue     # 注册
│   ├── MyProfile.vue    # 个人中心（订单列表）
│   └── Admin.vue        # 管理后台（电影CRUD + 统计 + 订单）
├── components/
│   ├── MovieCard.vue    # 电影卡片
│   ├── MovieSearch.vue  # 搜索栏
│   └── BuyTicket.vue    # 购票弹窗
└── router/
    └── index.js         # 路由定义 + 鉴权守卫
```

## 4. 核心设计

### 4.1 动态域名适配

`src/config.js` 通过 `window.location.hostname` 自动检测当前网络环境，动态拼接后端地址：

```js
const hostname = window.location.hostname  // 自动获取当前访问的 IP/域名
export const API_BASE = `http://${hostname}:8080/api`
export const UPLOAD_BASE = `http://${hostname}:8080/api/uploads`
```

- 本地开发：`localhost` → `http://localhost:8080/api`
- 局域网访问：`192.168.x.x` → `http://192.168.x.x:8080/api`
- 切换网络无需修改任何代码

### 4.2 HTTP 客户端 (`services/api.js`)

统一请求封装，所有 API 调用经过同一管道：

```
apiXxx() → request(method, path, body)
         → fetch(API_BASE + path)
         → 解包 { code, message, data }
         → resolveUrls(data)  // 自动补全图片 URL
         → 返回 data
```

**关键机制：**

- **JWT 管理**：`localStorage` 存储 `mt-token` 和 `mt-user`
- **自动鉴权**：每个请求自动附加 `Authorization: Bearer {token}`
- **401 处理**：token 过期自动清除并跳转登录页
- **图片 URL 补全**：响应中 `poster`/`avatar`/`url` 等字段如果是相对路径（如 `/uploads/poster.jpg`），自动拼接为完整 URL
- **错误处理**：后端返回 `code !== 200` 时抛出异常，包含 `message` 信息

**API 函数清单（23 个）：**

| 分类 | 函数 | 对应后端接口 |
|------|------|-------------|
| 用户 | `apiRegister` | `POST /api/user/register` |
| | `apiLogin` | `POST /api/user/login` |
| | `apiGetProfile` | `GET /api/user/profile` |
| | `apiUpdateProfile` | `PUT /api/user/profile` |
| 电影 | `apiGetMovies` | `GET /api/movie/list` |
| | `apiGetMovie` | `GET /api/movie/{id}` |
| 影院 | `apiGetCinemas` | `GET /api/cinema/list` |
| 排片 | `apiGetShowtimes` | `GET /api/showtime/list` |
| | `apiGetSeats` | `GET /api/showtime/{id}/seats` |
| 订单 | `apiLockSeats` | `POST /api/order/lock` |
| | `apiCreateOrder` | `POST /api/order/create` |
| | `apiGetOrderStatus` | `GET /api/order/status/{orderNo}` |
| | `apiGetOrder` | `GET /api/order/{orderNo}` |
| | `apiGetOrders` | `GET /api/order/list` |
| | `apiCancelOrder` | `POST /api/order/cancel/{orderNo}` |
| 管理员 | `apiAdminLogin` | `POST /api/admin/login` |
| | `apiCreateMovie` | `POST /api/admin/movie` |
| | `apiUpdateMovie` | `PUT /api/admin/movie/{id}` |
| | `apiDeleteMovie` | `DELETE /api/admin/movie/{id}` |
| | `apiCreateShowtime` | `POST /api/admin/showtime` |
| | `apiGetAdminOrders` | `GET /api/admin/order/list` |
| | `apiGetStatistics` | `GET /api/admin/statistics` |
| 通用 | `apiUploadImage` | `POST /api/upload/image` |

### 4.3 路由鉴权

`router/index.js` 使用 `beforeEach` 守卫实现三级权限控制：

| 路由 | 权限要求 | 未满足时行为 |
|------|---------|-------------|
| `/`、`/cinemas`、`/movie/:id`、`/login`、`/register` | 无 | 正常访问 |
| `/seat-select`、`/payment`、`/my` | 登录 (`requiresAuth`) | 跳转 `/login?redirect=...` |
| `/admin` | 管理员 (`requiresAuth + requiresAdmin`) | 跳转 `/login?redirect=...` |

登录成功后自动跳转到 `redirect` 参数指定的页面。

### 4.4 订单流程

```
选座页面                    支付页面                   个人中心
   │                         │                         │
   ├─ lockSeats()            ├─ createOrder()          ├─ getOrders()
   │  锁定座位(15分钟TTL)     │  创建订单                │  查看订单列表
   │                         │                         │
   ├─ 跳转 /payment          ├─ pollOrderStatus()      ├─ cancelOrder()
   │  携带 showtimeId         │  轮询订单状态(2秒间隔)    │  取消未支付订单
   │  + seatIds + lockToken   │                         │
   │                         ├─ 支付成功 → 跳转 /my    │
   └─────────────────────────┴─────────────────────────┘
```

### 4.5 状态管理 (Pinia Stores)

| Store | 状态 | 主要 Actions |
|-------|------|-------------|
| `auth` | `token`, `user`, `isLoggedIn`, `isAdmin` | `login()`, `register()`, `logout()`, `restoreSession()` |
| `movies` | `movies`, `total`, `loading` | `fetchMovies(params)`, `fetchMovie(id)` |
| `cinemas` | `cinemas`, `total` | `fetchCinemas(params)` |
| `showtimes` | `showtimes` | `fetchShowtimes({ movieId, cinemaId, date })` |
| `orders` | `currentOrder`, `orders` | `lockSeats()`, `createOrder()`, `pollOrderStatus()`, `fetchOrders()`, `cancelOrder()` |

### 4.6 后端响应格式

所有接口遵循统一格式：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

分页接口返回 `PageResult`：

```json
{
  "code": 200,
  "data": {
    "list": [...],
    "total": 100,
    "page": 1,
    "size": 10
  }
}
```

> 注意：排片列表 (`/showtime/list`) 返回扁平数组，不做分页。

## 5. Vite 配置

`vite.config.js` 配置了 API 代理，开发模式下 `/api` 请求转发到后端：

```js
server: {
  proxy: {
    '/api': 'http://localhost:8080',
  },
}
```

> 注意：由于前端使用动态域名（`config.js` 中拼接完整 URL），实际请求直接发往后端，不经过 Vite 代理。代理仅作为备用方案。

## 6. 启动方式

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，需要后端同时运行在 `http://localhost:8080`。

## 7. 重构变更记录

本次重构将前端从"嵌入式 Node.js 后端 + localStorage 双数据源"架构改为纯 Vite SPA 对接 Spring Boot 后端。

### 7.1 删除的文件

| 文件 | 原因 |
|------|------|
| `server/` 目录 | 嵌入式 Express 后端，已由 Spring Boot 替代 |
| `server.cjs` | Node 启动脚本 |
| `database/` 目录 | 本地 JSON 数据库 |
| `start.bat` / `start.sh` | 启动脚本 |
| `ARCHITECTURE.md` | 旧架构文档 |
| `src/views/file.vue` | 废弃页面 |
| `src/views/Movies.vue` | 合并到 Home.vue |
| `src/services/db.js` | 双数据源 CRUD（242 行），被 api.js 替代 |
| `src/stores/halls.js` | 影厅数据，改由后端提供 |
| `src/stores/templates.js` | 场次模板，改由后端提供 |
| `src/stores/wallet.js` | 虚拟钱包，改由后端管理 |

### 7.2 新建的文件

| 文件 | 说明 |
|------|------|
| `src/config.js` | 动态域名配置 |
| `src/services/api.js` | HTTP 客户端 + 23 个 API 函数 |

### 7.3 修改的文件

| 文件 | 主要变更 |
|------|---------|
| `vite.config.js` | 代理目标 `localhost:3001` → `localhost:8080` |
| `src/router/index.js` | 删除 `/movies` 路由，添加 `beforeEach` 鉴权守卫 |
| `src/App.vue` | 删除钱包余额显示，简化导航栏，移除 `/movies` 链接 |
| `src/stores/auth.js` | JWT 认证，`login`/`register`/`logout`/`restoreSession` |
| `src/stores/movies.js` | 改用分页 API，删除本地 CRUD |
| `src/stores/cinemas.js` | 改用分页 API，删除硬编码数据 |
| `src/stores/showtimes.js` | 改用 API，适配新数据结构 |
| `src/stores/orders.js` | 新流程：`lockSeats` → `createOrder` → `pollOrderStatus` |
| `src/views/Home.vue` | 合并电影列表，服务端分页 + 搜索，预览异步加载详情 |
| `src/views/MovieDetail.vue` | 异步 fetch，loading 状态，适配新字段（`genre`/`showDate`/`showTime`） |
| `src/views/Cinemas.vue` | 分页 API，简化筛选 |
| `src/views/SeatSelect.vue` | 从 `GET /showtime/{id}/seats` 获取座位 |
| `src/views/Payment.vue` | lock→create→poll 流程，删除钱包/支付方式选择 |
| `src/views/Login.vue` | JWT token 存储 |
| `src/views/Register.vue` | 添加 phone 字段 |
| `src/views/MyProfile.vue` | 订单列表从 API 获取 |
| `src/views/Admin.vue` | 电影 CRUD + 统计 + 订单查看 |
| `src/components/MovieCard.vue` | `movie.type` → `movie.genre` |
| `src/components/BuyTicket.vue` | 适配新排片数据结构 |

### 7.4 后端配合修改

| 文件 | 变更 |
|------|------|
| `UserService.java` | `login()` 和 `getProfile()` 返回的 `userInfo` 添加 `role` 字段 |

## 8. 测试账号

所有密码统一为 `123456`：

| 用户名 | 角色 | 钱包余额 |
|--------|------|---------|
| `admin` | 管理员 | 9999.00 |
| `zhangsan` | 普通用户 | 800.00 |
| `lisi` | 普通用户 | 1200.00 |
| `wangwu` | 普通用户 | 500.00 |
| `zhaoliu` | 普通用户 | 300.00 |
| `sunqi` | 普通用户 | 1500.00 |

## 9. 测试数据

完整测试数据 SQL 位于 `docs/test-data.sql`，包含：

- 6 个用户、10 部电影、4 家影院、12 个影厅
- 968 个座位、35 个排片、14 个订单
- 11 条支付记录、9 条评论、14 条通知、10 张电影剧照

导入命令：

```bash
mysql -u root -pclh123456 movie_ticket < docs/test-data.sql
```

## 10. 已知限制

- 海报图片需手动放入 `backend/uploads/` 目录（可用 `docs/generate-posters.py` 生成占位图）
- 支付方式仅支持虚拟钱包，无真实支付集成
- 无 WebSocket，排片/座位状态通过轮询更新
