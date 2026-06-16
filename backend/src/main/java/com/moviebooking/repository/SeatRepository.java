package com.moviebooking.repository;

import com.moviebooking.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByHallIdOrderByRowNumAscColNumAsc(Long hallId);

    @Query("SELECT os.seatId FROM OrderSeat os WHERE os.orderId IN (SELECT o.id FROM Order o WHERE o.showtimeId = :showtimeId AND o.status = 'paid')")
    List<Long> findSoldSeatIdsByShowtimeId(@Param("showtimeId") Long showtimeId);
}
