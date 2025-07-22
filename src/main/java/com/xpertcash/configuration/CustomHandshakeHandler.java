package com.xpertcash.configuration;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import jakarta.servlet.http.HttpServletRequest;

import java.security.Principal;
import java.util.Map;

@Component
public class CustomHandshakeHandler extends DefaultHandshakeHandler {

    private final JwtUtil jwtUtil;

    public CustomHandshakeHandler(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected Principal determineUser(ServerHttpRequest request,
                                      WebSocketHandler wsHandler,
                                      Map<String, Object> attributes) {

        if (request instanceof ServletServerHttpRequest servletRequest) {
            HttpServletRequest httpReq = servletRequest.getServletRequest();

            String token = extractToken(httpReq);

            if (token != null) {
                try {
                    Long userId = jwtUtil.extractUserId(token);
                    if (userId != null) {
                        System.out.println("🎯 Utilisateur WebSocket identifié : " + userId);
                        return () -> userId.toString(); // très important
                    }
                } catch (Exception e) {
                    System.out.println("⛔ Erreur extraction token WebSocket : " + e.getMessage());
                }
            } else {
                System.out.println("⛔ Token non trouvé dans le handshake");
            }
        }

        return null;
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            System.out.println("🔑 Token extrait du header Authorization");
            return authHeader.substring(7);
        }

        String query = request.getQueryString();
        if (query != null) {
            for (String param : query.split("&")) {
                if (param.startsWith("token=")) {
                    System.out.println("🔑 Token extrait de la query string");
                    return param.substring("token=".length());
                }
            }
        }

        return null;
    }

    
}
