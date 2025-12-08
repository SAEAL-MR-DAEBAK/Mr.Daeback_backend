package com.saeal.MrDaebackService.voiceOrder.enums;

/**
 * 음성 주문 대화 흐름 상태 (간소화 버전)
 *
 * ★ 새로운 간소화 플로우:
 * 1. ORDERING → 주문 진행 중 (메뉴+스타일+수량 한번에, 기념일/배달시간 자동 추출)
 * 2. CHECKOUT_READY → 결제 준비 완료
 *
 * ★ 주소는 첫 번째 주소로 자동 선택
 * ★ 기념일/배달시간은 첫 메시지에서 자동 추출
 * ★ 커스터마이징은 사용자가 원할 때만 처리
 */
public enum OrderFlowState {
    IDLE,                       // 대기 상태 (초기 인사)
    ORDERING,                   // 주문 진행 중 (메뉴 선택, 커스터마이징 등 통합)
    CHECKOUT_READY,             // 결제 준비 완료 → 프론트에서 Cart API 호출

    // ★ 레거시 호환용 (기존 프론트엔드와의 호환성 유지)
    @Deprecated SELECTING_ADDRESS,
    @Deprecated ASKING_OCCASION,
    @Deprecated ASKING_DELIVERY_TIME,
    @Deprecated SELECTING_MENU,
    @Deprecated SELECTING_STYLE,
    @Deprecated SELECTING_QUANTITY,
    @Deprecated ASKING_MORE_DINNER,
    @Deprecated CUSTOMIZING_MENU,
    @Deprecated SELECTING_ADDITIONAL_MENU,
    @Deprecated ENTERING_MEMO,
    @Deprecated CONFIRMING
}
