package com.tuna.ecommerce.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class PermissionInterceptorConfiguration implements WebMvcConfigurer{
       @Bean
    PermissionInterceptor getPermissionInterceptor() {
        return new PermissionInterceptor();
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        String[] whiteList = {
                            "/",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/register",
            "/api/v1/categories/**",
            "/api/v1/products/**",
            "/api/v1/attribute/**",
            "/api/v1/attributes-values/**",
            "/api/v1/coupon/**"
        };
        registry.addInterceptor(getPermissionInterceptor())
                .excludePathPatterns(whiteList);
    }
}
