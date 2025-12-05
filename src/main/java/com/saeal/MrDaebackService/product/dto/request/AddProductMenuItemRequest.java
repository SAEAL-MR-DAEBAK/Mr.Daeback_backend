package com.saeal.MrDaebackService.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class AddProductMenuItemRequest {

    @NotBlank
    private String menuItemId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private BigDecimal unitPrice;
}
