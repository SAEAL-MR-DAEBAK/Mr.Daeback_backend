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

        // ★ 커스터마이징용 필드
        private String menuItemName;    // 메뉴 아이템 이름 (예: "스테이크", "와인")
        private String item;            // LLM이 반환하는 아이템 이름 (menuItemName 대체)
        private String action;          // 액션 (add, remove, increase, decrease, 빼줘, 추가 등)
        private Integer menuItemQuantity; // 메뉴 아이템 수량 변경량
        private Integer itemIndex;      // 대상 아이템 인덱스 (#1, #2 -> 1, 2)

        // ★ 특별 요청사항/메모
        private String specialRequest;  // 특별 요청 (예: "젓가락 빼주세요")
        private String memo;            // 메모/요청사항 (예: "일회용 수저", "문 앞에 놔주세요")

        // ★ 복수 디너 처리
        private String pendingMenuName; // 대기 중인 다음 메뉴 이름 (2개 동시 언급 시)

        // ★ 기념일/배달 시간 관련
        private String occasionType;    // 기념일 종류 (생일, 기념일, 프로포즈, 결혼기념일 등)
        private String deliveryDate;    // 배달 날짜 (내일, 모레, 12월 25일 등)
        private String deliveryTime;    // 배달 시간 (저녁 7시, 오후 6시 등)

        /**
         * 메뉴 아이템 이름 가져오기 (menuItemName 또는 item)
         */
        public String getEffectiveMenuItemName() {
            if (item != null && !item.isEmpty()) {
                return item;
            }
            return menuItemName;
        }

        /**
         * 제외 액션인지 확인
         */
        public boolean isRemoveAction() {
            if (action == null) return false;
            String lowerAction = action.toLowerCase();
            return lowerAction.contains("remove") || lowerAction.contains("빼")
                    || lowerAction.contains("제외") || lowerAction.contains("삭제");
        }

        /**
         * 추가 액션인지 확인
         */
        public boolean isAddAction() {
            if (action == null) return false;
            String lowerAction = action.toLowerCase();
            return lowerAction.contains("add") || lowerAction.contains("추가")
                    || lowerAction.contains("더") || lowerAction.contains("넣어");
        }
    }
}
