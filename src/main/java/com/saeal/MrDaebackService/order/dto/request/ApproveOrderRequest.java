package com.saeal.MrDaebackService.order.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ApproveOrderRequest {
    @NotNull
    private Boolean approved; // true: 승인, false: 거절
    
    private String rejectionReason; // 거절 사유 (거절 시 필수)
}





