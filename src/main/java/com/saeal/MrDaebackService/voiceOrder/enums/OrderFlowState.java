package com.saeal.MrDaebackService.voiceOrder.enums;

/**
 * 음성 주문 대화 흐름 상태
 */
public enum OrderFlowState {
    IDLE,               // 대기 상태
    SELECTING_ADDRESS,  // 배달 주소 선택 중 (처음에 받음)
    SELECTING_MENU,     // 메뉴 선택 중
    SELECTING_STYLE,    // 서빙 스타일 선택 중
    SELECTING_QUANTITY, // 수량 선택 중
    CUSTOMIZING,        // 주문 옵션 처리 중 (메뉴 구성 변경/추가 메뉴/특별 요청사항)
    ASKING_MORE,        // 추가 주문 여부 확인
    CONFIRMING          // 최종 확인 중 → 모달 표시 → 프론트에서 Cart API 호출
}
