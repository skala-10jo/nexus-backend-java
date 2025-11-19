package com.nexus.backend.config;

import com.nexus.backend.entity.User;
import com.nexus.backend.repository.UserRepository;
import com.nexus.backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * WebSocket authentication interceptor.
 * Extracts JWT token from STOMP CONNECT frame and authenticates the user.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);

                try {
                    if (jwtTokenProvider.validateToken(token)) {
                        String username = jwtTokenProvider.getUsernameFromToken(token);
                        log.info("WebSocket JWT token validated, username: {}", username);

                        // Load user from database
                        User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found: " + username));

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                                );

                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        accessor.setUser(authentication);

                        // Store username in session attributes for future messages
                        if (accessor.getSessionAttributes() != null) {
                            accessor.getSessionAttributes().put("username", username);
                            log.info("Stored username in session: {}", username);
                        }

                        log.info("WebSocket authenticated user: {}", username);
                    } else {
                        log.warn("WebSocket JWT token validation failed");
                    }
                } catch (Exception e) {
                    log.error("WebSocket authentication failed: {}", e.getMessage(), e);
                }
            }
        } else if (accessor != null && (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.MESSAGE.equals(accessor.getCommand()))) {
            // For SEND/MESSAGE commands, retrieve username from session attributes
            if (accessor.getSessionAttributes() != null) {
                String username = (String) accessor.getSessionAttributes().get("username");
                log.info("Retrieved username from session: {}", username);

                if (username != null) {
                    try {
                        // Load user from database
                        User user = userRepository.findByUsername(username)
                                .orElseThrow(() -> new RuntimeException("User not found: " + username));

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                                );
                        accessor.setUser(authentication);
                        log.info("WebSocket SEND/MESSAGE authenticated with user: {}", username);
                    } catch (Exception e) {
                        log.error("Failed to authenticate WebSocket SEND/MESSAGE: {}", e.getMessage());
                    }
                } else {
                    log.warn("No username found in session attributes");
                }
            } else {
                log.warn("Session attributes is null");
            }
        }

        return message;
    }
}
