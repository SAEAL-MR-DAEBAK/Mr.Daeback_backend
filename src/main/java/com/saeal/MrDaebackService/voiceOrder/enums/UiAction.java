package com.saeal.MrDaebackService.voiceOrder.enums;

/**
 * 프론트엔드 UI 액션 - 백엔드에서 결정하여 전달
 */
public enum UiAction {
    NONE,                    // 특별한 UI 액션 없음 (메시지만 표시)
    SHOW_CONFIRM_MODAL,      // 장바구니 담기 확인 모달 표시
    SHOW_CANCEL_CONFIRM,     // 주문 취소 확인 모달 표시
    UPDATE_ORDER_LIST        // 임시장바구니 업데이트
}
