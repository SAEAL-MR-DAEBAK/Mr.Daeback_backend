package com.saeal.MrDaebackService.order.controller;

import com.saeal.MrDaebackService.order.dto.response.OrderResponseDto;
import com.saeal.MrDaebackService.order.service.OrderService;
import com.saeal.MrDaebackService.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Tag(name = "Order API", description = "Order 조회 API 입니다.")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    @Operation(summary = "로그인한 사용자의 주문 목록 조회", description = "현재 인증된 사용자에 연결된 모든 주문을 반환합니다.")
    public ResponseEntity<List<OrderResponseDto>> getMyOrders() {
        UUID userId = userService.getCurrentUserId();
        List<OrderResponseDto> response = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(response);
    }
}
