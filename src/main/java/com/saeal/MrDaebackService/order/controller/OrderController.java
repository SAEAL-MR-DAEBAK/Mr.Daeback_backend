package com.saeal.MrDaebackService.order.controller;

import com.saeal.MrDaebackService.order.dto.request.ApproveOrderRequest;
import com.saeal.MrDaebackService.order.dto.request.UpdateDeliveryStatusRequest;
import com.saeal.MrDaebackService.order.dto.response.OrderResponseDto;
import com.saeal.MrDaebackService.order.service.OrderService;
import com.saeal.MrDaebackService.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

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
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(summary = "로그인한 사용자의 주문 목록 조회", description = "현재 인증된 사용자에 연결된 모든 주문을 반환합니다.")
    public ResponseEntity<List<OrderResponseDto>> getMyOrders() {
        UUID userId = userService.getCurrentUserId();
        List<OrderResponseDto> response = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @Operation(summary = "주문 상세 조회", description = "주문 ID로 주문 상세 정보를 조회합니다.")
    public ResponseEntity<OrderResponseDto> getOrderById(@PathVariable UUID orderId) {
        OrderResponseDto response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/admin/all")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "최신 주문 목록 조회 (관리자)", description = "관리자가 최신 주문 50개를 시간순으로 조회합니다.")
    public ResponseEntity<List<OrderResponseDto>> getRecentOrders() {
        List<OrderResponseDto> response = orderService.getRecentOrders(50);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/search")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "주문 검색 (관리자)", description = "주문 번호나 사용자 아이디로 주문을 검색합니다.")
    public ResponseEntity<List<OrderResponseDto>> searchOrders(
            @RequestParam(required = false) String orderNumber,
            @RequestParam(required = false) String username
    ) {
        List<OrderResponseDto> allOrders = orderService.getAllOrders();
        
        if (orderNumber != null && !orderNumber.isBlank()) {
            allOrders = allOrders.stream()
                    .filter(order -> order.getOrderNumber().toUpperCase().contains(orderNumber.toUpperCase()))
                    .toList();
        }
        
        if (username != null && !username.isBlank()) {
            allOrders = allOrders.stream()
                    .filter(order -> order.getUsername() != null && 
                            order.getUsername().toLowerCase().contains(username.toLowerCase()))
                    .toList();
        }
        
        return ResponseEntity.ok(allOrders);
    }

    @PostMapping("/admin/{orderId}/approve")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "주문 승인/거절 (관리자)", description = "관리자가 주문을 승인하거나 거절합니다.")
    public ResponseEntity<OrderResponseDto> approveOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody ApproveOrderRequest request
    ) {
        OrderResponseDto response = orderService.approveOrder(
                orderId,
                request.getApproved(),
                request.getRejectionReason()
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/admin/{orderId}/delivery-status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "배송 상태 변경 (관리자)", description = "관리자가 주문의 배송 상태를 변경합니다 (조리 중, 배달 중, 배달 완료).")
    public ResponseEntity<OrderResponseDto> updateDeliveryStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody UpdateDeliveryStatusRequest request
    ) {
        OrderResponseDto response = orderService.updateDeliveryStatus(orderId, request.getDeliveryStatus());
        return ResponseEntity.ok(response);
    }
}
