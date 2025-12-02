package com.saeal.MrDaebackService.admin.controller;

import com.saeal.MrDaebackService.dinner.dto.request.CreateDinnerMenuItemRequest;
import com.saeal.MrDaebackService.dinner.dto.request.CreateDinnerRequest;
import com.saeal.MrDaebackService.dinner.dto.response.DinnerMenuItemResponseDto;
import com.saeal.MrDaebackService.dinner.dto.response.DinnerResponseDto;
import com.saeal.MrDaebackService.dinner.service.DinnerService;
import com.saeal.MrDaebackService.menuItems.dto.CreateMenuItemRequest;
import com.saeal.MrDaebackService.menuItems.dto.MenuItemResponseDto;
import com.saeal.MrDaebackService.menuItems.dto.UpdateMenuItemStockRequest;
import com.saeal.MrDaebackService.menuItems.service.MenuItemsService;
import com.saeal.MrDaebackService.servingStyle.dto.request.CreateServingStyleRequest;
import com.saeal.MrDaebackService.servingStyle.dto.response.ServingStyleResponseDto;
import com.saeal.MrDaebackService.servingStyle.service.ServingStyleService;
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
@RequestMapping("/api/admin")
@Tag(name = "Admin API", description = "관리자 전용 API")
public class AdminController {

    private final DinnerService dinnerService;
    private final MenuItemsService menuItemsService;
    private final ServingStyleService servingStyleService;

    @PostMapping("/dinners/createDinner")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dinner 생성", description = "새로운 Dinner를 생성함. 관리자용 API")
    public ResponseEntity<DinnerResponseDto> createDinner(@Valid @RequestBody CreateDinnerRequest request) {
        DinnerResponseDto response = dinnerService.createDinner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/menu-items/createMenuItem")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "MenuItem 생성", description = "새로운 MenuItem을 생성함. 관리자용 API")
    public ResponseEntity<MenuItemResponseDto> createMenuItem(
            @Valid @RequestBody CreateMenuItemRequest request
    ) {
        MenuItemResponseDto response = menuItemsService.createMenuItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/serving-styles/createServingStyle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Serving Style 생성", description = "새로운 Serving Style을 생성함. 관리자용 API")
    public ResponseEntity<ServingStyleResponseDto> createServingStyle(
            @Valid @RequestBody CreateServingStyleRequest request
    ) {
        ServingStyleResponseDto response = servingStyleService.createServingStyle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/dinners/menu-items")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dinner의 Default MenuItem 매핑", description = "Dinner에 default로 들어있는 메뉴 아이템을 매핑, 관리자용 API")
    public ResponseEntity<DinnerMenuItemResponseDto> createDinnerMenuItem(
            @Valid @RequestBody CreateDinnerMenuItemRequest request
    ) {
        DinnerMenuItemResponseDto response = dinnerService.createDinnerMenuItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/menu-items/{menuItemId}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "MenuItem 재고 수정", description = "MenuItem 재고를 수정합니다. 관리자용 API")
    public ResponseEntity<MenuItemResponseDto> updateMenuItemStock(
            @PathVariable UUID menuItemId,
            @Valid @RequestBody UpdateMenuItemStockRequest request
    ) {
        MenuItemResponseDto response = menuItemsService.updateMenuItemStock(menuItemId, request);
        return ResponseEntity.ok(response);
    }
}
