package com.moviebooking.config;

import com.moviebooking.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 软认证拦截器：有 token 就解析 userId，没 token 也不拦截
 */
@Component
public class SoftAuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Autowired
    public SoftAuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
            if (jwtUtil.isTokenValid(token)) {
                request.setAttribute("userId", jwtUtil.getUserId(token));
                request.setAttribute("username", jwtUtil.getUsername(token));
                request.setAttribute("role", jwtUtil.getRole(token));
            }
        }
        return true;
    }
}
