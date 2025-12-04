package com.saeal.MrDaebackService.order.service;

import com.saeal.MrDaebackService.cart.domain.Cart;
import com.saeal.MrDaebackService.cart.enums.CartStatus;
import com.saeal.MrDaebackService.order.domain.Order;
import com.saeal.MrDaebackService.order.domain.OrderItem;
import com.saeal.MrDaebackService.order.enums.DeliveryStatus;
import com.saeal.MrDaebackService.order.enums.OrderStatus;
import com.saeal.MrDaebackService.order.enums.PaymentStatus;
import com.saeal.MrDaebackService.order.dto.response.OrderResponseDto;
import com.saeal.MrDaebackService.order.repository.OrderRepository;
import com.saeal.MrDaebackService.product.domain.Product;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public Order createOrderFromCart(Cart cart) {
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Product product : cart.getProducts()) {
            Integer quantity = cart.getProductQuantities().get(product.getId());
            if (quantity == null) {
                throw new IllegalStateException("Quantity not found for product: " + product.getId());
            }
            BigDecimal lineTotal = product.getTotalPrice().multiply(BigDecimal.valueOf(quantity));
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal discountAmount = cart.getDiscountAmount();
        BigDecimal deliveryFee = cart.getDeliveryFee();
        BigDecimal grandTotal = subtotal.add(deliveryFee).subtract(discountAmount);

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(cart.getUser())
                .cart(cart)
                .subtotal(subtotal)
                .discountAmount(discountAmount)
                .deliveryFee(deliveryFee)
                .grandTotal(grandTotal)
                .currency("KRW")
                .orderStatus(OrderStatus.PENDING_APPROVAL) // 관리자 승인 대기
                .paymentStatus(PaymentStatus.PENDING) // 관리자 승인 전까지는 PENDING
                .deliveryStatus(DeliveryStatus.READY)
                .deliveryMethod(cart.getDeliveryMethod())
                .deliveryAddress(cart.getDeliveryAddress())
                .deliveryMemo(cart.getMemo())
                .recipientName(cart.getUser().getDisplayName())
                .recipientPhone(cart.getUser().getPhoneNumber())
                .recipientEmail(cart.getUser().getEmail())
                .paymentTransactionId(UUID.randomUUID().toString())
                .memo(cart.getMemo())
                .orderedAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        for (Product product : cart.getProducts()) {
            Integer quantity = cart.getProductQuantities().get(product.getId());
            BigDecimal unitPrice = product.getTotalPrice();
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .lineTotal(lineTotal)
                    .optionSummary(product.getProductName())
                    .build();
            order.getOrderItems().add(orderItem);
        }

        return order;
    }

    @Transactional
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    @Transactional
    public List<OrderResponseDto> getOrdersByUserId(UUID userId) {
        return orderRepository.findByUserId(userId).stream()
                .map(OrderResponseDto::from)
                .toList();
    }

    @Transactional
    public List<OrderResponseDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(OrderResponseDto::from)
                .toList();
    }

    @Transactional
    public List<OrderResponseDto> getRecentOrders(int limit) {
        return orderRepository.findAll().stream()
                .sorted((a, b) -> b.getOrderedAt().compareTo(a.getOrderedAt())) // 최신순 정렬
                .limit(limit)
                .map(OrderResponseDto::from)
                .toList();
    }

    @Transactional
    public OrderResponseDto getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        return OrderResponseDto.from(order);
    }

    @Transactional
    public OrderResponseDto approveOrder(UUID orderId, boolean approved, String rejectionReason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (approved) {
            order.setOrderStatus(OrderStatus.APPROVED);
            order.setPaymentStatus(PaymentStatus.SUCCEEDED);
            order.setRejectionReason(null);
        } else {
            if (rejectionReason == null || rejectionReason.isBlank()) {
                throw new IllegalArgumentException("Rejection reason is required when rejecting an order");
            }
            order.setOrderStatus(OrderStatus.REJECTED);
            order.setPaymentStatus(PaymentStatus.FAILED);
            order.setRejectionReason(rejectionReason);
        }

        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        return OrderResponseDto.from(saved);
    }

    @Transactional
    public OrderResponseDto updateDeliveryStatus(UUID orderId, DeliveryStatus deliveryStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (order.getOrderStatus() != OrderStatus.APPROVED) {
            throw new IllegalStateException("Only approved orders can have delivery status updated");
        }

        order.setDeliveryStatus(deliveryStatus);
        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);
        return OrderResponseDto.from(saved);
    }

    private String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
