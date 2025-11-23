package com.saeal.MrDaebackService.product.controller;

import com.saeal.MrDaebackService.product.dto.request.CreateProductRequest;
import com.saeal.MrDaebackService.product.dto.response.ProductResponseDto;
import com.saeal.MrDaebackService.product.service.ProductService;
import com.saeal.MrDaebackService.product.dto.response.ProductMenuItemResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
