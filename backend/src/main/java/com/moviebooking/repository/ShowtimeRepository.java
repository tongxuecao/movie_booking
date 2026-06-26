package com.moviebooking.repository;

import com.moviebooking.entity.Showtime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {
    @Query("SELECT s FROM Showtime s WHERE s.movieId = :movieId AND (:cinemaId IS NULL OR s.hallId IN (SELECT h.id FROM Hall h WHERE h.cinemaId = :cinemaId)) AND (:date IS NULL OR s.showDate = :date) AND s.status = 'normal'")
    List<Showtime> findByMovieIdAndCinemaIdAndDate(@Param("movieId") Long movieId, @Param("cinemaId") Long cinemaId, @Param("date") LocalDate date);

    List<Showtime> findByHallIdAndShowDate(Long hallId, LocalDate showDate);

    @Query("SELECT s FROM Showtime s WHERE (:movieId IS NULL OR s.movieId = :movieId) AND (:date IS NULL OR s.showDate = :date) ORDER BY s.showDate DESC, s.showTime ASC")
    Page<Showtime> findAdminShowtimes(@Param("movieId") Long movieId, @Param("date") LocalDate date, Pageable pageable);
}
