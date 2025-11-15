package com.saeal.MrDaebackService.dinner;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "dinner")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dinner {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String dinnerName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal basePrice;

    @Builder.Default
    @Column(nullable = false)
    private boolean isActive = true;
}
