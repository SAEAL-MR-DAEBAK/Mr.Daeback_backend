package com.saeal.MrDaebackService.dinner.dto.response;

import com.saeal.MrDaebackService.dinner.domain.Dinner;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DinnerResponseDto {

    private String id;
    private String dinnerName;
    private String description;
    private BigDecimal basePrice;
    private boolean isActive;
    private List<DinnerMenuItemResponseDto> menuItems;

    public static DinnerResponseDto from(Dinner dinner) {
        return new DinnerResponseDto(
                dinner.getId().toString(),
                dinner.getDinnerName(),
                dinner.getDescription(),
                dinner.getBasePrice(),
                dinner.isActive(),
                new ArrayList<>()  // menuItems는 별도로 조회 필요
        );
    }

    /**
     * 메뉴 아이템 정보 포함하여 변환
     */
    public static DinnerResponseDto fromWithMenuItems(Dinner dinner) {
        List<DinnerMenuItemResponseDto> items = new ArrayList<>();
        if (dinner.getDinnerMenuItems() != null) {
            items = dinner.getDinnerMenuItems().stream()
                    .map(DinnerMenuItemResponseDto::from)
                    .collect(Collectors.toList());
        }

        return DinnerResponseDto.builder()
                .id(dinner.getId().toString())
                .dinnerName(dinner.getDinnerName())
                .description(dinner.getDescription())
                .basePrice(dinner.getBasePrice())
                .isActive(dinner.isActive())
                .menuItems(items)
                .build();
    }
}
