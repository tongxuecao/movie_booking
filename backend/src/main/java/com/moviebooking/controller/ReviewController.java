package com.moviebooking.controller;

import com.moviebooking.common.ApiResult;
import com.moviebooking.dto.ReviewRequest;
import com.moviebooking.service.ReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/review")
public class ReviewController {

    private final ReviewService reviewService;

    @Autowired
    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ApiResult<?> createReview(HttpServletRequest request, @Valid @RequestBody ReviewRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(reviewService.createReview(userId, body));
    }

    @GetMapping("/list")
    public ApiResult<?> getReviewList(
            @RequestParam Long movieId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.success(reviewService.getReviewList(movieId, page, size));
    }
}
