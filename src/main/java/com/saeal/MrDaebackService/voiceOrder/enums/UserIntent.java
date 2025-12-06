package com.saeal.MrDaebackService.voiceOrder.enums;

/**
 * 사용자 발화 의도 분류
 */
public enum UserIntent {
    ORDER_MENU,         // 메뉴 주문
    SET_QUANTITY,       // 수량 설정
    SELECT_STYLE,       // 서빙 스타일 선택
    SELECT_ADDRESS,     // 배달 주소 선택
    ADD_TO_CART,        // 장바구니 담기 요청
    CONFIRM_YES,        // 긍정 응답
    CONFIRM_NO,         // 부정 응답
    EDIT_ORDER,         // 주문 수정
    REMOVE_ITEM,        // 개별 아이템 삭제
    CANCEL_ORDER,       // 주문 전체 취소
    ASK_MENU_INFO,      // 메뉴 정보 문의
    GREETING,           // 인사
    UNKNOWN             // 알 수 없음
}
