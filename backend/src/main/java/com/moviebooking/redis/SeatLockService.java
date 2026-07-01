package com.moviebooking.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SeatLockService {

    private static final Logger log = LoggerFactory.getLogger(SeatLockService.class);

    private final StringRedisTemplate redisTemplate;

    @Value("${app.seat-lock.ttl-seconds}")
    private int lockTtlSeconds;

    private static final String LOCK_KEY_PREFIX = "seat:lock:";
    private static final String USER_LOCK_PREFIX = "user:lock:";

    private DefaultRedisScript<List> lockSeatsScript;

    @Autowired
    public SeatLockService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        lockSeatsScript = new DefaultRedisScript<>();
        lockSeatsScript.setLocation(new ClassPathResource("lua/lock_seats.lua"));
        lockSeatsScript.setResultType(List.class);
    }

    /**
     * Lua 脚本原子锁座：检查所有座位 → 全部锁定
     * 返回 null 表示成功，返回冲突的 seatKey 表示失败
     */
    public String tryLockSeats(Long showtimeId, Long userId, List<Long> seatIds) {
        List<String> keys = new ArrayList<>();
        for (Long seatId : seatIds) {
            keys.add(LOCK_KEY_PREFIX + showtimeId + ":" + seatId);
        }

        // 先检查用户是否已有锁定
        String userLockKey = USER_LOCK_PREFIX + userId;
        String existing = redisTemplate.opsForValue().get(userLockKey);
        if (existing != null) {
            return "user_already_locked";
        }

        // 执行 Lua 脚本原子锁座
        List<?> result = redisTemplate.execute(
                lockSeatsScript,
                keys,
                String.valueOf(userId),
                String.valueOf(lockTtlSeconds)
        );

        long success = ((Number) result.get(0)).longValue();
        if (success == 0) {
            // 返回冲突的 key（Lua 脚本返回的第二个元素）
            return result.get(1).toString();
        }

        // 记录用户的锁定信息（用于后续创建订单）
        String lockInfo = showtimeId + ":" + String.join(",", seatIds.stream().map(String::valueOf).toList());
        redisTemplate.opsForValue().set(userLockKey, lockInfo);
        redisTemplate.expire(userLockKey, java.time.Duration.ofSeconds(lockTtlSeconds));

        return null; // 成功
    }

    /**
     * 验证并释放锁（创建订单时调用）
     */
    public boolean verifyAndReleaseLock(Long showtimeId, Long userId, List<Long> seatIds) {
        String userLockKey = USER_LOCK_PREFIX + userId;
        String lockInfo = redisTemplate.opsForValue().get(userLockKey);
        if (lockInfo == null) {
            return false;
        }

        // 释放所有座位锁
        for (Long seatId : seatIds) {
            String key = LOCK_KEY_PREFIX + showtimeId + ":" + seatId;
            String lockedBy = redisTemplate.opsForValue().get(key);
            if (lockedBy != null && lockedBy.equals(String.valueOf(userId))) {
                redisTemplate.delete(key);
            }
        }

        // 释放用户锁
        redisTemplate.delete(userLockKey);
        return true;
    }

    /**
     * 强制释放用户的锁定（取消订单时调用）
     */
    public void releaseUserLock(Long userId) {
        String userLockKey = USER_LOCK_PREFIX + userId;
        String lockInfo = redisTemplate.opsForValue().get(userLockKey);
        if (lockInfo == null) return;

        // 解析并释放
        String[] parts = lockInfo.split(":");
        if (parts.length == 2) {
            Long showtimeId = Long.parseLong(parts[0]);
            String[] seatIdStrs = parts[1].split(",");
            for (String seatIdStr : seatIdStrs) {
                String key = LOCK_KEY_PREFIX + showtimeId + ":" + seatIdStr;
                redisTemplate.delete(key);
            }
        }
        redisTemplate.delete(userLockKey);
    }

    /**
     * 获取某场次所有被 Redis 锁定的座位 ID 集合
     */
    public Set<Long> getLockedSeatIds(Long showtimeId) {
        Set<String> keys = redisTemplate.keys(LOCK_KEY_PREFIX + showtimeId + ":*");
        if (keys == null || keys.isEmpty()) return Collections.emptySet();
        return keys.stream()
                .map(k -> Long.parseLong(k.substring(k.lastIndexOf(":") + 1)))
                .collect(Collectors.toSet());
    }

    /**
     * 检查座位是否被当前用户锁定
     */
    public boolean isLockedByUser(Long showtimeId, Long userId, List<Long> seatIds) {
        for (Long seatId : seatIds) {
            String key = LOCK_KEY_PREFIX + showtimeId + ":" + seatId;
            String lockedBy = redisTemplate.opsForValue().get(key);
            if (lockedBy == null || !lockedBy.equals(String.valueOf(userId))) {
                return false;
            }
        }
        return true;
    }
}
