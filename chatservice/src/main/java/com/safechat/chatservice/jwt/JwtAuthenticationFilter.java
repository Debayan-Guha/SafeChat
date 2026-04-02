package com.safechat.chatservice.jwt;

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

import com.safechat.chatservice.utility.encryption.AesEncryption;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AesEncryption aesEncryption;
    private final JwtUtils jwtUtils;

    JwtAuthenticationFilter(AesEncryption aesEncryption, JwtUtils jwtUtils) {
        this.aesEncryption = aesEncryption;
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {

            String encryptToken = authorizationHeader.substring(7);
                try {
                    if (jwtUtils.tokenVerification(encryptToken)) {

                        Claims claims = jwtUtils.extractAllClaims(aesEncryption.decrypt(encryptToken));
                        String role = (String) claims.get("role");

                        if (role != null) {
                            String uid = (String) claims.get("uid");

                            List<GrantedAuthority> authorities = new ArrayList<>();
                            authorities.add(new SimpleGrantedAuthority("ROLE_" + role));

                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(uid, encryptToken,
                                    authorities);

                            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    }
                } catch (Exception e) {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    return;
                }
        }
        filterChain.doFilter(request, response); 
    }

}
