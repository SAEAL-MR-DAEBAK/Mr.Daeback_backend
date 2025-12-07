package com.saeal.MrDaebackService.menuItems.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class CreateMenuItemRequest {

    @NotBlank
    private String name;

    @NotNull
    @Min(0)
    private Integer stock;

    @NotNull
    private BigDecimal unitPrice;

    private String unitType;
}
