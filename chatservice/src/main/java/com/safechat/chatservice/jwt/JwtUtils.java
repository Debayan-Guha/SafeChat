package com.safechat.chatservice.jwt;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtils {

    private final String SECRET_KEY;
    private final long JWT_EXPIRATION_TIME;
    private final String JWT_ISSUER;

    JwtUtils(@Value("${jwt.secretKey}") String SECRET_KEY, @Value("${jwt.expirationTime}") long JWT_EXPIRATION_TIME,
            @Value("${jwt.issuer}") String JWT_ISSUER) {
        this.SECRET_KEY = SECRET_KEY;
        this.JWT_EXPIRATION_TIME = JWT_EXPIRATION_TIME;
        this.JWT_ISSUER = JWT_ISSUER;
    }

    private SecretKey getSigningKey() {

        byte[] keyInBytes = SECRET_KEY.getBytes();
        return Keys.hmacShaKeyFor(keyInBytes);
    }

    public boolean tokenVerification(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token).getPayload();
    }
}
