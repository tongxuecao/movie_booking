-- 管理员账号（密码: admin123，BCrypt 加密）
INSERT INTO users (username, password, phone, role, wallet_balance, version, created_at, updated_at)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800000000', 'admin', 10000.00, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE username=username;

-- 测试用户（密码: 123456）
INSERT INTO users (username, password, phone, role, wallet_balance, version, created_at, updated_at)
VALUES ('zhangsan', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', '13800138000', 'user', 1000.00, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE username=username;

-- 示例电影
INSERT INTO movies (title, duration, release_date, rating, description, genre, director, actors, poster, status, created_at, updated_at)
VALUES ('流浪地球3', 150, '2026-06-01', 9.2, '太阳即将毁灭，人类在地球表面建造出巨大的推进器...', '科幻', '郭帆', '吴京 / 刘德华 / 李雪健', '/uploads/poster1.jpg', 'showing', NOW(), NOW())
ON DUPLICATE KEY UPDATE title=title;

INSERT INTO movies (title, duration, release_date, rating, description, genre, director, actors, poster, status, created_at, updated_at)
VALUES ('封神第二部', 140, '2026-07-01', 8.8, '殷商大军兵临城下...', '奇幻', '乌尔善', '费翔 / 黄渤 / 于适', '/uploads/poster2.jpg', 'showing', NOW(), NOW())
ON DUPLICATE KEY UPDATE title=title;

-- 示例影院
INSERT INTO cinemas (name, address, phone, status, business_hours, created_at, updated_at)
VALUES ('万达影城（国贸店）', '北京市朝阳区建国路93号', '010-12345678', 'open', '09:00-23:00', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

-- 示例影厅
INSERT INTO halls (cinema_id, name, seat_rows, seat_cols, hall_type, created_at, updated_at)
VALUES (1, '1号厅（IMAX）', 10, 20, 'imax', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;

INSERT INTO halls (cinema_id, name, seat_rows, seat_cols, hall_type, created_at, updated_at)
VALUES (1, '2号厅', 8, 15, 'normal', NOW(), NOW())
ON DUPLICATE KEY UPDATE name=name;
