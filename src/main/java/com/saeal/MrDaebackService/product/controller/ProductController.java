package com.saeal.MrDaebackService.product.controller;

import com.saeal.MrDaebackService.product.dto.request.CreateProductRequest;
import com.saeal.MrDaebackService.product.dto.request.AddProductMenuItemRequest;
import com.saeal.MrDaebackService.product.dto.request.UpdateProductMenuItemRequest;
import com.saeal.MrDaebackService.product.dto.response.ProductMenuItemResponseDto;
import com.saeal.MrDaebackService.product.dto.response.ProductResponseDto;
import com.saeal.MrDaebackService.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Tag(name = "Product API", description = "Product 생성 API 입니다.")
public class ProductController {

    private final ProductService productService;

    @PostMapping("/createProduct")
    @Operation(summary = "상품 생성", description = "Dinner, ServingStyle, MenuItems들을 통해 상품을 생성합니다. ")
    public ResponseEntity<ProductResponseDto> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponseDto response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{productId}/menu-items")
    @Operation(summary = "Product의 MenuItem 리스트 반환", description = "Product에 매핑된 MenuItem 목록을 반환합니다.")
    public ResponseEntity<List<ProductMenuItemResponseDto>> getProductMenuItems(
            @PathVariable UUID productId
    ) {
        List<ProductMenuItemResponseDto> response = productService.getProductMenuItems(productId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{productId}/menu-items")
    @Operation(summary = "Product에 MenuItem 추가", description = "특정 Product에 MenuItem을 추가합니다.")
    public ResponseEntity<ProductMenuItemResponseDto> addMenuItemToProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody AddProductMenuItemRequest request
    ) {
        ProductMenuItemResponseDto response = productService.addMenuItemToProduct(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{productId}/menu-items/{menuItemId}")
    @Operation(summary = "Product의 MenuItem 수량 수정", description = "특정 Product에 포함된 MenuItem의 수량만 변경합니다.")
    public ResponseEntity<ProductMenuItemResponseDto> updateProductMenuItem(
            @PathVariable UUID productId,
            @PathVariable UUID menuItemId,
            @Valid @RequestBody UpdateProductMenuItemRequest request
    ) {
        ProductMenuItemResponseDto response = productService.updateProductMenuItem(productId, menuItemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{productId}/menu-items/{menuItemId}")
    @Operation(summary = "Product에서 MenuItem 제거", description = "특정 Product에 포함된 MenuItem을 제거합니다.")
    public ResponseEntity<Void> removeProductMenuItem(
            @PathVariable UUID productId,
            @PathVariable UUID menuItemId
    ) {
        productService.removeProductMenuItem(productId, menuItemId);
        return ResponseEntity.noContent().build();
    }
}
