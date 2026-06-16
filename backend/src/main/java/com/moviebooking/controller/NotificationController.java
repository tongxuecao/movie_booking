package com.moviebooking.controller;

import com.moviebooking.common.ApiResult;
import com.moviebooking.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")
public class NotificationController {

    private final NotificationService notificationService;

    @Autowired
    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/list")
    public ApiResult<?> getNotificationList(HttpServletRequest request,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(notificationService.getNotificationList(userId, page, size));
    }

    @PutMapping("/{id}/read")
    public ApiResult<?> markAsRead(HttpServletRequest request, @PathVariable Long id) {
        Long userId = (Long) request.getAttribute("userId");
        notificationService.markAsRead(userId, id);
        return ApiResult.success("标记成功");
    }
}
