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
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class BoxOfficeService {

    private static final Logger log = LoggerFactory.getLogger(BoxOfficeService.class);
    private static final String BOX_OFFICE_KEY = "box:office:today";
    private static final String BOX_OFFICE_MOVIES_KEY = "box:office:today:movies";
    private static final String BOX_OFFICE_CUMULATIVE_KEY = "box:office:cumulative";
    private static final String BOX_OFFICE_CUMULATIVE_MOVIES_KEY = "box:office:cumulative:movies";

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

    @PostConstruct
    public void init() {
        // 启动时清除旧版无TTL缓存，避免脏数据
        clearCache();
        log.info("票房缓存已初始化");
    }

    /**
     * 获取今日票房排行数据（仅统计今日创建的已支付订单）
     */
    public Map<String, Object> getTodayBoxOffice() {
        // 先从Redis缓存获取
        String totalRevenue = redisTemplate.opsForValue().get(BOX_OFFICE_KEY);
        String moviesJson = redisTemplate.opsForValue().get(BOX_OFFICE_MOVIES_KEY);

        if (totalRevenue != null && moviesJson != null) {
            log.info("票房缓存命中: totalRevenue={}, movies={}", totalRevenue, moviesJson);
            Map<String, Object> result = new HashMap<>();
            result.put("totalRevenue", new BigDecimal(totalRevenue));
            result.put("movies", parseMoviesJson(moviesJson));
            return result;
        }

        log.info("票房缓存未命中，重新计算");
        // 缓存未命中，计算并缓存
        return calculateAndCacheBoxOffice();
    }

    /**
     * 计算今日票房排行并缓存（仅统计今日创建的已支付订单，排除退款和已删除电影）
     */
    private Map<String, Object> calculateAndCacheBoxOffice() {
        LocalDate today = LocalDate.now();

        List<Order> todayPaidOrders = orderRepository.findTodayPaidOrders(
                OrderStatus.paid, today);
        log.info("今日票房查询: date={}, 查到{}笔paid订单", today, todayPaidOrders.size());

        Map<String, Object> result = aggregateByMovie(todayPaidOrders);

        redisTemplate.opsForValue().set(BOX_OFFICE_KEY,
                result.get("totalRevenue").toString(), 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(BOX_OFFICE_MOVIES_KEY,
                moviesToJson((List<Map<String, Object>>) result.get("movies")), 5, TimeUnit.MINUTES);

        return result;
    }

    /**
     * 按电影聚合订单票房（公共方法，今日和累计复用）
     */
    private Map<String, Object> aggregateByMovie(List<Order> orders) {
        Map<Long, BigDecimal> movieRevenueMap = new HashMap<>();
        Map<Long, Integer> movieTicketCountMap = new HashMap<>();
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (Order order : orders) {
            Showtime showtime = showtimeRepository.findById(order.getShowtimeId()).orElse(null);
            if (showtime == null) continue;
            Movie movie = movieRepository.findById(showtime.getMovieId()).orElse(null);
            if (movie == null) continue;
            totalRevenue = totalRevenue.add(order.getTotalAmount());
            movieRevenueMap.merge(showtime.getMovieId(), order.getTotalAmount(), BigDecimal::add);
            movieTicketCountMap.merge(showtime.getMovieId(), 1, Integer::sum);
        }

        List<Map<String, Object>> movieBoxOfficeList = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : movieRevenueMap.entrySet()) {
            Long movieId = entry.getKey();
            Movie movie = movieRepository.findById(movieId).orElse(null);
            if (movie != null) {
                Map<String, Object> movieInfo = new HashMap<>();
                movieInfo.put("movieId", movieId);
                movieInfo.put("title", movie.getTitle());
                movieInfo.put("poster", movie.getPoster());
                movieInfo.put("revenue", entry.getValue());
                movieInfo.put("ticketCount", movieTicketCountMap.getOrDefault(movieId, 0));
                movieBoxOfficeList.add(movieInfo);
            }
        }

        movieBoxOfficeList.sort((a, b) -> {
            BigDecimal revenueA = (BigDecimal) a.get("revenue");
            BigDecimal revenueB = (BigDecimal) b.get("revenue");
            return revenueB.compareTo(revenueA);
        });

        if (movieBoxOfficeList.size() > 10) {
            movieBoxOfficeList = movieBoxOfficeList.subList(0, 10);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalRevenue", totalRevenue);
        result.put("movies", movieBoxOfficeList);
        return result;
    }

    /**
     * 获取累计票房排行（上映至今已放映场次，不含预售）
     */
    public Map<String, Object> getCumulativeBoxOffice() {
        String totalRevenue = redisTemplate.opsForValue().get(BOX_OFFICE_CUMULATIVE_KEY);
        String moviesJson = redisTemplate.opsForValue().get(BOX_OFFICE_CUMULATIVE_MOVIES_KEY);

        if (totalRevenue != null && moviesJson != null) {
            log.info("累计票房缓存命中: totalRevenue={}", totalRevenue);
            Map<String, Object> result = new HashMap<>();
            result.put("totalRevenue", new BigDecimal(totalRevenue));
            result.put("movies", parseMoviesJson(moviesJson));
            return result;
        }

        log.info("累计票房缓存未命中，重新计算");
        return calculateAndCacheCumulativeBoxOffice();
    }

    private Map<String, Object> calculateAndCacheCumulativeBoxOffice() {
        LocalDate today = LocalDate.now();

        // 上映至今已放映场次的已支付订单（showDate <= 今天，排除未来预售）
        List<Order> cumulativePaidOrders = orderRepository.findCumulativePaidOrders(
                OrderStatus.paid, today);
        log.info("累计票房查询: untilDate={}, 查到{}笔paid订单", today, cumulativePaidOrders.size());

        Map<String, Object> result = aggregateByMovie(cumulativePaidOrders);

        redisTemplate.opsForValue().set(BOX_OFFICE_CUMULATIVE_KEY,
                result.get("totalRevenue").toString(), 5, TimeUnit.MINUTES);
        redisTemplate.opsForValue().set(BOX_OFFICE_CUMULATIVE_MOVIES_KEY,
                moviesToJson((List<Map<String, Object>>) result.get("movies")), 5, TimeUnit.MINUTES);

        return result;
    }

    /**
     * 清除票房缓存（退款后调用，确保数据实时更新）
     */
    public void clearCache() {
        redisTemplate.delete(BOX_OFFICE_KEY);
        redisTemplate.delete(BOX_OFFICE_MOVIES_KEY);
        redisTemplate.delete(BOX_OFFICE_CUMULATIVE_KEY);
        redisTemplate.delete(BOX_OFFICE_CUMULATIVE_MOVIES_KEY);
    }

    /**
     * 定时任务：每5分钟更新票房排行缓存
     */
    @Scheduled(fixedRate = 300000) // 5分钟
    public void syncTodayBoxOffice() {
        log.info("开始同步票房排行数据...");
        try {
            calculateAndCacheBoxOffice();
            log.info("票房排行数据同步完成");
        } catch (Exception e) {
            log.error("票房排行数据同步失败", e);
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
              .append(",\"poster\":\"").append(movie.get("poster") != null ? movie.get("poster") : "").append("\"")
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
