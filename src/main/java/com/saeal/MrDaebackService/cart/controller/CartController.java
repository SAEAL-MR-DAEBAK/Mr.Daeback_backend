package com.saeal.MrDaebackService.cart.controller;

import com.saeal.MrDaebackService.cart.dto.request.CreateCartRequest;
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
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/carts")
@Tag(name = "Cart API", description = "장바구니 생성 및 결제 API")
public class CartController {

    private final CartService cartService;

    /**
     * 장바구니 생성
     * - GUI: CheckoutStep에서 결제 전 호출
     * - LLM: VoiceOrderService CONFIRM_ORDER에서 호출
     */
    @PostMapping("/createCart")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(summary = "장바구니 생성", description = "Product 목록으로 장바구니를 생성합니다.")
    public ResponseEntity<CartResponseDto> createCart(
            @Valid @RequestBody CreateCartRequest request
    ) {
        CartResponseDto response = cartService.createCart(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 장바구니 결제 (Order 생성)
     * - GUI: CheckoutStep에서 결제 버튼 클릭 시 호출
     * - LLM: VoiceOrderService CONFIRM_ORDER에서 호출
     */
    @PostMapping("/{cartId}/checkout")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(summary = "장바구니 결제", description = "OPEN 상태 장바구니를 결제하고 Order를 생성합니다.")
    public ResponseEntity<OrderResponseDto> checkoutCart(
            @PathVariable UUID cartId
    ) {
        OrderResponseDto response = cartService.checkout(cartId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
