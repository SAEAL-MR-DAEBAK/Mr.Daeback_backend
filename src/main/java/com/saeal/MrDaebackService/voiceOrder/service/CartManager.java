package com.saeal.MrDaebackService.voiceOrder.service;

import com.saeal.MrDaebackService.dinner.dto.response.DinnerResponseDto;
import com.saeal.MrDaebackService.servingStyle.dto.response.ServingStyleResponseDto;
import com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto;
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
    public List<OrderItemDto> convertToOrderItemDtoList(List<ChatRequestDto.OrderItemRequestDto> currentOrder) {
        List<OrderItemDto> result = new ArrayList<>();
        if (currentOrder == null) return result;

        for (ChatRequestDto.OrderItemRequestDto item : currentOrder) {
            // 추가 메뉴 아이템 변환
            List<OrderItemDto.AdditionalMenuItemDto> additionalMenuItems = new ArrayList<>();
            if (item.getAdditionalMenuItems() != null) {
                for (var additionalItem : item.getAdditionalMenuItems()) {
                    additionalMenuItems.add(OrderItemDto.AdditionalMenuItemDto.builder()
                            .menuItemId(additionalItem.getMenuItemId())
                            .menuItemName(additionalItem.getMenuItemName())
                            .quantity(additionalItem.getQuantity())
                            .build());
                }
            }

            result.add(OrderItemDto.builder()
                    .dinnerId(item.getDinnerId())
                    .dinnerName(item.getDinnerName())
                    .servingStyleId(item.getServingStyleId())
                    .servingStyleName(item.getServingStyleName())
                    .productId(item.getProductId())  // productId 포함
                    .quantity(item.getQuantity())
                    .basePrice(item.getBasePrice())
                    .unitPrice(item.getUnitPrice())
                    .totalPrice(item.getTotalPrice())
                    .additionalMenuItems(additionalMenuItems)
                    .build());
        }
        return result;
    }

    /**
     * OrderItemDto 리스트를 OrderItemRequestDto 리스트로 변환
     */
    public List<com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.OrderItemRequestDto> convertToOrderItemRequestDtoList(List<OrderItemDto> orderItems) {
        List<com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.OrderItemRequestDto> result = new ArrayList<>();
        if (orderItems == null) return result;

        for (OrderItemDto item : orderItems) {
            // 추가 메뉴 아이템 변환
            List<com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.AdditionalMenuItemDto> additionalMenuItems = new ArrayList<>();
            if (item.getAdditionalMenuItems() != null) {
                for (var additionalItem : item.getAdditionalMenuItems()) {
                    com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.AdditionalMenuItemDto dto = 
                            new com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.AdditionalMenuItemDto(
                            additionalItem.getMenuItemId(),
                            additionalItem.getMenuItemName(),
                            additionalItem.getQuantity()
                    );
                    additionalMenuItems.add(dto);
                }
            }

            com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.OrderItemRequestDto orderItemDto = 
                    new com.saeal.MrDaebackService.voiceOrder.dto.request.ChatRequestDto.OrderItemRequestDto(
                    item.getDinnerId(),
                    item.getDinnerName(),
                    item.getServingStyleId(),
                    item.getServingStyleName(),
                    item.getProductId(),
                    item.getQuantity(),
                    item.getBasePrice(),
                    item.getUnitPrice(),
                    item.getTotalPrice(),
                    additionalMenuItems
            );
            result.add(orderItemDto);
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
                .productId(null)  // 아직 Product 생성 안 됨
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
                .productId(item.getProductId())  // productId 유지
                .quantity(quantity)
                .basePrice(item.getBasePrice())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getUnitPrice() * quantity)
                .additionalMenuItems(item.getAdditionalMenuItems() != null 
                    ? new ArrayList<>(item.getAdditionalMenuItems()) 
                    : new ArrayList<>())
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
                .productId(item.getProductId())  // 기존 productId 유지
                .quantity(item.getQuantity())
                .basePrice(item.getBasePrice())
                .unitPrice(newUnitPrice)
                .totalPrice(newUnitPrice * item.getQuantity())
                .additionalMenuItems(item.getAdditionalMenuItems() != null 
                    ? new ArrayList<>(item.getAdditionalMenuItems()) 
                    : new ArrayList<>())
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
                .additionalMenuItems(item.getAdditionalMenuItems() != null 
                    ? new ArrayList<>(item.getAdditionalMenuItems()) 
                    : new ArrayList<>())
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

    /**
     * 아이템에 추가 메뉴 아이템 추가
     */
    public OrderItemDto addAdditionalMenuItem(OrderItemDto item, OrderItemDto.AdditionalMenuItemDto additionalMenuItem) {
        List<OrderItemDto.AdditionalMenuItemDto> additionalMenuItems = new ArrayList<>(
                item.getAdditionalMenuItems() != null ? item.getAdditionalMenuItems() : new ArrayList<>()
        );
        
        // 이미 있는 메뉴 아이템인지 확인
        boolean found = false;
        for (int i = 0; i < additionalMenuItems.size(); i++) {
            if (additionalMenuItems.get(i).getMenuItemId().equals(additionalMenuItem.getMenuItemId())) {
                // 기존 수량에 추가
                int newQuantity = additionalMenuItems.get(i).getQuantity() + additionalMenuItem.getQuantity();
                additionalMenuItems.set(i, OrderItemDto.AdditionalMenuItemDto.builder()
                        .menuItemId(additionalMenuItem.getMenuItemId())
                        .menuItemName(additionalMenuItem.getMenuItemName())
                        .quantity(newQuantity)
                        .build());
                found = true;
                break;
            }
        }
        
        if (!found) {
            additionalMenuItems.add(additionalMenuItem);
        }

        return OrderItemDto.builder()
                .dinnerId(item.getDinnerId())
                .dinnerName(item.getDinnerName())
                .servingStyleId(item.getServingStyleId())
                .servingStyleName(item.getServingStyleName())
                .quantity(item.getQuantity())
                .basePrice(item.getBasePrice())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .additionalMenuItems(additionalMenuItems)
                .build();
    }

    /**
     * 아이템에서 추가 메뉴 아이템 제거
     */
    public OrderItemDto removeAdditionalMenuItem(OrderItemDto item, String menuItemId) {
        List<OrderItemDto.AdditionalMenuItemDto> additionalMenuItems = new ArrayList<>(
                item.getAdditionalMenuItems() != null ? item.getAdditionalMenuItems() : new ArrayList<>()
        );
        
        additionalMenuItems.removeIf(ami -> ami.getMenuItemId().equals(menuItemId));

        return OrderItemDto.builder()
                .dinnerId(item.getDinnerId())
                .dinnerName(item.getDinnerName())
                .servingStyleId(item.getServingStyleId())
                .servingStyleName(item.getServingStyleName())
                .quantity(item.getQuantity())
                .basePrice(item.getBasePrice())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getTotalPrice())
                .additionalMenuItems(additionalMenuItems)
                .build();
    }
}
