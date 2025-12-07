package com.saeal.MrDaebackService.voiceOrder.enums;

/**
 * 프론트엔드 UI 액션 - 백엔드에서 결정하여 전달
 */
public enum UiAction {
    NONE,                    // 특별한 UI 액션 없음 (메시지만 표시)
    SHOW_CONFIRM_MODAL,      // 최종 주문 확인 모달 표시
    SHOW_CANCEL_CONFIRM,     // 주문 취소 확인
    UPDATE_ORDER_LIST,       // 임시장바구니 업데이트
    PROCEED_TO_CHECKOUT      // 결제 진행 → 프론트에서 Cart API 호출 후 주문내역으로 리디렉션
}
