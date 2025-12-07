package com.saeal.MrDaebackService.voiceOrder.service.intent;

import com.saeal.MrDaebackService.voiceOrder.dto.LlmResponseDto;
import com.saeal.MrDaebackService.voiceOrder.dto.response.OrderItemDto;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Intent 처리에 필요한 컨텍스트 정보
 */
@Data
@Builder
public class IntentContext {
    private String userMessage;
    private LlmResponseDto llmResponse;
    private List<OrderItemDto> orderItems;
    private String selectedAddress;
    private List<String> userAddresses;

    // 진행 중인 아이템 정보
    private OrderItemDto pendingItem;
    private int pendingItemIndex;

    // 기념일/배달 시간 정보
    private String occasionType;                 // 기념일 종류 (생일, 기념일, 프로포즈 등)
    private LocalDateTime requestedDeliveryTime; // 희망 배달 시간
}
