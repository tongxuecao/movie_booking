package com.moviebooking.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class OrderStreamProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderStreamProducer.class);

    private final StringRedisTemplate redisTemplate;

    private static final String STREAM_KEY = "order:stream";

    @Autowired
    public OrderStreamProducer(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 将订单创建请求推入 Redis Stream
     */
    public void sendOrderRequest(String orderNo, Long userId, Long showtimeId, String seatIds, String lockToken) {
        Map<String, String> message = new HashMap<>();
        message.put("orderNo", orderNo);
        message.put("userId", String.valueOf(userId));
        message.put("showtimeId", String.valueOf(showtimeId));
        message.put("seatIds", seatIds);
        message.put("lockToken", lockToken);

        try {
            redisTemplate.opsForStream().add(
                    StreamRecords.newRecord()
                            .ofObject(message)
                            .withStreamKey(STREAM_KEY)
            );
            log.info("订单请求已推入 Stream: orderNo={}", orderNo);
        } catch (Exception e) {
            log.error("推入 Stream 失败: orderNo={}", orderNo, e);
            throw new RuntimeException("订单排队失败，请重试");
        }
    }
}
