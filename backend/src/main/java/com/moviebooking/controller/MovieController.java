package com.moviebooking.controller;

import com.moviebooking.common.ApiResult;
import com.moviebooking.service.MovieService;
import com.moviebooking.service.WishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/movie")
public class MovieController {

    private final MovieService movieService;
    private final WishService wishService;

    @Autowired
    public MovieController(MovieService movieService, WishService wishService) {
        this.movieService = movieService;
        this.wishService = wishService;
    }

    @GetMapping("/list")
    public ApiResult<?> getMovieList(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.success(movieService.getMovieList(status, keyword, page, size));
    }

    @GetMapping("/{id}")
    public ApiResult<?> getMovieDetail(@PathVariable Long id) {
        return ApiResult.success(movieService.getMovieDetail(id));
    }

    @GetMapping("/most-expected")
    public ApiResult<?> getMostExpectedMovies(@RequestParam(defaultValue = "5") int limit) {
        return ApiResult.success(wishService.getMostExpectedMovies(limit));
    }

    @PostMapping("/{id}/wish")
    public ApiResult<?> toggleWish(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return ApiResult.error(401, "请先登录");
        }
        return ApiResult.success(wishService.toggleWish(userId, id));
    }

    @GetMapping("/{id}/wish-status")
    public ApiResult<?> getWishStatus(@PathVariable Long id, jakarta.servlet.http.HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(wishService.getWishStatus(userId, id));
    }
}
