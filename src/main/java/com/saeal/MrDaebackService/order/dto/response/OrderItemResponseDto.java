package com.saeal.MrDaebackService.order.dto.response;

import com.saeal.MrDaebackService.order.domain.OrderItem;
import com.saeal.MrDaebackService.product.dto.response.ProductMenuItemResponseDto;
import com.saeal.MrDaebackService.product.enums.ProductType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponseDto {
    private String productId;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal lineTotal;
    private String optionSummary;
    private ProductType productType; // Product 타입 (DINNER_PRODUCT, ADDITIONAL_MENU_PRODUCT)
    private List<ProductMenuItemResponseDto> menuItems; // Product의 메뉴 아이템 목록

    public static OrderItemResponseDto from(OrderItem orderItem) {
        // Product의 메뉴 아이템 목록 변환 (JOIN FETCH로 이미 로드되어 있어야 함)
        List<ProductMenuItemResponseDto> menuItems = null;
        try {
            if (orderItem.getProduct() != null && orderItem.getProduct().getProductMenuItems() != null) {
                menuItems = orderItem.getProduct().getProductMenuItems().stream()
                        .map(ProductMenuItemResponseDto::from)
                        .collect(Collectors.toList());
            } else {
                menuItems = new java.util.ArrayList<>();
            }
        } catch (Exception e) {
            // LAZY 로딩 문제가 발생할 수 있으므로 빈 리스트 반환
            menuItems = new java.util.ArrayList<>();
        }

        ProductType productType = null;
        if (orderItem.getProduct() != null) {
            productType = orderItem.getProduct().getProductType();
        }

        return new OrderItemResponseDto(
                orderItem.getProduct() != null ? orderItem.getProduct().getId().toString() : null,
                orderItem.getProduct() != null ? orderItem.getProduct().getProductName() : null,
                orderItem.getQuantity(),
                orderItem.getUnitPrice(),
                orderItem.getLineTotal(),
                orderItem.getOptionSummary(),
                productType,
                menuItems
        );
    }
}
