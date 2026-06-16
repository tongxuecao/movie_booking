package com.moviebooking.repository;

import com.moviebooking.entity.OrderSeat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OrderSeatRepository extends JpaRepository<OrderSeat, Long> {
    List<OrderSeat> findByOrderId(Long orderId);
}
