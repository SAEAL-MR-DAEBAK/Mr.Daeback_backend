package com.saeal.MrDaebackService.voiceOrder.dto.response;

import com.saeal.MrDaebackService.voiceOrder.enums.UiAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponseDto {
    private String userMessage;        // 사용자 메시지 (STT 변환 결과 포함)
    private String assistantMessage;   // AI 응답 메시지
    private UiAction uiAction;         // 프론트 UI 액션 (백엔드에서 결정)
    private List<OrderItemDto> currentOrder;  // 현재 장바구니
    private Integer totalPrice;        // 총 가격
    private String selectedAddress;    // 선택된 배달 주소
}
