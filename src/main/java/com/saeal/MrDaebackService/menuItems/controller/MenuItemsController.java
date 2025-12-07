package com.saeal.MrDaebackService.menuItems.controller;

import com.saeal.MrDaebackService.menuItems.dto.MenuItemResponseDto;
import com.saeal.MrDaebackService.menuItems.service.MenuItemsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/menu-items")
@Tag(name = "Menu Items API", description = "Menu Items 관련 API 입니다.")
public class MenuItemsController {

    private final MenuItemsService menuItemsService;

    @GetMapping("/getAllMenuItems")
    @Operation(summary = "MenuItem 전체 조회", description = "등록된 모든 MenuItem 리스트를 반환합니다.")
    public ResponseEntity<List<MenuItemResponseDto>> getAllMenuItems() {
        List<MenuItemResponseDto> response = menuItemsService.getAllMenuItems();
        return ResponseEntity.ok(response);
    }
}
