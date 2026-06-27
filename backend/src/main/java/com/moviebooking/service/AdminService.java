package com.moviebooking.service;

import com.moviebooking.common.BusinessException;
import com.moviebooking.dto.UserUpdateRequest;
import com.moviebooking.entity.User;
import com.moviebooking.entity.enums.MovieStatus;
import com.moviebooking.entity.enums.OrderStatus;
import com.moviebooking.entity.enums.UserRole;
import com.moviebooking.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AdminService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final MovieRepository movieRepository;

    @Autowired
    public AdminService(OrderRepository orderRepository, UserRepository userRepository, MovieRepository movieRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.movieRepository = movieRepository;
    }

    public Map<String, Object> getStatistics() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();

        long todayOrderCount = orderRepository.countPaidOrdersSince(todayStart, OrderStatus.paid);
        BigDecimal todayRevenue = orderRepository.sumRevenueSince(todayStart, OrderStatus.paid);
        long totalUsers = userRepository.count();
        long totalMovies = movieRepository.count();
        BigDecimal totalBoxOffice = orderRepository.sumAllRevenue(OrderStatus.paid);

        // 最近7天数据
        List<Map<String, Object>> recent7Days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            long count = orderRepository.countPaidOrdersSince(start, OrderStatus.paid) - orderRepository.countPaidOrdersSince(end, OrderStatus.paid);
            BigDecimal revenue = orderRepository.sumRevenueSince(start, OrderStatus.paid).subtract(orderRepository.sumRevenueSince(end, OrderStatus.paid));

            Map<String, Object> day = new HashMap<>();
            day.put("date", date.toString());
            day.put("orderCount", Math.max(0, count));
            day.put("revenue", revenue.max(BigDecimal.ZERO));
            recent7Days.add(day);
        }

        // 电影票房前10排行
        List<Map<String, Object>> topMoviesByRevenue = new ArrayList<>();
        try {
            var rows = orderRepository.findTopMoviesByRevenue(PageRequest.of(0, 10));
            for (var row : rows) {
                Long movieId = ((Number) row[0]).longValue();
                Long count = ((Number) row[1]).longValue();
                BigDecimal revenue = (BigDecimal) row[2];
                String title = "电影" + movieId;
                var movie = movieRepository.findById(movieId);
                if (movie.isPresent()) {
                    title = movie.get().getTitle();
                }
                Map<String, Object> item = new HashMap<>();
                item.put("movieId", movieId);
                item.put("title", title);
                item.put("orderCount", count);
                item.put("revenue", revenue);
                topMoviesByRevenue.add(item);
            }
        } catch (Exception e) {
            // ignore if no data
        }

        // 电影想看前10排行
        List<Map<String, Object>> topMoviesByWishCount = new ArrayList<>();
        try {
            var movies = movieRepository.findTopByOrderByWishCountDesc(PageRequest.of(0, 10));
            for (var movie : movies) {
                Map<String, Object> item = new HashMap<>();
                item.put("movieId", movie.getId());
                item.put("title", movie.getTitle());
                item.put("wishCount", movie.getWishCount());
                topMoviesByWishCount.add(item);
            }
        } catch (Exception e) {
            // ignore if no data
        }

        Map<String, Object> result = new HashMap<>();
        result.put("todayOrderCount", todayOrderCount);
        result.put("todayRevenue", todayRevenue);
        result.put("totalUsers", totalUsers);
        result.put("totalMovies", totalMovies);
        result.put("totalBoxOffice", totalBoxOffice);
        result.put("recent7Days", recent7Days);
        result.put("topMoviesByRevenue", topMoviesByRevenue);
        result.put("topMoviesByWishCount", topMoviesByWishCount);
        return result;
    }

    // ==================== 用户管理 ====================

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public Map<String, Object> getUserList(String keyword, int page, int size) {
        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<User> userPage = userRepository.searchUsers(kw, PageRequest.of(page - 1, size));

        List<Map<String, Object>> list = new ArrayList<>();
        for (User u : userPage.getContent()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("username", u.getUsername());
            item.put("phone", maskPhone(u.getPhone()));
            item.put("role", u.getRole().name());
            item.put("walletBalance", u.getWalletBalance());
            item.put("avatar", u.getAvatar());
            item.put("createdAt", u.getCreatedAt());
            list.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", userPage.getTotalElements());
        result.put("page", page);
        result.put("size", size);
        return result;
    }

    public Map<String, Object> getUserDetail(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        Map<String, Object> detail = new HashMap<>();
        detail.put("id", u.getId());
        detail.put("username", u.getUsername());
        detail.put("phone", u.getPhone());
        detail.put("role", u.getRole().name());
        detail.put("walletBalance", u.getWalletBalance());
        detail.put("avatar", u.getAvatar());
        detail.put("version", u.getVersion());
        detail.put("createdAt", u.getCreatedAt());
        detail.put("updatedAt", u.getUpdatedAt());
        return detail;
    }

    public void updateUser(Long userId, UserUpdateRequest req) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> BusinessException.notFound("用户不存在"));

        if (req.getUsername() != null && !req.getUsername().isBlank()) {
            String newName = req.getUsername().trim();
            if (!newName.equals(u.getUsername()) && userRepository.existsByUsername(newName)) {
                throw BusinessException.badRequest("用户名已被占用");
            }
            u.setUsername(newName);
        }
        if (req.getPhone() != null) {
            u.setPhone(req.getPhone().trim());
        }
        if (req.getRole() != null) {
            try {
                u.setRole(UserRole.valueOf(req.getRole()));
            } catch (IllegalArgumentException e) {
                throw BusinessException.badRequest("无效的角色: " + req.getRole());
            }
        }
        if (req.getWalletBalance() != null) {
            if (req.getWalletBalance().compareTo(BigDecimal.ZERO) < 0) {
                throw BusinessException.badRequest("余额不能为负数");
            }
            u.setWalletBalance(req.getWalletBalance());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        userRepository.save(u);
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
