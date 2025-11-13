package com.saeal.MrDaebackService.auth.dto;

import com.saeal.MrDaebackService.user.dto.response.UserResponseDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponseDto {
    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final UserResponseDto user;
}
