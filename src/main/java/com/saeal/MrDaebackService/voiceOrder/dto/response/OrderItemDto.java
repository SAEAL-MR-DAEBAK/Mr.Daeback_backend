package com.saeal.MrDaebackService.voiceOrder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDto {
    private String dinnerId;
    private String dinnerName;
    private String servingStyleId;
    private String servingStyleName;
    private String productId;   // 생성된 Product ID (스타일 선택 후 생성됨)
    private int quantity;
    private int basePrice;      // dinner 기본 가격 (스타일 변경 시 필요)
    private int unitPrice;
    private int totalPrice;
    @Builder.Default
    private List<AdditionalMenuItemDto> additionalMenuItems = new ArrayList<>();

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdditionalMenuItemDto {
        private String menuItemId;
        private String menuItemName;
        private int quantity;
    }
}
