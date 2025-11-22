package com.saeal.MrDaebackService.dinner.dto.response;

import com.saeal.MrDaebackService.dinner.DinnerMenuItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DinnerMenuItemResponseDto {
    private String menuItemId;
    private String menuItemName;
    private Integer defaultQuantity;

    public static DinnerMenuItemResponseDto from(DinnerMenuItem dinnerMenuItem) {
        return new DinnerMenuItemResponseDto(
                dinnerMenuItem.getMenuItem().getId().toString(),
                dinnerMenuItem.getMenuItem().getName(),
                dinnerMenuItem.getDefaultQuantity()
        );
    }
}
