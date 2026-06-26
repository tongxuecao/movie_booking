package com.moviebooking.controller;

import com.moviebooking.common.ApiResult;
import com.moviebooking.service.BoxOfficeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/box-office")
public class BoxOfficeController {

    private final BoxOfficeService boxOfficeService;

    @Autowired
    public BoxOfficeController(BoxOfficeService boxOfficeService) {
        this.boxOfficeService = boxOfficeService;
    }

    @GetMapping("/today")
    public ApiResult<?> getTodayBoxOffice() {
        return ApiResult.success(boxOfficeService.getTodayBoxOffice());
    }
}
