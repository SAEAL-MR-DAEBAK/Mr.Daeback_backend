package com.saeal.MrDaebackService.menuItems;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateMenuItemStockRequest {

    @NotNull
    @Min(0)
    private Integer stock;
}
