-- ============================================================
-- 电影票预订系统 - 最小测试数据
-- 数据库: movie_ticket
-- 所有用户密码统一为: 123456
-- BCrypt hash: $2a$10$46Rl1nNTsWP7wnPrWgOqbuRvN6UsRMBjbKsZ9qzEZklAoteaIINs2
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE notifications;
TRUNCATE TABLE reviews;
TRUNCATE TABLE payments;
TRUNCATE TABLE order_seats;
TRUNCATE TABLE orders;
TRUNCATE TABLE seats;
TRUNCATE TABLE showtimes;
TRUNCATE TABLE halls;
TRUNCATE TABLE cinemas;
TRUNCATE TABLE movie_images;
TRUNCATE TABLE movies;
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 1. 用户 (users) — 密码统一: 123456
-- ============================================================
INSERT INTO users (id, username, password, phone, role, wallet_balance, version, created_at, updated_at) VALUES
(1, 'admin',  '$2a$10$46Rl1nNTsWP7wnPrWgOqbuRvN6UsRMBjbKsZ9qzEZklAoteaIINs2', '13800000001', 'admin', 9999.00, 0, NOW(), NOW()),
(2, 'user1',  '$2a$10$46Rl1nNTsWP7wnPrWgOqbuRvN6UsRMBjbKsZ9qzEZklAoteaIINs2', '13900001111', 'user',  1000.00, 0, NOW(), NOW());

-- ============================================================
-- 2. 电影 (movies)
-- ============================================================
INSERT INTO movies (id, title, duration, release_date, rating, description, genre, director, actors, poster, status, created_at, updated_at) VALUES
(1, '流浪地球3', 153, '2026-01-30', 9.2,
 '太阳即将毁灭，人类在地球表面建造出巨大的推进器，寻找新的家园。',
 '科幻 / 冒险', '郭帆', '吴京 / 刘德华 / 李雪健 / 沙溢',
 '/uploads/poster_ld3.jpg', 'showing', NOW(), NOW()),
(2, '哪吒之魔童闹海', 140, '2026-07-26', NULL,
 '哪吒与敖丙在海底世界遭遇新的危机，两人必须联手对抗，守护陈塘关与龙族的和平。',
 '动画 / 奇幻', '饺子', '吕艳婷 / 囧森瑟夫 / 瀚墨 / 陈浩',
 '/uploads/poster_nz2.jpg', 'upcoming', NOW(), NOW());

-- ============================================================
-- 3. 影院 (cinemas)
-- ============================================================
INSERT INTO cinemas (id, name, address, phone, status, business_hours, longitude, latitude, created_at, updated_at) VALUES
(1, '万达影城(CBD店)', '北京市朝阳区建国路93号万达广场4层', '010-59605960', 'open', '09:00-24:00', 116.4700000, 39.9100000, NOW(), NOW());

-- ============================================================
-- 4. 影厅 (halls)
-- ============================================================
INSERT INTO halls (id, cinema_id, name, seat_rows, seat_cols, hall_type, created_at, updated_at) VALUES
(1, 1, '1号厅(普通)', 8, 10, 'normal', NOW(), NOW()),
(2, 1, 'IMAX厅',     9, 12, 'imax',   NOW(), NOW());

-- ============================================================
-- 5. 座位 (seats)
-- ============================================================
-- Hall 1: 普通厅 8x10
INSERT INTO seats (hall_id, row_num, col_num, seat_type, status, created_at, updated_at)
SELECT 1, r, c, 'normal', 'active', NOW(), NOW()
FROM (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) r_nums
CROSS JOIN (SELECT 1 c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) c_nums;

-- Hall 2: IMAX厅 9x12
INSERT INTO seats (hall_id, row_num, col_num, seat_type, status, created_at, updated_at)
SELECT 2, r, c,
  CASE
    WHEN r = 1 THEN 'vip'
    WHEN r = 9 AND c IN (5,6,7,8) THEN 'couple'
    ELSE 'normal'
  END,
  'active', NOW(), NOW()
FROM (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) r_nums
CROSS JOIN (SELECT 1 c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12) c_nums;

-- ============================================================
-- 6. 排片 (showtimes)
-- ============================================================
INSERT INTO showtimes (id, movie_id, hall_id, show_date, show_time, price, status, created_at, updated_at) VALUES
(1, 1, 1, CURDATE(),     '10:30', 49.90, 'normal', NOW(), NOW()),
(2, 1, 1, CURDATE(),     '15:00', 49.90, 'normal', NOW(), NOW()),
(3, 1, 1, CURDATE(),     '19:30', 54.90, 'normal', NOW(), NOW()),
(4, 1, 2, CURDATE(),     '14:00', 79.90, 'normal', NOW(), NOW()),
(5, 1, 2, CURDATE(),     '19:00', 89.90, 'normal', NOW(), NOW()),
(6, 1, 1, CURDATE() + 1, '10:00', 49.90, 'normal', NOW(), NOW()),
(7, 1, 2, CURDATE() + 1, '14:30', 79.90, 'normal', NOW(), NOW());

-- ============================================================
-- 完成! 测试数据统计:
-- 用户: 2 (1管理员 + 1普通用户), 密码: 123456
-- 电影: 2 (1热映 + 1即将上映)
-- 影院: 1
-- 影厅: 2 (1普通 + 1IMAX)
-- 座位: 188个
-- 排片: 7场 (今天+明天)
-- ============================================================
