package com.saeal.MrDaebackService.cart.domain;

import com.saeal.MrDaebackService.cart.enums.DeliveryMethod;
import com.saeal.MrDaebackService.cart.enums.CartStatus;
import com.saeal.MrDaebackService.product.domain.Product;
import com.saeal.MrDaebackService.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "cart")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToMany
    @JoinTable(
            name = "cart_product",
            joinColumns = @JoinColumn(name = "cart_id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "product_id", nullable = false)
    )
    @Builder.Default
    private List<Product> products = new ArrayList<>();

    @ElementCollection
    @CollectionTable(
            name = "cart_product_quantity",
            joinColumns = @JoinColumn(name = "cart_id", nullable = false)
    )
    @MapKeyColumn(name = "product_id")
    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Map<UUID, Integer> productQuantities = new HashMap<>();

    // ★ 프론트엔드에서 계산한 단가 저장 (커스터마이징 반영된 가격)
    @ElementCollection
    @CollectionTable(
            name = "cart_product_unit_price",
            joinColumns = @JoinColumn(name = "cart_id", nullable = false)
    )
    @MapKeyColumn(name = "product_id")
    @Column(name = "unit_price", nullable = false, precision = 18, scale = 2)
    @Builder.Default
    private Map<UUID, BigDecimal> productUnitPrices = new HashMap<>();

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal deliveryFee;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal grandTotal;

    @Column(length = 255)
    private String deliveryAddress;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod;

    @Column(length = 255)
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CartStatus status;

    private LocalDateTime expiresAt;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
        if (productQuantities == null) {
            productQuantities = new HashMap<>();
        }
        if (status == null) {
            status = CartStatus.OPEN;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
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
