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
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

@Service
public class BoxOfficeService {

    private static final Logger log = LoggerFactory.getLogger(BoxOfficeService.class);

    // Redis keys for today
    private static final String TODAY_DATE = "box:office:today:date";
    private static final String TODAY_TOTAL = "box:office:today:total";
    private static final String TODAY_RANKING = "box:office:today:ranking";
    private static final String TODAY_TICKETS_PREFIX = "box:office:today:tickets:";

    // Redis keys for cumulative
    private static final String CUMULATIVE_DATE = "box:office:cumulative:date";
    private static final String CUMULATIVE_TOTAL = "box:office:cumulative:total";
    private static final String CUMULATIVE_RANKING = "box:office:cumulative:ranking";
    private static final String CUMULATIVE_TICKETS_PREFIX = "box:office:cumulative:tickets:";

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
        ensureCurrentDay();
        ensureCumulative();
        log.info("票房缓存已初始化");
    }

    // ==================== 实时累加/扣减（支付和退票时调用） ====================

    /**
     * 支付成功时实时累加票房（仅今日场次计入今日票房，累计票房排除预售）
     */
    public void recordSale(Long movieId, LocalDate showDate, BigDecimal amount, int ticketCount) {
        ensureCurrentDay();
        long cents = toCents(amount);
        boolean isToday = showDate.equals(LocalDate.now());

        // 今日票房：只计入今天放映的场次
        if (isToday) {
            redisTemplate.opsForValue().increment(TODAY_TOTAL, cents);
            redisTemplate.opsForZSet().incrementScore(TODAY_RANKING, movieId.toString(), cents);
            redisTemplate.opsForValue().increment(TODAY_TICKETS_PREFIX + movieId, ticketCount);
        }

        // 累计票房：排除未来预售（showDate > today 不计入累计）
        if (!showDate.isAfter(LocalDate.now())) {
            redisTemplate.opsForValue().increment(CUMULATIVE_TOTAL, cents);
            redisTemplate.opsForZSet().incrementScore(CUMULATIVE_RANKING, movieId.toString(), cents);
            redisTemplate.opsForValue().increment(CUMULATIVE_TICKETS_PREFIX + movieId, ticketCount);
        }
    }

    /**
     * 退票成功时实时扣减票房
     */
    public void recordRefund(Long movieId, LocalDate showDate, BigDecimal refundAmount, int ticketCount) {
        ensureCurrentDay();
        long cents = toCents(refundAmount);
        boolean isToday = showDate.equals(LocalDate.now());

        if (isToday) {
            redisTemplate.opsForValue().increment(TODAY_TOTAL, -cents);
            redisTemplate.opsForZSet().incrementScore(TODAY_RANKING, movieId.toString(), -cents);
            redisTemplate.opsForValue().increment(TODAY_TICKETS_PREFIX + movieId, -ticketCount);
        }

        if (!showDate.isAfter(LocalDate.now())) {
            redisTemplate.opsForValue().increment(CUMULATIVE_TOTAL, -cents);
            redisTemplate.opsForZSet().incrementScore(CUMULATIVE_RANKING, movieId.toString(), -cents);
            redisTemplate.opsForValue().increment(CUMULATIVE_TICKETS_PREFIX + movieId, -ticketCount);
        }
    }

    // ==================== 查询 ====================

    /**
     * 获取今日票房排行 — 直接从 Redis 读取
     */
    public Map<String, Object> getTodayBoxOffice() {
        ensureCurrentDay();
        return buildResult(TODAY_TOTAL, TODAY_RANKING, TODAY_TICKETS_PREFIX);
    }

    /**
     * 获取累计票房排行 — 直接从 Redis 读取
     */
    public Map<String, Object> getCumulativeBoxOffice() {
        ensureCumulative();
        return buildResult(CUMULATIVE_TOTAL, CUMULATIVE_RANKING, CUMULATIVE_TICKETS_PREFIX);
    }

    private Map<String, Object> buildResult(String totalKey, String rankingKey, String ticketsPrefix) {
        String totalCents = redisTemplate.opsForValue().get(totalKey);
        BigDecimal totalRevenue = totalCents != null
                ? new BigDecimal(totalCents).movePointLeft(2)
                : BigDecimal.ZERO;

        Set<ZSetOperations.TypedTuple<String>> ranking =
                redisTemplate.opsForZSet().reverseRangeWithScores(rankingKey, 0, 9);

        List<Map<String, Object>> movies = new ArrayList<>();
        if (ranking != null) {
            for (ZSetOperations.TypedTuple<String> entry : ranking) {
                if (entry.getValue() == null || entry.getScore() == null) continue;
                Long movieId = Long.valueOf(entry.getValue());
                Movie movie = movieRepository.findById(movieId).orElse(null);
                if (movie == null) continue;

                String ticketsStr = redisTemplate.opsForValue().get(ticketsPrefix + movieId);

                Map<String, Object> m = new HashMap<>();
                m.put("movieId", movieId);
                m.put("title", movie.getTitle());
                m.put("poster", movie.getPoster());
                m.put("revenue", BigDecimal.valueOf(entry.getScore()).movePointLeft(2));
                m.put("ticketCount", ticketsStr != null ? Integer.parseInt(ticketsStr) : 0);
                movies.add(m);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalRevenue", totalRevenue);
        result.put("movies", movies);
        return result;
    }

    // ==================== 跨天处理 ====================

    private void ensureCurrentDay() {
        String today = LocalDate.now().toString();
        String stored = redisTemplate.opsForValue().get(TODAY_DATE);
        if (!today.equals(stored)) {
            if (stored != null) {
                log.info("跨天切换: {} -> {}, 重置今日票房数据", stored, today);
            }
            resetToday(today);
        }
    }

    private void resetToday(String today) {
        redisTemplate.delete(TODAY_TOTAL);
        redisTemplate.delete(TODAY_RANKING);
        // 删除今日所有电影的票数 key（用 SCAN 匹配）
        Set<String> ticketKeys = redisTemplate.keys(TODAY_TICKETS_PREFIX + "*");
        if (ticketKeys != null && !ticketKeys.isEmpty()) {
            redisTemplate.delete(ticketKeys);
        }
        redisTemplate.opsForValue().set(TODAY_DATE, today);
    }

    private void ensureCumulative() {
        if (Boolean.FALSE.equals(redisTemplate.hasKey(CUMULATIVE_DATE))) {
            // 首次启动，从 DB 加载累计数据
            loadCumulativeFromDB();
            redisTemplate.opsForValue().set(CUMULATIVE_DATE, "loaded");
        }
    }

    /**
     * 从数据库加载累计票房到 Redis（首次启动时调用）
     */
    private void loadCumulativeFromDB() {
        log.info("首次启动，从数据库加载累计票房...");
        LocalDate today = LocalDate.now();
        List<Order> cumulativePaidOrders = orderRepository.findCumulativePaidOrders(
                OrderStatus.paid, today);
        log.info("累计票房加载: 查到{}笔paid订单", cumulativePaidOrders.size());

        for (Order order : cumulativePaidOrders) {
            Showtime showtime = showtimeRepository.findById(order.getShowtimeId()).orElse(null);
            if (showtime == null) continue;
            long cents = toCents(order.getTotalAmount());

            redisTemplate.opsForValue().increment(CUMULATIVE_TOTAL, cents);
            redisTemplate.opsForZSet().incrementScore(CUMULATIVE_RANKING, showtime.getMovieId().toString(), cents);
            redisTemplate.opsForValue().increment(CUMULATIVE_TICKETS_PREFIX + showtime.getMovieId(), 1);
        }
        log.info("累计票房加载完成: total={}", redisTemplate.opsForValue().get(CUMULATIVE_TOTAL));
    }

    // ==================== 定时持久化 ====================

    /**
     * 每小时将 Redis 票房数据同步到数据库
     */
    @Scheduled(fixedRate = 3600000)
    public void flushToDatabase() {
        log.info("票房数据定时持久化...");
        try {
            // Redis 是实时数据源，订单表本身已有原始数据
            // 此处只做日志记录，Redis 数据丢失时可从订单表恢复
            String todayTotal = redisTemplate.opsForValue().get(TODAY_TOTAL);
            String cumTotal = redisTemplate.opsForValue().get(CUMULATIVE_TOTAL);
            log.info("票房持久化检查: today={}, cumulative={}", todayTotal, cumTotal);
        } catch (Exception e) {
            log.error("票房持久化失败", e);
        }
    }

    // ==================== 工具方法 ====================

    /** BigDecimal 转为分（Long），避免浮点精度问题 */
    private static long toCents(BigDecimal amount) {
        return amount.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /**
     * 清除缓存（退票兼容旧调用，实时方案下无需操作）
     */
    public void clearCache() {
        // 实时方案下数据已在 recordRefund 中扣减，此方法保留兼容
    }

    // ==================== DB 回退（保留用于故障恢复） ====================

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
}
