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
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(auth -> auth
                                                // Public endpoints (no authentication required)
                                                .requestMatchers(
                                                                "/api/v1/userservice/users/account",
                                                                "/api/v1/userservice/users/check-displayname",
                                                                "/api/v1/userservice/users/check-email",
                                                                "/api/v1/userservice/users/otp/**",
                                                                "/api/v1/userservice/auth/login", // ✅ Fixed
                                                                "/api/v1/userservice/auth/admin/login") // ✅ Fixed
                                                .permitAll()

                                                // Admin only endpoints
                                                .requestMatchers(
                                                                "/api/v1/userservice/admin/**",
                                                                "/api/v1/userservice/auth/admin/logout")
                                                .hasRole("ADMIN")

                                                // User endpoints (authenticated)
                                                .requestMatchers(
                                                                "/api/v1/userservice/users/profile",
                                                                "/api/v1/userservice/users/{userId}",
                                                                "/api/v1/userservice/users/search",
                                                                "/api/v1/userservice/users/keys/**",
                                                                "/api/v1/userservice/users/account/delete-request",
                                                                "/api/v1/userservice/users/account/delete-instant",
                                                                "/api/v1/userservice/users/account/delete-cancel",
                                                                "/api/v1/userservice/auth/logout")
                                                .authenticated()

                                                .requestMatchers(
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html",
                                                                "/api-docs/**",
                                                                "/v3/api-docs/**")
                                                .permitAll()

                                                .anyRequest().authenticated())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }
}