package com.saeal.MrDaebackService.cart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class CartMenuItemsTotalResponse {
    private final String cartId;
    private final BigDecimal totalMenuItemsPrice;
}
