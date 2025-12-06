package com.saeal.MrDaebackService.voiceOrder.enums;

/**
 * 음성 주문 대화 흐름 상태
 *
 * 순서:
 * 1. IDLE → 대기
 * 2. SELECTING_ADDRESS → 배달 주소 선택
 * 3. SELECTING_MENU → 디너 메뉴 선택
 * 4. SELECTING_STYLE → 서빙 스타일 선택
 * 5. SELECTING_QUANTITY → 수량 선택
 * 6. ASKING_MORE_DINNER → 추가 디너 주문 여부 확인
 * 7. SELECTING_ADDITIONAL_MENU → 추가 메뉴 아이템 선택 (개별 메뉴)
 * 8. ENTERING_MEMO → 메모/요청사항 입력
 * 9. CONFIRMING → 최종 확인
 * 10. CHECKOUT_READY → 결제 준비 완료 → 프론트에서 Cart API 호출 후 리디렉션
 */
public enum OrderFlowState {
    IDLE,                       // 대기 상태
    SELECTING_ADDRESS,          // 배달 주소 선택 중
    SELECTING_MENU,             // 디너 메뉴 선택 중
    SELECTING_STYLE,            // 서빙 스타일 선택 중
    SELECTING_QUANTITY,         // 수량 선택 중
    ASKING_MORE_DINNER,         // 추가 디너 주문 여부 확인
    SELECTING_ADDITIONAL_MENU,  // 추가 메뉴 아이템 선택 (스테이크 추가, 와인 추가 등)
    ENTERING_MEMO,              // 메모/요청사항 입력 (일회용품, 배달 요청 등)
    CONFIRMING,                 // 최종 확인 중
    CHECKOUT_READY              // 결제 준비 완료 → 프론트에서 Cart API 호출
}
