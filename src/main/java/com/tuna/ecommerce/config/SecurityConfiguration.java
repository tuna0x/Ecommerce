package com.tuna.ecommerce.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.web.SecurityFilterChain;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;
import com.tuna.ecommerce.ultil.SecurityUtil;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfiguration {
    @Value("${tuna.jwt.base64-secret}")
    private String jwtKey;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl roleHierarchy = new RoleHierarchyImpl();
        String hierarchy = "SUPER_ADMIN > ROLE_USER";
        roleHierarchy.setHierarchy(hierarchy);
        return roleHierarchy;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
            CustomAuthenticationEntryPoint customAuthenticationEntryPoint) throws Exception {

        String[] whiteList = {
                "/",
                "/api/v1/auth/login",
                "/api/v1/auth/refresh",
                "/api/v1/auth/register",
                 "/api/v1/auth/social-login",
                "/api/v1/auth/otp/send",
                "/api/v1/auth/otp/verify",
                "/api/v1/auth/check-email",
                "/api/v1/auth/forgot-password",
                "/api/v1/auth/reset-password",
                "/websocket/**",
                "/actuator/health",
                "/api/v1/public/**",
                "/api/v1/tracking/log",
                "/api/v1/tracking/logs",
                "/api/v1/tracking/analytics"
        };
        http.csrf(c -> c.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(
                        authz -> authz
                                .requestMatchers(whiteList).permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/payment/vn-pay-callback").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/payment/vn-pay").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/products/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/flash-sales/active").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/attribute/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/attributes-values/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/brands/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/banners/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/coupons/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/product-detail/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/fee/**").permitAll()
                                .requestMatchers(HttpMethod.GET, "/api/v1/reviews/**").permitAll()
                                .anyRequest().authenticated())
                .oauth2ResourceServer((oauth2) -> oauth2.jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(customAuthenticationEntryPoint))

                .exceptionHandling(
                        exceptions -> exceptions
                                .authenticationEntryPoint(new BearerTokenAuthenticationEntryPoint()) // 401
                                .accessDeniedHandler(new BearerTokenAccessDeniedHandler())) // 403
                .formLogin(f -> f.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("permission");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey()));
    }

    // @FunctionalInterface
    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(
                getSecretKey()).macAlgorithm(SecurityUtil.JWT_ALGORITHM).build();
        return token -> {
            try {
                return jwtDecoder.decode(token);
            } catch (Exception e) {
                throw e;
            }
        };
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(jwtKey).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, SecurityUtil.JWT_ALGORITHM.getName());
    }

}
