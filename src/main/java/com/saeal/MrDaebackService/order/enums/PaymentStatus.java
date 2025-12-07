package com.saeal.MrDaebackService.order.enums;

public enum PaymentStatus {
    PENDING,    // 결제 대기
    SUCCEEDED,  // 결제 성공
    FAILED,     // 결제 실패
    REFUNDED    // 환불 완료
}
