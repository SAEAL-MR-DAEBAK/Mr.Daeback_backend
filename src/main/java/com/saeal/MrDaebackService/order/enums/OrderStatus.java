package com.saeal.MrDaebackService.order.enums;

public enum OrderStatus {
    PLACED,            // 주문 생성
    PENDING_APPROVAL, // 관리자 승인 대기
    APPROVED,          // 관리자 승인 완료
    REJECTED,          // 관리자 거절
    PAID,              // 결제 완료
    CANCELLED,         // 취소
    REFUNDED           // 환불 완료
}
