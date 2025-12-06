package com.saeal.MrDaebackService.voiceOrder.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatRequestDto {
    private String message;                      // 텍스트 메시지
    private String audioBase64;                  // Base64 인코딩된 오디오
    private String audioFormat;                  // 오디오 포맷 (webm, wav 등)
    private List<ChatMessageDto> conversationHistory;  // 대화 히스토리
    private List<OrderItemRequestDto> currentOrder;    // 현재 장바구니
    private String selectedAddress;              // 선택된 배달 주소

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessageDto {
        private String role;    // "user" or "assistant"
        private String content;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemRequestDto {
        private String dinnerId;
        private String dinnerName;
        private String servingStyleId;
        private String servingStyleName;
        private int quantity;
        private int basePrice;      // dinner 기본 가격
        private int unitPrice;
        private int totalPrice;
    }
}
