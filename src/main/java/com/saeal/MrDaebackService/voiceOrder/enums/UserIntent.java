package com.saeal.MrDaebackService.voiceOrder.enums;

/**
 * 사용자 발화 의도 분류 (간소화 버전)
 */
public enum UserIntent {
    // ★ 간소화된 주요 Intent
    ORDER_START,            // 첫 인사 또는 기념일/배달시간 언급 (occasionType, deliveryDate, deliveryTime 추출)
    ORDER_MENU,             // 메뉴 주문 (menuName, styleName, quantity 한번에 추출)
    CUSTOMIZE_MENU,         // 구성요소 변경 (components, excludedItems)
    PROCEED_CHECKOUT,       // 결제 진행 ("결제할게요", "응", "네")
    CANCEL_ORDER,           // 주문 취소
    ASK_MENU_INFO,          // 메뉴 정보 문의
    GREETING,               // 인사

    // ★ 레거시 호환용 (기존 핸들러와의 호환성 유지)
    @Deprecated SET_QUANTITY,
    @Deprecated SELECT_STYLE,
    @Deprecated SELECT_ADDRESS,
    @Deprecated ADD_MORE_DINNER,
    @Deprecated NO_MORE_DINNER,
    @Deprecated NO_CUSTOMIZE,
    @Deprecated ADD_ADDITIONAL_MENU,
    @Deprecated NO_ADDITIONAL_MENU,
    @Deprecated SET_MEMO,
    @Deprecated NO_MEMO,
    @Deprecated ADD_TO_CART,
    @Deprecated CONFIRM_YES,
    @Deprecated CONFIRM_NO,
    @Deprecated EDIT_ORDER,
    @Deprecated REMOVE_ITEM,
    @Deprecated SET_OCCASION,
    @Deprecated SET_DELIVERY_TIME,
    @Deprecated ASK_RECOMMENDATION,

    UNKNOWN                 // 알 수 없음
}
