package com.saeal.MrDaebackService.voiceOrder.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 음성 주문 결제 요청 DTO
 * - 프론트에서 currentOrder 그대로 전송
 * - 백엔드에서 Product → Cart → Order 생성
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceCheckoutRequest {

    private List<OrderItemRequest> orderItems;
    private List<AdditionalMenuItem> additionalMenuItems;
    private String deliveryAddress;
    private String memo;
    private LocalDateTime requestedDeliveryTime; // 희망 배달 시간
    private String occasionType; // 기념일 종류 (생일, 기념일, 프로포즈 등)

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        private String dinnerId;
        private String dinnerName;
        private String servingStyleId;
        private String servingStyleName;
        private int quantity;
        private int basePrice;      // dinner 기본 가격
        private int unitPrice;      // basePrice + styleExtraPrice
        private int totalPrice;     // unitPrice * quantity

        // ★ 커스터마이징 정보
        private Map<String, Integer> components;      // 구성요소 수량 (예: {"스테이크": 2, "샐러드": 1})
        private List<String> excludedItems;           // 제외 아이템 (예: ["와인"])
        private BigDecimal calculatedUnitPrice;       // 최종 계산된 단가 (커스터마이징 반영)
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdditionalMenuItem {
        private String menuItemName;  // 예: "salad", "wine", "steak"
        private int quantity;
    }
}
