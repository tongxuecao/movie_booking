package com.moviebooking.controller;

import com.moviebooking.common.ApiResult;
import com.moviebooking.service.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/movie")
public class MovieController {

    private final MovieService movieService;

    @Autowired
    public MovieController(MovieService movieService) {
        this.movieService = movieService;
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
}
