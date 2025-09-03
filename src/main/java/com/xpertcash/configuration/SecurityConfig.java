package com.xpertcash.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1) CORS
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 2) CSRF
                .csrf(csrf -> csrf
                        // on stocke le token dans un cookie lisible par JS
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // on ignore CSRF pour WS et pour toute l'API d'auth
                        .ignoringRequestMatchers(
                                new AntPathRequestMatcher("/ws/**"),
                                new AntPathRequestMatcher("/api/auth/**")
                        )
                )
                // 3) Autorisations
                .authorizeHttpRequests(auth -> auth
                        // on autorise ces endpoints sans auth
                        .requestMatchers("/csrf").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        // tout le reste est ouvert (ou vous pouvez restreindre)
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "http://192.168.1.8:4200",
                "https://tchakeda.com",
                "https://www.tchakeda.com"
                // "https://xpertcash.tchakeda.com/api/v1"
        ));
        configuration.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "X-XSRF-TOKEN"
        ));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // appliqué à toutes les routes
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}