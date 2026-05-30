package com.melodyshop.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/register", "/api/auth/send-verification", "/api/auth/login", "/api/auth/refresh",
                        "/api/auth/revoke-tokens").permitAll()
                .requestMatchers("/api/auth/v3/api-docs/**", "/api/auth/swagger-ui/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // Face authentication - all endpoints permitted (Gateway handles JWT validation)
                .requestMatchers("/api/auth/face/**").permitAll()
                .anyRequest().permitAll() // JWT validation is handled by API Gateway
            );
        return http.build();
    }
}
