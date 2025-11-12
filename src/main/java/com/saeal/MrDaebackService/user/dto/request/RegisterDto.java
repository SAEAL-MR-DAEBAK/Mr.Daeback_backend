package com.saeal.MrDaebackService.user.dto.request;

import lombok.Getter;

@Getter
public class RegisterDto {

    private String email;
    private String username;
    private String password;
    private String displayName;
    private String phoneNumber;
    private String address;
}
