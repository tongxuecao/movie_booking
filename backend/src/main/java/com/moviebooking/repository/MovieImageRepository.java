package com.moviebooking.repository;

import com.moviebooking.entity.MovieImage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MovieImageRepository extends JpaRepository<MovieImage, Long> {
    List<MovieImage> findByMovieIdOrderBySortOrder(Long movieId);
    List<MovieImage> findByMovieIdAndImageTypeOrderBySortOrderAsc(Long movieId, com.moviebooking.entity.enums.ImageType imageType);
    MovieImage findByMovieIdAndIsCoverTrue(Long movieId);
    void deleteByMovieIdAndImageType(Long movieId, com.moviebooking.entity.enums.ImageType imageType);
}
