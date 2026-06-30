package com.moviebooking.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final SoftAuthInterceptor softAuthInterceptor;

    @Value("${app.upload.path}")
    private String uploadPath;

    @Autowired
    public WebConfig(JwtInterceptor jwtInterceptor, SoftAuthInterceptor softAuthInterceptor) {
        this.jwtInterceptor = jwtInterceptor;
        this.softAuthInterceptor = softAuthInterceptor;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 软认证：有token解析userId，无token不拦截
        registry.addInterceptor(softAuthInterceptor)
                .addPathPatterns("/review/status/*", "/movie/*/wish-status")
                .order(0);

        // 硬认证：其他需要登录的接口
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/user/register",
                        "/user/login",
                        "/admin/login",
                        "/movie/list",
                        "/movie/most-expected",
                        "/movie/*",
                        "/movie/*/wish-status",
                        "/box-office/today",
                        "/cinema/list",
                        "/cinema/*",
                        "/showtime/list",
                        "/showtime/*",
                        "/showtime/*/seats",
                        "/uploads/**",
                        "/review/list",
                        "/review/status/*",
                        "/error"
                )
                .order(1);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absPath = new File(uploadPath).getAbsoluteFile().toURI().toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absPath);
    }
}
