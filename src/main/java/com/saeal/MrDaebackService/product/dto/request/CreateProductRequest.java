package com.saeal.MrDaebackService.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateProductRequest {

    @NotNull
    private String dinnerId;

    @NotNull
    private String servingStyleId;

    @NotNull
    @Min(1)
    private Integer quantity;

    private String memo;

    private String productName;
}
