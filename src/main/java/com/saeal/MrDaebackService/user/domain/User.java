package com.saeal.MrDaebackService.user.domain;

import com.saeal.MrDaebackService.user.enums.Authority;
import com.saeal.MrDaebackService.user.enums.LoyaltyLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "app_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String email;

    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Authority authority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoyaltyLevel loyaltyLevel;

    @Column(nullable = false)
    private Long visitCount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal totalSpent;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @ElementCollection
    @CollectionTable(name = "user_addresses", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "address", nullable = false)
    @Builder.Default
    private List<String> addresses = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<UserCard> userCards = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        if (this.loyaltyLevel == null) {
            this.loyaltyLevel = LoyaltyLevel.BASIC;
        }
        if (this.visitCount == null) {
            this.visitCount = 0L;
        }
        if (this.totalSpent == null) {
            this.totalSpent = BigDecimal.ZERO;
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
