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

        // White list for public endpoints, websockets, and payment callbacks
        if ((path != null && (path.startsWith("/api/v1/public/") || path.startsWith("/api/v1/auth/") || path.startsWith("/websocket") || path.startsWith("/api/v1/payment/") || path.startsWith("/api/v1/tracking/log") || path.startsWith("/api/v1/tracking/logs") || path.startsWith("/api/v1/tracking/analytics") || path.startsWith("/api/v1/users/{id}/analytics") || path.startsWith("/api/v1/users/{id}/admin-notes"))) ||
            (requestURI != null && (requestURI.startsWith("/api/v1/public/") || requestURI.startsWith("/api/v1/auth/") || requestURI.startsWith("/websocket") || requestURI.startsWith("/api/v1/payment/") || requestURI.startsWith("/api/v1/tracking/log") || requestURI.startsWith("/api/v1/tracking/logs") || requestURI.startsWith("/api/v1/tracking/analytics") || requestURI.contains("/analytics") || requestURI.contains("/admin-notes")))) {
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
