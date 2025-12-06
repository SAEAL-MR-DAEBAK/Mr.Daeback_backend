package com.saeal.MrDaebackService.voiceOrder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private int quantity;
    private int basePrice;      // dinner 기본 가격 (스타일 변경 시 필요)
    private int unitPrice;
    private int totalPrice;
}
