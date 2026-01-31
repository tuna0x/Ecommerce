package com.tuna.ecommerce.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.tuna.ecommerce.domain.Permission;
import com.tuna.ecommerce.domain.Role;
import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.service.UserService;
import com.tuna.ecommerce.ultil.SecurityUtil;
import com.tuna.ecommerce.ultil.err.PermissionException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


public class PermissionInterceptor implements HandlerInterceptor {
    @Autowired UserService userService;
    @Override
    @Transactional
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response, Object handler)
            throws Exception {

        String path = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String requestURI = request.getRequestURI();
        String httpMethod = request.getMethod();
        System.out.println(">>> RUN preHandle");
        System.out.println(">>> path= " + path);
        System.out.println(">>> httpMethod= " + httpMethod);
        System.out.println(">>> requestURI= " + requestURI);

        // check permission
        String email=SecurityUtil.getCurrentUserLogin().isPresent() ==true ? SecurityUtil.getCurrentUserLogin().get() :"";
        if (email != null && !email.isEmpty()) {
            User user=this.userService.findByUsername(email);
            if (user != null) {
                Role role=user.getRole();
                if (role!=null) {
                    List<Permission> list=role.getPermissions();
                    boolean isAllow=list.stream().anyMatch(x->
                    x.getApiPath().equals(path)&&
                    x.getMethod().equals(httpMethod));

                    if (isAllow==false) {
                        throw new PermissionException("you don't have permission to access this endpoint");
                    }
                }else{
                    throw new PermissionException("you don't have permission to access this endpoint");
                }
            }
        }

        return true;
    }
}
