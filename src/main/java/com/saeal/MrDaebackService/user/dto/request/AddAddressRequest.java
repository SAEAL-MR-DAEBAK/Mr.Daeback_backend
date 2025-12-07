package com.saeal.MrDaebackService.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AddAddressRequest {

    @NotBlank
    private String address;
}
