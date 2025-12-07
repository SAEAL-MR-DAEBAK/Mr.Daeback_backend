package com.saeal.MrDaebackService.voiceOrder.service.intent;

import com.saeal.MrDaebackService.voiceOrder.dto.response.OrderItemDto;
import com.saeal.MrDaebackService.voiceOrder.enums.OrderFlowState;
import com.saeal.MrDaebackService.voiceOrder.enums.UiAction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Intent 처리 결과
 */
@Data
@Builder
public class IntentResult {
    private String message;
    private OrderFlowState nextState;
    private UiAction uiAction;
    private List<OrderItemDto> updatedOrder;
    private String selectedAddress;
    private String memo;
    private String occasionType;                 // 기념일 종류
    private LocalDateTime requestedDeliveryTime; // 희망 배달 시간

    /**
     * 기본 결과 생성 (상태 변경 없음)
     */
    public static IntentResult of(String message, OrderFlowState nextState) {
        return IntentResult.builder()
                .message(message)
                .nextState(nextState)
                .uiAction(UiAction.NONE)
                .build();
    }

    /**
     * UI 액션 포함 결과 생성
     */
    public static IntentResult withAction(String message, OrderFlowState nextState, UiAction uiAction) {
        return IntentResult.builder()
                .message(message)
                .nextState(nextState)
                .uiAction(uiAction)
                .build();
    }
}
