package com.moviebooking.controller;

import com.moviebooking.common.ApiResult;
import com.moviebooking.dto.*;
import com.moviebooking.entity.enums.UserRole;
import com.moviebooking.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
public class AdminController {

    private final UserService userService;
    private final MovieService movieService;
    private final CinemaService cinemaService;
    private final ShowtimeService showtimeService;
    private final OrderService orderService;
    private final AdminService adminService;

    @Autowired
    public AdminController(UserService userService, MovieService movieService,
                           CinemaService cinemaService, ShowtimeService showtimeService,
                           OrderService orderService, AdminService adminService) {
        this.userService = userService;
        this.movieService = movieService;
        this.cinemaService = cinemaService;
        this.showtimeService = showtimeService;
        this.orderService = orderService;
        this.adminService = adminService;
    }

    // --- 管理员登录 ---

    @PostMapping("/login")
    public ApiResult<?> adminLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResult.success("登录成功", userService.login(request, UserRole.admin));
    }

    // --- 电影管理 ---

    @PostMapping("/movie")
    public ApiResult<?> createMovie(@RequestBody MovieRequest request) {
        return ApiResult.success("添加成功", movieService.createMovie(request));
    }

    @PutMapping("/movie/{id}")
    public ApiResult<?> updateMovie(@PathVariable Long id, @RequestBody MovieRequest request) {
        movieService.updateMovie(id, request);
        return ApiResult.success("修改成功");
    }

    @DeleteMapping("/movie/{id}")
    public ApiResult<?> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ApiResult.success("删除成功");
    }

    // --- 影院管理 ---

    @PostMapping("/cinema")
    public ApiResult<?> createCinema(@RequestBody CinemaRequest request) {
        return ApiResult.success("添加成功", cinemaService.createCinema(request));
    }

    @GetMapping("/cinema/list")
    public ApiResult<?> getAdminCinemaList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.success(cinemaService.getCinemaList(page, size));
    }

    @GetMapping("/cinema/{id}/halls")
    public ApiResult<?> getCinemaHalls(@PathVariable Long id) {
        return ApiResult.success(cinemaService.getCinemaHalls(id));
    }

    @PostMapping("/hall")
    public ApiResult<?> createHall(@RequestBody HallRequest request) {
        return ApiResult.success("添加成功", cinemaService.createHall(request));
    }

    // --- 排片管理 ---

    @PostMapping("/showtime")
    public ApiResult<?> createShowtime(@RequestBody ShowtimeRequest request) {
        return ApiResult.success("添加成功", showtimeService.createShowtime(request));
    }

    @PostMapping("/showtime/batch")
    public ApiResult<?> batchCreateShowtime(@RequestBody BatchShowtimeRequest request) {
        return ApiResult.success("批量添加成功", showtimeService.batchCreateShowtime(request));
    }

    @GetMapping("/showtime/list")
    public ApiResult<?> getAdminShowtimeList(
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) String date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.success(showtimeService.getAdminShowtimeList(movieId, date, page, size));
    }

    @DeleteMapping("/showtime/{id}")
    public ApiResult<?> deleteShowtime(@PathVariable Long id) {
        showtimeService.deleteShowtime(id);
        return ApiResult.success("删除成功");
    }

    // --- 订单管理 ---

    @GetMapping("/order/list")
    public ApiResult<?> getAdminOrderList(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.success(orderService.getAdminOrderList(status, page, size));
    }

    @GetMapping("/order/{orderNo}")
    public ApiResult<?> getAdminOrderDetail(@PathVariable String orderNo) {
        return ApiResult.success(orderService.getAdminOrderDetail(orderNo));
    }

    // --- 用户管理 ---

    @GetMapping("/user/list")
    public ApiResult<?> getUserList(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResult.success(adminService.getUserList(keyword, page, size));
    }

    @GetMapping("/user/{id}")
    public ApiResult<?> getUserDetail(@PathVariable Long id) {
        return ApiResult.success(adminService.getUserDetail(id));
    }

    @PutMapping("/user/{id}")
    public ApiResult<?> updateUser(@PathVariable Long id, @RequestBody UserUpdateRequest request) {
        adminService.updateUser(id, request);
        return ApiResult.success("修改成功");
    }

    @PutMapping("/user/{id}/status")
    public ApiResult<?> toggleUserStatus(@PathVariable Long id) {
        adminService.toggleUserStatus(id);
        return ApiResult.success("操作成功");
    }

    // --- 数据统计 ---

    @GetMapping("/statistics")
    public ApiResult<?> getStatistics() {
        return ApiResult.success(adminService.getStatistics());
    }
}
