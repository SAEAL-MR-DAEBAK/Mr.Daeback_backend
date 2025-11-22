package com.saeal.MrDaebackService.servingStyle.dto.response;

import com.saeal.MrDaebackService.servingStyle.domain.ServingStyle;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ServingStyleResponseDto {

    private String id;
    private String styleName;
    private String description;
    private BigDecimal extraPrice;
    private boolean isActive;

    public static ServingStyleResponseDto from(ServingStyle servingStyle) {
        return new ServingStyleResponseDto(
                servingStyle.getId().toString(),
                servingStyle.getStyleName(),
                servingStyle.getDescription(),
                servingStyle.getExtraPrice(),
                servingStyle.isActive()
        );
    }
}
