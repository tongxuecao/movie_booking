package com.moviebooking.service;

import com.moviebooking.common.BusinessException;
import com.moviebooking.dto.LoginRequest;
import com.moviebooking.dto.RegisterRequest;
import com.moviebooking.dto.UpdateProfileRequest;
import com.moviebooking.entity.User;
import com.moviebooking.entity.enums.UserRole;
import com.moviebooking.entity.enums.UserStatus;
import com.moviebooking.repository.UserRepository;
import com.moviebooking.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public UserService(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    public Map<String, Object> register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw BusinessException.badRequest("用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setRole(UserRole.user);
        user = userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("phone", maskPhone(user.getPhone()));
        result.put("walletBalance", user.getWalletBalance());
        result.put("createdAt", user.getCreatedAt());
        return result;
    }

    public Map<String, Object> login(LoginRequest request, UserRole requiredRole) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> BusinessException.badRequest("用户名或密码错误"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw BusinessException.badRequest("用户名或密码错误");
        }

        if (requiredRole == UserRole.admin && user.getRole() != UserRole.admin) {
            throw BusinessException.badRequest("无管理员权限");
        }

        if (user.getStatus() == UserStatus.disabled) {
            throw BusinessException.badRequest("账号已被禁用");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole().name());

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("role", user.getRole().name());
        userInfo.put("phone", maskPhone(user.getPhone()));
        userInfo.put("walletBalance", user.getWalletBalance());
        userInfo.put("avatar", user.getAvatar());

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("userInfo", userInfo);
        return result;
    }

    public Map<String, Object> getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("role", user.getRole().name());
        result.put("phone", maskPhone(user.getPhone()));
        result.put("walletBalance", user.getWalletBalance());
        result.put("avatar", user.getAvatar());
        result.put("createdAt", user.getCreatedAt());
        return result;
    }

    public void updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        if (request.getUsername() != null && !request.getUsername().isBlank()) {
            String newName = request.getUsername().trim();
            if (!newName.equals(user.getUsername()) && userRepository.existsByUsername(newName)) {
                throw BusinessException.badRequest("用户名已被占用");
            }
            user.setUsername(newName);
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }
        userRepository.save(user);
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw BusinessException.badRequest("旧密码错误");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    public Map<String, Object> recharge(Long userId, BigDecimal amount, String password) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BusinessException.badRequest("充值金额必须大于0");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw BusinessException.badRequest("密码错误");
        }

        // 充值
        userRepository.addBalance(userId, amount);

        // 获取更新后的余额
        User updatedUser = userRepository.findById(userId).orElseThrow();
        Map<String, Object> result = new HashMap<>();
        result.put("walletBalance", updatedUser.getWalletBalance());
        result.put("rechargeAmount", amount);
        return result;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
