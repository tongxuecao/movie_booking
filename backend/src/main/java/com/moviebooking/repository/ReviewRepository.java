package com.moviebooking.repository;

import com.moviebooking.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    Page<Review> findByMovieId(Long movieId, Pageable pageable);

    Optional<Review> findByUserIdAndMovieId(Long userId, Long movieId);

    @Query("SELECT AVG(r.rating), COUNT(r) FROM Review r WHERE r.movieId = :movieId")
    Object[] getAverageRatingAndCount(@Param("movieId") Long movieId);
}
