package com.saeal.MrDaebackService.user.service;

import com.saeal.MrDaebackService.user.domain.User;
import com.saeal.MrDaebackService.user.dto.request.RegisterDto;
import com.saeal.MrDaebackService.user.dto.response.UserResponseDto;
import com.saeal.MrDaebackService.user.enums.Authority;
import com.saeal.MrDaebackService.user.enums.LoyaltyLevel;
import com.saeal.MrDaebackService.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDto register(RegisterDto registerDto) {
        if (userRepository.existsByEmailIgnoreCase(registerDto.getEmail())) {
            throw new IllegalArgumentException("Email already exist");
        }
        if (userRepository.existsByUsernameIgnoreCase(registerDto.getUsername())) {
            throw new IllegalArgumentException("Username already exist");
        }

        User user = User.builder()
                .email(registerDto.getEmail())
                .username(registerDto.getUsername())
                .password(passwordEncoder.encode(registerDto.getPassword()))
                .displayName(registerDto.getDisplayName())
                .phoneNumber(registerDto.getPhoneNumber())
                .address(registerDto.getAddress())
                .authority(Authority.ROLE_USER)
                .loyaltyLevel(LoyaltyLevel.BRONZE)
                .visitCount(0L)
                .totalSpent(BigDecimal.ZERO)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return UserResponseDto.from(user);
    }

    @Transactional
    public UserResponseDto makeAdmin(String username) {
        User user = userRepository.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));

        user.setAuthority(Authority.ROLE_ADMIN);
        user.setUpdatedAt(LocalDateTime.now());

        return UserResponseDto.from(user);
    }
}
