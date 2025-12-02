package com.saeal.MrDaebackService.menuItems.controller;

import com.saeal.MrDaebackService.menuItems.dto.CreateMenuItemRequest;
import com.saeal.MrDaebackService.menuItems.dto.MenuItemResponseDto;
import com.saeal.MrDaebackService.menuItems.service.MenuItemsService;
import com.saeal.MrDaebackService.menuItems.dto.UpdateMenuItemStockRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menu-items")
@Tag(name = "Menu Items API", description = "Menu Items 관련 API 입니다.")
public class MenuItemsController {

    private final MenuItemsService menuItemsService;

    @PatchMapping("/{menuItemId}/stock")
    public ResponseEntity<MenuItemResponseDto> updateMenuItemStock(
            @PathVariable UUID menuItemId,
            @Valid @RequestBody UpdateMenuItemStockRequest request
    ) {
        MenuItemResponseDto response = menuItemsService.updateMenuItemStock(menuItemId, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "MenuItem 전체 조회", description = "등록된 모든 MenuItem 리스트를 반환합니다.")
    public ResponseEntity<List<MenuItemResponseDto>> getAllMenuItems() {
        List<MenuItemResponseDto> response = menuItemsService.getAllMenuItems();
        return ResponseEntity.ok(response);
    }
}
