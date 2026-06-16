package com.moviebooking.controller;

import com.moviebooking.common.ApiResult;
import com.moviebooking.service.ShowtimeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/showtime")
public class ShowtimeController {

    private final ShowtimeService showtimeService;

    @Autowired
    public ShowtimeController(ShowtimeService showtimeService) {
        this.showtimeService = showtimeService;
    }

    @GetMapping("/list")
    public ApiResult<?> getShowtimeList(
            @RequestParam Long movieId,
            @RequestParam(required = false) Long cinemaId,
            @RequestParam(required = false) String date) {
        return ApiResult.success(showtimeService.getShowtimeList(movieId, cinemaId, date));
    }

    @GetMapping("/{id}/seats")
    public ApiResult<?> getSeatMap(@PathVariable Long id) {
        return ApiResult.success(showtimeService.getSeatMap(id));
    }
}
