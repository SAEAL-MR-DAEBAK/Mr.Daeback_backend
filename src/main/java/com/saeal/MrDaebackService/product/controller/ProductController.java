package com.saeal.MrDaebackService.product.controller;

import com.saeal.MrDaebackService.product.dto.request.CreateProductRequest;
import com.saeal.MrDaebackService.product.dto.request.CreateAdditionalMenuProductRequest;
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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Tag(name = "Product API", description = "Product 생성 및 수정 API")
public class ProductController {

    private final ProductService productService;

    /**
     * Product 생성 (Dinner + Style 기반)
     * - GUI: StyleStep에서 호출
     * - LLM: VoiceOrderService에서 호출
     */
    @PostMapping("/createProduct")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(summary = "상품 생성", description = "Dinner, ServingStyle 기반으로 상품을 생성합니다.")
    public ResponseEntity<ProductResponseDto> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponseDto response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 추가 메뉴 Product 생성
     * - GUI: CheckoutStep에서 공통 추가 메뉴용
     */
    @PostMapping("/createAdditionalMenuProduct")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(summary = "추가 메뉴 상품 생성", description = "추가 메뉴를 별도 Product로 생성합니다.")
    public ResponseEntity<?> createAdditionalMenuProduct(
            @Valid @RequestBody CreateAdditionalMenuProductRequest request
    ) {
        try {
            ProductResponseDto response = productService.createAdditionalMenuProduct(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    /**
     * MenuItem 수량 수정
     * - GUI: CheckoutStep에서 커스터마이징 반영 시 호출
     * - LLM: VoiceOrderService CUSTOMIZE_MENU에서 호출
     */
    @PatchMapping("/{productId}/menu-items/{menuItemId}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(summary = "MenuItem 수량 수정", description = "Product에 포함된 MenuItem의 수량을 변경합니다.")
    public ResponseEntity<ProductMenuItemResponseDto> updateProductMenuItem(
            @PathVariable UUID productId,
            @PathVariable UUID menuItemId,
            @Valid @RequestBody UpdateProductMenuItemRequest request
    ) {
        ProductMenuItemResponseDto response = productService.updateProductMenuItem(productId, menuItemId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Product 메모 수정
     * - GUI: CheckoutStep에서 특별 요청사항 저장 시 호출
     */
    @PatchMapping("/{productId}/memo")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(summary = "Product 메모 수정", description = "Product의 메모(특별 요청사항)를 수정합니다.")
    public ResponseEntity<Void> updateProductMemo(
            @PathVariable UUID productId,
            @RequestBody Map<String, String> request
    ) {
        String memo = request.get("memo");
        productService.updateProductMemo(productId, memo);
        return ResponseEntity.ok().build();
    }
}
