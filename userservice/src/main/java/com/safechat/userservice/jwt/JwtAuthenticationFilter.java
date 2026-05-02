package com.safechat.userservice.jwt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.safechat.userservice.service.authService.AuthService;
import com.safechat.userservice.utility.encryption.AesEncryption;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final String SERVICE_NAME = "JwtAuthenticationFilter";
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    
    private final AesEncryption aesEncryption;
    private final JwtUtils jwtUtils;
    private final AuthService authService;

    JwtAuthenticationFilter(AesEncryption aesEncryption, JwtUtils jwtUtils, AuthService authService) {
        this.aesEncryption = aesEncryption;
        this.jwtUtils = jwtUtils;
        this.authService = authService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String METHOD_NAME = "doFilterInternal";
        
        log.debug("{} - Processing request for path: {}", METHOD_NAME, request.getRequestURI());

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {

            log.debug("{} - Bearer token found in Authorization header", METHOD_NAME);
            
            String encryptToken = authorizationHeader.substring(7);
            log.debug("{} - Extracted encrypted token", METHOD_NAME);
            
            try {
                log.debug("{} - Verifying and validating token", METHOD_NAME);
                
                if (authService.verifyAndValidateToken(encryptToken)) {
                    log.debug("{} - Token verification successful", METHOD_NAME);
                    
                    log.debug("{} - Decrypting token", METHOD_NAME);
                    Claims claims = jwtUtils.extractAllClaims(aesEncryption.decrypt(encryptToken));
                    String role = (String) claims.get("role");
                    log.debug("{} - Extracted role: {}", METHOD_NAME, role);

                    if (role != null) {
                        String uid = (String) claims.get("uid");
                        log.debug("{} - Extracted uid: {}", METHOD_NAME, uid);

                        List<GrantedAuthority> authorities = new ArrayList<>();
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(uid,
                                encryptToken,
                                authorities);

                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                        
                        log.info("{} - Authentication successful for uid: {}, role: {}", METHOD_NAME, uid, role);
                    } else {
                        log.warn("{} - Token has no role claim", METHOD_NAME);
                        response.setStatus(HttpStatus.UNAUTHORIZED.value());
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"statusCode\":401,\"message\":\"Invalid or expired token\",\"timestamp\":"
                                        + System.currentTimeMillis() + "}");
                        return;
                    }
                } else {
                    log.warn("{} - Token verification failed", METHOD_NAME);
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"statusCode\":401,\"message\":\"Invalid or expired token\",\"timestamp\":"
                                    + System.currentTimeMillis() + "}");
                    return;
                }
            } catch (Exception e) {
                log.error("{} - Exception during token validation: {}", METHOD_NAME, e.getMessage());
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"statusCode\":401,\"message\":\"Invalid or expired token\",\"timestamp\":"
                                    + System.currentTimeMillis() + "}");
                    return;
            }
        } else {
            log.debug("{} - No Bearer token found in Authorization header", METHOD_NAME);
        }
        
        log.debug("{} - Continuing filter chain", METHOD_NAME);
        filterChain.doFilter(request, response);
    }

}