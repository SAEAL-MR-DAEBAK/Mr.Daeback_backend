package com.saeal.MrDaebackService.product.dto.response;

import com.saeal.MrDaebackService.product.domain.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponseDto {
    private String id;
    private String productName;
    private String dinnerId;
    private String dinnerName;
    private String servingStyleId;
    private String servingStyleName;
    private BigDecimal totalPrice;
    private int quantity;
    private String memo;
    private List<ProductMenuItemResponseDto> productMenuItems;

    public static ProductResponseDto from(Product product) {
        List<ProductMenuItemResponseDto> items = product.getProductMenuItems().stream()
                .map(ProductMenuItemResponseDto::from)
                .collect(Collectors.toList());

        return new ProductResponseDto(
                product.getId().toString(),
                product.getProductName(),
                product.getDinner().getId().toString(),
                product.getDinner().getDinnerName(),
                product.getServingStyle().getId().toString(),
                product.getServingStyle().getStyleName(),
                product.getTotalPrice(),
                product.getQuantity(),
                product.getMemo(),
                items
        );
    }
}
