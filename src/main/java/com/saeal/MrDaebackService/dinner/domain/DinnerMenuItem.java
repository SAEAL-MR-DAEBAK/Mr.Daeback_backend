package com.saeal.MrDaebackService.dinner.domain;

import com.saeal.MrDaebackService.menuItems.domain.MenuItems;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "dinner_menu_item")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DinnerMenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dinner_id", nullable = false)
    private Dinner dinner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id", nullable = false)
    private MenuItems menuItem;

    @Column(nullable = false)
    private Integer defaultQuantity;
}
