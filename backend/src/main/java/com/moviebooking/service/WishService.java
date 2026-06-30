package com.moviebooking.service;

import com.moviebooking.common.BusinessException;
import com.moviebooking.entity.Movie;
import com.moviebooking.entity.enums.MovieStatus;
import com.moviebooking.repository.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class WishService {

    private static final Logger log = LoggerFactory.getLogger(WishService.class);
    private static final String WISH_KEY_PREFIX = "movie:wish:";
    private static final String WISH_COUNT_PREFIX = "movie:wish:count:";
    private static final String USER_WISH_PREFIX = "user:wish:";

    private final MovieRepository movieRepository;
    private final StringRedisTemplate redisTemplate;

    @Autowired
    public WishService(MovieRepository movieRepository, StringRedisTemplate redisTemplate) {
        this.movieRepository = movieRepository;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 用户想看某部电影
     */
    public Map<String, Object> toggleWish(Long userId, Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new BusinessException("电影不存在"));

        String wishKey = WISH_KEY_PREFIX + movieId;
        Boolean isMember = redisTemplate.opsForSet().isMember(wishKey, String.valueOf(userId));

        if (Boolean.TRUE.equals(isMember)) {
            // 取消想看
            redisTemplate.opsForSet().remove(wishKey, String.valueOf(userId));
            redisTemplate.opsForSet().remove(USER_WISH_PREFIX + userId, String.valueOf(movieId));
            redisTemplate.opsForValue().decrement(WISH_COUNT_PREFIX + movieId);
            return Map.of("wishCount", getWishCount(movieId), "isWished", false);
        } else {
            // 添加想看
            redisTemplate.opsForSet().add(wishKey, String.valueOf(userId));
            redisTemplate.opsForSet().add(USER_WISH_PREFIX + userId, String.valueOf(movieId));
            redisTemplate.opsForValue().increment(WISH_COUNT_PREFIX + movieId);
            return Map.of("wishCount", getWishCount(movieId), "isWished", true);
        }
    }

    /**
     * 查询用户是否想看某部电影
     */
    public Map<String, Object> getWishStatus(Long userId, Long movieId) {
        String wishKey = WISH_KEY_PREFIX + movieId;
        Boolean isWished = false;

        if (userId != null) {
            isWished = redisTemplate.opsForSet().isMember(wishKey, String.valueOf(userId));
        }

        return Map.of(
                "wishCount", getWishCount(movieId),
                "isWished", Boolean.TRUE.equals(isWished)
        );
    }

    /**
     * 获取想看数量
     */
    private long getWishCount(Long movieId) {
        String countStr = redisTemplate.opsForValue().get(WISH_COUNT_PREFIX + movieId);
        if (countStr != null) {
            return Long.parseLong(countStr);
        }

        // 从Redis Set获取实际数量
        Long count = redisTemplate.opsForSet().size(WISH_KEY_PREFIX + movieId);
        return count != null ? count : 0;
    }

    /**
     * 获取用户想看列表
     */
    public List<Map<String, Object>> getUserWishList(Long userId) {
        Set<String> movieIds = redisTemplate.opsForSet().members(USER_WISH_PREFIX + userId);
        if (movieIds == null || movieIds.isEmpty()) return List.of();

        List<Map<String, Object>> result = new ArrayList<>();
        for (String id : movieIds) {
            Movie movie = movieRepository.findById(Long.valueOf(id)).orElse(null);
            if (movie == null) continue;
            Map<String, Object> m = new HashMap<>();
            m.put("id", movie.getId());
            m.put("title", movie.getTitle());
            m.put("poster", movie.getPoster());
            m.put("genre", movie.getGenre());
            m.put("duration", movie.getDuration());
            m.put("status", movie.getStatus().name());
            m.put("releaseDate", movie.getReleaseDate());
            m.put("rating", movie.getRating());
            m.put("wishCount", getWishCount(movie.getId()));
            result.add(m);
        }
        result.sort((a, b) -> {
            String sa = (String) a.get("status");
            String sb = (String) b.get("status");
            if ("showing".equals(sa) && !"showing".equals(sb)) return -1;
            if (!"showing".equals(sa) && "showing".equals(sb)) return 1;
            return Long.compare((long) b.get("wishCount"), (long) a.get("wishCount"));
        });
        return result;
    }

    /**
     * 获取最受期待的电影（想看数量最多的即将上映电影）
     */
    public List<Map<String, Object>> getMostExpectedMovies(int limit) {
        // 获取所有upcoming电影
        List<Movie> movies = movieRepository.findByStatus(MovieStatus.upcoming);

        // 按Redis中的实时想看数量排序
        movies.sort((a, b) -> Long.compare(getWishCount(b.getId()), getWishCount(a.getId())));

        // 取前limit个
        if (movies.size() > limit) {
            movies = movies.subList(0, limit);
        }

        // 构建返回数据
        return movies.stream().map(movie -> {
            Map<String, Object> map = new HashMap<>();
            map.put("movieId", movie.getId());
            map.put("title", movie.getTitle());
            map.put("poster", movie.getPoster());
            map.put("wishCount", getWishCount(movie.getId()));
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 定时任务：每小时同步想看数量到Movie表
     */
    @Scheduled(fixedRate = 3600000) // 1小时
    public void syncWishCount() {
        log.info("开始同步想看数量...");
        try {
            List<Movie> movies = movieRepository.findAll();
            for (Movie movie : movies) {
                long wishCount = getWishCount(movie.getId());
                Integer currentWishCount = movie.getWishCount();
                if (currentWishCount == null || currentWishCount != (int) wishCount) {
                    movie.setWishCount((int) wishCount);
                    movieRepository.save(movie);
                }
            }
            log.info("想看数量同步完成");
        } catch (Exception e) {
            log.error("想看数量同步失败", e);
        }
    }
}
