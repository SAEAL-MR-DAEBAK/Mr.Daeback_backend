package com.saeal.MrDaebackService.voiceOrder.enums;

/**
 * 음성 주문 대화 흐름 상태
 *
 * 순서:
 * 1. IDLE → 대기
 * 2. SELECTING_ADDRESS → 배달 주소 선택
 * 3. ASKING_OCCASION → 기념일 종류 질문 (특별한 날을 위한 서비스!)
 * 4. ASKING_DELIVERY_TIME → 배달 시간 질문
 * 5. SELECTING_MENU → 디너 메뉴 선택
 * 6. SELECTING_STYLE → 서빙 스타일 선택
 * 7. SELECTING_QUANTITY → 수량 선택
 * 8. ASKING_MORE_DINNER → 추가 디너 주문 여부 확인
 * 9. CUSTOMIZING_MENU → 개별 상품 메뉴 구성 커스터마이징 (+/- 메뉴)
 * 10. SELECTING_ADDITIONAL_MENU → 추가 메뉴 아이템 선택 (개별 메뉴)
 * 11. ENTERING_MEMO → 메모/요청사항 입력
 * 12. CONFIRMING → 최종 확인
 * 13. CHECKOUT_READY → 결제 준비 완료 → 프론트에서 Cart API 호출 후 리디렉션
 */
public enum OrderFlowState {
    IDLE,                       // 대기 상태
    SELECTING_ADDRESS,          // 배달 주소 선택 중
    ASKING_OCCASION,            // 기념일 종류 질문 중 (생일, 기념일, 프로포즈 등)
    ASKING_DELIVERY_TIME,       // 배달 시간 질문 중
    SELECTING_MENU,             // 디너 메뉴 선택 중
    SELECTING_STYLE,            // 서빙 스타일 선택 중
    SELECTING_QUANTITY,         // 수량 선택 중
    ASKING_MORE_DINNER,         // 추가 디너 주문 여부 확인
    CUSTOMIZING_MENU,           // 개별 상품 메뉴 구성 커스터마이징 (GUI처럼 +/- 가능)
    SELECTING_ADDITIONAL_MENU,  // 추가 메뉴 아이템 선택 (스테이크 추가, 와인 추가 등)
    ENTERING_MEMO,              // 메모/요청사항 입력 (일회용품, 배달 요청 등)
    CONFIRMING,                 // 최종 확인 중
    CHECKOUT_READY              // 결제 준비 완료 → 프론트에서 Cart API 호출
}
