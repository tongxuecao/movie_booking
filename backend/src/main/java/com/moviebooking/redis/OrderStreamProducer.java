package com.moviebooking.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class OrderStreamProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderStreamProducer.class);

    private final StringRedisTemplate redisTemplate;

    private static final String QUEUE_KEY = "order:queue";

    @Autowired
    public OrderStreamProducer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 将订单创建请求推入 Redis List 队列
     */
    public void sendOrderRequest(String orderNo, Long userId, Long showtimeId, String seatIds, String lockToken) {
        // 用简单分隔符拼接，避免依赖 JSON 库
        String message = orderNo + "|" + userId + "|" + showtimeId + "|" + seatIds + "|" + lockToken;

        try {
            redisTemplate.opsForList().rightPush(QUEUE_KEY, message);
            log.info("订单请求已推入队列: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("推入队列失败: orderNo={}", orderNo, e);
            throw new RuntimeException("订单排队失败，请重试");
        }
    }
}
