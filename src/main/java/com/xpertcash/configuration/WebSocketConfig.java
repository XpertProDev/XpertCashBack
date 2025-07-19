package com.xpertcash.configuration;

import com.xpertcash.entity.User;
import com.xpertcash.repository.UsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.util.Collections;
import java.util.List;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private JwtUtil jwtUtil; // Votre utilitaire JWT
    @Autowired
    private UsersRepository usersRepository;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue", "/topic");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/auth/ws")
                .setAllowedOriginPatterns("*")
                .setAllowedOrigins("http://192.168.1.15:4200") // Adaptez à votre URL Angular
                .withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                        try {
                            Long userId = jwtUtil.extractUserId(token);
                            User user = usersRepository.findById(userId)
                                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

                            // Correction ici : création manuelle des autorités
                            List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                                    new SimpleGrantedAuthority("ROLE_" + user.getRole().getName())
                            );
                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(user, null, authorities);

                            SecurityContextHolder.getContext().setAuthentication(auth);
                            accessor.setUser(auth); // Associe l'utilisateur à la session WebSocket
                            System.out.println("Utilisateur authentifié pour WebSocket : " + user.getId());
                        } catch (Exception e) {
                            System.err.println("Erreur d'authentification WebSocket : " + e.getMessage());
                            throw new RuntimeException("Token invalide");
                        }
                    } else {
                        System.err.println("Aucun token fourni pour la connexion WebSocket");
                    }
                }
                return message;
            }
        });
    }
}