package com.saeal.MrDaebackService.user.service;

import com.saeal.MrDaebackService.user.domain.User;
import com.saeal.MrDaebackService.user.dto.request.RegisterDto;
import com.saeal.MrDaebackService.user.dto.response.UserResponseDto;
import com.saeal.MrDaebackService.user.enums.Authority;
import com.saeal.MrDaebackService.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        userRepository.save(user);

        return UserResponseDto.from(user);
    }
}
