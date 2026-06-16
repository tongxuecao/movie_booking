package com.moviebooking.service;

import com.moviebooking.common.BusinessException;
import com.moviebooking.common.PageResult;
import com.moviebooking.dto.ReviewRequest;
import com.moviebooking.entity.Review;
import com.moviebooking.repository.ReviewRepository;
import com.moviebooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
    }

    public Map<String, Object> createReview(Long userId, ReviewRequest request) {
        Review review = new Review();
        review.setUserId(userId);
        review.setMovieId(request.getMovieId());
        review.setOrderId(request.getOrderId());
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review = reviewRepository.save(review);

        return Map.of("id", review.getId());
    }

    public PageResult<Map<String, Object>> getReviewList(Long movieId, int page, int size) {
        Page<Review> reviewPage = reviewRepository.findByMovieId(movieId, PageRequest.of(page - 1, size, Sort.by("createdAt").descending()));

        var list = reviewPage.getContent().stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("userId", r.getUserId());
            var user = userRepository.findById(r.getUserId()).orElse(null);
            map.put("username", user != null ? user.getUsername() : "未知用户");
            map.put("rating", r.getRating());
            map.put("content", r.getContent());
            map.put("createdAt", r.getCreatedAt());
            return map;
        }).toList();

        return new PageResult<>(list, reviewPage.getTotalElements(), page, size);
    }
}
