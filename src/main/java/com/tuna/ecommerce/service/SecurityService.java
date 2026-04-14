package com.tuna.ecommerce.service;

import java.util.List;
import org.springframework.stereotype.Service;
import com.tuna.ecommerce.domain.response.user.ResUserPermissionDTO;
import com.tuna.ecommerce.ultil.SecurityUtil;
import lombok.RequiredArgsConstructor;

@Service("securityService")
@RequiredArgsConstructor
public class SecurityService {
    private final UserService userService;

    /**
     * Kiểm tra xem user hiện tại có quyền cụ thể không.
     * @param module Tên module (ví dụ: USERS, PRODUCTS)
     * @param action Hành động (ví dụ: CREATE, UPDATE, DELETE)
     * @return true nếu có quyền hoặc là SUPER_ADMIN
     */
    public boolean hasPermission(String module, String action) {
        String email = SecurityUtil.getCurrentUserLogin().orElse("");
        if (email.isEmpty()) return false;

        // 1. Kiểm tra SUPER_ADMIN bypass
        if (isSuperAdmin()) return true;

        // 2. Kiểm tra danh sách quyền
        List<ResUserPermissionDTO> permissions = this.userService.getPermissionsByEmail(email);
        if (permissions == null) return false;

        return permissions.stream().anyMatch(p -> 
            p.getModule().equalsIgnoreCase(module) && 
            p.getName().toUpperCase().contains(action.toUpperCase())
        );
    }

    /**
     * Kiểm tra xem user hiện tại có phải là Super Admin không.
     */
    public boolean isSuperAdmin() {
        return SecurityUtil.getCurrentUserLogin()
                .map(email -> {
                    var user = userService.findByUsername(email);
                    return user != null && user.getRole() != null && "SUPER_ADMIN".equals(user.getRole().getName());
                })
                .orElse(false);
    }
}
