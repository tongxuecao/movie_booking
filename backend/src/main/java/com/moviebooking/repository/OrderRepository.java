package com.moviebooking.repository;

import com.moviebooking.entity.Order;
import com.moviebooking.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNo(String orderNo);

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :start AND o.status = 'paid'")
    long countPaidOrdersSince(@Param("start") LocalDateTime start);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt >= :start AND o.status = 'paid'")
    java.math.BigDecimal sumRevenueSince(@Param("start") LocalDateTime start);

    @Query("SELECT o.showtimeId, COUNT(o), SUM(o.totalAmount) FROM Order o WHERE o.status = 'paid' GROUP BY o.showtimeId ORDER BY COUNT(o) DESC")
    List<Object[]> findTopShowtimes(Pageable pageable);
}
