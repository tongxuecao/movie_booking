# 电影票预订系统

基于 Spring Boot 3 + Vue 3 的电影票预订系统，支持高并发选座、异步下单、虚拟钱包支付。

## 技术栈

| 层级 | 技术 |
|------|------|
| 前端 | Vue 3 + Vite + Vant 4 |
| 后端 | Spring Boot 3.2.5 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis（锁座 + Stream） |
| 认证 | JWT |

## 项目结构

```
├── backend/                    # 后端 (78 个 Java 文件)
│   ├── controller/             # 9 个控制器
│   ├── service/                # 9 个服务类
│   ├── repository/             # 12 个 JPA Repository
│   ├── entity/                 # 12 个实体 + 13 个枚举
│   ├── redis/                  # SeatLockService + Stream
│   └── resources/
│       ├── application.yml     # 配置文件
│       └── db/init.sql         # 数据库初始化
├── docs/                       # 项目文档
└── CDM_PDM/                    # 数据库设计文件
```

## 核心功能

- **Redis Lua 原子锁座**：15 分钟 TTL，防止并发超卖
- **Redis Stream 异步下单**：流量削峰，提升系统吞吐
- **虚拟钱包支付**：乐观锁保证余额并发安全
- **JWT 认证**：无状态认证，支持角色权限控制

## 快速启动

### 环境要求

- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- Maven 3.8+

### 启动步骤

```bash
# 1. 初始化数据库
mysql -u root -p < backend/src/main/resources/db/init.sql

# 2. 启动 Redis
redis-server

# 3. 启动后端
cd backend
mvn spring-boot:run
```

### 配置说明

编辑 `backend/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    username: root
    password: your_password  # 修改为你的 MySQL 密码
  data:
    redis:
      password: your_redis_password  # 修改为你的 Redis 密码
```

## API 接口

基础路径：`http://localhost:8080/api`

| 模块 | 接口数 | 说明 |
|------|--------|------|
| 用户 | 4 | 注册、登录、个人信息 |
| 电影 | 2 | 列表、详情 |
| 影院/场次 | 3 | 影院列表、场次列表、座位图 |
| 订单 | 6 | 锁座、下单、支付、退票 |
| 管理 | 10 | 电影/影院/排片管理、数据统计 |
| 其他 | 4 | 文件上传、评价、通知 |

完整接口文档：`docs/api-spec.md`

## 技术亮点

1. **高并发锁座**：Redis 原子操作，支持 1000+ 并发
2. **异步削峰**：Redis Stream 消息队列，保护数据库
3. **数据安全**：乐观锁防止钱包超扣
4. **前后端分离**：RESTful API，并行开发

## 文档

- [系统设计文档](docs/2026-05-26-movie-ticket-booking-system-design.md)
- [API 接口规范](docs/api-spec.md)
- [数据流图](docs/data-flow-diagram.md)
- [Redis 测试指南](docs/redis-test-guide.md)
- [数据库 ER 图](CDM_PDM/er-diagram.md)

## 开发进度

- ✅ 后端开发（78 个 Java 文件）
-   前端开发（Vue 3 + Vant 4）
-   性能测试（JMeter 1000 并发）

## 许可证

MIT License
