package com.moviebooking.repository;

import com.moviebooking.entity.MovieImage;
import com.moviebooking.entity.enums.ImageType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MovieImageRepository extends JpaRepository<MovieImage, Long> {
    List<MovieImage> findByMovieIdOrderBySortOrder(Long movieId);
    List<MovieImage> findByMovieIdAndImageTypeOrderBySortOrderAsc(Long movieId, ImageType imageType);
    MovieImage findByMovieIdAndIsCoverTrue(Long movieId);

    @Modifying
    @Query("DELETE FROM MovieImage mi WHERE mi.movieId = :movieId AND mi.imageType = :imageType")
    void deleteByMovieIdAndImageType(@Param("movieId") Long movieId, @Param("imageType") ImageType imageType);
}
