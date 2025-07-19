package com.xpertcash.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry config) {
//        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
//        taskScheduler.setPoolSize(4);
//        taskScheduler.setThreadNamePrefix("wss-heartbeat-");
//        taskScheduler.initialize();
//
//        config.enableSimpleBroker("/topic", "/queue")
//                .setTaskScheduler(taskScheduler)
//                .setHeartbeatValue(new long[]{10000, 10000});
//        config.setApplicationDestinationPrefixes("/app");
//        config.setUserDestinationPrefix("/user");
//    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/queue", "/topic");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

//    @Override
//    public void registerStompEndpoints(StompEndpointRegistry registry) {
//        registry.addEndpoint("/api/auth/ws")
//                .setAllowedOriginPatterns("*")
//                .withSockJS()
//                .setSuppressCors(true);
//    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/api/auth/ws")
                .setAllowedOriginPatterns("*")
                .setAllowedOrigins("http://192.168.1.9:4200")
                .withSockJS();
    }

//    @Override
//    public void configureClientInboundChannel(ChannelRegistration registration) {
//        // 1️⃣ Pool de threads
//        registration.taskExecutor()
//                .corePoolSize(8)
//                .maxPoolSize(16)
//                .queueCapacity(100)
//                .keepAliveSeconds(60);
//
//        // 2️⃣ Intercepteur STOMP
//        registration.interceptors(new ChannelInterceptor() {
//            @Override
//            public Message<?> preSend(Message<?> message, MessageChannel channel) {
//                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
//                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
//                    // ← Ici : on affiche le header Authorization venu du client
//                    String tokenHeader = accessor.getFirstNativeHeader("Authorization");
//                    System.out.println("🔑 STOMP CONNECT Authorization header = '" + tokenHeader + "'");
//                }
//                return message;
//            }
//        });
//    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                System.out.println("STOMP Command: " + accessor.getCommand());

                if (accessor.getCommand() == StompCommand.CONNECT) {
                    String token = accessor.getFirstNativeHeader("Authorization");
                    System.out.println("Authorization header: " + token);
                }

                return message;
            }

            @Override
            public void afterSendCompletion(Message<?> message, MessageChannel channel, boolean sent, Exception ex) {
                if (ex != null) {
                    System.err.println("Error sending message: " + ex.getMessage());
                    ex.printStackTrace();
                }
            }
        });
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor()
                .corePoolSize(4)
                .maxPoolSize(8)
                .queueCapacity(50)       // <— idem pour la sortie
                .keepAliveSeconds(60);
    }
}
