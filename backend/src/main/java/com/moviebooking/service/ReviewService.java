package com.moviebooking.service;

import com.moviebooking.common.BusinessException;
import com.moviebooking.common.PageResult;
import com.moviebooking.dto.ReviewRequest;
import com.moviebooking.entity.Movie;
import com.moviebooking.entity.Review;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.OrderRepository;
import com.moviebooking.repository.ReviewRepository;
import com.moviebooking.entity.enums.OrderStatus;
import com.moviebooking.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

@Service
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final MovieRepository movieRepository;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository, UserRepository userRepository,
                        OrderRepository orderRepository, MovieRepository movieRepository) {
        this.reviewRepository = reviewRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.movieRepository = movieRepository;
    }

    public Map<String, Object> createReview(Long userId, ReviewRequest request) {
        // 验证用户是否看过这部电影（使用原生SQL，避免JPQL枚举问题）
        boolean hasWatched = orderRepository.existsByUserIdAndMovieIdAndPaidNative(userId, request.getMovieId()) > 0;
        if (!hasWatched) {
            throw new BusinessException("只有看过这部电影的用户才能评分");
        }

        // 检查是否已评分
        Optional<Review> existingReview = reviewRepository.findByUserIdAndMovieId(userId, request.getMovieId());

        Review review;
        if (existingReview.isPresent()) {
            // 更新评分
            review = existingReview.get();
            review.setRating(request.getRating());
            review.setContent(request.getContent());
        } else {
            // 创建新评分
            review = new Review();
            review.setUserId(userId);
            review.setMovieId(request.getMovieId());
            // orderId 可选，如果有则设置
            if (request.getOrderId() != null) {
                review.setOrderId(request.getOrderId());
            }
            review.setRating(request.getRating());
            review.setContent(request.getContent());
        }

        review = reviewRepository.save(review);

        // 更新Movie表的平均评分
        updateMovieRating(request.getMovieId());

        return Map.of("id", review.getId(), "rating", review.getRating());
    }

    /**
     * 查询用户评分状态
     */
    public Map<String, Object> getReviewStatus(Long userId, Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException("电影不存在"));

        boolean canReview = false;
        boolean hasReviewed = false;
        Integer userRating = null;

        if (userId != null) {
            // 检查用户是否看过这部电影（使用原生SQL，避免JPQL枚举问题）
            canReview = orderRepository.existsByUserIdAndMovieIdAndPaidNative(userId, movieId) > 0;
            log.info("[ReviewStatus] userId={}, movieId={}, canReview={}", userId, movieId, canReview);

            // 检查用户是否已评分
            Optional<Review> existingReview = reviewRepository.findByUserIdAndMovieId(userId, movieId);
            if (existingReview.isPresent()) {
                hasReviewed = true;
                userRating = existingReview.get().getRating();
            }
        }

        return Map.of(
                "canReview", canReview,
                "hasReviewed", hasReviewed,
                "userRating", userRating,
                "averageRating", movie.getRating() != null ? movie.getRating() : null,
                "ratingCount", movie.getRatingCount() != null ? movie.getRatingCount() : 0
        );
    }

    /**
     * 更新Movie表的平均评分
     */
    private void updateMovieRating(Long movieId) {
        Object[] result = reviewRepository.getAverageRatingAndCount(movieId);
        if (result != null && result[0] != null) {
            Movie movie = movieRepository.findById(movieId).orElse(null);
            if (movie != null) {
                movie.setRating(new java.math.BigDecimal(result[0].toString()));
                movie.setRatingCount((int) result[1]);
                movieRepository.save(movie);
            }
        }
    }

    public PageResult<Map<String, Object>> getReviewList(Long movieId, int page, int size) {
        Page<Review> reviewPage = reviewRepository.findByMovieId(movieId, PageRequest.of(page - 1, size, Sort.by("createdAt").descending()));

        var list = reviewPage.getContent().stream().map(r -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.getId());
            map.put("userId", r.getUserId());
            var user = userRepository.findById(r.getUserId()).orElse(null);
            map.put("username", user != null ? user.getUsername() : "未知用户");
            map.put("avatar", user != null ? user.getAvatar() : null);
            map.put("rating", r.getRating());
            map.put("content", r.getContent());
            map.put("createdAt", r.getCreatedAt());
            return map;
        }).toList();

        return new PageResult<>(list, reviewPage.getTotalElements(), page, size);
    }
}
