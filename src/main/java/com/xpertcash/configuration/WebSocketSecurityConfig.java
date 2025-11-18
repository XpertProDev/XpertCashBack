// package com.xpertcash.configuration;

// // WebSocket désactivé - imports commentés
// // import org.springframework.context.annotation.Configuration;
// import org.springframework.security.config.annotation.web.messaging.MessageSecurityMetadataSourceRegistry;
// import org.springframework.security.config.annotation.web.socket.AbstractSecurityWebSocketMessageBrokerConfigurer;


// // @Configuration
// public class WebSocketSecurityConfig
//         extends AbstractSecurityWebSocketMessageBrokerConfigurer {

//     @Override
//     protected void configureInbound(MessageSecurityMetadataSourceRegistry messages) {
//         messages
//                 // autorise les abonnements à vos topics
//                 .simpSubscribeDestMatchers("/topic/**").permitAll()
//                 // autorise les envois vers vos @MessageMapping("/app/**")
//                 .simpDestMatchers("/app/**").permitAll()
//                 // et : autorise tout le reste (UNSUBSCRIBE, DISCONNECT, heartbeats…)
//                 .anyMessage().permitAll();
//     }

//     @Override
//     protected boolean sameOriginDisabled() {
//         return true;
//     }
// }
