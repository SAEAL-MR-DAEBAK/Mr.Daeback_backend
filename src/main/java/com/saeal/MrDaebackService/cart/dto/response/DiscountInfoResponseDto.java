package com.saeal.MrDaebackService.cart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DiscountInfoResponseDto {
    private String loyaltyLevel;
    private BigDecimal discountRate; // 0.05, 0.10 등
    private BigDecimal discountPercent; // 5, 10 등
}
