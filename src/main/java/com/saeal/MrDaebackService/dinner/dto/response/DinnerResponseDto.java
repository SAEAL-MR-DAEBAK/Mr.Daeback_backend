package com.saeal.MrDaebackService.dinner.dto.response;

import com.saeal.MrDaebackService.dinner.Dinner;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class DinnerResponseDto {

    private String id;
    private String dinnerName;
    private String description;
    private BigDecimal basePrice;
    private boolean isActive;

    public static DinnerResponseDto from(Dinner dinner) {
        return new DinnerResponseDto(
                dinner.getId().toString(),
                dinner.getDinnerName(),
                dinner.getDescription(),
                dinner.getBasePrice(),
                dinner.isActive()
        );
    }
}
