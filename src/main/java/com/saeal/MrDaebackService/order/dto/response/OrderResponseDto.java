package com.saeal.MrDaebackService.order.dto.response;

import com.saeal.MrDaebackService.order.domain.Order;
import com.saeal.MrDaebackService.order.enums.DeliveryStatus;
import com.saeal.MrDaebackService.order.enums.OrderStatus;
import com.saeal.MrDaebackService.order.enums.PaymentStatus;
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
public class OrderResponseDto {
    private String id;
    private String orderNumber;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private DeliveryStatus deliveryStatus;
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal deliveryFee;
    private BigDecimal grandTotal;
    private String currency;
    private DeliveryMethod deliveryMethod;
    private String deliveryAddress;
    private String deliveryMemo;
    private String recipientName;
    private String recipientPhone;
    private String recipientEmail;
    private String paymentTransactionId;
    private String memo;
    private LocalDateTime orderedAt;
    private LocalDateTime updatedAt;
    private List<OrderItemResponseDto> items;

    public static OrderResponseDto from(Order order) {
        List<OrderItemResponseDto> itemResponses = order.getOrderItems().stream()
                .map(OrderItemResponseDto::from)
                .collect(Collectors.toList());

        return new OrderResponseDto(
                order.getId().toString(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                order.getPaymentStatus(),
                order.getDeliveryStatus(),
                order.getSubtotal(),
                order.getDiscountAmount(),
                order.getDeliveryFee(),
                order.getGrandTotal(),
                order.getCurrency(),
                order.getDeliveryMethod(),
                order.getDeliveryAddress(),
                order.getDeliveryMemo(),
                order.getRecipientName(),
                order.getRecipientPhone(),
                order.getRecipientEmail(),
                order.getPaymentTransactionId(),
                order.getMemo(),
                order.getOrderedAt(),
                order.getUpdatedAt(),
                itemResponses
        );
    }
}
