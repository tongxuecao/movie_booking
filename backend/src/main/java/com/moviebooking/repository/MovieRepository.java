package com.moviebooking.repository;

import com.moviebooking.entity.Movie;
import com.moviebooking.entity.enums.MovieStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Page<Movie> findByStatus(MovieStatus status, Pageable pageable);

    java.util.List<Movie> findByStatus(MovieStatus status);

    @Query("SELECT m FROM Movie m WHERE (:status IS NULL OR m.status = :status) AND (:keyword IS NULL OR m.title LIKE %:keyword% OR m.director LIKE %:keyword% OR m.actors LIKE %:keyword% OR m.genre LIKE %:keyword%)")
    Page<Movie> findByStatusAndKeyword(@Param("status") MovieStatus status, @Param("keyword") String keyword, Pageable pageable);

    long countByStatus(MovieStatus status);

    @Query("SELECT m FROM Movie m WHERE m.status = :status ORDER BY m.wishCount DESC")
    java.util.List<Movie> findTopByStatusOrderByWishCountDesc(@Param("status") MovieStatus status, Pageable pageable);

    @Query("SELECT m FROM Movie m ORDER BY m.wishCount DESC")
    java.util.List<Movie> findTopByOrderByWishCountDesc(Pageable pageable);
}
