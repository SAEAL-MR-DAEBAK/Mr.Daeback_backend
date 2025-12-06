package com.saeal.MrDaebackService.product.domain;

import com.saeal.MrDaebackService.dinner.domain.Dinner;
import com.saeal.MrDaebackService.product.enums.ProductType;
import com.saeal.MrDaebackService.servingStyle.domain.ServingStyle;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "product")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ProductType productType = ProductType.DINNER_PRODUCT;

    @ManyToOne(fetch = FetchType.LAZY)
    private Dinner dinner;

    @ManyToOne(fetch = FetchType.LAZY)
    private ServingStyle servingStyle;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProductMenuItem> productMenuItems = new ArrayList<>();

    @Column(nullable = false, length = 100)
    private String productName;

    @Column(nullable = false)
    @Setter
    private BigDecimal totalPrice;

    @Column(nullable = false)
    private int quantity;

    @Column(length = 255)
    @Setter
    private String memo;

    @Column(length = 255)
    private String address;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (productType == null) {
            productType = ProductType.DINNER_PRODUCT;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
