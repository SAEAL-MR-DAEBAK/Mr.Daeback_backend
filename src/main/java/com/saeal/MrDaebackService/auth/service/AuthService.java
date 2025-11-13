package com.saeal.MrDaebackService.auth.service;

import com.saeal.MrDaebackService.auth.domain.RefreshToken;
import com.saeal.MrDaebackService.auth.dto.LoginDto;
import com.saeal.MrDaebackService.auth.dto.LoginResponseDto;
import com.saeal.MrDaebackService.auth.repository.RefreshTokenRepository;
import com.saeal.MrDaebackService.jwt.JwtTokenProvider;
import com.saeal.MrDaebackService.user.domain.User;
import com.saeal.MrDaebackService.user.dto.response.UserResponseDto;
import com.saeal.MrDaebackService.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public LoginResponseDto login(LoginDto loginDto) {
        User user = userRepository
                .findByUsernameIgnoreCase(loginDto.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        return issueTokens(user);
    }

    @Transactional
    public LoginResponseDto refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenRepository
                .findByToken(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        if (refreshToken.isExpired(LocalDateTime.now(ZoneOffset.UTC))) {
            refreshTokenRepository.delete(refreshToken);
            throw new IllegalArgumentException("Refresh token expired");
        }

        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user);
        LocalDateTime expiryDate = calculateExpiry(jwtTokenProvider.getRefreshExpirationInMillis());

        refreshToken.rotate(newRefreshToken, expiryDate);
        refreshTokenRepository.save(refreshToken);

        return new LoginResponseDto(newAccessToken, newRefreshToken, "Bearer", UserResponseDto.from(user));
    }

    private LoginResponseDto issueTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshTokenValue = jwtTokenProvider.generateRefreshToken(user);
        LocalDateTime expiryDate = calculateExpiry(jwtTokenProvider.getRefreshExpirationInMillis());

        RefreshToken refreshToken = refreshTokenRepository
                .findByUserId(user.getId())
                .map(existing -> {
                    existing.rotate(refreshTokenValue, expiryDate);
                    return existing;
                })
                .orElseGet(() -> RefreshToken.builder()
                        .user(user)
                        .token(refreshTokenValue)
                        .expiryDate(expiryDate)
                        .build());

        refreshTokenRepository.save(refreshToken);

        return new LoginResponseDto(accessToken, refreshTokenValue, "Bearer", UserResponseDto.from(user));
    }

    private LocalDateTime calculateExpiry(long validityInMillis) {
        Instant expiresAt = Instant.now().plusMillis(validityInMillis);
        return LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC);
    }
}
