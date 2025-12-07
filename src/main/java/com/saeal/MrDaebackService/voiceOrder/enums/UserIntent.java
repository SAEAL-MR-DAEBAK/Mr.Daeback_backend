package com.saeal.MrDaebackService.voiceOrder.enums;

/**
 * 사용자 발화 의도 분류
 */
public enum UserIntent {
    // 디너 주문 관련
    ORDER_MENU,             // 디너 메뉴 주문
    SET_QUANTITY,           // 수량 설정
    SELECT_STYLE,           // 서빙 스타일 선택
    SELECT_ADDRESS,         // 배달 주소 선택

    // 추가 디너 주문
    ADD_MORE_DINNER,        // 다른 디너 추가 주문하겠다
    NO_MORE_DINNER,         // 디너는 더 안 주문하겠다

    // 구성요소 커스터마이징 (각 디너의 메뉴 구성 변경)
    CUSTOMIZE_MENU,         // 구성요소 변경 (스테이크 빼줘, 샐러드 추가)
    NO_CUSTOMIZE,           // 구성요소 변경 없음 (그대로 할게요)

    // 추가 메뉴 아이템 (스테이크, 와인 등)
    ADD_ADDITIONAL_MENU,    // 추가 메뉴 아이템 주문
    NO_ADDITIONAL_MENU,     // 추가 메뉴 없음

    // 메모/요청사항
    SET_MEMO,               // 메모/요청사항 설정
    NO_MEMO,                // 메모 없음

    // 주문 완료/결제
    PROCEED_CHECKOUT,       // 결제 진행
    ADD_TO_CART,            // 장바구니 담기 (레거시 호환)

    // 확인 응답
    CONFIRM_YES,            // 긍정 응답
    CONFIRM_NO,             // 부정 응답

    // 수정/취소
    EDIT_ORDER,             // 주문 수정
    REMOVE_ITEM,            // 개별 아이템 삭제
    CANCEL_ORDER,           // 주문 전체 취소

    // 기념일/배달 시간
    SET_OCCASION,           // 기념일 종류 설정 (생일, 기념일, 프로포즈 등)
    SET_DELIVERY_TIME,      // 배달 시간 설정
    ASK_RECOMMENDATION,     // 디너 추천 요청

    // 기타
    ASK_MENU_INFO,          // 메뉴 정보 문의
    GREETING,               // 인사
    UNKNOWN                 // 알 수 없음
}
