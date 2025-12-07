package com.saeal.MrDaebackService.voiceOrder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 추가 메뉴 아이템 DTO
 * - 디너 구성이 아닌 별도 추가 메뉴 (예: 샐러드 2개 추가)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdditionalMenuItemDto {
    private String menuItemName;  // 메뉴 이름 (예: "샐러드", "와인")
    private int quantity;         // 수량
    private int unitPrice;        // 단가
    private int totalPrice;       // 총 가격 (unitPrice * quantity)
}
