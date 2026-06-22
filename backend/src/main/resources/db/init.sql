-- ============================================================
-- 电影票预订系统 - 数据库初始化脚本
-- MySQL 8.0
-- 与 JPA 实体类完全对齐
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS movie_ticket
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE movie_ticket;

-- ============================================================
-- 按外键依赖顺序删除
-- ============================================================
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS order_seats;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS seats;
DROP TABLE IF EXISTS showtimes;
DROP TABLE IF EXISTS movie_images;
DROP TABLE IF EXISTS halls;
DROP TABLE IF EXISTS movies;
DROP TABLE IF EXISTS cinemas;
DROP TABLE IF EXISTS users;

-- ============================================================
-- 1. 用户表
-- ============================================================
CREATE TABLE users (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    username        VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名',
    password        VARCHAR(255) NOT NULL COMMENT '密码(BCrypt加密)',
    phone           VARCHAR(20) COMMENT '手机号',
    role            VARCHAR(10) NOT NULL DEFAULT 'user' COMMENT '角色: user/admin',
    wallet_balance  DECIMAL(10,2) DEFAULT 1000.00 COMMENT '虚拟钱包余额(注册送1000)',
    version         INT DEFAULT 0 COMMENT '乐观锁版本号',
    avatar          VARCHAR(255) COMMENT '头像URL',
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
    rating          DECIMAL(3,1) COMMENT '评分(0.0-9.9)',
    genre           VARCHAR(200) COMMENT '类型(动作/喜剧/科幻等)',
    director        VARCHAR(100) COMMENT '导演',
    actors          VARCHAR(500) COMMENT '主演(逗号分隔)',
    status          VARCHAR(15) DEFAULT 'upcoming' COMMENT '状态: upcoming/showing/ended',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_release_date (release_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影表';

-- ============================================================
-- 3. 电影图片表
-- ============================================================
CREATE TABLE movie_images (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id        BIGINT NOT NULL COMMENT '电影ID',
    image_url       VARCHAR(255) NOT NULL COMMENT '图片URL',
    image_type      VARCHAR(10) DEFAULT 'poster' COMMENT '图片类型: poster/still/banner',
    sort_order      INT DEFAULT 0 COMMENT '排序',
    is_cover        TINYINT(1) DEFAULT 0 COMMENT '是否封面: 0否 1是',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    INDEX idx_movie_id (movie_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电影图片表';

-- ============================================================
-- 4. 影院表
-- ============================================================
CREATE TABLE cinemas (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(200) NOT NULL COMMENT '影院名称',
    address         VARCHAR(500) COMMENT '地址',
    phone           VARCHAR(20) COMMENT '联系电话',
    status          VARCHAR(15) DEFAULT 'open' COMMENT '状态: open/suspended/preparing/closed',
    business_hours  VARCHAR(100) COMMENT '营业时间',
    longitude       DECIMAL(10,7) COMMENT '经度',
    latitude        DECIMAL(10,7) COMMENT '纬度',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='影院表';

-- ============================================================
-- 5. 影厅表
-- ============================================================
CREATE TABLE halls (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    cinema_id       BIGINT NOT NULL COMMENT '影院ID',
    name            VARCHAR(50) NOT NULL COMMENT '影厅名称',
    seat_rows       INT COMMENT '座位行数',
    seat_cols       INT COMMENT '座位列数',
    hall_type       VARCHAR(10) DEFAULT 'normal' COMMENT '影厅类型: normal/imax/vip/threeD',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (cinema_id) REFERENCES cinemas(id) ON DELETE CASCADE,
    INDEX idx_cinema_id (cinema_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='影厅表';

-- ============================================================
-- 6. 场次表
-- ============================================================
CREATE TABLE showtimes (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    movie_id        BIGINT NOT NULL COMMENT '电影ID',
    hall_id         BIGINT NOT NULL COMMENT '影厅ID',
    show_date       DATE NOT NULL COMMENT '放映日期',
    show_time       TIME NOT NULL COMMENT '放映时间',
    price           DECIMAL(8,2) NOT NULL COMMENT '基础票价',
    status          VARCHAR(20) DEFAULT 'normal' COMMENT '状态: normal/cancelled/sold_out',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    FOREIGN KEY (hall_id) REFERENCES halls(id) ON DELETE CASCADE,
    INDEX idx_movie_date (movie_id, show_date),
    INDEX idx_hall_date (hall_id, show_date, show_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='场次表';

-- ============================================================
-- 7. 座位表
-- ============================================================
CREATE TABLE seats (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    hall_id         BIGINT NOT NULL COMMENT '影厅ID',
    row_num         INT NOT NULL COMMENT '行号',
    col_num         INT NOT NULL COMMENT '列号',
    seat_type       VARCHAR(10) DEFAULT 'normal' COMMENT '座位类型: normal/vip/couple',
    status          VARCHAR(15) DEFAULT 'active' COMMENT '状态: active/maintenance',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (hall_id) REFERENCES halls(id) ON DELETE CASCADE,
    UNIQUE KEY uk_hall_seat (hall_id, row_num, col_num)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='座位表';

-- ============================================================
-- 8. 订单表
-- ============================================================
CREATE TABLE orders (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no        VARCHAR(64) UNIQUE NOT NULL COMMENT '订单号',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    showtime_id     BIGINT NOT NULL COMMENT '场次ID',
    total_amount    DECIMAL(10,2) NOT NULL COMMENT '总金额',
    status          VARCHAR(15) DEFAULT 'pending' COMMENT '状态: pending/paid/refunded/cancelled',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (showtime_id) REFERENCES showtimes(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_order_no (order_no),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单表';

-- ============================================================
-- 9. 订单座位关联表
-- ============================================================
CREATE TABLE order_seats (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id        BIGINT NOT NULL COMMENT '订单ID',
    seat_id         BIGINT NOT NULL COMMENT '座位ID',
    price           DECIMAL(8,2) NOT NULL COMMENT '座位价格',
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (seat_id) REFERENCES seats(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_seat_id (seat_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单座位关联表';

-- ============================================================
-- 10. 支付记录表
-- ============================================================
CREATE TABLE payments (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id        BIGINT NOT NULL COMMENT '订单ID',
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    payment_method  VARCHAR(10) DEFAULT 'wallet' COMMENT '支付方式: wallet',
    amount          DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    status          VARCHAR(10) DEFAULT 'success' COMMENT '状态: success/failed/refunded',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_order_id (order_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- ============================================================
-- 11. 评价表
-- ============================================================
CREATE TABLE reviews (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    movie_id        BIGINT NOT NULL COMMENT '电影ID',
    order_id        BIGINT COMMENT '订单ID(可选)',
    rating          INT NOT NULL COMMENT '评分(1-5)',
    content         TEXT COMMENT '评价内容',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (movie_id) REFERENCES movies(id) ON DELETE CASCADE,
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE SET NULL,
    INDEX idx_user_id (user_id),
    INDEX idx_movie_id (movie_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评价表';

-- ============================================================
-- 12. 通知表
-- ============================================================
CREATE TABLE notifications (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id         BIGINT NOT NULL COMMENT '用户ID',
    title           VARCHAR(200) NOT NULL COMMENT '通知标题',
    content         TEXT COMMENT '通知内容',
    type            VARCHAR(10) DEFAULT 'order' COMMENT '类型: order/system',
    status          VARCHAR(10) DEFAULT 'unread' COMMENT '状态: unread/read',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';

-- ============================================================
-- 初始数据
-- ============================================================

-- 管理员账号 (密码: admin123, BCrypt加密)
INSERT INTO users (username, password, phone, role, wallet_balance) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13800138000', 'admin', 10000.00);

-- 测试用户 (密码: user123)
INSERT INTO users (username, password, phone, role, wallet_balance) VALUES
('testuser', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13900139001', 'user', 1000.00),
('zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13900139002', 'user', 1000.00);

-- 示例电影
INSERT INTO movies (title, description, duration, release_date, rating, status, director, actors, genre) VALUES
('流浪地球3', '太阳即将毁灭，人类在地球表面建造出巨大的推进器，寻找新的家园。', 150, '2026-07-01', 9.2, 'showing', '郭帆', '吴京, 刘德华, 李雪健', '科幻'),
('满江红', '南宋绍兴年间，岳飞死后四年，秦桧率兵与金国会谈。', 120, '2026-06-15', 8.5, 'showing', '张艺谋', '沈腾, 易烊千玺, 张译', '悬疑'),
('封神第二部', '姬发回到西岐后，纣王派大军征讨，一场大战即将爆发。', 140, '2026-08-01', 8.8, 'upcoming', '乌尔善', '费翔, 黄渤, 于适', '奇幻'),
('唐人街探案4', '唐仁和秦风接到新案件，前往伦敦展开调查。', 130, '2026-05-01', 7.8, 'ended', '陈思诚', '王宝强, 刘昊然', '喜剧');

-- 示例电影图片
INSERT INTO movie_images (movie_id, image_url, image_type, sort_order, is_cover) VALUES
(1, '/images/movies/earth3_poster.jpg', 'poster', 1, 1),
(1, '/images/movies/earth3_still1.jpg', 'still', 2, 0),
(2, '/images/movies/mjh_poster.jpg', 'poster', 1, 1),
(3, '/images/movies/fs2_poster.jpg', 'poster', 1, 1),
(4, '/images/movies/trt4_poster.jpg', 'poster', 1, 1);

-- 示例影院
INSERT INTO cinemas (name, address, phone, status, business_hours, longitude, latitude) VALUES
('万达影城(国贸店)', '北京市朝阳区建国路93号万达广场4层', '010-88886666', 'open', '09:00-24:00', 116.4610000, 39.9080000),
('CGV影城(朝阳大悦城店)', '北京市朝阳区朝阳北路101号朝阳大悦城7层', '010-66668888', 'open', '09:30-23:30', 116.4980000, 39.9260000),
('UME影城(双井店)', '北京市朝阳区东三环中路双井桥东百子湾路', '010-77778888', 'open', '10:00-23:00', 116.4680000, 39.8960000);

-- 示例影厅
INSERT INTO halls (cinema_id, name, seat_rows, seat_cols, hall_type) VALUES
(1, '1号厅(IMAX)', 10, 16, 'imax'),
(1, '2号厅', 8, 12, 'normal'),
(1, '3号厅(4DX)', 8, 10, 'threeD'),
(2, 'A厅', 10, 14, 'normal'),
(2, 'B厅(IMAX)', 10, 16, 'imax'),
(3, 'VIP厅', 6, 8, 'vip');

-- 生成座位数据 (影厅1: 10行16列)
INSERT INTO seats (hall_id, row_num, col_num, seat_type)
SELECT 1, rn.cn, cn.cn,
    CASE
        WHEN rn.cn = 1 THEN 'couple'
        WHEN rn.cn >= 8 THEN 'vip'
        ELSE 'normal'
    END
FROM
    (SELECT 1 AS cn UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS rn
CROSS JOIN
    (SELECT 1 AS cn UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
     UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16) AS cn;

-- 生成座位数据 (影厅2: 8行12列)
INSERT INTO seats (hall_id, row_num, col_num, seat_type)
SELECT 2, rn.cn, cn.cn,
    CASE
        WHEN rn.cn >= 7 THEN 'vip'
        ELSE 'normal'
    END
FROM
    (SELECT 1 AS cn UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) AS rn
CROSS JOIN
    (SELECT 1 AS cn UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
     UNION SELECT 11 UNION SELECT 12) AS cn;

-- 生成座位数据 (影厅3: 8行10列)
INSERT INTO seats (hall_id, row_num, col_num, seat_type)
SELECT 3, rn.cn, cn.cn, 'normal'
FROM
    (SELECT 1 AS cn UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) AS rn
CROSS JOIN
    (SELECT 1 AS cn UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS cn;

-- 生成座位数据 (影厅4: 10行14列)
INSERT INTO seats (hall_id, row_num, col_num, seat_type)
SELECT 4, rn.cn, cn.cn, 'normal'
FROM
    (SELECT 1 AS cn UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS rn
CROSS JOIN
    (SELECT 1 AS cn UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
     UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14) AS cn;

-- 生成座位数据 (影厅5: 10行16列)
INSERT INTO seats (hall_id, row_num, col_num, seat_type)
SELECT 5, rn.cn, cn.cn,
    CASE
        WHEN rn.cn >= 8 THEN 'vip'
        ELSE 'normal'
    END
FROM
    (SELECT 1 AS cn UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) AS rn
CROSS JOIN
    (SELECT 1 AS cn UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10
     UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14 UNION SELECT 15 UNION SELECT 16) AS cn;

-- 生成座位数据 (影厅6: 6行8列)
INSERT INTO seats (hall_id, row_num, col_num, seat_type)
SELECT 6, rn.cn, cn.cn,
    CASE
        WHEN rn.cn >= 5 THEN 'vip'
        ELSE 'normal'
    END
FROM
    (SELECT 1 AS cn UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) AS rn
CROSS JOIN
    (SELECT 1 AS cn UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5
     UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) AS cn;

-- 示例场次
INSERT INTO showtimes (movie_id, hall_id, show_date, show_time, price, status) VALUES
-- 流浪地球3 场次
(1, 1, '2026-06-20', '10:00:00', 80.00, 'normal'),
(1, 1, '2026-06-20', '14:00:00', 80.00, 'normal'),
(1, 1, '2026-06-20', '19:30:00', 100.00, 'normal'),
(1, 5, '2026-06-20', '11:00:00', 90.00, 'normal'),
-- 满江红 场次
(2, 2, '2026-06-20', '10:30:00', 60.00, 'normal'),
(2, 2, '2026-06-20', '15:00:00', 60.00, 'normal'),
(2, 4, '2026-06-20', '20:00:00', 70.00, 'normal'),
-- 封神第二部 场次(预售)
(3, 1, '2026-08-01', '10:00:00', 100.00, 'normal'),
(3, 1, '2026-08-01', '14:00:00', 100.00, 'normal'),
-- 唐人街探案4 场次(已结束)
(4, 3, '2026-05-10', '14:00:00', 50.00, 'sold_out'),
(4, 3, '2026-05-10', '19:00:00', 50.00, 'sold_out');

-- ============================================================
-- 完成提示
-- ============================================================
SELECT '数据库初始化完成!' AS message;
SELECT COUNT(*) AS '用户数' FROM users;
SELECT COUNT(*) AS '电影数' FROM movies;
SELECT COUNT(*) AS '图片数' FROM movie_images;
SELECT COUNT(*) AS '影院数' FROM cinemas;
SELECT COUNT(*) AS '影厅数' FROM halls;
SELECT COUNT(*) AS '座位数' FROM seats;
SELECT COUNT(*) AS '场次数' FROM showtimes;
