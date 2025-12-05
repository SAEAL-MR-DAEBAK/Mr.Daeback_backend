package com.saeal.MrDaebackService.voiceOrder.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * LLM이 반환하는 JSON 응답 구조
 * - LLM은 intent, entities, message만 반환
 * - 상태 전이, 장바구니 관리는 백엔드에서 처리
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LlmResponseDto {

    private String intent;          // 사용자 의도 (ORDER_MENU, SELECT_STYLE, ...)
    private ExtractedEntities entities;  // 추출된 엔티티
    private String message;         // 사용자에게 보여줄 메시지

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExtractedEntities {
        private String menuName;        // 메뉴 이름 (예: "불고기", "Valentine Dinner")
        private String styleName;       // 스타일 이름 (예: "Simple Style", "배달")
        private Integer quantity;       // 수량
        private Integer addressIndex;   // 주소 인덱스 (1, 2, 3...)
        private String newAddress;      // 새로 등록할 주소
        private List<AdditionalMenuItemEntity> additionalMenuItems; // 추가 메뉴 아이템 목록
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AdditionalMenuItemEntity {
        private String menuItemName;    // 메뉴 아이템 이름 (예: "김치", "콜라")
        private Integer quantity;       // 수량
    }
}
