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
        // null 체크 추가
        if (productMenuItem == null) {
            return null;
        }
        
        // menuItem이 null일 수 있으므로 체크
        String menuItemId = null;
        String menuItemName = null;
        if (productMenuItem.getMenuItem() != null) {
            menuItemId = productMenuItem.getMenuItem().getId() != null 
                    ? productMenuItem.getMenuItem().getId().toString() 
                    : null;
            menuItemName = productMenuItem.getMenuItem().getName();
        }
        
        return new ProductMenuItemResponseDto(
                menuItemId,
                menuItemName,
                productMenuItem.getQuantity(),
                productMenuItem.getUnitPrice(),
                productMenuItem.getLineTotal()
        );
    }
}
