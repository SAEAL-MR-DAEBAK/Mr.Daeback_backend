package com.saeal.MrDaebackService.dinner.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateDinnerMenuItemRequest {

    @NotNull
    private String dinnerId;

    @NotNull
    private String menuItemId;

    @NotNull
    @Min(1)
    private Integer defaultQuantity;
}
