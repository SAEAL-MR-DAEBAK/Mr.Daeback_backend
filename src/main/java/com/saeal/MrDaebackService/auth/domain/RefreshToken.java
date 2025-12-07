package com.saeal.MrDaebackService.auth.domain;

import com.saeal.MrDaebackService.user.domain.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 512)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public boolean isExpired(LocalDateTime now) {
        return now.isAfter(expiryDate);
    }

    public void rotate(String nextToken, LocalDateTime nextExpiry) {
        this.token = nextToken;
        this.expiryDate = nextExpiry;
    }
}
