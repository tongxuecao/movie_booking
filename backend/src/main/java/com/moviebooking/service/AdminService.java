package com.moviebooking.service;

import com.moviebooking.entity.enums.MovieStatus;
import com.moviebooking.entity.enums.OrderStatus;
import com.moviebooking.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
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

        long todayOrderCount = orderRepository.countPaidOrdersSince(todayStart);
        BigDecimal todayRevenue = orderRepository.sumRevenueSince(todayStart);
        long totalUsers = userRepository.count();
        long totalMovies = movieRepository.count();

        // 最近7天数据
        List<Map<String, Object>> recent7Days = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = date.plusDays(1).atStartOfDay();

            long count = orderRepository.countPaidOrdersSince(start) - orderRepository.countPaidOrdersSince(end);
            BigDecimal revenue = orderRepository.sumRevenueSince(start).subtract(orderRepository.sumRevenueSince(end));

            Map<String, Object> day = new HashMap<>();
            day.put("date", date.toString());
            day.put("orderCount", Math.max(0, count));
            day.put("revenue", revenue.max(BigDecimal.ZERO));
            recent7Days.add(day);
        }

        // 热门电影（简化：查最近订单最多的电影）
        List<Map<String, Object>> topMovies = new ArrayList<>();
        // 使用简化查询
        try {
            var topShowtimes = orderRepository.findTopShowtimes(org.springframework.data.domain.PageRequest.of(0, 5));
            for (var row : topShowtimes) {
                Long showtimeId = (Long) row[0];
                Long count = (Long) row[1];
                BigDecimal revenue = (BigDecimal) row[2];
                // 简化：直接用 showtimeId 查 movie title
                Map<String, Object> movie = new HashMap<>();
                movie.put("movieId", showtimeId);
                movie.put("title", "电影" + showtimeId);
                movie.put("orderCount", count);
                movie.put("revenue", revenue);
                topMovies.add(movie);
            }
        } catch (Exception e) {
            // ignore if no data
        }

        Map<String, Object> result = new HashMap<>();
        result.put("todayOrderCount", todayOrderCount);
        result.put("todayRevenue", todayRevenue);
        result.put("totalUsers", totalUsers);
        result.put("totalMovies", totalMovies);
        result.put("recent7Days", recent7Days);
        result.put("topMovies", topMovies);
        return result;
    }
}
