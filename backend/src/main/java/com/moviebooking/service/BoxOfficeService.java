package com.moviebooking.service;

import com.moviebooking.entity.Movie;
import com.moviebooking.entity.Order;
import com.moviebooking.entity.Showtime;
import com.moviebooking.entity.enums.OrderStatus;
import com.moviebooking.repository.MovieRepository;
import com.moviebooking.repository.OrderRepository;
import com.moviebooking.repository.ShowtimeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BoxOfficeService {

    private static final Logger log = LoggerFactory.getLogger(BoxOfficeService.class);
    private static final String BOX_OFFICE_KEY = "box:office:today";
    private static final String BOX_OFFICE_MOVIES_KEY = "box:office:today:movies";

    private final OrderRepository orderRepository;
    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public BoxOfficeService(OrderRepository orderRepository,
                           ShowtimeRepository showtimeRepository,
                           MovieRepository movieRepository,
                           StringRedisTemplate redisTemplate) {
        this.orderRepository = orderRepository;
        this.showtimeRepository = showtimeRepository;
        this.movieRepository = movieRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取今日票房数据
     */
    public Map<String, Object> getTodayBoxOffice() {
        // 先从Redis缓存获取
        String totalRevenue = redisTemplate.opsForValue().get(BOX_OFFICE_KEY);
        String moviesJson = redisTemplate.opsForValue().get(BOX_OFFICE_MOVIES_KEY);

        if (totalRevenue != null && moviesJson != null) {
            Map<String, Object> result = new HashMap<>();
            result.put("totalRevenue", new BigDecimal(totalRevenue));
            result.put("movies", parseMoviesJson(moviesJson));
            return result;
        }

        // 缓存未命中，计算并缓存
        return calculateAndCacheBoxOffice();
    }

    /**
     * 计算今日票房并缓存
     */
    private Map<String, Object> calculateAndCacheBoxOffice() {
        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.of(LocalDate.now(), LocalTime.MAX);

        // 查询今日已支付订单
        List<Order> todayOrders = orderRepository.findByStatus(OrderStatus.paid, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .filter(order -> order.getCreatedAt().isAfter(todayStart) && order.getCreatedAt().isBefore(todayEnd))
                .collect(Collectors.toList());

        // 计算总票房
        BigDecimal totalRevenue = todayOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 按电影统计票房
        Map<Long, BigDecimal> movieRevenueMap = new HashMap<>();
        Map<Long, Integer> movieTicketCountMap = new HashMap<>();

        for (Order order : todayOrders) {
            Showtime showtime = showtimeRepository.findById(order.getShowtimeId()).orElse(null);
            if (showtime != null) {
                Long movieId = showtime.getMovieId();
                movieRevenueMap.merge(movieId, order.getTotalAmount(), BigDecimal::add);
                movieTicketCountMap.merge(movieId, 1, Integer::sum);
            }
        }

        // 构建电影票房列表
        List<Map<String, Object>> movieBoxOfficeList = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : movieRevenueMap.entrySet()) {
            Long movieId = entry.getKey();
            Movie movie = movieRepository.findById(movieId).orElse(null);
            if (movie != null) {
                Map<String, Object> movieInfo = new HashMap<>();
                movieInfo.put("movieId", movieId);
                movieInfo.put("title", movie.getTitle());
                movieInfo.put("revenue", entry.getValue());
                movieInfo.put("ticketCount", movieTicketCountMap.getOrDefault(movieId, 0));
                movieBoxOfficeList.add(movieInfo);
            }
        }

        // 按票房排序
        movieBoxOfficeList.sort((a, b) -> {
            BigDecimal revenueA = (BigDecimal) a.get("revenue");
            BigDecimal revenueB = (BigDecimal) b.get("revenue");
            return revenueB.compareTo(revenueA);
        });

        // 取前10名
        if (movieBoxOfficeList.size() > 10) {
            movieBoxOfficeList = movieBoxOfficeList.subList(0, 10);
        }

        // 缓存到Redis（5分钟过期）
        redisTemplate.opsForValue().set(BOX_OFFICE_KEY, totalRevenue.toString());
        redisTemplate.opsForValue().set(BOX_OFFICE_MOVIES_KEY, moviesToJson(movieBoxOfficeList));

        Map<String, Object> result = new HashMap<>();
        result.put("totalRevenue", totalRevenue);
        result.put("movies", movieBoxOfficeList);
        return result;
    }

    /**
     * 定时任务：每5分钟更新今日票房缓存
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void syncTodayBoxOffice() {
        log.info("开始同步今日票房数据...");
        try {
            calculateAndCacheBoxOffice();
            log.info("今日票房数据同步完成");
        } catch (Exception e) {
            log.error("今日票房数据同步失败", e);
        }
    }

    private String moviesToJson(List<Map<String, Object>> movies) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < movies.size(); i++) {
            Map<String, Object> movie = movies.get(i);
            if (i > 0) sb.append(",");
            sb.append("{\"movieId\":").append(movie.get("movieId"))
              .append(",\"title\":\"").append(movie.get("title")).append("\"")
              .append(",\"revenue\":").append(movie.get("revenue"))
              .append(",\"ticketCount\":").append(movie.get("ticketCount"))
              .append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private List<Map<String, Object>> parseMoviesJson(String json) {
        // 简单解析JSON，实际项目中应使用Jackson
        List<Map<String, Object>> movies = new ArrayList<>();
        if (json == null || json.equals("[]")) return movies;

        // 移除方括号
        String content = json.substring(1, json.length() - 1);
        if (content.isEmpty()) return movies;

        // 分割每个电影对象
        String[] movieObjects = content.split("\\},\\{");
        for (String movieObj : movieObjects) {
            // 清理花括号
            movieObj = movieObj.replace("{", "").replace("}", "");
            Map<String, Object> movie = new HashMap<>();

            // 解析键值对
            String[] pairs = movieObj.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String value = kv[1].trim().replace("\"", "");
                    if (key.equals("movieId") || key.equals("ticketCount")) {
                        movie.put(key, Integer.parseInt(value));
                    } else if (key.equals("revenue")) {
                        movie.put(key, new BigDecimal(value));
                    } else {
                        movie.put(key, value);
                    }
                }
            }
            movies.add(movie);
        }
        return movies;
    }
}
