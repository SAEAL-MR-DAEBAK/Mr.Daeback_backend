package com.saeal.MrDaebackService.order.domain;

import com.saeal.MrDaebackService.cart.domain.Cart;
import com.saeal.MrDaebackService.cart.enums.DeliveryMethod;
import com.saeal.MrDaebackService.order.enums.DeliveryStatus;
import com.saeal.MrDaebackService.order.enums.OrderStatus;
import com.saeal.MrDaebackService.order.enums.PaymentStatus;
import com.saeal.MrDaebackService.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 40)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal deliveryFee;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal grandTotal;

    @Column(length = 3, nullable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus orderStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DeliveryStatus deliveryStatus;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod;

    @Column(length = 255)
    private String deliveryAddress;

    @Column(length = 255)
    private String deliveryMemo;

    @Column(length = 100)
    private String recipientName;

    @Column(length = 50)
    private String recipientPhone;

    @Column(length = 100)
    private String recipientEmail;

    @Column(length = 100)
    private String paymentMethodId;

    @Column(length = 100)
    private String paymentTransactionId;

    @Column(length = 255)
    private String memo;

    @Column(nullable = false)
    private LocalDateTime orderedAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (subtotal == null) {
            subtotal = BigDecimal.ZERO;
        }
        if (discountAmount == null) {
            discountAmount = BigDecimal.ZERO;
        }
        if (deliveryFee == null) {
            deliveryFee = BigDecimal.ZERO;
        }
        if (grandTotal == null) {
            grandTotal = BigDecimal.ZERO;
        }
        if (currency == null) {
            currency = "KRW";
        }
        if (orderStatus == null) {
            orderStatus = OrderStatus.PLACED;
        }
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.PENDING;
        }
        if (deliveryStatus == null) {
            deliveryStatus = DeliveryStatus.READY;
        }
        if (orderedAt == null) {
            orderedAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
