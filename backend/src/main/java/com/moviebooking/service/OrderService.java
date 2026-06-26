package com.moviebooking.service;

import com.moviebooking.common.BusinessException;
import com.moviebooking.common.PageResult;
import com.moviebooking.dto.CreateOrderRequest;
import com.moviebooking.dto.LockSeatsRequest;
import com.moviebooking.entity.*;
import com.moviebooking.entity.enums.OrderStatus;
import com.moviebooking.entity.enums.SeatType;
import com.moviebooking.redis.OrderStreamProducer;
import com.moviebooking.redis.SeatLockService;
import com.moviebooking.repository.*;
import com.moviebooking.util.OrderNoGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderSeatRepository orderSeatRepository;
    private final PaymentRepository paymentRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final SeatLockService seatLockService;
    private final OrderStreamProducer orderStreamProducer;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;
    private final HallRepository hallRepository;
    private final CinemaRepository cinemaRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    public OrderService(OrderRepository orderRepository, OrderSeatRepository orderSeatRepository,
                        PaymentRepository paymentRepository, ShowtimeRepository showtimeRepository,
                        SeatRepository seatRepository, SeatLockService seatLockService,
                        OrderStreamProducer orderStreamProducer, UserRepository userRepository,
                        MovieRepository movieRepository, HallRepository hallRepository,
                        CinemaRepository cinemaRepository) {
        this.orderRepository = orderRepository;
        this.orderSeatRepository = orderSeatRepository;
        this.paymentRepository = paymentRepository;
        this.showtimeRepository = showtimeRepository;
        this.seatRepository = seatRepository;
        this.seatLockService = seatLockService;
        this.orderStreamProducer = orderStreamProducer;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
        this.hallRepository = hallRepository;
        this.cinemaRepository = cinemaRepository;
    }

    public Map<String, Object> lockSeats(Long userId, LockSeatsRequest request) {
        // 检查座位是否属于该场次的影厅
        Showtime showtime = showtimeRepository.findById(request.getShowtimeId())
                .orElseThrow(() -> BusinessException.notFound("场次不存在"));

        List<Seat> seats = seatRepository.findAllById(request.getSeatIds());
        if (seats.size() != request.getSeatIds().size()) {
            throw BusinessException.badRequest("部分座位不存在");
        }

        // 检查情侣座必须成对选
        long coupleCount = seats.stream().filter(s -> s.getSeatType() == SeatType.couple).count();
        if (coupleCount > 0 && coupleCount % 2 != 0) {
            throw BusinessException.badRequest("情侣座必须成对选择");
        }

        // 调用 Redis 锁座
        String lockResult = seatLockService.tryLockSeats(request.getShowtimeId(), userId, request.getSeatIds());
        if (lockResult != null) {
            if ("user_already_locked".equals(lockResult)) {
                throw BusinessException.badRequest("您已有未完成的订单，请先完成或取消");
            }
            throw BusinessException.conflict("座位已被其他用户锁定，请重新选择");
        }

        // 计算价格
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<Map<String, Object>> seatInfoList = new ArrayList<>();
        for (Seat seat : seats) {
            BigDecimal seatPrice = calculateSeatPrice(showtime.getPrice(), seat.getSeatType());
            totalAmount = totalAmount.add(seatPrice);

            Map<String, Object> seatInfo = new HashMap<>();
            seatInfo.put("id", seat.getId());
            seatInfo.put("row", seat.getRowNum());
            seatInfo.put("col", seat.getColNum());
            seatInfo.put("type", seat.getSeatType().name());
            seatInfo.put("price", seatPrice);
            seatInfoList.add(seatInfo);
        }

        // 生成 lockToken（简单实现：userId + timestamp 的 hash）
        String lockToken = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String expireTime = LocalDateTime.now().plusMinutes(15).format(FORMATTER);

        Map<String, Object> result = new HashMap<>();
        result.put("lockToken", lockToken);
        result.put("expireTime", expireTime);
        result.put("seats", seatInfoList);
        result.put("totalAmount", totalAmount);
        return result;
    }

    public Map<String, Object> createOrder(Long userId, CreateOrderRequest request) {
        // 验证锁是否有效
        if (!seatLockService.isLockedByUser(request.getShowtimeId(), userId, request.getSeatIds())) {
            throw BusinessException.badRequest("座位锁已过期，请重新选择");
        }

        String orderNo = OrderNoGenerator.generate();

        // 推入 Redis Stream 异步处理
        String seatIdsStr = request.getSeatIds().stream()
                .map(String::valueOf)
                .reduce((a, b) -> a + "," + b)
                .orElse("");

        orderStreamProducer.sendOrderRequest(orderNo, userId, request.getShowtimeId(), seatIdsStr, request.getLockToken());

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("status", "processing");
        return result;
    }

    public Map<String, Object> getOrderStatus(String orderNo, Long userId) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw BusinessException.badRequest("无权查看此订单");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("status", order.getStatus().name());
        result.put("message", order.getRemark() != null ? order.getRemark() : getStatusMessage(order.getStatus()));
        return result;
    }

    public Map<String, Object> getOrderDetail(String orderNo, Long userId) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw BusinessException.badRequest("无权查看此订单");
        }

        return buildOrderDetail(order);
    }

    public PageResult<Map<String, Object>> getOrderList(Long userId, String status, int page, int size) {
        Page<Order> orderPage;
        if (status != null && !status.isEmpty()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            orderPage = orderRepository.findByUserIdAndStatus(userId, orderStatus, PageRequest.of(page - 1, size, Sort.by("createdAt").descending()));
        } else {
            orderPage = orderRepository.findByUserId(userId, PageRequest.of(page - 1, size, Sort.by("createdAt").descending()));
        }

        var list = orderPage.getContent().stream().map(this::toOrderBrief).toList();
        return new PageResult<>(list, orderPage.getTotalElements(), page, size);
    }

    @Transactional
    public Map<String, Object> cancelOrder(String orderNo, Long userId) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));

        if (!order.getUserId().equals(userId)) {
            throw BusinessException.badRequest("无权操作此订单");
        }

        if (order.getStatus() != OrderStatus.paid) {
            throw BusinessException.badRequest("只有已支付的订单可以退票");
        }

        // 退款到钱包
        userRepository.addBalance(userId, order.getTotalAmount());

        // 更新订单状态
        order.setStatus(OrderStatus.refunded);
        orderRepository.save(order);

        // 创建退款记录
        Payment payment = new Payment();
        payment.setOrderId(order.getId());
        payment.setUserId(userId);
        payment.setAmount(order.getTotalAmount().negate());
        payment.setStatus(com.moviebooking.entity.enums.PaymentStatus.refunded);
        paymentRepository.save(payment);

        // 释放用户锁（如果还在）
        seatLockService.releaseUserLock(userId);

        User user = userRepository.findById(userId).orElseThrow();

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", orderNo);
        result.put("refundAmount", order.getTotalAmount());
        result.put("walletBalance", user.getWalletBalance());
        return result;
    }

    // --- Admin methods ---

    public PageResult<Map<String, Object>> getAdminOrderList(String status, int page, int size) {
        Page<Order> orderPage;
        if (status != null && !status.isEmpty()) {
            OrderStatus orderStatus = OrderStatus.valueOf(status);
            orderPage = orderRepository.findByStatus(orderStatus, PageRequest.of(page - 1, size, Sort.by("createdAt").descending()));
        } else {
            orderPage = orderRepository.findAll(PageRequest.of(page - 1, size, Sort.by("createdAt").descending()));
        }
        var list = orderPage.getContent().stream().map(this::toOrderBrief).toList();
        return new PageResult<>(list, orderPage.getTotalElements(), page, size);
    }

    public Map<String, Object> getAdminOrderDetail(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> BusinessException.notFound("订单不存在"));
        return buildOrderDetail(order);
    }

    // --- Private helpers ---

    private Map<String, Object> buildOrderDetail(Order order) {
        Showtime showtime = showtimeRepository.findById(order.getShowtimeId()).orElse(null);
        Movie movie = (showtime != null) ? movieRepository.findById(showtime.getMovieId()).orElse(null) : null;
        Hall hall = (showtime != null) ? hallRepository.findById(showtime.getHallId()).orElse(null) : null;
        Cinema cinema = (hall != null) ? cinemaRepository.findById(hall.getCinemaId()).orElse(null) : null;

        List<OrderSeat> orderSeats = orderSeatRepository.findByOrderId(order.getId());
        List<Map<String, Object>> seatList = new ArrayList<>();
        for (OrderSeat os : orderSeats) {
            Seat seat = seatRepository.findById(os.getSeatId()).orElse(null);
            if (seat == null) continue;
            Map<String, Object> sm = new HashMap<>();
            sm.put("row", seat.getRowNum());
            sm.put("col", seat.getColNum());
            sm.put("type", seat.getSeatType().name());
            seatList.add(sm);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("orderNo", order.getOrderNo());
        result.put("status", order.getStatus().name());
        result.put("movieTitle", movie != null ? movie.getTitle() : null);
        result.put("moviePoster", movie != null ? movie.getPoster() : null);
        result.put("cinemaName", cinema != null ? cinema.getName() : null);
        result.put("hallName", hall != null ? hall.getName() : null);
        result.put("showDate", showtime != null ? showtime.getShowDate() : null);
        result.put("showTime", showtime != null ? showtime.getShowTime() : null);
        result.put("seats", seatList);
        result.put("totalAmount", order.getTotalAmount());
        result.put("createdAt", order.getCreatedAt());
        return result;
    }

    private Map<String, Object> toOrderBrief(Order order) {
        Showtime showtime = showtimeRepository.findById(order.getShowtimeId()).orElse(null);
        Movie movie = (showtime != null) ? movieRepository.findById(showtime.getMovieId()).orElse(null) : null;
        Cinema cinema = null;
        if (showtime != null) {
            Hall hall = hallRepository.findById(showtime.getHallId()).orElse(null);
            if (hall != null) cinema = cinemaRepository.findById(hall.getCinemaId()).orElse(null);
        }

        List<OrderSeat> orderSeats = orderSeatRepository.findByOrderId(order.getId());

        Map<String, Object> map = new HashMap<>();
        map.put("orderNo", order.getOrderNo());
        map.put("status", order.getStatus().name());
        map.put("movieTitle", movie != null ? movie.getTitle() : null);
        map.put("moviePoster", movie != null ? movie.getPoster() : null);
        map.put("cinemaName", cinema != null ? cinema.getName() : null);
        map.put("showDate", showtime != null ? showtime.getShowDate() : null);
        map.put("showTime", showtime != null ? showtime.getShowTime() : null);
        map.put("seatCount", orderSeats.size());
        map.put("totalAmount", order.getTotalAmount());
        map.put("createdAt", order.getCreatedAt());
        return map;
    }

    private BigDecimal calculateSeatPrice(BigDecimal basePrice, SeatType seatType) {
        return switch (seatType) {
            case vip -> basePrice.multiply(BigDecimal.valueOf(1.5)).setScale(2, RoundingMode.HALF_UP);
            case couple -> basePrice.multiply(BigDecimal.valueOf(2)).setScale(2, RoundingMode.HALF_UP);
            default -> basePrice;
        };
    }

    private String getStatusMessage(OrderStatus status) {
        return switch (status) {
            case pending -> "排队中";
            case paid -> "支付成功";
            case refunded -> "已退款";
            case cancelled -> "支付失败";
        };
    }

}
