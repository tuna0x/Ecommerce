package com.tuna.ecommerce.config;

import java.util.Collections;

import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.service.UserService;

@Component("userDetailService")
public class UserDetailCustom implements UserDetailsService{
    private final UserService userService;

    public UserDetailCustom(@Lazy UserService userService) {
        this.userService = userService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user=this.userService.findByUsernameForAuth(username);
        if (user == null) {
            throw new UsernameNotFoundException("user's not found");
        }
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            user.getActive() != null ? user.getActive() : true,
            true,
            true,
            true,
            Collections.singletonList(new SimpleGrantedAuthority(user.getRole() != null ? user.getRole().getName() : "ROLE_USER"))
        );
    }
}
