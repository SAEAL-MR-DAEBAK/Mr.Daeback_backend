package com.saeal.MrDaebackService.auth.controller;

import com.saeal.MrDaebackService.auth.dto.LoginDto;
import com.saeal.MrDaebackService.auth.dto.LoginResponseDto;
import com.saeal.MrDaebackService.auth.service.AuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
@Tag(name = "Auth API", description = "로그인 등 인증 관련 API 입니다.")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginDto loginDto) {
        LoginResponseDto result = authService.login(loginDto);
        return ResponseEntity.ok(result);
    }
}
