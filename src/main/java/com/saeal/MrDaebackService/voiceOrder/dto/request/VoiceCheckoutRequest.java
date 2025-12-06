package com.saeal.MrDaebackService.voiceOrder.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

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

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequest {
        private String dinnerId;
        private String servingStyleId;
        private int quantity;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AdditionalMenuItem {
        private String menuItemName;  // 예: "salad", "wine", "steak"
        private int quantity;
    }
}
