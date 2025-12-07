package com.saeal.MrDaebackService.dinner.dto.response;

import com.saeal.MrDaebackService.dinner.domain.DinnerMenuItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DinnerMenuItemResponseDto {
    private String menuItemId;
    private String menuItemName;
    private Integer defaultQuantity;
    private BigDecimal unitPrice;  // 메뉴 아이템 단가

    public static DinnerMenuItemResponseDto from(DinnerMenuItem dinnerMenuItem) {
        return new DinnerMenuItemResponseDto(
                dinnerMenuItem.getMenuItem().getId().toString(),
                dinnerMenuItem.getMenuItem().getName(),
                dinnerMenuItem.getDefaultQuantity(),
                dinnerMenuItem.getMenuItem().getUnitPrice()
        );
    }
}
