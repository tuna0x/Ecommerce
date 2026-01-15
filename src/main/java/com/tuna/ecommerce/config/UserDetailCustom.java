package com.tuna.ecommerce.config;

import java.util.Collections;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import com.tuna.ecommerce.domain.User;
import com.tuna.ecommerce.service.UserService;

import lombok.AllArgsConstructor;

@Component("userDetailService")
@AllArgsConstructor
public class UserDetailCustom implements UserDetailsService{
    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user=this.userService.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("user's not found");
        }
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPassword(),
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
