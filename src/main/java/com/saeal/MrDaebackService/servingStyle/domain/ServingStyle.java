package com.saeal.MrDaebackService.servingStyle.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "serving_style")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServingStyle {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false)
    private String styleName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal extraPrice;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private boolean isActive = true;
}
