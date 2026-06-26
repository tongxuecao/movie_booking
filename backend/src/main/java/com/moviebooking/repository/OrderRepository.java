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

    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :start AND o.status = :status")
    long countPaidOrdersSince(@Param("start") LocalDateTime start, @Param("status") OrderStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.createdAt >= :start AND o.status = :status")
    java.math.BigDecimal sumRevenueSince(@Param("start") LocalDateTime start, @Param("status") OrderStatus status);

    @Query("SELECT o.showtimeId, COUNT(o), SUM(o.totalAmount) FROM Order o WHERE o.status = :status GROUP BY o.showtimeId ORDER BY COUNT(o) DESC")
    List<Object[]> findTopShowtimes(@Param("status") OrderStatus status, Pageable pageable);

    @Query("SELECT COUNT(o) > 0 FROM Order o WHERE o.userId = :userId AND o.status = :status AND o.showtimeId IN (SELECT s.id FROM Showtime s WHERE s.movieId = :movieId)")
    boolean existsByUserIdAndMovieIdAndPaid(@Param("userId") Long userId, @Param("movieId") Long movieId, @Param("status") OrderStatus status);

    @Query(value = "SELECT CASE WHEN COUNT(*) > 0 THEN 1 ELSE 0 END FROM orders o JOIN showtimes s ON o.showtime_id = s.id WHERE o.user_id = :userId AND o.status = 'paid' AND s.movie_id = :movieId", nativeQuery = true)
    int existsByUserIdAndMovieIdAndPaidNative(@Param("userId") Long userId, @Param("movieId") Long movieId);

    List<Order> findByStatusAndCreatedAtBefore(OrderStatus status, LocalDateTime createdAt);
}
