package com.saeal.MrDaebackService.voiceOrder.dto.response;

import com.saeal.MrDaebackService.voiceOrder.enums.OrderFlowState;
import com.saeal.MrDaebackService.voiceOrder.enums.UiAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {
    private String userMessage;        // 사용자 메시지 (STT 변환 결과 포함)
    private String assistantMessage;   // AI 응답 메시지
    private OrderFlowState flowState;  // 현재 주문 흐름 상태
    private UiAction uiAction;         // 프론트 UI 액션 (백엔드에서 결정)
    private List<OrderItemDto> currentOrder;  // 현재 장바구니
    private List<AdditionalMenuItemDto> additionalMenuItems;  // 추가 메뉴 아이템
    private Integer totalPrice;        // 총 가격
    private String selectedAddress;    // 선택된 배달 주소
    private String memo;               // 메모/요청사항
    private LocalDateTime requestedDeliveryTime; // 희망 배달 시간
    private String occasionType;       // 기념일 종류 (생일, 기념일, 프로포즈 등)
}
