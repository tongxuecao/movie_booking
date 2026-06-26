package com.moviebooking.controller;

import com.moviebooking.common.ApiResult;
import com.moviebooking.dto.LoginRequest;
import com.moviebooking.dto.RegisterRequest;
import com.moviebooking.dto.ChangePasswordRequest;
import com.moviebooking.dto.RechargeRequest;
import com.moviebooking.dto.UpdateProfileRequest;
import com.moviebooking.entity.enums.UserRole;
import com.moviebooking.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResult<?> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResult.success("注册成功", userService.register(request));
    }

    @PostMapping("/login")
    public ApiResult<?> login(@Valid @RequestBody LoginRequest request) {
        return ApiResult.success("登录成功", userService.login(request, UserRole.user));
    }

    @GetMapping("/profile")
    public ApiResult<?> getProfile(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success(userService.getProfile(userId));
    }

    @PutMapping("/profile")
    public ApiResult<?> updateProfile(HttpServletRequest request, @RequestBody UpdateProfileRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        userService.updateProfile(userId, body);
        return ApiResult.success("修改成功");
    }

    @PutMapping("/password")
    public ApiResult<?> changePassword(HttpServletRequest request, @Valid @RequestBody ChangePasswordRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        userService.changePassword(userId, body.getOldPassword(), body.getNewPassword());
        return ApiResult.success("密码修改成功");
    }

    @PostMapping("/recharge")
    public ApiResult<?> recharge(HttpServletRequest request, @Valid @RequestBody RechargeRequest body) {
        Long userId = (Long) request.getAttribute("userId");
        return ApiResult.success("充值成功", userService.recharge(userId, body.getAmount(), body.getPassword()));
    }
}
