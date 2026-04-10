package com.safechat.userservice.jwt;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {

            String encryptToken = authorizationHeader.substring(7);
            try {
                if (authService.verifyAndValidateToken(encryptToken)) {

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
                    }
                } else {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"statusCode\":401,\"message\":\"Invalid or expired token\",\"timestamp\":"
                                    + System.currentTimeMillis() + "}");
                    return;
                }
            } catch (Exception e) {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType("application/json");
                    response.getWriter().write(
                            "{\"statusCode\":401,\"message\":\"Invalid or expired token\",\"timestamp\":"
                                    + System.currentTimeMillis() + "}");
                    return;
            }
        }
        filterChain.doFilter(request, response);
    }

}
