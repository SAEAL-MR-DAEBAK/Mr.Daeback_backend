package com.saeal.MrDaebackService.menuItems.service;

import com.saeal.MrDaebackService.menuItems.domain.MenuItems;
import com.saeal.MrDaebackService.menuItems.dto.CreateMenuItemRequest;
import com.saeal.MrDaebackService.menuItems.dto.MenuItemResponseDto;
import com.saeal.MrDaebackService.menuItems.dto.UpdateMenuItemStockRequest;
import com.saeal.MrDaebackService.menuItems.repository.MenuItemsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MenuItemsService {

    private final MenuItemsRepository menuItemsRepository;

    @Transactional
    public MenuItemResponseDto createMenuItem(CreateMenuItemRequest request) {
        LocalDateTime now = LocalDateTime.now();

        MenuItems menuItem = MenuItems.builder()
                .name(request.getName())
                .stock(request.getStock())
                .unitType(request.getUnitType())
                .createdAt(now)
                .updatedAt(now)
                .build();

        MenuItems savedMenuItem = menuItemsRepository.save(menuItem);
        return MenuItemResponseDto.from(savedMenuItem);
    }

    @Transactional
    public MenuItemResponseDto updateMenuItemStock(UUID id, UpdateMenuItemStockRequest request) {
        MenuItems menuItem = menuItemsRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + id));

        menuItem.setStock(request.getStock());
        menuItem.setUpdatedAt(LocalDateTime.now());

        MenuItems updatedMenuItem = menuItemsRepository.save(menuItem);
        return MenuItemResponseDto.from(updatedMenuItem);
    }

    @Transactional
    public List<MenuItemResponseDto> getAllMenuItems() {
        return menuItemsRepository.findAll().stream()
                .map(MenuItemResponseDto::from)
                .toList();
    }
}
