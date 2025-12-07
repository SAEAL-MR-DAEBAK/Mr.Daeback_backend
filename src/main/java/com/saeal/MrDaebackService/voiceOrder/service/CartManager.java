package com.saeal.MrDaebackService.voiceOrder.service;

import com.saeal.MrDaebackService.dinner.dto.response.DinnerMenuItemResponseDto;
import com.saeal.MrDaebackService.dinner.dto.response.DinnerResponseDto;
import com.saeal.MrDaebackService.servingStyle.dto.response.ServingStyleResponseDto;
import com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.OrderItemRequestDto;
import com.saeal.MrDaebackService.voiceOrder.dto.response.OrderItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 임시 장바구니 관리
 */
@Component
@Slf4j
public class CartManager {

    /**
     * 기존 장바구니를 OrderItemDto 리스트로 변환
     */
    public List<OrderItemDto> convertToOrderItemDtoList(List<OrderItemRequestDto> currentOrder) {
        List<OrderItemDto> result = new ArrayList<>();
        if (currentOrder == null) return result;

        for (OrderItemRequestDto item : currentOrder) {
            // components가 null이면 빈 Map 사용
            Map<String, Integer> components = item.getComponents() != null
                    ? new LinkedHashMap<>(item.getComponents())
                    : new LinkedHashMap<>();

            // excludedItems가 null이면 빈 List 사용
            List<String> excludedItems = item.getExcludedItems() != null
                    ? new ArrayList<>(item.getExcludedItems())
                    : new ArrayList<>();

            result.add(OrderItemDto.builder()
                    .dinnerId(item.getDinnerId())
                    .dinnerName(item.getDinnerName())
                    .servingStyleId(item.getServingStyleId())
                    .servingStyleName(item.getServingStyleName())
                    .quantity(item.getQuantity())
                    .basePrice(item.getBasePrice())
                    .unitPrice(item.getUnitPrice())
                    .totalPrice(item.getTotalPrice())
                    .components(components)
                    .excludedItems(excludedItems)
                    .itemIndex(item.getItemIndex())
                    .build());
        }
        return result;
    }

    /**
     * 메뉴 추가
     */
    public OrderItemDto addMenu(DinnerResponseDto dinner, int quantity) {
        int basePrice = dinner.getBasePrice().intValue();
        return OrderItemDto.builder()
                .dinnerId(dinner.getId().toString())
                .dinnerName(dinner.getDinnerName())
                .quantity(quantity)
                .basePrice(basePrice)
                .unitPrice(basePrice)
                .totalPrice(basePrice * quantity)
                .components(extractComponents(dinner))
                .build();
    }

    /**
     * 메뉴 추가 (수량 없이 - 임시 아이템)
     */
    public OrderItemDto addMenuWithoutQuantity(DinnerResponseDto dinner) {
        int basePrice = dinner.getBasePrice().intValue();
        return OrderItemDto.builder()
                .dinnerId(dinner.getId().toString())
                .dinnerName(dinner.getDinnerName())
                .quantity(0)  // 아직 확정되지 않음
                .basePrice(basePrice)
                .unitPrice(basePrice)
                .totalPrice(0)
                .components(extractComponents(dinner))
                .build();
    }

    /**
     * DinnerResponseDto에서 구성요소 추출
     */
    private Map<String, Integer> extractComponents(DinnerResponseDto dinner) {
        Map<String, Integer> components = new LinkedHashMap<>();
        if (dinner.getMenuItems() != null) {
            for (DinnerMenuItemResponseDto item : dinner.getMenuItems()) {
                components.put(item.getMenuItemName(), item.getDefaultQuantity());
            }
        }
        return components;
    }

    /**
     * 수량 설정
     */
    public OrderItemDto setQuantity(OrderItemDto item, int quantity) {
        return OrderItemDto.builder()
                .dinnerId(item.getDinnerId())
                .dinnerName(item.getDinnerName())
                .servingStyleId(item.getServingStyleId())
                .servingStyleName(item.getServingStyleName())
                .quantity(quantity)
                .basePrice(item.getBasePrice())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getUnitPrice() * quantity)
                .components(item.getComponents())
                .excludedItems(item.getExcludedItems())
                .itemIndex(item.getItemIndex())
                .build();
    }

    /**
     * 아이템에 스타일 적용 (처음 적용)
     */
    public OrderItemDto applyStyleToItem(OrderItemDto item, ServingStyleResponseDto style) {
        int newUnitPrice = item.getBasePrice() + style.getExtraPrice().intValue();

        return OrderItemDto.builder()
                .dinnerId(item.getDinnerId())
                .dinnerName(item.getDinnerName())
                .servingStyleId(style.getId().toString())
                .servingStyleName(style.getStyleName())
                .quantity(item.getQuantity())
                .basePrice(item.getBasePrice())
                .unitPrice(newUnitPrice)
                .totalPrice(newUnitPrice * item.getQuantity())
                .components(item.getComponents())
                .excludedItems(item.getExcludedItems())
                .itemIndex(item.getItemIndex())
                .build();
    }

    /**
     * 스타일 변경 (기존 스타일 → 새 스타일)
     * basePrice + 새 스타일 가격으로 계산
     */
    public OrderItemDto changeStyle(OrderItemDto item, ServingStyleResponseDto newStyle) {
        int newUnitPrice = item.getBasePrice() + newStyle.getExtraPrice().intValue();

        return OrderItemDto.builder()
                .dinnerId(item.getDinnerId())
                .dinnerName(item.getDinnerName())
                .servingStyleId(newStyle.getId().toString())
                .servingStyleName(newStyle.getStyleName())
                .quantity(item.getQuantity())
                .basePrice(item.getBasePrice())
                .unitPrice(newUnitPrice)
                .totalPrice(newUnitPrice * item.getQuantity())
                .components(item.getComponents())
                .excludedItems(item.getExcludedItems())
                .itemIndex(item.getItemIndex())
                .build();
    }

    /**
     * 총 가격 계산
     */
    public int calculateTotalPrice(List<OrderItemDto> orderItems) {
        return orderItems.stream()
                .mapToInt(OrderItemDto::getTotalPrice)
                .sum();
    }
}
