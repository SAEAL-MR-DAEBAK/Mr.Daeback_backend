package com.saeal.MrDaebackService.user.dto.response;

import com.saeal.MrDaebackService.user.domain.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private String id;
    private String email;
    private String username;
    private String displayName;
    private String phoneNumber;
    private List<String> addresses;
    private List<UserCardResponseDto> cards;
    private String authority;
    private String loyaltyLevel;
    private Long visitCount;
    private String totalSpent;

    public static UserResponseDto from(User user) {
        return new UserResponseDto(
                user.getId().toString(),
                user.getEmail(),
                user.getUsername(),
                user.getDisplayName(),
                user.getPhoneNumber(),
                user.getAddresses() == null ? Collections.emptyList() : user.getAddresses(),
                user.getUserCards() == null
                        ? Collections.emptyList()
                        : user.getUserCards().stream()
                        .map(UserCardResponseDto::from)
                        .collect(Collectors.toList()),
                user.getAuthority().name(),
                user.getLoyaltyLevel().name(),
                user.getVisitCount(),
                user.getTotalSpent().toPlainString()
        );
    }
}
