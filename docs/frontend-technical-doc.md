# 前端技术文档

## 技术栈

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.5 | Composition API + `<script setup>` |
| Vite | 8 | 构建工具 + 开发服务器 + 代理 |
| Pinia | 3 | 状态管理 |
| Vue Router | 5.1 | 路由 + 导航守卫 |
| Element Plus | 2.14 | UI 组件库（表格、分页、上传、弹窗等） |
| ECharts | 6.1 | 管理后台图表（折线图、柱状图） |
| 字体 | PingFang SC / Microsoft YaHei | 中文字体栈 |

---

## 完整文件结构

```
frontend/src/
│
├── views/                            # 页面（13 个）
│   │
│   ├── Home.vue                      # 首页
│   │   ├── 横幅区
│   │   ├── 标签页切换（正在热映 / 即将上映）
│   │   ├── MovieSearch 搜索组件
│   │   ├── MovieCard 网格（4 列 → 响应式递减）
│   │   ├── Element Plus 分页
│   │   └── SideBar 侧边栏（票房排行 + 最受期待）
│   │
│   ├── Login.vue                     # 登录页
│   │   └── 卡片表单（用户名 + 密码），resize 到 380px 居中
│   │
│   ├── Register.vue                  # 注册页
│   │   └── 卡片表单（用户名 + 手机号 + 密码 + 确认密码）
│   │
│   ├── MovieDetail.vue               # 电影详情页
│   │   ├── 左侧：海报大图
│   │   ├── 右侧：标题、导演、演员、类型、片长、简介
│   │   ├── WishButton 想看按钮
│   │   ├── RatingComponent 评价组件
│   │   ├── "选座购票" 按钮 → 打开 BuyTicket 弹窗
│   │   └── 响应式 768px：左右布局变上下布局
│   │
│   ├── Cinemas.vue                   # 影院列表
│   │   └── 搜索框 + 影院卡片列表（含地址、电话），分页
│   │
│   ├── CinemaDetail.vue              # 影院详情
│   │   ├── 影院信息（名称、地址、电话）
│   │   └── 今日排片卡片列表（电影名 + 时间 + 价格 + 购票按钮）
│   │
│   ├── SeatSelect.vue                # 选座页（核心）
│   │   ├── 屏幕投影提示
│   │   ├── 座位网格（按行分组，座位类型：普通/VIP/情侣）
│   │   ├── 图例（可选/已售/已选/情侣座）
│   │   ├── 最多选 6 个座位，情侣座成对选择
│   │   └── 底部固定栏：已选座位明细 + 总价 + "确认订单" 按钮
│   │
│   ├── Payment.vue                   # 支付页
│   │   ├── 订单摘要（电影名、影院、影厅、日期时间、座位、金额）
│   │   ├── 15 分钟倒计时
│   │   ├── 支付弹窗（密码输入 + 确认支付）
│   │   ├── 余额不足 → 充值流程
│   │   └── 支付成功/失败/超时 三种终态
│   │
│   ├── MyProfile.vue                 # 个人中心
│   │   ├── 侧边栏导航：个人信息 / 我的订单 / 钱包
│   │   ├── 个人信息：头像上传、昵称修改、手机号、密码修改
│   │   ├── 订单列表：标签页过滤（全部/待支付/已支付/已取消）
│   │   ├── 订单详情弹窗 + 取消 + 支付按钮
│   │   └── 钱包：余额显示 + 充值弹窗
│   │
│   ├── NotFound.vue                  # 404 页面
│   │
│   └── admin/                        # 管理后台（6 个）
│       ├── AdminLayout.vue           # 后台布局容器
│       │   ├── 左侧固定菜单栏（5 个菜单项，图标+文字）
│       │   ├── 右侧内容区：`<component :is>` 动态切换页面
│       │   └── 菜单：电影管理 / 排片管理 / 影院管理 / 订单管理 / 数据统计
│       │
│       ├── AdminMovies.vue           # 电影管理
│       │   ├── 表格（海报/片名/导演/类型/状态/操作）
│       │   ├── 搜索 + 分页
│       │   ├── 添加/编辑弹窗表单：电影名、状态、导演、类型（下拉 20 种）、片长、上映日期、主演、简介、海报上传（Element Plus el-upload）
│       │   └── 删除确认
│       │
│       ├── AdminShowtimes.vue        # 排片管理
│       │   ├── 添加排片表单：选择电影 → 选择影院 → 选择影厅（级联下拉）、日期、时间、票价
│       │   ├── 表格列表（电影名/影院/影厅/日期时间/票价/操作）
│       │   └── 过滤 + 删除
│       │
│       ├── AdminCinemas.vue          # 影院管理
│       │   ├── 影院列表 + 添加影院弹窗
│       │   └── 查看影厅弹窗（列出影厅 + 添加影厅：名称、行列数、类型）
│       │
│       ├── AdminOrders.vue           # 订单管理
│       │   └── 订单列表 + 状态过滤（全部/待处理/已支付/已取消/已完成）
│       │
│       └── AdminStatistics.vue       # 数据统计仪表盘
│           ├── 5 个统计卡片（今日订单/今日收入/总票房/总用户/总电影）
│           ├── ECharts 折线图：近 7 天订单数与收入趋势
│           ├── ECharts 柱状图：票房 Top 10 电影
│           ├── ECharts 柱状图：想看 Top 10 电影
│           └── 对应数据表格
│
├── components/                       # 公用组件（6 个）
│   ├── MovieCard.vue                 # 电影卡片
│   │   ├── 海报图（或占位符 🎬），宽高比 3:4
│   │   ├── 评分角标（右上角）
│   │   ├── 片名、导演/类型文字
│   │   ├── "购票" 按钮
│   │   └── hover 上浮 4px 阴影动画，点击跳转电影详情
│   │
│   ├── MovieSearch.vue               # 搜索栏
│   │   └── Element Plus el-input + 搜索按钮，支持回车和点击触发 search 事件
│   │
│   ├── SideBar.vue                   # 首页侧边栏
│   │   ├── 票房排行卡片：总票房金额 + Top 10 电影排名（票房+票数）
│   │   ├── 最受期待卡片：Top N 电影（想看人数）
│   │   ├── 点击电影 → 跳转详情
│   │   └── 响应式 1024px：横向排列；640px：回归纵向
│   │
│   ├── BuyTicket.vue                 # 购票弹窗（Teleport to body）
│   │   ├── 电影信息头部（海报 + 片名 + 类型 + 片长）
│   │   ├── 日期选择按钮组（显示月-日 + 星期几）
│   │   ├── 场次表格：放映时间 / 影厅 / 影院 / 余座 / 票价 / 选座购票按钮
│   │   ├── 未登录 → 关闭弹窗 + emit needLogin
│   │   └── 响应式表格：overflow-x auto，768/480 缩小字号
│   │
│   ├── WishButton.vue                # 想看按钮
│   │   ├── 心形图标 toggle（实心/空心）
│   │   ├── 显示想看人数
│   │   └── 未登录 → emit need-login
│   │
│   └── RatingComponent.vue           # 评价组件
│       ├── 电影均分 + 评分人数（星星 + 数字）
│       ├── "写评价" 按钮（已登录 + 已购票）
│       ├── 评价弹窗：1-10 分星星选择 + 评语（500 字限制）
│       └── 评价列表（头像 + 用户名 + 星级 + 评语 + 时间），分页
│
├── stores/                           # Pinia 状态管理（5 个）
│   ├── auth.js                       # useAuthStore
│   │   ├── 状态：currentUser（ref，从 localStorage 恢复）
│   │   ├── 计算：isLoggedIn, isAdmin, walletBalance, avatar
│   │   └── 动作：login, adminLogin, register, logout, restoreSession, refreshProfile
│   │
│   ├── movies.js                     # useMovieStore
│   │   ├── 状态：movies[], total, currentPage, pageSize=20
│   │   └── 动作：fetchMovies(params), fetchMovie(id)
│   │
│   ├── cinemas.js                    # useCinemaStore
│   │   ├── 状态：cinemas[], total, currentPage
│   │   └── 动作：fetchCinemas, getCinema(id), fetchCinema(id)
│   │
│   ├── orders.js                     # useOrderStore
│   │   ├── 状态：orders[], total, paymentContext（持久化到 localStorage）
│   │   └── 动作：lockSeats, createOrder, payOrder, pollOrderStatus,
│   │              cancelOrder, getOrderDetail, setPaymentContext
│   │
│   └── showtimes.js                  # useShowtimeStore
│       ├── 状态：cinemaGroups[], loading
│       └── 动作：fetchShowtimes(params), getById(id)
│
├── services/api.js                   # API 封装层（50+ 函数）
│   ├── JWT 管理：getToken, setToken, removeToken, getStoredUser
│   ├── 核心请求：request(method, path, body)
│   │   ├── 自动添加 Authorization Bearer Header
│   │   ├── 自动解析 JSON
│   │   ├── code 401 → 自动清空 token + 跳转登录页
│   │   └── resolveUrls() 递归转换图片字段
│   ├── 文件上传：upload(file)  FormData multipart
│   └── 业务 API：13 个模块（用户/电影/票房/评价/影院/场次/订单/管理/上传/通知等）
│
├── router/index.js                   # 路由配置
│   ├── 11 条路由（全部懒加载 import()）
│   ├── 导航守卫 beforeEach
│   │   ├── requiresAuth → 检查 localStorage token → 跳转 /login?redirect=xxx
│   │   └── requiresAdmin → 检查 storedUser.role === 'admin' → 跳转 /login
│   └── history 模式（无 hash）
│
├── config.js                         # 全局配置
│   ├── API_BASE = '/api'
│   └── resolveImageUrl(url) 统一图片 URL 格式
│
├── style.css                         # 全局样式
│   ├── CSS 变量：--primary: #e74c3c（红色）, --accent: #39c5bb（青色）
│   ├── Element Plus 主题色重映射
│   ├── 全局 reset（box-sizing, margin）
│   └── 中文字体栈
│
├── App.vue                           # 根组件
│   ├── header（非 /admin 路由显示）
│   │   ├── Logo + 首页/影院导航链接
│   │   ├── 已登录：头像 + 用户名（链接 /my）
│   │   └── 未登录：登录/注册按钮
│   │   ├── 响应式 768px：缩小间距/字号、隐藏用户名文字
│   │   └── 响应式 480px：隐藏注册链接
│   ├── main（flex:1，容纳 <router-view>）
│   └── footer（非 /admin 路由显示）
│       ├── 三栏网格（品牌/支持/关于）
│       ├── 免责声明
│       └── 响应式 768px：纵向堆叠
│
└── main.js                           # 入口
    ├── createApp → use(Pinia) → use(Router) → use(ElementPlus) → mount
    └── 引入全局 style.css
```

---

## 路由与导航守卫

### 路由表

| 路径 | 名称 | 页面 | meta 认证 |
|------|------|------|-----------|
| `/` | Home | 首页 | 无 |
| `/cinemas` | Cinemas | 影院列表 | 无 |
| `/cinema/:id` | CinemaDetail | 影院详情 | 无 |
| `/movie/:id` | MovieDetail | 电影详情 | 无 |
| `/login` | Login | 登录 | 无 |
| `/register` | Register | 注册 | 无 |
| `/seat-select/:showtimeId` | SeatSelect | 选座 | requiresAuth |
| `/payment` | Payment | 支付 | requiresAuth |
| `/my` | MyProfile | 个人中心 | requiresAuth |
| `/admin` | Admin | 管理后台 | requiresAuth + requiresAdmin |
| `/:pathMatch(.*)*` | NotFound | 404 | 无 |

### 导航守卫逻辑

```javascript
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth) {
    const token = localStorage.getItem('mt-token')
    if (!token) return next('/login?redirect=' + encodeURIComponent(to.fullPath))
  }
  if (to.meta.requiresAdmin) {
    const user = getStoredUser()
    if (!user || user.role !== 'admin') return next('/login')
  }
  next()
})
```

- 所有组件使用动态 `import()` 懒加载，首屏加载更快
- 登录后通过 `redirect` 参数跳回登录前页面

---

## 数据流架构

```
用户操作
  │
  ▼
Vue 组件（.vue）         ←  UI 层，处理用户交互和渲染
  │
  ├── 读取/修改状态
  ▼
Pinia Store（stores/）    ←  状态层，管理数据缓存和业务逻辑
  │
  ├── 调用 API 函数
  ▼
api.js（services/）       ←  网络层，封装 HTTP 请求
  │
  ├── fetch('/api/...')   ←  Vite 开发服务器代理到 backend
  ▼
Spring Boot 后端           ←  数据持久化和核心业务
  │
  ▼
响应返回 → api.js
  │
  ├── resolveUrls()        ←  递归处理 poster/avatar 等图片字段
  │     调用 resolveImageUrl()
  │     /uploads/xxx.jpg → /api/uploads/xxx.jpg
  ▼
Store 更新 → Vue 响应式系统 → 组件自动重新渲染
```

### 图片 URL 处理

```javascript
// config.js
export function resolveImageUrl(url) {
  if (!url) return ''
  // 外部 URL 原样返回
  if (url.startsWith('http://') || url.startsWith('https://')) return url
  // 去掉重复的 /api 前缀，统一加回 /api/，走 Vite 代理
  let clean = url.replace(/^\/api(?=\/uploads)/, '').replace(/^\/+/, '')
  return '/api/' + clean
}
```

`api.js` 的 `IMAGE_KEYS` 集合定义了需要处理的字段名：`poster`, `avatar`, `moviePoster`, `url`。所有 API 响应数据递归遍历，匹配到这些字段名时自动调用 `resolveImageUrl()`。

---

## 购票流程详解

### 完整用户路径

```
首页/电影详情
  │
  ├── 点击 "购票" → BuyTicket 弹窗打开
  │      fetchShowtimes({movieId}) → 显示所有影院+日期的场次
  │      选择日期 → 看到当天所有场次（时间、影厅、影院、余座、票价）
  │      点击 "选座购票"
  │      │
  │      └── 未登录 → 弹出登录提示 → 跳转 /login?redirect=...
  │          登录后回到原页面
  │
  ▼
/seat-select/:showtimeId
  │   fetchSeats(showtimeId) → 渲染座位网格
  │   点击座位选中/取消（最多 6 个）
  │   情侣座成对选中/取消
  │   点击 "确认订单"
  │   │
  │   ├── lockSeats(showtimeId, seatIds) → 拿到 lockToken
  │   ├── createOrder(showtimeId, seatIds, lockToken) → 拿到 orderNo
  │   └── setPaymentContext({orderNo, expireTime}) → 跳转 /payment
  │
  ▼
/payment
  │   从 store 读取 paymentContext
  │   getOrderDetail(orderNo) → 显示订单信息
  │   15 分钟倒计时
  │   │
  │   ├── 点击 "去支付" → 密码弹窗
  │   │     输入密码 → payOrder(orderNo, password)
  │   │     余额不足 → 提示充值 → 调起充值弹窗 → 充值后继续支付
  │   │     支付成功 → 显示成功页
  │   │
  │   ├── 超时 → 订单自动取消，显示超时页
  │   └── 手动取消 → cancelOrder(orderNo) → 退款
```

### 座位类型与定价

| 座位类型 | 价格 | 选择规则 |
|----------|------|----------|
| normal | 基础价 | 单个选择，最多 6 个 |
| vip | 基础价 × 1.5 | 单个选择 |
| couple | 基础价 × 2.0 | 必须成对选择（一次选 2 个相邻座位） |

前端根据座位类型显示不同颜色和价格，couple 座点击时自动选中/取消相邻的两个座位。

---

## 管理后台架构

### 布局

`AdminLayout.vue` 使用 **组件动态切换** 而非路由跳转：

```vue
<!-- 左侧菜单 -->
<div class="admin-sidebar">
  <div v-for="tab in tabs" :class="{active: currentTab === tab.key}"
       @click="currentTab = tab.key">
  </div>
</div>
<!-- 右侧内容区 -->
<component :is="currentComponent" />
```

5 个菜单项对应 5 个组件，切换时不触发路由导航，保持状态。

### 权限控制

双保险：
1. 路由守卫 `requiresAdmin` 检查 token 和 role
2. `AdminLayout.vue` `onMounted` 内二次校验，非 admin 重定向

### 统计仪表盘

`AdminStatistics.vue` 使用 ECharts 渲染：

- xAxis: 最近 7 天日期
- series[0]: 每日订单数（柱状图）
- series[1]: 每日收入（折线图，双 y 轴）
- 票房 Top 10：水平柱状图（y 轴为电影名，x 轴为收入）
- 想看 Top 10：同风格柱状图

---

## 样式系统

### CSS 变量

```css
:root {
  --primary: #e74c3c;       /* 主色：红色，用于按钮、价格、强调 */
  --primary-hover: #c0392b;
  --accent: #39c5bb;        /* 强调色：青色 */
  --border: #e8e8e8;
  --text-light: #999;
  --shadow: 0 2px 12px rgba(0,0,0,0.08);
  --radius: 10px;
  --card-bg: #fff;
}
```

Element Plus 主题色通过 CSS 变量映射到 `--primary`。

### 响应式断点

桌面优先策略（max-width 降级），统一断点：

| 断点 | 适用设备 | 主要调整 |
|------|----------|----------|
| 1200px | 小屏桌面 | 电影网格 4→3 列 |
| 1024px | 平板横屏 | 首页布局左右→上下，SideBar 横排 |
| 768px | 平板竖屏 | header 缩小、footer 堆叠、网格 4→2 列、弹窗全宽 |
| 480px | 手机 | 卡片内边距缩小、按钮全宽、input 16px 防 iOS 缩放 |

### 已适配响应式的文件

- `App.vue` — header/footer 导航
- `Login.vue` / `Register.vue` — 登录注册卡片
- `Home.vue` — 电影网格 + 侧边栏重排（移动端 SideBar 置顶，order: 2）
- `MovieDetail.vue` — 详情页布局
- `BuyTicket.vue` — 场次表格
- `SeatSelect.vue` — 座位网格 + 底部操作栏
- `Payment.vue` — 支付弹窗
- `CinemaDetail.vue` — 场次卡片
- `MyProfile.vue` — 个人中心
- `SideBar.vue` — 侧边栏
- `AdminMovies.vue` — 管理后台电影表单
- `AdminStatistics.vue` — 统计仪表盘

---

## Vite 开发配置

```javascript
// vite.config.js
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': 'http://localhost:8080',  // 所有 /api 请求代理到后端
    },
  },
})
```

- 前端开发端口：`5173`
- 通过 `npx vite --host` 暴露局域网地址，手机可访问测试
- 图片 URL `/api/uploads/xxx.jpg` 经代理到 `localhost:8080/api/uploads/xxx.jpg`

## 配置常量

```javascript
// config.js
export const API_BASE = '/api'
```

- `API_BASE` — 所有 fetch 请求的基础路径
- `resolveImageUrl(url)` — 图片 URL 归一化函数

## Token 存储

| Key | 存储内容 |
|-----|----------|
| `mt-token` | JWT 字符串 |
| `mt-user` | JSON：{id, username, role, phone, walletBalance, avatar} |

## 启动与部署

```bash
cd frontend
npm install                     # 安装依赖（仅首次）
npx vite                        # 开发启动（仅 localhost）
npx vite --host                 # 开发启动（暴露局域网，手机可测试）
npx vite build                  # 生产构建 → dist/
```

启动后访问 `http://localhost:5173`（桌面）或 `http://<本机IP>:5173`（手机）。
