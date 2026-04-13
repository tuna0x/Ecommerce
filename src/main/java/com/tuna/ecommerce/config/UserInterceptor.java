package com.tuna.ecommerce.config;

import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class UserInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            List<String> authorization = accessor.getNativeHeader("Authorization");
            if (authorization != null && !authorization.isEmpty()) {
                String bearerToken = authorization.get(0);
                if (bearerToken.startsWith("Bearer ")) {
                    String token = bearerToken.substring(7);
                    try {
                        Jwt jwt = jwtDecoder.decode(token);
                        Authentication auth = jwtAuthenticationConverter.convert(jwt);
                        
                        // Chuẩn hóa tên người dùng về chữ thường để khớp với logic gửi tin
                        final String email = auth.getName().toLowerCase();
                        Authentication fixedAuth = new Authentication() {
                            @Override
                            public String getName() { return email; }
                            @Override
                            public java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> getAuthorities() { return auth.getAuthorities(); }
                            @Override
                            public Object getCredentials() { return auth.getCredentials(); }
                            @Override
                            public Object getDetails() { return auth.getDetails(); }
                            @Override
                            public Object getPrincipal() { return auth.getPrincipal(); }
                            @Override
                            public boolean isAuthenticated() { return auth.isAuthenticated(); }
                            @Override
                            public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException { auth.setAuthenticated(isAuthenticated); }
                        };
                        
                        accessor.setUser(fixedAuth);
                        System.out.println(">>> WebSocket Authenticated user: " + email);
                    } catch (Exception e) {
                        System.err.println(">>> WebSocket Auth Error: " + e.getMessage());
                    }
                }
            }
        }
        return message;
    }
}
