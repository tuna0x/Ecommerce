package com.tuna.ecommerce.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.tuna.ecommerce.domain.Permission;
import com.tuna.ecommerce.domain.Role;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.domain.response.user.ResUserPermissionDTO;
import com.tuna.ecommerce.service.UserService;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.err.PermissionException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class PermissionInterceptor implements HandlerInterceptor {
    @Autowired
    UserService userService;

    @Autowired
    com.tuna.ecommerce.service.SecurityService securityService;

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response, Object handler)
            throws Exception {

        String path = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String httpMethod = request.getMethod();
        String requestURI = request.getRequestURI();

        // SECURITY: Tighten whitelist and align with SecurityConfiguration
        // Public endpoints that don't require permission checks
        if (path != null && (
                path.equals("/") ||
                path.startsWith("/api/v1/auth/") ||
                path.startsWith("/api/v1/public/") ||
                path.startsWith("/websocket") ||
                path.startsWith("/v3/api-docs") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/actuator/") ||
                // Specific payment callbacks are public
                path.equals("/api/v1/payment/vn-pay-callback") ||
                path.equals("/api/v1/payment/payos-callback") ||
                path.equals("/api/v1/payment/vn-pay") ||
                // Tracking endpoints (public loggers)
                path.startsWith("/api/v1/tracking/")
            )) {
            return true;
        }

        if (path == null) {
            return true;
        }

        // 1. QUICK BYPASS FOR SUPER_ADMIN
        if (this.securityService.isSuperAdmin()) {
            return true;
        }

        // check permission
        String email = SecurityUtil.getCurrentUserLogin().isPresent() == true ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        if (email != null && !email.isEmpty()) {
            List<ResUserPermissionDTO> list = this.userService.getPermissionsByEmail(email);
            if (list != null) {
                boolean isAllow = list.stream().anyMatch(x -> x.getApiPath().equals(path) &&
                        x.getMethod().equals(httpMethod));

                if (isAllow == false) {
                    throw new PermissionException("you don't have permission to access this endpoint");
                }
            } else {
                throw new PermissionException("you don't have permission to access this endpoint");
            }
        }

        return true;
    }
}
