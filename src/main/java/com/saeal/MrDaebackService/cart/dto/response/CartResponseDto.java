package com.saeal.MrDaebackService.cart.dto.response;

import com.saeal.MrDaebackService.cart.domain.Cart;
import com.saeal.MrDaebackService.cart.enums.DeliveryMethod;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CartResponseDto {
    private String id;
    private String userId;
    private List<CartItemResponseDto> items;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal grandTotal;
    private String deliveryAddress;
    private DeliveryMethod deliveryMethod;
    private String memo;
    private LocalDateTime requestedDeliveryTime; // 희망 배달 시간
    private String occasionType; // 기념일 종류
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CartResponseDto from(Cart cart) {
        List<CartItemResponseDto> itemResponses = cart.getProducts().stream()
                .map(product -> {
                    Integer qty = cart.getProductQuantities().get(product.getId());
                    BigDecimal unitPrice = product.getTotalPrice();
                    BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(qty));
                    return new CartItemResponseDto(
                            product.getId().toString(),
                            product.getProductName(),
                            qty,
                            unitPrice,
                            lineTotal
                    );
                })
                .collect(Collectors.toList());

        return new CartResponseDto(
                cart.getId().toString(),
                cart.getUser().getId().toString(),
                itemResponses,
                cart.getSubtotal(),
                cart.getDiscountAmount(),
                cart.getDeliveryFee(),
                cart.getGrandTotal(),
                cart.getDeliveryAddress(),
                cart.getDeliveryMethod(),
                cart.getMemo(),
                cart.getRequestedDeliveryTime(),
                cart.getOccasionType(),
                cart.getExpiresAt(),
                cart.getCreatedAt(),
                cart.getUpdatedAt()
        );
    }
}
