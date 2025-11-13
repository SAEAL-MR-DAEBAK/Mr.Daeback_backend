package com.saeal.MrDaebackService.auth.service;

import com.saeal.MrDaebackService.auth.dto.LoginDto;
import com.saeal.MrDaebackService.auth.dto.LoginResponseDto;
import com.saeal.MrDaebackService.jwt.JwtTokenProvider;
import com.saeal.MrDaebackService.user.domain.User;
import com.saeal.MrDaebackService.user.dto.response.UserResponseDto;
import com.saeal.MrDaebackService.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public LoginResponseDto login(LoginDto loginDto) {
        User user = userRepository
                .findByUsernameIgnoreCase(loginDto.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(loginDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtTokenProvider.generateToken(user);
        return new LoginResponseDto(token, "Bearer", UserResponseDto.from(user));
    }
}
