package com.safechat.userservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import com.safechat.userservice.jwt.JwtAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (no authentication required)
                        .requestMatchers(
                                "/api/v1/users/account",
                                "/api/v1/users/check-displayname",
                                "/api/v1/users/check-email",
                                "/api/v1/users/otp/**",
                                "/api/v1/users/auth/login",
                                "/api/v1/users/auth/admin/login")
                        .permitAll()

                        // Admin only endpoints
                        .requestMatchers(
                                "/api/v1/users/admin/**",
                                "/api/v1/users/auth/admin/logout")
                        .hasRole("ADMIN")

                        // User endpoints (authenticated)
                        .requestMatchers(
                                "/api/v1/users/profile",
                                "/api/v1/users/{userId}",
                                "/api/v1/users/search",
                                "/api/v1/users/keys/**",
                                "/api/v1/users/account/delete-request",
                                "/api/v1/users/account/delete-instant",
                                "/api/v1/users/account/delete-cancel",
                                "/api/v1/users/auth/logout")
                        .authenticated()

                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/api-docs/**",
                                "/v3/api-docs/**")
                        .permitAll()

                        // Any other request requires authentication
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}