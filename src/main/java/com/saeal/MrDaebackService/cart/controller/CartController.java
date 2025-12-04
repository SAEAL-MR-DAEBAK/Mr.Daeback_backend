package com.saeal.MrDaebackService.cart.controller;

import com.saeal.MrDaebackService.cart.dto.request.CreateCartRequest;
import com.saeal.MrDaebackService.cart.dto.response.CartMenuItemsTotalResponse;
import com.saeal.MrDaebackService.cart.dto.response.CartResponseDto;
import com.saeal.MrDaebackService.cart.service.CartService;
import com.saeal.MrDaebackService.order.dto.response.OrderResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/carts")
@Tag(name = "Cart API", description = "Cart 생성 API 입니다.")
public class CartController {

    private final CartService cartService;

    @PostMapping("/createCart")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "장바구니 생성", description = "장바구니를 생성합니다. ")
    public ResponseEntity<CartResponseDto> createCart(
            @Valid @RequestBody CreateCartRequest request
    ) {
        CartResponseDto response = cartService.createCart(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "로그인한 사용자의 장바구니 목록 조회", description = "현재 인증된 사용자에 연결된 모든 카트를 반환합니다.")
    public ResponseEntity<List<CartResponseDto>> getMyCarts() {
        List<CartResponseDto> response = cartService.getCartsForCurrentUser();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{cartId}/checkout")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "카트 결제(주문 생성)", description = "OPEN 상태 카트를 CHECKED_OUT으로 변경하고 Order를 생성합니다.")
    public ResponseEntity<OrderResponseDto> checkoutCart(
            @PathVariable UUID cartId
    ) {
        OrderResponseDto response = cartService.checkout(cartId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{cartId}/menu-items/total-price")
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "카트 내 Product의 MenuItem 가격 총합 반환", description = "선택한 카트에 담긴 Product의 MenuItem 라인합을 카트 수량을 반영하여 합산합니다.")
    public ResponseEntity<CartMenuItemsTotalResponse> getCartMenuItemsTotal(
            @PathVariable UUID cartId
    ) {
        CartMenuItemsTotalResponse response = cartService.calculateCartMenuItemsTotal(cartId);
        return ResponseEntity.ok(response);
    }
}
