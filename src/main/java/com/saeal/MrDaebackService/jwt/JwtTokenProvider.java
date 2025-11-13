package com.saeal.MrDaebackService.jwt;

import com.saeal.MrDaebackService.user.domain.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    private final Key accessTokenKey;
    private final Key refreshTokenKey;
    private final long accessExpirationInMillis;
    private final long refreshExpirationInMillis;

    public JwtTokenProvider(
            @Value("${jwt.access-secret:${jwt.secret}}") String accessSecret,
            @Value("${jwt.refresh-secret:${jwt.secret}}") String refreshSecret,
            @Value("${jwt.access-expiration}") long accessExpirationInMillis,
            @Value("${jwt.refresh-expiration}") long refreshExpirationInMillis
    ) {
        this.accessTokenKey = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshTokenKey = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessExpirationInMillis = accessExpirationInMillis;
        this.refreshExpirationInMillis = refreshExpirationInMillis;
    }

    public String generateAccessToken(User user) {
        return generateToken(user, accessTokenKey, accessExpirationInMillis, "access");
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, refreshTokenKey, refreshExpirationInMillis, "refresh");
    }

    public long getRefreshExpirationInMillis() {
        return refreshExpirationInMillis;
    }

    public long getAccessExpirationInMillis() {
        return accessExpirationInMillis;
    }

    private String generateToken(User user, Key key, long expirationInMillis, String tokenType) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationInMillis);

        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("username", user.getUsername())
                .claim("authority", user.getAuthority().name())
                .claim("tokenType", tokenType)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
