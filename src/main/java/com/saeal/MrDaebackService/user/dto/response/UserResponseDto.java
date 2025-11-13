package com.saeal.MrDaebackService.user.dto.response;

import com.saeal.MrDaebackService.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private String id;
    private String email;
    private String username;
    private String displayName;
    private String phoneNumber;
    private String address;
    private String authority;

    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getId().toString(),
                user.getEmail(),
                user.getUsername(),
                user.getDisplayName(),
                user.getPhoneNumber(),
                user.getAddress(),
                user.getAuthority().name()
        );
    }
}
