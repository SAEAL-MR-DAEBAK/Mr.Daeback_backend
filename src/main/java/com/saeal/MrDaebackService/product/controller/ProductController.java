package com.saeal.MrDaebackService.product.controller;

import com.saeal.MrDaebackService.product.dto.request.CreateProductRequest;
import com.saeal.MrDaebackService.product.dto.response.ProductResponseDto;
import com.saeal.MrDaebackService.product.service.ProductService;
import com.saeal.MrDaebackService.dinner.dto.response.DinnerMenuItemResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Tag(name = "Product API", description = "Product 생성 API 입니다.")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(
            @Valid @RequestBody CreateProductRequest request
    ) {
        ProductResponseDto response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/dinners/{dinnerId}/menu-items")
    public ResponseEntity<List<DinnerMenuItemResponseDto>> getDinnerMenuItems(
            @PathVariable UUID dinnerId
    ) {
        List<DinnerMenuItemResponseDto> response = productService.getDinnerMenuItems(dinnerId);
        return ResponseEntity.ok(response);
    }
}
