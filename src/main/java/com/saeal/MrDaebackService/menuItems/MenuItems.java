package com.saeal.MrDaebackService.menuItems;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "menu_Items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MenuItems {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(nullable = false, length = 20)
    private String unit;

    @Column(nullable = false)
    private Integer defaultQuantity; // 디너 1세트당 기본 개수

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isOptional = false;  // 빠질 수 있는지

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean isAdditionalAllowed = false; // 추가 가능한지

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean isStockManaged = true; // 재고 관리 대상인지

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

}
