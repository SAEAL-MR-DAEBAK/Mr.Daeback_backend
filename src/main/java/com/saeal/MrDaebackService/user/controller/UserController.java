package com.saeal.MrDaebackService.user.controller;

import com.saeal.MrDaebackService.user.dto.request.RegisterDto;
import com.saeal.MrDaebackService.user.dto.response.UserResponseDto;
import com.saeal.MrDaebackService.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "User API", description = "User API 입니다.")
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserResponseDto> register(@Valid @RequestBody RegisterDto registerDto) {
        UserResponseDto result = userService.register(registerDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PatchMapping("/{username}/make-admin")
    public ResponseEntity<UserResponseDto> makeAdmin(@PathVariable String username) {
        UserResponseDto result = userService.makeAdmin(username);
        return ResponseEntity.ok(result);
    }
}
