package com.saeal.MrDaebackService.cart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DiscountSummaryResponseDto {
    private BigDecimal subtotalBeforeDiscount;
    private BigDecimal grandTotalAfterDiscount;
    private BigDecimal discountRate; // 0.05, 0.10 등
}
