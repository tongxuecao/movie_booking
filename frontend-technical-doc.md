# 电影票预订系统 — 前端技术文档

> 本文档记录前端架构、核心模块、关键文件和开发注意事项。

---

## 目录

1. [技术栈](#1-技术栈)
2. [项目结构](#2-项目结构)
3. [核心配置](#3-核心配置)
4. [状态管理 (Pinia)](#4-状态管理-pinia)
5. [路由设计](#5-路由设计)
6. [API 层](#6-api-层)
7. [页面模块详解](#7-页面模块详解)
8. [组件库](#8-组件库)
9. [图片上传体系](#9-图片上传体系)
10. [关键流程](#10-关键流程)
11. [已知问题与修复记录](#11-已知问题与修复记录)

---

## 1. 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | — | 框架，Composition API (`<script setup>`) |
| Vite | — | 构建工具 |
| Vue Router 5 | — | 路由，HTML5 history 模式 |
| Pinia | — | 状态管理 |
| Element Plus | — | UI 组件库（全局引入） |
| 原生 fetch | — | HTTP 请求（未使用 axios） |

---

## 2. 项目结构

```
frontend/src/
├── main.js                  ← 入口文件，注册 Vue/Pinia/Router/ElementPlus
├── App.vue                  ← 根组件：顶部导航 + router-view + 底部 footer
├── config.js                ← API_BASE、UPLOAD_BASE、resolveImageUrl
│
├── router/
│   └── index.js             ← 路由定义 + beforeEach 登录守卫
│
├── stores/
│   ├── auth.js              ← 用户认证状态（登录/注册/登出/profile）
│   ├── movies.js            ← 电影列表缓存
│   ├── cinemas.js           ← 影院列表缓存
│   └── orders.js            ← 订单管理（锁座/创建/轮询/取消 + 支付上下文持久化）
│
├── services/
│   └── api.js               ← 所有 API 接口封装（JWT 管理 + fetch 请求 + 图片上传）
│
├── views/
│   ├── Home.vue             ← 首页：轮播 + 电影列表 + 搜索 + 状态筛选
│   ├── Login.vue            ← 登录页
│   ├── Register.vue         ← 注册页
│   ├── MovieDetail.vue      ← 电影详情 + 排片列表
│   ├── Cinemas.vue          ← 影院列表
│   ├── CinemaDetail.vue     ← 影院详情 + 该影院排片
│   ├── SeatSelect.vue       ← 选座页（座位网格 + 价格计算 + 锁座）
│   ├── Payment.vue          ← 支付页（倒计时 + 订单确认 + 轮询结果）
│   ├── MyProfile.vue        ← 个人中心（侧边栏导航：资料/订单/钱包/修改密码）
│   ├── Admin.vue            ← 管理后台（电影/排片/统计/订单管理）
│   └── NotFound.vue         ← 404 页面
│
├── components/
│   ├── MovieCard.vue        ← 电影海报卡片（首页列表项）
│   └── BuyTicket.vue        ← 购票组件（场次选择 + 跳转选座）
│
└── style.css                ← 全局 CSS 变量 + 基础样式
```

---

## 3. 核心配置

### config.js

```js
const hostname = window.location.hostname  // 自动适配局域网访问
const BACKEND_PORT = 8080

export const API_BASE = `http://${hostname}:${BACKEND_PORT}/api`
export const UPLOAD_BASE = `http://${hostname}:${BACKEND_PORT}/api`
```

- `API_BASE`：所有 API 请求前缀（后端 context-path 为 `/api`）
- `UPLOAD_BASE`：图片 URL 拼接前缀（与 API_BASE 相同，因为后端静态资源在 `/api/uploads/**`）
- `resolveImageUrl(url)`：将后端返回的相对路径（如 `/uploads/xxx.jpg`）转为完整 URL

### style.css 全局变量

| 变量 | 值 | 用途 |
|------|-----|------|
| `--primary` | `#e74c3c` | 主色（红色），用于操作按钮 |
| `--primary-hover` | `#c0392b` | 主色悬停 |
| `--accent` | `#39c5bb` | 强调色（青色），用于视觉状态 |
| `--text` | `#333` | 正文颜色 |
| `--text-light` | `#999` | 辅助文字 |
| `--border` | `#eee` | 边框颜色 |
| `--shadow` | `0 2px 12px rgba(0,0,0,0.06)` | 卡片阴影 |

---

## 4. 状态管理 (Pinia)

### auth.js — 用户认证

| 属性/方法 | 说明 |
|-----------|------|
| `currentUser` | 当前用户对象（ref） |
| `isLoggedIn` | 是否已登录（computed） |
| `isAdmin` | 是否管理员（computed） |
| `currentUsername` | 用户名（computed） |
| `walletBalance` | 钱包余额（computed，自动 Number 转换） |
| `avatar` | 头像 URL（computed） |
| `login(username, password)` | 登录，自动存储 token 和用户信息 |
| `register(username, password, phone)` | 注册 |
| `logout()` | 登出，清除 token |
| `restoreSession()` | 页面刷新后恢复登录状态 |
| `refreshProfile()` | 从后端重新拉取用户信息并同步 localStorage |

### orders.js — 订单管理

| 属性/方法 | 说明 |
|-----------|------|
| `orders` | 订单列表 |
| `paymentContext` | 支付上下文（持久化到 localStorage key: `mt-payment-ctx`） |
| `setPaymentContext(ctx)` | 保存支付上下文（锁座后 → 支付页） |
| `clearPaymentContext()` | 清除支付上下文 |
| `lockSeats(showtimeId, seatIds)` | 锁座 |
| `createOrder(showtimeId, seatIds, lockToken)` | 创建订单 |
| `pollOrderStatus(orderNo)` | 轮询订单状态（1s 延迟 + 20 次重试 + 2s 间隔） |
| `cancelOrder(orderNo)` | 取消订单 |

### movies.js / cinemas.js

简单的列表缓存 store，提供 `fetchMovies(params)` / `fetchCinemas(params)` 方法。

---

## 5. 路由设计

| 路径 | 页面 | 需要登录 | 说明 |
|------|------|----------|------|
| `/` | Home | 否 | 首页，电影列表 |
| `/login` | Login | 否 | 登录页 |
| `/register` | Register | 否 | 注册页 |
| `/movie/:id` | MovieDetail | 否 | 电影详情 |
| `/cinemas` | Cinemas | 否 | 影院列表 |
| `/cinema/:id` | CinemaDetail | 否 | 影院详情 |
| `/seat/:showtimeId` | SeatSelect | 是 | 选座页 |
| `/payment` | Payment | 是 | 支付页 |
| `/my` | MyProfile | 是 | 个人中心 |
| `/admin` | Admin | 是(管理员) | 管理后台 |
| `/:pathMatch(.*)*` | NotFound | 否 | 404 |

**路由守卫** (`beforeEach`)：
- 需要登录的页面未登录时跳转 `/login?redirect=xxx`
- 管理页面非管理员跳转 `/login?redirect=/admin`

---

## 6. API 层

### api.js 结构

```
JWT 管理 (getToken/setToken/removeToken/getStoredUser/setStoredUser)
    ↓
核心请求 (request → fetch + JWT header + JSON 解析 + resolveUrls)
    ↓
HTTP 方法 (get/post/put/del)
    ↓
业务接口 (apiXxx 导出函数)
```

### 图片 URL 处理链路

```
后端返回: { poster: "/uploads/abc.jpg" }
    ↓ resolveUrls (自动识别 poster/avatar/url 等 key)
    ↓ resolveImageUrl (拼接 UPLOAD_BASE)
前端使用: { poster: "http://localhost:8080/api/uploads/abc.jpg" }
```

### 接口清单

| 函数 | 方法 | 路径 | 说明 |
|------|------|------|------|
| `apiRegister` | POST | `/user/register` | 注册 |
| `apiLogin` | POST | `/user/login` | 登录 |
| `apiGetProfile` | GET | `/user/profile` | 获取个人信息 |
| `apiUpdateProfile` | PUT | `/user/profile` | 修改资料（username/phone/avatar） |
| `apiChangePassword` | PUT | `/user/password` | 修改密码 |
| `apiGetMovies` | GET | `/movie/list` | 电影列表 |
| `apiGetMovie` | GET | `/movie/{id}` | 电影详情 |
| `apiGetCinemas` | GET | `/cinema/list` | 影院列表 |
| `apiGetShowtimes` | GET | `/showtime/list` | 排片列表 |
| `apiGetShowtime` | GET | `/showtime/{id}` | 单个排片详情 |
| `apiGetSeats` | GET | `/showtime/{id}/seats` | 座位图 |
| `apiLockSeats` | POST | `/order/lock` | 锁座 |
| `apiCreateOrder` | POST | `/order/create` | 创建订单 |
| `apiGetOrderStatus` | GET | `/order/status/{orderNo}` | 订单状态 |
| `apiGetOrder` | GET | `/order/{orderNo}` | 订单详情 |
| `apiGetOrders` | GET | `/order/list` | 订单列表 |
| `apiCancelOrder` | POST | `/order/cancel/{orderNo}` | 取消订单 |
| `apiAdminLogin` | POST | `/admin/login` | 管理员登录 |
| `apiCreateMovie` | POST | `/admin/movie` | 添加电影 |
| `apiUpdateMovie` | PUT | `/admin/movie/{id}` | 修改电影 |
| `apiDeleteMovie` | DELETE | `/admin/movie/{id}` | 删除电影 |
| `apiCreateShowtime` | POST | `/admin/showtime` | 添加排片 |
| `apiGetAdminOrders` | GET | `/admin/order/list` | 管理员订单列表 |
| `apiGetStatistics` | GET | `/admin/statistics` | 数据统计 |
| `apiUploadImage` | POST | `/upload/image` | 上传图片 |

---

## 7. 页面模块详解

### Home.vue — 首页

- 顶部轮播（假数据 banner）
- 状态筛选标签（正在热映 `showing` / 即将上映 `upcoming`）
- 电影搜索（300ms 防抖）
- 电影卡片网格（MovieCard 组件）
- 分页

### MovieDetail.vue — 电影详情

- 电影信息展示（海报/评分/类型/时长/状态标签）
- 排片列表（按影院分组，显示日期/时间/票价/剩余座位）
- 点击场次 → 跳转 `/seat/:showtimeId`

### SeatSelect.vue — 选座

- 座位网格（按行分组，支持普通/VIP/情侣座）
- 最多选 6 个座位
- 价格计算：普通 1x / VIP 1.5x / 情侣座 2x
- 锁座后跳转支付页，支付上下文持久化到 localStorage

### Payment.vue — 支付

- 从 localStorage 恢复支付上下文（防刷新丢失）
- 15 分钟倒计时（< 2 分钟紧急动画）
- 订单摘要展示
- 支付后轮询订单状态（最多 20 次，2s 间隔）

### MyProfile.vue — 个人中心

**侧边栏导航**：
- 个人资料（用户名/手机号/角色/注册时间，支持修改昵称）
- 购票记录（状态筛选标签：全部/已支付/处理中/已取消，订单详情弹窗）
- 我的钱包（余额展示）
- 修改密码（旧密码+新密码+确认密码弹窗）

**头像功能**：
- 点击上传（隐藏 file input + hover 遮罩）
- 查看大图（Teleport 全屏预览弹窗）
- 上传后自动刷新 profile

### Admin.vue — 管理后台

**四个标签页**：
1. 电影管理：搜索/分页/添加/编辑/删除，海报用 el-upload 上传
2. 排片管理：按电影ID+影厅ID+时间创建排片
3. 数据统计：今日订单/收入/总用户/总影片/热门排行
4. 订单管理：全部订单列表

**电影编辑弹窗**：
- 表单字段：名称、导演、类型、片长、评分、上映日期、主演、简介、状态、海报
- 海报支持两种方式：el-upload 上传 + 手动输入 URL

---

## 8. 组件库

### MovieCard.vue

- 电影海报卡片，点击跳转电影详情
- 支持键盘访问（`role="link"`, `tabindex="0"`）
- 按钮文字："购票"

### BuyTicket.vue

- 场次选择组件（日期选择 + 场次列表）
- 用于电影详情页的购票流程

---

## 9. 图片上传体系

### 上传流程

```
前端选择文件
    ↓
FormData.append('file', file)
    ↓
POST /api/upload/image (携带 JWT)
    ↓
后端 UploadService:
  1. 校验非空 + 扩展名 (jpg/jpeg/png/gif/webp)
  2. UUID 重命名
  3. 保存到 ./uploads/ (绝对路径)
  4. 返回 { url: "/uploads/xxx.jpg" }
    ↓
前端 resolveUrls → resolveImageUrl
    ↓
完整 URL: http://localhost:8080/api/uploads/xxx.jpg
    ↓
Spring 资源处理器 (/api/uploads/** → 物理目录) 返回文件
```

### 关键配置

| 配置项 | 文件 | 值 |
|--------|------|-----|
| 存储目录 | application.yml | `./uploads/` |
| URL 前缀 | application.yml | `/uploads/` |
| 最大大小 | application.yml | 10MB |
| 静态映射 | WebConfig.java | `/uploads/**` → 物理目录 |
| 前端基址 | config.js | `UPLOAD_BASE = http://host:8080/api` |

### 头像上传（MyProfile.vue）

1. 点击头像 → 触发隐藏 `<input type="file">`
2. 校验大小 < 5MB
3. 调用 `apiUploadImage(file)` 上传
4. 调用 `apiUpdateProfile({ avatar: data.url })` 更新用户头像
5. `auth.refreshProfile()` 刷新本地状态

### 海报上传（Admin.vue）

使用 Element Plus 的 `<el-upload>` 组件：
- `action` 指向上传接口
- 自动携带 JWT token（通过 `:headers`）
- 上传前校验类型和大小
- 上传成功后 `resolveImageUrl` 转换 URL

---

## 10. 关键流程

### 登录流程

```
用户输入账号密码 → apiLogin → POST /user/login
    → 后端返回 { token, userInfo }
    → 前端 setToken + setStoredUser
    → auth.currentUser 更新
    → router.push(redirect || '/')
```

### 购票流程

```
首页 → 电影详情 → 选择场次 → 选座页
    → 选择座位 → 确认选座 → lockSeats → 锁座成功
    → setPaymentContext (持久化到 localStorage)
    → 跳转支付页 → 倒计时 → 点击支付
    → createOrder → pollOrderStatus (轮询)
    → 支付成功 → 跳转个人中心
```

### 订单状态轮询

```
createOrder 返回 { orderNo, status: "processing" }
    ↓
等待 1 秒（给后端处理时间）
    ↓
循环最多 20 次:
  GET /order/status/{orderNo}
  → 如果 status !== 'processing' → 返回结果
  → 否则等待 2 秒继续
    ↓
超时返回 { status: 'timeout' }
```

---

## 11. 已知问题与修复记录

### 2026-06-26 修复

| 问题 | 原因 | 修复文件 |
|------|------|----------|
| 图片上传后不显示 | `UPLOAD_BASE` 缺少 `/api` 前缀（context-path） | `config.js` |
| 管理员编辑电影报 500 | `MovieStatus.valueOf()` 对无效值抛异常 | `MovieService.java` |
| 前端状态值 `coming` 与后端枚举 `upcoming` 不匹配 | 前后端命名不一致 | `Admin.vue`, `Home.vue`, `MovieDetail.vue` |
| 文件上传失败 (IOException) | `transferTo` 使用相对路径 | `UploadService.java` |
| 静态资源映射不正确 | `addResourceLocations` 使用相对路径 | `WebConfig.java` |

### 开发注意事项

1. **后端 context-path 是 `/api`**：所有前端 URL 都需要包含 `/api` 前缀
2. **图片 URL 处理**：后端返回相对路径，前端通过 `resolveImageUrl` 拼接完整地址
3. **Java BigDecimal**：后端可能以字符串返回数字，前端用 `Number(v)` 转换
4. **支付上下文**：持久化到 localStorage（key: `mt-payment-ctx`），防刷新丢失
5. **状态枚举**：前端值必须与后端 `MovieStatus` 枚举名一致：`upcoming`/`showing`/`ended`
