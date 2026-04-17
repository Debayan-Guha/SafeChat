package com.safechat.chatservice.jwt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.safechat.chatservice.utility.encryption.AesEncryption;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final Map<String, String> VALID_SERVICES = new HashMap<>();

    static {
        VALID_SERVICES.put("USER-SERVICE", "ROLE_USER-SERVICE");
        VALID_SERVICES.put("CHAT-SERVICE", "ROLE_CHAT-SERVICE");
        // Add more services as needed
    }

    private final AesEncryption aesEncryption;
    private final JwtUtils jwtUtils;
    private final String apiKeyToken;

    JwtAuthenticationFilter(AesEncryption aesEncryption, JwtUtils jwtUtils,
            @Value("${service.apikey}") String apiKeyToken) {
        this.aesEncryption = aesEncryption;
        this.jwtUtils = jwtUtils;
        this.apiKeyToken = apiKeyToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // interservice communication
        if (authorizationHeader != null && authorizationHeader.startsWith("Service ")) {

            try {
                String token = authorizationHeader.substring(8);
                String claimedServiceName = request.getHeader("serviceName");

                if (token.equals(apiKeyToken) && claimedServiceName != null) {

                    // SECURITY FIX: Validate service and get its fixed role
                    String predefinedRole = VALID_SERVICES.get(claimedServiceName);

                    if (predefinedRole != null) {
                        logger.debug("Service authentication successful: {}", claimedServiceName);

                        List<GrantedAuthority> authorities = new ArrayList<>();
                        // Use fixed role from map, NOT from header
                        authorities.add(new SimpleGrantedAuthority(predefinedRole));

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                claimedServiceName, null, authorities);

                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        logger.warn("Unknown service attempted to authenticate: {}", claimedServiceName);
                    }
                } else {
                    if (!token.equals(apiKeyToken)) {
                        logger.error("Invalid API token");
                    } else {
                        logger.error("Missing serviceName header");
                    }
                }

            } catch (Exception e) {
                logger.error("Failed service to service communication: {}", e.getMessage());
            }

        } else if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            // User JWT authentication
            String encryptToken = authorizationHeader.substring(7);
            try {
                if (jwtUtils.tokenVerification(encryptToken)) {

                    Claims claims = jwtUtils.extractAllClaims(aesEncryption.decrypt(encryptToken));
                    String role = (String) claims.get("role");

                    if (role != null) {
                        String uid = (String) claims.get("uid");

                        List<GrantedAuthority> authorities = new ArrayList<>();
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(uid,
                                encryptToken,
                                authorities);

                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);

                        logger.debug("User authentication successful: {}", uid);
                    }
                }
            } catch (Exception e) {
                logger.error("User authentication failed: {}", e.getMessage());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}