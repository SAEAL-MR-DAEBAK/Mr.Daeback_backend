package com.saeal.MrDaebackService.menuItems;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menu-items")
@Tag(name = "Menu Items API", description = "Menu Items 관련 API 입니다.")
public class MenuItemsController {

    private final MenuItemsService menuItemsService;

    @PostMapping("/createMenuItem")
    public ResponseEntity<MenuItemResponseDto> createMenuItem(
            @Valid @RequestBody CreateMenuItemRequest request
    ) {
        MenuItemResponseDto response = menuItemsService.createMenuItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{menuItemId}/stock")
    public ResponseEntity<MenuItemResponseDto> updateMenuItemStock(
            @PathVariable UUID menuItemId,
            @Valid @RequestBody UpdateMenuItemStockRequest request
    ) {
        MenuItemResponseDto response = menuItemsService.updateMenuItemStock(menuItemId, request);
        return ResponseEntity.ok(response);
    }
}
