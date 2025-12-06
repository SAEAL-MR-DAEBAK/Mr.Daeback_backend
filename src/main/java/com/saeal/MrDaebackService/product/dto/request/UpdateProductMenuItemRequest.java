package com.saeal.MrDaebackService.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateProductMenuItemRequest {

    @NotNull
    @Min(0)  // 0 허용 (메뉴 아이템 제거 가능)
    private Integer quantity;
}
