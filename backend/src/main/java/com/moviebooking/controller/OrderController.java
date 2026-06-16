package com.moviebooking.controller;

import com.moviebooking.common.ApiResult;
import com.moviebooking.dto.CreateOrderRequest;
import com.moviebooking.dto.LockSeatsRequest;
import com.moviebooking.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderService orderService;

    @Autowired
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/lock")
    public ApiResult<?> lockSeats(HttpServletRequest request, @Valid @RequestBody LockSeatsRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success("锁定成功", orderService.lockSeats(userId, body));
    }

    @PostMapping("/create")
    public ApiResult<?> createOrder(HttpServletRequest request, @Valid @RequestBody CreateOrderRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success("订单提交成功，正在排队处理", orderService.createOrder(userId, body));
    }

    @GetMapping("/status/{orderNo}")
    public ApiResult<?> getOrderStatus(HttpServletRequest request, @PathVariable String orderNo) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(orderService.getOrderStatus(orderNo, userId));
    }

    @GetMapping("/{orderNo}")
    public ApiResult<?> getOrderDetail(HttpServletRequest request, @PathVariable String orderNo) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(orderService.getOrderDetail(orderNo, userId));
    }

    @GetMapping("/list")
    public ApiResult<?> getOrderList(HttpServletRequest request,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(orderService.getOrderList(userId, status, page, size));
    }

    @PostMapping("/cancel/{orderNo}")
    public ApiResult<?> cancelOrder(HttpServletRequest request, @PathVariable String orderNo) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success("退票成功，退款已到账", orderService.cancelOrder(orderNo, userId));
    }
}
