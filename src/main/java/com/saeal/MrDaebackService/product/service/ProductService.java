package com.saeal.MrDaebackService.product.service;

import com.saeal.MrDaebackService.dinner.domain.Dinner;
import com.saeal.MrDaebackService.dinner.domain.DinnerMenuItem;
import com.saeal.MrDaebackService.dinner.repository.DinnerMenuItemRepository;
import com.saeal.MrDaebackService.dinner.repository.DinnerRepository;
import com.saeal.MrDaebackService.product.domain.Product;
import com.saeal.MrDaebackService.product.domain.ProductMenuItem;
import com.saeal.MrDaebackService.product.dto.request.CreateProductRequest;
import com.saeal.MrDaebackService.product.dto.response.ProductResponseDto;
import com.saeal.MrDaebackService.product.repository.ProductRepository;
import com.saeal.MrDaebackService.servingStyle.domain.ServingStyle;
import com.saeal.MrDaebackService.servingStyle.repository.ServingStyleRepository;
import com.saeal.MrDaebackService.dinner.dto.response.DinnerMenuItemResponseDto;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final DinnerRepository dinnerRepository;
    private final ServingStyleRepository servingStyleRepository;
    private final DinnerMenuItemRepository dinnerMenuItemRepository;

    @Transactional
    public ProductResponseDto createProduct(CreateProductRequest request) {
        UUID dinnerId = UUID.fromString(request.getDinnerId());
        UUID servingStyleId = UUID.fromString(request.getServingStyleId());

        Dinner dinner = dinnerRepository.findById(dinnerId)
                .orElseThrow(() -> new IllegalArgumentException("Dinner not found: " + dinnerId));
        ServingStyle servingStyle = servingStyleRepository.findById(servingStyleId)
                .orElseThrow(() -> new IllegalArgumentException("Serving style not found: " + servingStyleId));

        int quantity = request.getQuantity();
        BigDecimal unitPrice = dinner.getBasePrice().add(servingStyle.getExtraPrice());
        BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(quantity));

        String productName = request.getProductName();
        if (productName == null || productName.isBlank()) {
            productName = dinner.getDinnerName() + " - " + servingStyle.getStyleName();
        }

        Product product = Product.builder()
                .dinner(dinner)
                .servingStyle(servingStyle)
                .productName(productName)
                .totalPrice(totalPrice)
                .quantity(quantity)
                .memo(request.getMemo())
                .build();

        List<DinnerMenuItem> dinnerMenuItems = getDinnerMenuItemsEntities(dinnerId);
        for (DinnerMenuItem dinnerMenuItem : dinnerMenuItems) {
            ProductMenuItem productMenuItem = ProductMenuItem.builder()
                    .product(product)
                    .menuItem(dinnerMenuItem.getMenuItem())
                    .quantity(dinnerMenuItem.getDefaultQuantity())
                    .unitPrice(BigDecimal.ZERO)
                    .lineTotal(BigDecimal.ZERO)
                    .build();
            product.getProductMenuItems().add(productMenuItem);
        }

        Product savedProduct = productRepository.save(product);
        return ProductResponseDto.from(savedProduct);
    }

    public List<DinnerMenuItemResponseDto> getDinnerMenuItems(UUID dinnerId) {
        return getDinnerMenuItemsEntities(dinnerId).stream()
                .map(DinnerMenuItemResponseDto::from)
                .toList();
    }

    private List<DinnerMenuItem> getDinnerMenuItemsEntities(UUID dinnerId) {
        return dinnerMenuItemRepository.findByDinnerId(dinnerId);
    }
}
