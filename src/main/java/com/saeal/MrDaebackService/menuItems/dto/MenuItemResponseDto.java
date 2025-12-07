package com.saeal.MrDaebackService.menuItems.dto;

import com.saeal.MrDaebackService.menuItems.domain.MenuItems;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemResponseDto {

    private String id;
    private String name;
    private Integer stock;
    private BigDecimal unitPrice;
    private String unitType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MenuItemResponseDto from(MenuItems menuItems) {
        return new MenuItemResponseDto(
                menuItems.getId().toString(),
                menuItems.getName(),
                menuItems.getStock(),
                menuItems.getUnitPrice(),
                menuItems.getUnitType(),
                menuItems.getCreatedAt(),
                menuItems.getUpdatedAt()
        );
    }
}
