package com.saeal.MrDaebackService.voiceOrder.enums;

/**
 * 음성 주문 대화 흐름 상태
 * 순서: IDLE → SELECTING_ADDRESS → SELECTING_MENU → SELECTING_QUANTITY → SELECTING_STYLE → ASKING_MORE → CONFIRMING
 */
public enum OrderFlowState {
    IDLE,               // 대기 상태
    SELECTING_ADDRESS,  // 배달 주소 선택 중 (주문 시작 시 먼저 주소 선택)
    SELECTING_MENU,     // 메뉴 선택 중
    SELECTING_QUANTITY, // 수량 선택 중
    SELECTING_STYLE,    // 서빙 스타일 선택 중
    ASKING_MORE,        // 추가 주문 여부 확인
    CONFIRMING          // 최종 확인 중 → 모달 표시 → 프론트에서 Cart API 호출
}
