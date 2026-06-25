-- ============================================================
-- 电影票预订系统 - 完整测试数据
-- 数据库: movie_ticket
-- 所有用户密码统一为: 123456
-- BCrypt hash: $2a$10$46Rl1nNTsWP7wnPrWgOqbuRvN6UsRMBjbKsZ9qzEZklAoteaIINs2
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 清空所有表
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
-- 1. 用户 (users)
-- 密码统一: 123456
-- ============================================================
INSERT INTO users (id, username, password, phone, role, wallet_balance, version, avatar, created_at, updated_at) VALUES
(1, 'admin',    '$2a$10$46Rl1nNTsWP7wnPrWgOqbuRvN6UsRMBjbKsZ9qzEZklAoteaIINs2', '13800000001', 'admin', 9999.00, 0, NULL, '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
(2, 'zhangsan', '$2a$10$46Rl1nNTsWP7wnPrWgOqbuRvN6UsRMBjbKsZ9qzEZklAoteaIINs2', '13900001111', 'user',  800.00,  0, NULL, '2026-02-15 14:30:00', '2026-02-15 14:30:00'),
(3, 'lisi',     '$2a$10$46Rl1nNTsWP7wnPrWgOqbuRvN6UsRMBjbKsZ9qzEZklAoteaIINs2', '13900002222', 'user',  1200.00, 0, NULL, '2026-03-01 09:00:00', '2026-03-01 09:00:00'),
(4, 'wangwu',   '$2a$10$46Rl1nNTsWP7wnPrWgOqbuRvN6UsRMBjbKsZ9qzEZklAoteaIINs2', '13900003333', 'user',  500.00,  0, NULL, '2026-03-10 16:00:00', '2026-03-10 16:00:00'),
(5, 'zhaoliu',  '$2a$10$46Rl1nNTsWP7wnPrWgOqbuRvN6UsRMBjbKsZ9qzEZklAoteaIINs2', '13900004444', 'user',  300.00,  0, NULL, '2026-04-01 11:00:00', '2026-04-01 11:00:00'),
(6, 'sunqi',    '$2a$10$46Rl1nNTsWP7wnPrWgOqbuRvN6UsRMBjbKsZ9qzEZklAoteaIINs2', '13900005555', 'user',  1500.00, 0, NULL, '2026-04-15 08:30:00', '2026-04-15 08:30:00');

-- ============================================================
-- 2. 电影 (movies)
-- 状态: showing=热映, upcoming=即将上映, ended=已下线
-- ============================================================
INSERT INTO movies (id, title, duration, release_date, rating, description, genre, director, actors, poster, status, created_at, updated_at) VALUES
(1, '流浪地球3', 153, '2026-01-30', 9.2,
 '太阳即将毁灭，人类在地球表面建造出巨大的推进器，寻找新的家园。然而宇宙之路危机四伏，为了拯救地球，流浪地球时代的年轻人再次挺身而出，展开争分夺秒的生死之战。',
 '科幻 / 冒险', '郭帆', '吴京 / 刘德华 / 李雪健 / 沙溢',
 '/uploads/poster_ld3.jpg', 'showing', '2026-01-10 10:00:00', '2026-01-10 10:00:00'),

(2, '唐人街探案4', 136, '2026-02-01', 7.8,
 '东京发生离奇命案，唐仁与秦风受邀前往调查，在繁华的东京街头展开一场全新的推理冒险，背后隐藏着更大的阴谋。',
 '喜剧 / 悬疑', '陈思诚', '王宝强 / 刘昊然 / 长泽雅美 / 三浦友和',
 '/uploads/poster_tj4.jpg', 'showing', '2026-01-15 10:00:00', '2026-01-15 10:00:00'),

(3, '封神第三部', 148, '2026-01-25', 8.5,
 '殷商末年，纣王暴虐无道，姬发在姜子牙的辅佐下，联合各方力量，发动牧野之战，推翻殷商统治，建立周朝。',
 '奇幻 / 动作', '乌尔善', '费翔 / 黄渤 / 于适 / 陈牧驰',
 '/uploads/poster_fs3.jpg', 'showing', '2026-01-05 10:00:00', '2026-01-05 10:00:00'),

(4, '热辣滚烫2', 120, '2026-02-10', 7.5,
 '乐莹在拳击比赛中找到了人生的方向，这一次她面临更大的挑战，不仅是赛场上的对手，更是内心的恐惧与过去的阴影。',
 '喜剧 / 剧情', '贾玲', '贾玲 / 雷佳音 / 张小斐 / 李雪琴',
 '/uploads/poster_rlgd2.jpg', 'showing', '2026-01-20 10:00:00', '2026-01-20 10:00:00'),

(5, '哪吒之魔童闹海', 140, '2026-07-26', NULL,
 '哪吒与敖丙在海底世界遭遇新的危机，神秘的海妖势力崛起，两人必须联手对抗，守护陈塘关与龙族的和平。',
 '动画 / 奇幻', '饺子', '吕艳婷 / 囧森瑟夫 / 瀚墨 / 陈浩',
 '/uploads/poster_nz2.jpg', 'upcoming', '2026-05-01 10:00:00', '2026-05-01 10:00:00'),

(6, '三体', 160, '2026-08-15', NULL,
 '纳米科学家汪淼被警官史强带入一个名为"科学边界"的组织，在调查过程中，他发现了一个惊天秘密——三体文明正在向地球进发。',
 '科幻 / 悬疑', '张艺谋', '张鲁一 / 于和伟 / 陈瑾 / 王子文',
 '/uploads/poster_st.jpg', 'upcoming', '2026-05-15 10:00:00', '2026-05-15 10:00:00'),

(7, '满江红', 159, '2025-01-22', 7.2,
 '南宋绍兴年间，岳飞死后四年，秦桧率兵与金国会谈。会谈前夜，金国使者死在宰相驻地，所携密信也不翼而飞。',
 '悬疑 / 历史', '张艺谋', '沈腾 / 易烊千玺 / 张译 / 雷佳音',
 '/uploads/poster_mjh.jpg', 'ended', '2025-01-10 10:00:00', '2025-01-10 10:00:00'),

(8, '孤注一掷', 130, '2025-08-08', 7.0,
 '程序员潘生和模特安娜被高薪招聘吸引，出国淘金，却意外卷入一场精心策划的网络诈骗，在异国他乡上演绝地求生。',
 '犯罪 / 剧情', '申奥', '张艺兴 / 金晨 / 咏梅 / 王传君',
 '/uploads/poster_gzyz.jpg', 'ended', '2025-07-20 10:00:00', '2025-07-20 10:00:00'),

(9, '长安三万里', 168, '2025-07-08', 8.3,
 '安史之乱后，整个长安城陷入危机。高适回忆起自己与李白的往事，一段跨越数十年的友情与大唐盛世的兴衰徐徐展开。',
 '动画 / 历史', '谢君伟', '杨天翔 / 凌振赫 / 吴俊全 / 宣晓鸣',
 '/uploads/poster_ca.jpg', 'ended', '2025-06-20 10:00:00', '2025-06-20 10:00:00'),

(10, '消失的她', 122, '2025-06-22', 7.5,
 '何非的妻子在海外旅行时神秘消失，他四处寻找却陷入更大的谜团。随着真相逐渐浮出水面，一个令人震惊的阴谋浮出水面。',
 '悬疑 / 犯罪', '崔睿', '朱一龙 / 倪妮 / 文咏珊 / 杜江',
 '/uploads/poster_xstd.jpg', 'ended', '2025-06-01 10:00:00', '2025-06-01 10:00:00');

-- ============================================================
-- 3. 影院 (cinemas)
-- ============================================================
INSERT INTO cinemas (id, name, address, phone, status, business_hours, longitude, latitude, created_at, updated_at) VALUES
(1, '万达影城(CBD店)',     '北京市朝阳区建国路93号万达广场4层',    '010-59605960', 'open', '09:00-24:00', 116.4700000, 39.9100000, '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
(2, 'CGV影城(颐堤港店)',   '北京市朝阳区酒仙桥路18号颐堤港3层',    '010-57625762', 'open', '09:30-23:30', 116.4900000, 39.9700000, '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
(3, '金逸影城(中关村店)',  '北京市海淀区中关村大街15号中关村广场', '010-82888288', 'open', '10:00-23:00', 116.3100000, 39.9800000, '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
(4, '博纳国际影城(悠唐店)','北京市朝阳区三丰北里2号悠唐购物中心',  '010-59775977', 'open', '09:00-24:00', 116.4300000, 39.9200000, '2026-01-01 10:00:00', '2026-01-01 10:00:00');

-- ============================================================
-- 4. 影厅 (halls)
-- 每个影院3个厅: 普通厅 + IMAX厅/VIP厅
-- ============================================================
INSERT INTO halls (id, cinema_id, name, seat_rows, seat_cols, hall_type, created_at, updated_at) VALUES
-- 万达影城
(1,  1, '1号厅(普通)',  8, 12, 'normal', '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
(2,  1, 'IMAX巨幕厅',   10, 14, 'imax',   '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
(3,  1, 'VIP厅',        6, 8,  'vip',    '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
-- CGV影城
(4,  2, 'A厅(普通)',     8, 10, 'normal', '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
(5,  2, 'IMAX厅',        10, 12, 'imax',   '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
(6,  2, '4D厅',          6, 10, 'threeD', '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
-- 金逸影城
(7,  3, '1号厅',         8, 10, 'normal', '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
(8,  3, '2号厅',         8, 10, 'normal', '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
(9,  3, 'VIP厅',         5, 8,  'vip',    '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
-- 博纳影城
(10, 4, '巨幕厅',        10, 12, 'imax',   '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
(11, 4, '标准厅',        8, 10, 'normal', '2026-01-01 10:00:00', '2026-01-01 10:00:00'),
(12, 4, '情侣厅',        4, 6,  'normal', '2026-01-01 10:00:00', '2026-01-01 10:00:00');

-- ============================================================
-- 5. 座位 (seats)
-- 为每个影厅生成座位矩阵
-- 最后一排的中间两个座位设为情侣座(couple)
-- 第一排设为VIP座(vip)（仅VIP厅和IMAX厅）
-- ============================================================

-- Hall 1: 万达1号普通厅 8x12
INSERT INTO seats (hall_id, row_num, col_num, seat_type, status, created_at, updated_at)
SELECT 1, r, c,
  CASE
    WHEN r = 8 AND c IN (5,6,7,8) THEN 'couple'
    ELSE 'normal'
  END,
  'active', NOW(), NOW()
FROM (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) r_nums
CROSS JOIN (SELECT 1 c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12) c_nums;

-- Hall 2: 万达IMAX厅 10x14
INSERT INTO seats (hall_id, row_num, col_num, seat_type, status, created_at, updated_at)
SELECT 2, r, c,
  CASE
    WHEN r = 1 THEN 'vip'
    WHEN r = 10 AND c IN (6,7,8,9) THEN 'couple'
    ELSE 'normal'
  END,
  'active', NOW(), NOW()
FROM (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) r_nums
CROSS JOIN (SELECT 1 c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12 UNION SELECT 13 UNION SELECT 14) c_nums;

-- Hall 3: 万达VIP厅 6x8
INSERT INTO seats (hall_id, row_num, col_num, seat_type, status, created_at, updated_at)
SELECT 3, r, c,
  CASE
    WHEN r = 1 THEN 'vip'
    WHEN r = 6 AND c IN (3,4,5,6) THEN 'couple'
    ELSE 'normal'
  END,
  'active', NOW(), NOW()
FROM (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r_nums
CROSS JOIN (SELECT 1 c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) c_nums;

-- Hall 4: CGV A厅 8x10
INSERT INTO seats (hall_id, row_num, col_num, seat_type, status, created_at, updated_at)
SELECT 4, r, c,
  CASE WHEN r = 8 AND c IN (4,5,6,7) THEN 'couple' ELSE 'normal' END,
  'active', NOW(), NOW()
FROM (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) r_nums
CROSS JOIN (SELECT 1 c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) c_nums;

-- Hall 5: CGV IMAX厅 10x12
INSERT INTO seats (hall_id, row_num, col_num, seat_type, status, created_at, updated_at)
SELECT 5, r, c,
  CASE
    WHEN r = 1 THEN 'vip'
    WHEN r = 10 AND c IN (5,6,7,8) THEN 'couple'
    ELSE 'normal'
  END,
  'active', NOW(), NOW()
FROM (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) r_nums
CROSS JOIN (SELECT 1 c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12) c_nums;

-- Hall 6: CGV 4D厅 6x10
INSERT INTO seats (hall_id, row_num, col_num, seat_type, status, created_at, updated_at)
SELECT 6, r, c,
  CASE WHEN r = 6 AND c IN (4,5,6,7) THEN 'couple' ELSE 'normal' END,
  'active', NOW(), NOW()
FROM (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) r_nums
CROSS JOIN (SELECT 1 c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) c_nums;

-- Hall 7: 金逸1号厅 8x10
INSERT INTO seats (hall_id, row_num, col_num, seat_type, status, created_at, updated_at)
SELECT 7, r, c,
  CASE WHEN r = 8 AND c IN (4,5,6,7) THEN 'couple' ELSE 'normal' END,
  'active', NOW(), NOW()
FROM (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) r_nums
CROSS JOIN (SELECT 1 c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) c_nums;

-- Hall 8: 金逸2号厅 8x10
INSERT INTO seats (hall_id, row_num, col_num, seat_type, status, created_at, updated_at)
SELECT 8, r, c,
  CASE WHEN r = 8 AND c IN (4,5,6,7) THEN 'couple' ELSE 'normal' END,
  'active', NOW(), NOW()
FROM (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) r_nums
CROSS JOIN (SELECT 1 c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) c_nums;

-- Hall 9: 金逸VIP厅 5x8
INSERT INTO seats (hall_id, row_num, col_num, seat_type, status, created_at, updated_at)
SELECT 9, r, c,
  CASE
    WHEN r = 1 THEN 'vip'
    WHEN r = 5 AND c IN (3,4,5,6) THEN 'couple'
    ELSE 'normal'
  END,
  'active', NOW(), NOW()
FROM (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5) r_nums
CROSS JOIN (SELECT 1 c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) c_nums;

-- Hall 10: 博纳巨幕厅 10x12
INSERT INTO seats (hall_id, row_num, col_num, seat_type, status, created_at, updated_at)
SELECT 10, r, c,
  CASE
    WHEN r = 1 THEN 'vip'
    WHEN r = 10 AND c IN (5,6,7,8) THEN 'couple'
    ELSE 'normal'
  END,
  'active', NOW(), NOW()
FROM (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) r_nums
CROSS JOIN (SELECT 1 c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10 UNION SELECT 11 UNION SELECT 12) c_nums;

-- Hall 11: 博纳标准厅 8x10
INSERT INTO seats (hall_id, row_num, col_num, seat_type, status, created_at, updated_at)
SELECT 11, r, c,
  CASE WHEN r = 8 AND c IN (4,5,6,7) THEN 'couple' ELSE 'normal' END,
  'active', NOW(), NOW()
FROM (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8) r_nums
CROSS JOIN (SELECT 1 c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9 UNION SELECT 10) c_nums;

-- Hall 12: 博纳情侣厅 4x6
INSERT INTO seats (hall_id, row_num, col_num, seat_type, status, created_at, updated_at)
SELECT 12, r, c,
  CASE
    WHEN r >= 3 THEN 'couple'
    ELSE 'normal'
  END,
  'active', NOW(), NOW()
FROM (SELECT 1 r UNION SELECT 2 UNION SELECT 3 UNION SELECT 4) r_nums
CROSS JOIN (SELECT 1 c UNION SELECT 2 UNION SELECT 3 UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) c_nums;

-- ============================================================
-- 6. 排片 (showtimes)
-- 今天是 2026-06-25，排片覆盖今天和未来几天
-- ============================================================
INSERT INTO showtimes (id, movie_id, hall_id, show_date, show_time, price, status, created_at, updated_at) VALUES
-- 流浪地球3 - 万达IMAX (今天 + 明天 + 后天)
(1,  1, 2,  '2026-06-25', '09:30', 79.90, 'normal', NOW(), NOW()),
(2,  1, 2,  '2026-06-25', '14:00', 79.90, 'normal', NOW(), NOW()),
(3,  1, 2,  '2026-06-25', '19:30', 89.90, 'normal', NOW(), NOW()),
(4,  1, 2,  '2026-06-26', '10:00', 79.90, 'normal', NOW(), NOW()),
(5,  1, 2,  '2026-06-26', '15:00', 79.90, 'normal', NOW(), NOW()),
(6,  1, 2,  '2026-06-27', '14:00', 79.90, 'normal', NOW(), NOW()),

-- 流浪地球3 - CGV巨幕
(7,  1, 5,  '2026-06-25', '10:00', 69.90, 'normal', NOW(), NOW()),
(8,  1, 5,  '2026-06-25', '16:30', 69.90, 'normal', NOW(), NOW()),
(9,  1, 5,  '2026-06-26', '13:00', 69.90, 'normal', NOW(), NOW()),

-- 流浪地球3 - 博纳巨幕
(10, 1, 10, '2026-06-25', '11:00', 74.90, 'normal', NOW(), NOW()),
(11, 1, 10, '2026-06-25', '20:00', 84.90, 'normal', NOW(), NOW()),

-- 唐探4 - 万达1号厅
(12, 2, 1,  '2026-06-25', '10:30', 49.90, 'normal', NOW(), NOW()),
(13, 2, 1,  '2026-06-25', '15:00', 49.90, 'normal', NOW(), NOW()),
(14, 2, 1,  '2026-06-25', '20:30', 54.90, 'normal', NOW(), NOW()),
(15, 2, 1,  '2026-06-26', '11:00', 49.90, 'normal', NOW(), NOW()),

-- 唐探4 - CGV 4D厅
(16, 2, 6,  '2026-06-25', '13:00', 59.90, 'normal', NOW(), NOW()),
(17, 2, 6,  '2026-06-26', '15:30', 59.90, 'normal', NOW(), NOW()),

-- 唐探4 - 金逸1号厅
(18, 2, 7,  '2026-06-25', '11:00', 44.90, 'normal', NOW(), NOW()),
(19, 2, 7,  '2026-06-25', '18:00', 49.90, 'normal', NOW(), NOW()),

-- 封神3 - 万达VIP厅
(20, 3, 3,  '2026-06-25', '14:00', 99.90, 'normal', NOW(), NOW()),
(21, 3, 3,  '2026-06-25', '19:00', 99.90, 'normal', NOW(), NOW()),
(22, 3, 3,  '2026-06-26', '15:00', 99.90, 'normal', NOW(), NOW()),

-- 封神3 - CGV A厅
(23, 3, 4,  '2026-06-25', '10:00', 49.90, 'normal', NOW(), NOW()),
(24, 3, 4,  '2026-06-25', '17:00', 54.90, 'normal', NOW(), NOW()),

-- 封神3 - 金逸VIP厅
(25, 3, 9,  '2026-06-25', '16:00', 89.90, 'normal', NOW(), NOW()),
(26, 3, 9,  '2026-06-26', '14:00', 89.90, 'normal', NOW(), NOW()),

-- 热辣滚烫2 - 万达1号厅
(27, 4, 1,  '2026-06-25', '12:00', 39.90, 'normal', NOW(), NOW()),
(28, 4, 1,  '2026-06-25', '17:30', 44.90, 'normal', NOW(), NOW()),

-- 热辣滚烫2 - 金逸2号厅
(29, 4, 8,  '2026-06-25', '13:30', 39.90, 'normal', NOW(), NOW()),
(30, 4, 8,  '2026-06-25', '19:00', 44.90, 'normal', NOW(), NOW()),

-- 热辣滚烫2 - 博纳标准厅
(31, 4, 11, '2026-06-25', '14:30', 39.90, 'normal', NOW(), NOW()),
(32, 4, 11, '2026-06-26', '10:30', 39.90, 'normal', NOW(), NOW()),

-- 流浪地球3 - 博纳情侣厅 (特色场次)
(33, 1, 12, '2026-06-25', '21:00', 129.90,'normal', NOW(), NOW()),

-- 流浪地球3 - 金逸2号厅
(34, 1, 8,  '2026-06-26', '10:00', 49.90, 'normal', NOW(), NOW()),
(35, 1, 8,  '2026-06-26', '16:00', 49.90, 'normal', NOW(), NOW());

-- ============================================================
-- 7. 订单 (orders)
-- 覆盖各种状态: pending / paid / cancelled / refunded
-- ============================================================
INSERT INTO orders (id, order_no, user_id, showtime_id, total_amount, status, created_at, updated_at) VALUES
-- zhangsan 的订单
(1, 'ORD20260620001', 2, 1,  159.80, 'paid',       '2026-06-20 10:30:00', '2026-06-20 10:31:00'),
(2, 'ORD20260621001', 2, 14, 99.80,  'paid',       '2026-06-21 14:00:00', '2026-06-21 14:01:00'),
(3, 'ORD20260623001', 2, 20, 199.80, 'paid',       '2026-06-23 09:00:00', '2026-06-23 09:01:00'),
(4, 'ORD20260624001', 2, 3,  179.80, 'cancelled',  '2026-06-24 16:00:00', '2026-06-24 16:30:00'),

-- lisi 的订单
(5, 'ORD20260619001', 3, 7,  139.80, 'paid',       '2026-06-19 11:00:00', '2026-06-19 11:01:00'),
(6, 'ORD20260622001', 3, 24, 109.80, 'paid',       '2026-06-22 15:00:00', '2026-06-22 15:01:00'),
(7, 'ORD20260624002', 3, 2,  179.80, 'refunded',   '2026-06-24 10:00:00', '2026-06-24 14:00:00'),

-- wangwu 的订单
(8, 'ORD20260621002', 4, 12, 99.80,  'paid',       '2026-06-21 09:30:00', '2026-06-21 09:31:00'),
(9, 'ORD20260624003', 4, 27, 79.80,  'paid',       '2026-06-24 12:00:00', '2026-06-24 12:01:00'),

-- zhaoliu 的订单
(10,'ORD20260622002', 5, 18, 89.80,  'paid',       '2026-06-22 10:00:00', '2026-06-22 10:01:00'),
(11,'ORD20260624004', 5, 33, 259.80, 'paid',       '2026-06-24 20:00:00', '2026-06-24 20:01:00'),

-- sunqi 的订单
(12,'ORD20260623002', 6, 10, 149.80, 'paid',       '2026-06-23 14:00:00', '2026-06-23 14:01:00'),
(13,'ORD20260624005', 6, 21, 199.80, 'cancelled',  '2026-06-24 18:00:00', '2026-06-24 18:30:00'),
(14,'ORD20260625001', 2, 28, 89.80,  'pending',    '2026-06-25 08:00:00', '2026-06-25 08:00:00');

-- ============================================================
-- 8. 订单座位 (order_seats)
-- 使用子查询根据 hall_id + row_num + col_num 查找实际 seat_id
-- ============================================================

INSERT INTO order_seats (order_id, seat_id, price) VALUES
-- 订单1: zhangsan 流浪地球3 万达IMAX(Hall2) row1 col1,col2
(1, (SELECT id FROM seats WHERE hall_id=2 AND row_num=1 AND col_num=1), 79.90),
(1, (SELECT id FROM seats WHERE hall_id=2 AND row_num=1 AND col_num=2), 79.90),

-- 订单2: zhangsan 唐探4 万达1号(Hall1) row5 col3,col4
(2, (SELECT id FROM seats WHERE hall_id=1 AND row_num=5 AND col_num=3), 49.90),
(2, (SELECT id FROM seats WHERE hall_id=1 AND row_num=5 AND col_num=4), 49.90),

-- 订单3: zhangsan 封神3 万达VIP(Hall3) row2 col3,col4
(3, (SELECT id FROM seats WHERE hall_id=3 AND row_num=2 AND col_num=3), 99.90),
(3, (SELECT id FROM seats WHERE hall_id=3 AND row_num=2 AND col_num=4), 99.90),

-- 订单4(已取消): zhangsan 流浪地球3 万达IMAX row3 col5,col6
(4, (SELECT id FROM seats WHERE hall_id=2 AND row_num=3 AND col_num=5), 89.90),
(4, (SELECT id FROM seats WHERE hall_id=2 AND row_num=3 AND col_num=6), 89.90),

-- 订单5: lisi 流浪地球3 CGV IMAX(Hall5) row2 col4,col5
(5, (SELECT id FROM seats WHERE hall_id=5 AND row_num=2 AND col_num=4), 69.90),
(5, (SELECT id FROM seats WHERE hall_id=5 AND row_num=2 AND col_num=5), 69.90),

-- 订单6: lisi 封神3 CGV A厅(Hall4) row4 col5,col6
(6, (SELECT id FROM seats WHERE hall_id=4 AND row_num=4 AND col_num=5), 54.90),
(6, (SELECT id FROM seats WHERE hall_id=4 AND row_num=4 AND col_num=6), 54.90),

-- 订单7(已退款): lisi 流浪地球3 万达IMAX row4 col7,col8
(7, (SELECT id FROM seats WHERE hall_id=2 AND row_num=4 AND col_num=7), 89.90),
(7, (SELECT id FROM seats WHERE hall_id=2 AND row_num=4 AND col_num=8), 89.90),

-- 订单8: wangwu 唐探4 万达1号(Hall1) row3 col6,col7
(8, (SELECT id FROM seats WHERE hall_id=1 AND row_num=3 AND col_num=6), 49.90),
(8, (SELECT id FROM seats WHERE hall_id=1 AND row_num=3 AND col_num=7), 49.90),

-- 订单9: wangwu 热辣滚烫2 万达1号(Hall1) row6 col5,col6
(9, (SELECT id FROM seats WHERE hall_id=1 AND row_num=6 AND col_num=5), 39.90),
(9, (SELECT id FROM seats WHERE hall_id=1 AND row_num=6 AND col_num=6), 39.90),

-- 订单10: zhaoliu 唐探4 金逸1号(Hall7) row3 col4,col5
(10, (SELECT id FROM seats WHERE hall_id=7 AND row_num=3 AND col_num=4), 44.90),
(10, (SELECT id FROM seats WHERE hall_id=7 AND row_num=3 AND col_num=5), 44.90),

-- 订单11: zhaoliu 流浪地球3 博纳情侣厅(Hall12) row3 col1,col2
(11, (SELECT id FROM seats WHERE hall_id=12 AND row_num=3 AND col_num=1), 129.90),
(11, (SELECT id FROM seats WHERE hall_id=12 AND row_num=3 AND col_num=2), 129.90),

-- 订单12: sunqi 流浪地球3 博纳巨幕(Hall10) row3 col5,col6
(12, (SELECT id FROM seats WHERE hall_id=10 AND row_num=3 AND col_num=5), 74.90),
(12, (SELECT id FROM seats WHERE hall_id=10 AND row_num=3 AND col_num=6), 74.90),

-- 订单13(已取消): sunqi 封神3 万达VIP(Hall3) row3 col3,col4
(13, (SELECT id FROM seats WHERE hall_id=3 AND row_num=3 AND col_num=3), 99.90),
(13, (SELECT id FROM seats WHERE hall_id=3 AND row_num=3 AND col_num=4), 99.90),

-- 订单14(待处理): zhangsan 热辣滚烫2 万达1号(Hall1) row7 col3,col4
(14, (SELECT id FROM seats WHERE hall_id=1 AND row_num=7 AND col_num=3), 44.90),
(14, (SELECT id FROM seats WHERE hall_id=1 AND row_num=7 AND col_num=4), 44.90);

-- ============================================================
-- 9. 支付记录 (payments)
-- 只有 paid 状态的订单有支付记录
-- ============================================================
INSERT INTO payments (id, order_id, user_id, payment_method, amount, status, created_at) VALUES
(1,  1,  2, 'wallet', 159.80, 'success', '2026-06-20 10:31:00'),
(2,  2,  2, 'wallet', 99.80,  'success', '2026-06-21 14:01:00'),
(3,  3,  2, 'wallet', 199.80, 'success', '2026-06-23 09:01:00'),
(4,  5,  3, 'wallet', 139.80, 'success', '2026-06-19 11:01:00'),
(5,  6,  3, 'wallet', 109.80, 'success', '2026-06-22 15:01:00'),
(6,  7,  3, 'wallet', 179.80, 'refunded','2026-06-24 14:00:00'),
(7,  8,  4, 'wallet', 99.80,  'success', '2026-06-21 09:31:00'),
(8,  9,  4, 'wallet', 79.80,  'success', '2026-06-24 12:01:00'),
(9,  10, 5, 'wallet', 89.80,  'success', '2026-06-22 10:01:00'),
(10, 11, 5, 'wallet', 259.80, 'success', '2026-06-24 20:01:00'),
(11, 12, 6, 'wallet', 149.80, 'success', '2026-06-23 14:01:00');

-- ============================================================
-- 10. 评论 (reviews)
-- ============================================================
INSERT INTO reviews (id, user_id, movie_id, order_id, rating, content, created_at) VALUES
(1, 2, 1, 1, 5, '特效太震撼了！国产科幻的天花板，剧情紧凑，演员演技在线，强烈推荐IMAX厅观看！',         '2026-06-20 22:00:00'),
(2, 3, 1, 5, 4, '整体不错，画面很震撼，但部分剧情节奏稍慢。刘德华的演技真的绝了。',                 '2026-06-19 23:30:00'),
(3, 2, 2, 2, 4, '唐探系列一如既往的好笑，推理部分也不错，东京的场景拍得很美。',                     '2026-06-21 21:00:00'),
(4, 4, 2, 8, 3, '笑点有，但感觉没有前几部好笑了，推理部分有些牵强。不过还是值回票价的。',           '2026-06-21 22:00:00'),
(5, 2, 3, 3, 5, '封神三部曲完美收官！最后的大战场面太壮观了，费翔演的纣王太有魅力。',               '2026-06-23 23:00:00'),
(6, 3, 3, 6, 4, '视觉效果一流，故事改编得也不错，值得去影院看。',                                   '2026-06-22 22:30:00'),
(7, 5, 2, 10,4, '王宝强太搞笑了，和刘昊然的搭档很默契。适合和朋友一起看。',                        '2026-06-22 21:00:00'),
(8, 4, 4, 9, 4, '贾玲这次不只有笑点，还有泪点，拳击场面拍得很燃！',                                '2026-06-24 22:00:00'),
(9, 6, 1, 12,5, '在巨幕厅看的，视觉效果炸裂！中国科幻之光！',                                     '2026-06-23 23:30:00');

-- ============================================================
-- 11. 通知 (notifications)
-- ============================================================
INSERT INTO notifications (id, user_id, title, content, type, status, created_at) VALUES
(1,  2, '订单支付成功',    '您的订单 ORD20260620001 已支付成功，观影日期：2026-06-25，请提前15分钟到场取票。', 'order',  'read',   '2026-06-20 10:31:00'),
(2,  2, '订单支付成功',    '您的订单 ORD20260621001 已支付成功，观影日期：2026-06-25，请提前15分钟到场取票。', 'order',  'read',   '2026-06-21 14:01:00'),
(3,  2, '订单支付成功',    '您的订单 ORD20260623001 已支付成功，观影日期：2026-06-25，请提前15分钟到场取票。', 'order',  'unread', '2026-06-23 09:01:00'),
(4,  2, '订单已取消',      '您的订单 ORD20260624001 已取消，座位已释放。',                                 'order',  'unread', '2026-06-24 16:30:00'),
(5,  3, '订单支付成功',    '您的订单 ORD20260619001 已支付成功，观影日期：2026-06-25。',                   'order',  'read',   '2026-06-19 11:01:00'),
(6,  3, '退款成功',        '您的订单 ORD20260624002 退款 ¥179.80 已原路返回。',                            'order',  'unread', '2026-06-24 14:00:00'),
(7,  4, '订单支付成功',    '您的订单 ORD20260621002 已支付成功。',                                        'order',  'read',   '2026-06-21 09:31:00'),
(8,  5, '订单支付成功',    '您的订单 ORD20260624004 已支付成功，情侣厅专享体验。',                         'order',  'unread', '2026-06-24 20:01:00'),
(9,  6, '订单已取消',      '您的订单 ORD20260624005 已取消，座位已释放。',                                 'order',  'unread', '2026-06-24 18:30:00'),
(10, 2, '系统维护通知',    '系统将于2026-06-30 02:00-06:00进行维护升级，届时暂停服务。',                    'system', 'unread', '2026-06-25 10:00:00'),
(11, 3, '新片上线提醒',    '《哪吒之魔童闹海》即将上映，现在预约可享早鸟优惠！',                           'system', 'unread', '2026-06-25 10:00:00'),
(12, 4, '新片上线提醒',    '《三体》即将上映，科幻迷不容错过！',                                          'system', 'unread', '2026-06-25 10:00:00'),
(13, 5, '评价提醒',        '您观看的《流浪地球3》可以评价了，分享您的观影感受吧。',                         'system', 'unread', '2026-06-25 10:00:00'),
(14, 6, '评价提醒',        '您观看的《流浪地球3》可以评价了，分享您的观影感受吧。',                         'system', 'unread', '2026-06-25 10:00:00');

-- ============================================================
-- 12. 电影剧照 (movie_images)
-- ============================================================
INSERT INTO movie_images (id, movie_id, image_url, image_type, sort_order, is_cover, created_at) VALUES
(1,  1, '/uploads/ld3_still1.jpg',  'still',  1, false, NOW()),
(2,  1, '/uploads/ld3_still2.jpg',  'still',  2, false, NOW()),
(3,  1, '/uploads/ld3_banner.jpg',  'banner', 0, false, NOW()),
(4,  2, '/uploads/tj4_still1.jpg',  'still',  1, false, NOW()),
(5,  2, '/uploads/tj4_still2.jpg',  'still',  2, false, NOW()),
(6,  3, '/uploads/fs3_still1.jpg',  'still',  1, false, NOW()),
(7,  3, '/uploads/fs3_banner.jpg',  'banner', 0, false, NOW()),
(8,  4, '/uploads/rlgd2_still1.jpg','still',  1, false, NOW()),
(9,  5, '/uploads/nz2_poster.jpg',  'poster', 0, true,  NOW()),
(10, 6, '/uploads/st_poster.jpg',   'poster', 0, true,  NOW());

-- ============================================================
-- 完成! 测试数据统计:
-- 用户: 6 (1管理员 + 5普通用户)
-- 电影: 10 (4热映 + 2即将 + 4已下线)
-- 影院: 4
-- 影厅: 12 (含IMAX/VIP/4D/情侣厅)
-- 座位: ~968个 (含普通/VIP/情侣座)
-- 排片: 35场 (覆盖今天+未来2天)
-- 订单: 14 (9已付 + 2已取消 + 1已退款 + 1待处理 + 1进行中)
-- 支付: 11条
-- 评论: 9条
-- 通知: 14条
-- 剧照: 10条
-- ============================================================
