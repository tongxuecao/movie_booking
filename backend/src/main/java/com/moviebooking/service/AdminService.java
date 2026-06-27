package com.moviebooking.service;

import com.moviebooking.entity.enums.MovieStatus;
import com.moviebooking.entity.enums.OrderStatus;
import com.moviebooking.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
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
}
