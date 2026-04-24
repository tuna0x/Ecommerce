package com.tuna.ecommerce.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsConfig {
    @Value("${tuna.frontend-url}")
    private String frontendUrl;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Restricted origins for better security. 
        // Using setAllowedOriginPatterns to support multiple origins and wildcard subdomains if needed.
        if (frontendUrl != null && !frontendUrl.isEmpty()) {
            configuration.setAllowedOriginPatterns(Arrays.asList(frontendUrl, "http://localhost:5173", "http://localhost:3000"));
        } else {
            configuration.setAllowedOriginPatterns(Arrays.asList("*")); // Fallback if not configured, but should be specific in prod
        }
        
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L); // 1 hour caching for pre-flight requests

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration); 
        return source;
    }

    /**
     * Register a CorsFilter with HIGHEST_PRECEDENCE to ensure CORS headers
     * are applied to ALL requests, including SockJS transport requests
     * (xhr-streaming, xhr-polling, info endpoint) which bypass Spring Security's CORS.
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilterRegistration(CorsConfigurationSource corsConfigurationSource) {
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(
                new CorsFilter(corsConfigurationSource));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return bean;
    }
}
