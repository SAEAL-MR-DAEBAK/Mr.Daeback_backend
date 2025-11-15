package com.saeal.MrDaebackService.menuItems;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
}
