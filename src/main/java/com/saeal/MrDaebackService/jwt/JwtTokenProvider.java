package com.saeal.MrDaebackService.jwt;

import com.saeal.MrDaebackService.security.JwtUserDetails;
import com.saeal.MrDaebackService.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.List;
import java.util.Date;
import java.util.UUID;

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

    public boolean validateAccessToken(String token) {
        return validateToken(token, accessTokenKey, "access");
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token, accessTokenKey);
        String tokenType = claims.get("tokenType", String.class);
        if (!"access".equals(tokenType)) {
            throw new IllegalArgumentException("Invalid token type");
        }
        UUID userId = UUID.fromString(claims.getSubject());
        String username = claims.get("username", String.class);
        String authority = claims.get("authority", String.class);

        JwtUserDetails principal = new JwtUserDetails(
                userId,
                username,
                List.of(new SimpleGrantedAuthority(authority))
        );

        return new UsernamePasswordAuthenticationToken(principal, token, principal.getAuthorities());
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

    private boolean validateToken(String token, Key key, String expectedType) {
        try {
            Claims claims = parseClaims(token, key);
            String tokenType = claims.get("tokenType", String.class);
            return expectedType.equals(tokenType);
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token, Key key) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
