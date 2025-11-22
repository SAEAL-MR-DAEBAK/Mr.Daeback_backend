package com.saeal.MrDaebackService.product.dto.response;

import com.saeal.MrDaebackService.product.domain.ProductMenuItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductMenuItemResponseDto {
    private String menuItemId;
    private String menuItemName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;

    public static ProductMenuItemResponseDto from(ProductMenuItem productMenuItem) {
        return new ProductMenuItemResponseDto(
                productMenuItem.getMenuItem().getId().toString(),
                productMenuItem.getMenuItem().getName(),
                productMenuItem.getQuantity(),
                productMenuItem.getUnitPrice(),
                productMenuItem.getLineTotal()
        );
    }
}
