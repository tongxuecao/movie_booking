package com.moviebooking.repository;

import com.moviebooking.entity.MovieImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovieImageRepository extends JpaRepository<MovieImage, Long> {
    List<MovieImage> findByMovieIdOrderBySortOrder(Long movieId);
    MovieImage findByMovieIdAndIsCoverTrue(Long movieId);
}
