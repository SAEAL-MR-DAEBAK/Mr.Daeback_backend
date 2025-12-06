package com.saeal.MrDaebackService.voiceOrder.service;

import com.saeal.MrDaebackService.dinner.dto.response.DinnerResponseDto;
import com.saeal.MrDaebackService.servingStyle.dto.response.ServingStyleResponseDto;
import com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.OrderItemRequestDto;
import com.saeal.MrDaebackService.voiceOrder.dto.response.OrderItemDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
            result.add(OrderItemDto.builder()
                    .dinnerId(item.getDinnerId())
                    .dinnerName(item.getDinnerName())
                    .servingStyleId(item.getServingStyleId())
                    .servingStyleName(item.getServingStyleName())
                    .quantity(item.getQuantity())
                    .basePrice(item.getBasePrice())
                    .unitPrice(item.getUnitPrice())
                    .totalPrice(item.getTotalPrice())
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
                .build();
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
