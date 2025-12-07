package com.saeal.MrDaebackService.voiceOrder.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 음성 주문 결제 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceCheckoutResponse {

    private boolean success;
    private String orderId;
    private String orderNumber;
    private BigDecimal totalPrice;
    private String message;
    private String errorMessage;

    public static VoiceCheckoutResponse success(String orderId, String orderNumber, BigDecimal totalPrice) {
        return VoiceCheckoutResponse.builder()
                .success(true)
                .orderId(orderId)
                .orderNumber(orderNumber)
                .totalPrice(totalPrice)
                .message("주문이 완료되었습니다!")
                .build();
    }

    public static VoiceCheckoutResponse failure(String errorMessage) {
        return VoiceCheckoutResponse.builder()
                .success(false)
                .errorMessage(errorMessage)
                .message("주문 처리 중 오류가 발생했습니다.")
                .build();
    }
}
