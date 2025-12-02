package com.saeal.MrDaebackService.user.dto.response;

import com.saeal.MrDaebackService.user.domain.UserCard;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class UserCardResponseDto {
    private String id;
    private String cardBrand;
    private String cardNumber;
    private Integer expiryMonth;
    private Integer expiryYear;
    private String cardHolderName;
    private String cvv;
    private boolean isDefault;

    public static UserCardResponseDto from(UserCard userCard) {
        return new UserCardResponseDto(
                userCard.getId().toString(),
                userCard.getCardBrand(),
                userCard.getCardNumber(),
                userCard.getExpiryMonth(),
                userCard.getExpiryYear(),
                userCard.getCardHolderName(),
                userCard.getCvv(),
                userCard.isDefault()
        );
    }
}
