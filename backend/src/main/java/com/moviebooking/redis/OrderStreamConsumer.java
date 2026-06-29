package com.moviebooking.redis;

import com.moviebooking.entity.Order;
import com.moviebooking.entity.enums.NotificationType;
import com.moviebooking.entity.enums.OrderStatus;
import com.moviebooking.repository.OrderRepository;
import com.moviebooking.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class OrderStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderStreamConsumer.class);

    private final StringRedisTemplate redisTemplate;
    private final OrderRepository orderRepository;
    private final SeatLockService seatLockService;
    private final NotificationService notificationService;

    private static final String QUEUE_KEY = "order:queue";

    @Autowired
    public OrderStreamConsumer(StringRedisTemplate redisTemplate, OrderRepository orderRepository,
                               SeatLockService seatLockService, NotificationService notificationService) {
        this.redisTemplate = redisTemplate;
        this.orderRepository = orderRepository;
        this.seatLockService = seatLockService;
        this.notificationService = notificationService;
    }

    /**
     * 每 500ms 从 Redis List 中消费一条订单消息，记录日志
     */
    @Scheduled(fixedDelay = 500)
    public void consume() {
        try {
            String message = redisTemplate.opsForList().leftPop(QUEUE_KEY, 200, TimeUnit.MILLISECONDS);
            if (message == null || message.isEmpty()) {
                return;
            }
            log.info("收到订单消息: {}", message);
        } catch (Exception e) {
            log.error("消费队列异常", e);
        }
    }

    /**
     * 每分钟清理超时未支付的订单（超过15分钟自动取消）
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void cleanExpiredOrders() {
        LocalDateTime expireTime = LocalDateTime.now().minusMinutes(15);
        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(OrderStatus.pending, expireTime);
        for (Order order : expiredOrders) {
            order.setStatus(OrderStatus.cancelled);
            order.setRemark("超时未支付，自动取消");
            orderRepository.save(order);
            seatLockService.releaseUserLock(order.getUserId());
            notificationService.create(order.getUserId(), "订单已取消",
                "订单 " + order.getOrderNo() + " 超时未支付，已自动取消，座位已释放",
                NotificationType.order);
            log.info("订单超时自动取消: orderNo={}", order.getOrderNo());
        }
    }
}
