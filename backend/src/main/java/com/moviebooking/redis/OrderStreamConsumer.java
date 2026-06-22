package com.moviebooking.redis;

import com.moviebooking.entity.*;
import com.moviebooking.entity.enums.OrderStatus;
import com.moviebooking.entity.enums.PaymentStatus;
import com.moviebooking.entity.enums.SeatType;
import com.moviebooking.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class OrderStreamConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderStreamConsumer.class);

    private final StringRedisTemplate redisTemplate;
    private final OrderRepository orderRepository;
    private final OrderSeatRepository orderSeatRepository;
    private final PaymentRepository paymentRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final UserRepository userRepository;
    private final SeatLockService seatLockService;

    private static final String QUEUE_KEY = "order:queue";

    @Autowired
    public OrderStreamConsumer(StringRedisTemplate redisTemplate, OrderRepository orderRepository,
                               OrderSeatRepository orderSeatRepository, PaymentRepository paymentRepository,
                               ShowtimeRepository showtimeRepository, SeatRepository seatRepository,
                               UserRepository userRepository, SeatLockService seatLockService) {
        this.redisTemplate = redisTemplate;
        this.orderRepository = orderRepository;
        this.orderSeatRepository = orderSeatRepository;
        this.paymentRepository = paymentRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.userRepository = userRepository;
        this.seatLockService = seatLockService;
    }

    /**
     * 每 500ms 从 Redis List 中消费一条订单消息
     */
    @Scheduled(fixedDelay = 500)
    public void consume() {
        try {
            // BLPOP: 阻塞式左弹出，等待最多 200ms
            String message = redisTemplate.opsForList().leftPop(QUEUE_KEY, 200, TimeUnit.MILLISECONDS);
            if (message == null || message.isEmpty()) {
                return;
            }

            try {
                processOrder(message);
            } catch (Exception e) {
                log.error("处理订单消息失败: {}", message, e);
            }
        } catch (Exception e) {
            log.error("消费队列异常", e);
        }
    }

    @Transactional
    public void processOrder(String message) {
        // 解析消息: orderNo|userId|showtimeId|seatIds|lockToken
        String[] parts = message.split("\\|");
        if (parts.length < 5) {
            log.error("消息格式错误: {}", message);
            return;
        }

        String orderNo = parts[0];
        Long userId = Long.parseLong(parts[1]);
        Long showtimeId = Long.parseLong(parts[2]);
        String seatIdsStr = parts[3];

        List<Long> seatIds = Arrays.stream(seatIdsStr.split(","))
                .map(Long::parseLong)
                .toList();

        log.info("处理订单: orderNo={}, userId={}, showtimeId={}, seatIds={}", orderNo, userId, showtimeId, seatIdsStr);

        // 1. 验证锁是否仍然有效
        if (!seatLockService.isLockedByUser(showtimeId, userId, seatIds)) {
            log.warn("锁已过期或无效: orderNo={}", orderNo);
            createFailedOrder(orderNo, userId, showtimeId, BigDecimal.ZERO, "座位锁已过期");
            return;
        }

        // 2. 获取场次和座位信息，计算价格
        Showtime showtime = showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new RuntimeException("场次不存在"));

        List<Seat> seats = seatRepository.findAllById(seatIds);
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderSeat> orderSeats = new ArrayList<>();

        for (Seat seat : seats) {
            BigDecimal seatPrice = calculateSeatPrice(showtime.getPrice(), seat.getSeatType());
            totalAmount = totalAmount.add(seatPrice);

            OrderSeat os = new OrderSeat();
            os.setSeatId(seat.getId());
            os.setPrice(seatPrice);
            orderSeats.add(os);
        }

        // 3. 扣减用户余额（乐观锁）
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        if (user.getWalletBalance().compareTo(totalAmount) < 0) {
            log.warn("余额不足: userId={}, balance={}, amount={}", userId, user.getWalletBalance(), totalAmount);
            createFailedOrder(orderNo, userId, showtimeId, totalAmount, "余额不足");
            seatLockService.releaseUserLock(userId);
            return;
        }

        int updated = userRepository.deductBalance(userId, totalAmount, user.getVersion());
        if (updated == 0) {
            log.warn("扣款失败（并发冲突）: userId={}", userId);
            createFailedOrder(orderNo, userId, showtimeId, totalAmount, "支付失败，请重试");
            seatLockService.releaseUserLock(userId);
            return;
        }

        // 4. 创建订单
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setShowtimeId(showtimeId);
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.paid);
        order = orderRepository.save(order);

        // 5. 创建订单座位关联
        for (OrderSeat os : orderSeats) {
            os.setOrderId(order.getId());
        }
        orderSeatRepository.saveAll(orderSeats);

        // 6. 创建支付记录
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setUserId(userId);
        payment.setAmount(totalAmount);
        payment.setStatus(PaymentStatus.success);
        paymentRepository.save(payment);

        // 7. 释放锁
        seatLockService.verifyAndReleaseLock(showtimeId, userId, seatIds);

        log.info("订单处理成功: orderNo={}, amount={}", orderNo, totalAmount);
    }

    private void createFailedOrder(String orderNo, Long userId, Long showtimeId, BigDecimal amount, String reason) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setShowtimeId(showtimeId);
        order.setTotalAmount(amount);
        order.setStatus(OrderStatus.cancelled);
        orderRepository.save(order);
        log.info("订单失败: orderNo={}, reason={}", orderNo, reason);
    }

    private BigDecimal calculateSeatPrice(BigDecimal basePrice, SeatType seatType) {
        return switch (seatType) {
            case vip -> basePrice.multiply(BigDecimal.valueOf(1.5)).setScale(2, RoundingMode.HALF_UP);
            case couple -> basePrice.multiply(BigDecimal.valueOf(2)).setScale(2, RoundingMode.HALF_UP);
            default -> basePrice;
        };
    }
}
