-- ============================================================
-- 电影票预订系统 - 数据库初始化脚本
-- MySQL 8.0
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS movie_ticket
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE movie_ticket;

-- ============================================================
-- 1. 用户表
-- ============================================================
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS seats;
DROP TABLE IF EXISTS showtimes;
DROP TABLE IF EXISTS halls;
DROP TABLE IF EXISTS cinemas;
DROP TABLE IF EXISTS movies;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    password        VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    phone           VARCHAR(20) COMMENT '手机号',
    email           VARCHAR(100) COMMENT '邮箱',
    avatar          VARCHAR(255) COMMENT '头像URL',
    role            ENUM('user', 'admin') DEFAULT 'user' COMMENT '角色',
    wallet_balance  DECIMAL(10,2) DEFAULT 1000.00 COMMENT '虚拟钱包余额(注册送1000)',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_username (username),
    INDEX idx_phone (phone)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 电影表
-- ============================================================
CREATE TABLE movies (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    title           VARCHAR(200) NOT NULL COMMENT '电影名称',
    description     TEXT COMMENT '电影简介',
    duration        INT COMMENT '时长(分钟)',
    release_date    DATE COMMENT '上映日期',
    poster          VARCHAR(255) COMMENT '海报URL',
    rating          DECIMAL(2,1) COMMENT '评分(0.0-9.9)',
    status          ENUM('upcoming', 'showing', 'ended') DEFAULT 'upcoming' COMMENT '状态',
    director        VARCHAR(100) COMMENT '导演',
    actors          VARCHAR(500) COMMENT '主演(逗号分隔)',
    genre           VARCHAR(100) COMMENT '类型(动作/喜剧/科幻等)',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_release_date (release_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影表';

-- ============================================================
-- 3. 影院表
-- ============================================================
CREATE TABLE cinemas (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(200) NOT NULL COMMENT '影院名称',
    address         VARCHAR(500) COMMENT '地址',
    latitude        DECIMAL(10,7) COMMENT '纬度',
    longitude       DECIMAL(10,7) COMMENT '经度',
    phone           VARCHAR(20) COMMENT '联系电话',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='影院表';

-- ============================================================
-- 4. 影厅表
-- ============================================================
CREATE TABLE halls (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    cinema_id       BIGINT NOT NULL COMMENT '影院ID',
    name            VARCHAR(50) COMMENT '影厅名称',
    seat_rows       INT COMMENT '座位行数',
    seat_cols       INT COMMENT '座位列数',
    hall_type       ENUM('normal', 'imax', '4dx') DEFAULT 'normal' COMMENT '影厅类型',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cinema_id) REFERENCES cinemas(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='影厅表';

-- ============================================================
-- 5. 场次表
-- ============================================================
CREATE TABLE showtimes (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id        BIGINT NOT NULL COMMENT '电影ID',
    hall_id         BIGINT NOT NULL COMMENT '影厅ID',
    show_date       DATE NOT NULL COMMENT '放映日期',
    show_time       TIME NOT NULL COMMENT '放映时间',
    price           DECIMAL(8,2) NOT NULL COMMENT '基础票价',
    status          ENUM('available', 'locked', 'sold_out') DEFAULT 'available' COMMENT '状态',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    FOREIGN KEY (hall_id) REFERENCES halls(id) ON DELETE CASCADE,
    INDEX idx_movie_date (movie_id, show_date),
    INDEX idx_hall_date (hall_id, show_date, show_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场次表';

-- ============================================================
-- 6. 座位表
-- ============================================================
CREATE TABLE seats (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    hall_id         BIGINT NOT NULL COMMENT '影厅ID',
    row_num         INT NOT NULL COMMENT '行号',
    col_num         INT NOT NULL COMMENT '列号',
    seat_type       ENUM('normal', 'vip', 'couple') DEFAULT 'normal' COMMENT '座位类型',
    status          ENUM('available', 'locked', 'sold') DEFAULT 'available' COMMENT '状态',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (hall_id) REFERENCES halls(id) ON DELETE CASCADE,
    UNIQUE KEY uk_hall_seat (hall_id, row_num, col_num)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='座位表';

-- ============================================================
-- 7. 订单表
-- ============================================================
CREATE TABLE orders (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(64) UNIQUE NOT NULL COMMENT '订单号',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    showtime_id     BIGINT NOT NULL COMMENT '场次ID',
    seat_ids        JSON COMMENT '座位ID列表',
    total_amount    DECIMAL(10,2) NOT NULL COMMENT '总金额',
    status          ENUM('pending', 'paid', 'cancelled', 'refunded') DEFAULT 'pending' COMMENT '状态',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    paid_at         DATETIME COMMENT '支付时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (showtime_id) REFERENCES showtimes(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ============================================================
-- 8. 支付记录表
-- ============================================================
CREATE TABLE payments (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id        BIGINT NOT NULL COMMENT '订单ID',
    payment_method  ENUM('mock', 'alipay', 'wechat', 'card') DEFAULT 'mock' COMMENT '支付方式',
    amount          DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    transaction_id  VARCHAR(100) COMMENT '交易流水号',
    status          ENUM('pending', 'success', 'failed', 'refunded') DEFAULT 'pending' COMMENT '状态',
    paid_at         DATETIME COMMENT '支付时间',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_transaction_id (transaction_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 管理员账号 (密码: admin123, BCrypt加密)
INSERT INTO users (username, password, phone, email, role, wallet_balance) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13800138000', 'admin@movie.com', 'admin', 10000.00);

-- 测试用户 (密码: user123)
INSERT INTO users (username, password, phone, email, role, wallet_balance) VALUES
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13900139001', 'user1@movie.com', 'user', 1000.00),
('zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13900139002', 'zhangsan@movie.com', 'user', 1000.00);

-- 示例电影
INSERT INTO movies (title, description, duration, release_date, rating, status, director, actors, genre) VALUES
('流浪地球3', '太阳即将毁灭，人类在地球表面建造出巨大的推进器，寻找新的家园。', 150, '2026-07-01', 9.2, 'showing', '郭帆', '吴京, 刘德华, 李雪健', '科幻'),
('满江红', '南宋绍兴年间，岳飞死后四年，秦桧率兵与金国会谈。', 120, '2026-06-15', 8.5, 'showing', '张艺谋', '沈腾, 易烊千玺, 张译', '悬疑'),
('封神第二部', '姬发回到西岐后，纣王派大军征讨，一场大战即将爆发。', 140, '2026-08-01', 8.8, 'upcoming', '乌尔善', '费翔, 黄渤, 于适', '奇幻'),
('唐人街探案4', '唐仁和秦风接到新案件，前往伦敦展开调查。', 130, '2026-05-01', 7.8, 'ended', '陈思诚', '王宝强, 刘昊然', '喜剧');

-- 示例影院
INSERT INTO cinemas (name, address, phone) VALUES
('万达影城(国贸店)', '北京市朝阳区建国路93号万达广场4层', '010-88886666'),
('CGV影城(朝阳大悦城店)', '北京市朝阳区朝阳北路101号朝阳大悦城7层', '010-66668888'),
('UME影城(双井店)', '北京市朝阳区东三环中路双井桥东百子湾路', '010-77778888');

-- 示例影厅
INSERT INTO halls (cinema_id, name, seat_rows, seat_cols, hall_type) VALUES
(1, '1号厅(IMAX)', 10, 16, 'imax'),
(1, '2号厅', 8, 12, 'normal'),
(1, '3号厅(4DX)', 8, 10, '4dx'),
(2, 'A厅', 10, 14, 'normal'),
(2, 'B厅(IMAX)', 10, 16, 'imax'),
(3, 'VIP厅', 6, 8, 'normal');

-- 生成座位数据 (影厅1: 10行16列)
INSERT INTO seats (hall_id, row_num, col_num, seat_type)
SELECT 1, r, c,
    CASE
        WHEN r = 1 THEN 'couple'  -- 第1排为情侣座
        WHEN r >= 8 THEN 'vip'    -- 后3排为VIP座
        ELSE 'normal'
    END
FROM
    (SELECT 1 AS r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) rows,
    (SELECT 1 AS c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
     UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16) cols;

-- 生成座位数据 (影厅2: 8行12列)
INSERT INTO seats (hall_id, row_num, col_num, seat_type)
SELECT 2, r, c,
    CASE
        WHEN r >= 7 THEN 'vip'
        ELSE 'normal'
    END
FROM
    (SELECT 1 AS r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) rows,
    (SELECT 1 AS c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
     UNION SELECT 11 UNION SELECT 12) cols;

-- 生成座位数据 (影厅3: 8行10列)
INSERT INTO seats (hall_id, row_num, col_num, seat_type)
SELECT 3, r, c, 'normal'
FROM
    (SELECT 1 AS r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) rows,
    (SELECT 1 AS c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) cols;

-- 生成座位数据 (影厅4: 10行14列)
INSERT INTO seats (hall_id, row_num, col_num, seat_type)
SELECT 4, r, c, 'normal'
FROM
    (SELECT 1 AS r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) rows,
    (SELECT 1 AS c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
     UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14) cols;

-- 生成座位数据 (影厅5: 10行16列)
INSERT INTO seats (hall_id, row_num, col_num, seat_type)
SELECT 5, r, c,
    CASE
        WHEN r >= 8 THEN 'vip'
        ELSE 'normal'
    END
FROM
    (SELECT 1 AS r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) rows,
    (SELECT 1 AS c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
     UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16) cols;

-- 生成座位数据 (影厅6: 6行8列)
INSERT INTO seats (hall_id, row_num, col_num, seat_type)
SELECT 6, r, c,
    CASE
        WHEN r >= 5 THEN 'vip'
        ELSE 'normal'
    END
FROM
    (SELECT 1 AS r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) rows,
    (SELECT 1 AS c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) cols;

-- 示例场次
INSERT INTO showtimes (movie_id, hall_id, show_date, show_time, price, status) VALUES
-- 流浪地球3 场次
(1, 1, '2026-06-20', '10:00:00', 80.00, 'available'),
(1, 1, '2026-06-20', '14:00:00', 80.00, 'available'),
(1, 1, '2026-06-20', '19:30:00', 100.00, 'available'),
(1, 5, '2026-06-20', '11:00:00', 90.00, 'available'),
-- 满江红 场次
(2, 2, '2026-06-20', '10:30:00', 60.00, 'available'),
(2, 2, '2026-06-20', '15:00:00', 60.00, 'available'),
(2, 4, '2026-06-20', '20:00:00', 70.00, 'available'),
-- 封神第二部 场次(预售)
(3, 1, '2026-08-01', '10:00:00', 100.00, 'available'),
(3, 1, '2026-08-01', '14:00:00', 100.00, 'available'),
-- 唐人街探案4 场次(已结束)
(4, 3, '2026-05-10', '14:00:00', 50.00, 'sold_out'),
(4, 3, '2026-05-10', '19:00:00', 50.00, 'sold_out');

-- ============================================================
-- 完成提示
-- ============================================================
SELECT '✅ 数据库初始化完成!' AS message;
SELECT COUNT(*) AS '用户数' FROM users;
SELECT COUNT(*) AS '电影数' FROM movies;
SELECT COUNT(*) AS '影院数' FROM cinemas;
SELECT COUNT(*) AS '影厅数' FROM halls;
SELECT COUNT(*) AS '座位数' FROM seats;
SELECT COUNT(*) AS '场次数' FROM showtimes;
