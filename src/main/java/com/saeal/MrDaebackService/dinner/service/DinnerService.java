package com.saeal.MrDaebackService.dinner.service;

import com.saeal.MrDaebackService.dinner.domain.Dinner;
import com.saeal.MrDaebackService.dinner.domain.DinnerMenuItem;
import com.saeal.MrDaebackService.dinner.dto.request.CreateDinnerMenuItemRequest;
import com.saeal.MrDaebackService.dinner.dto.request.CreateDinnerRequest;
import com.saeal.MrDaebackService.dinner.dto.response.DinnerResponseDto;
import com.saeal.MrDaebackService.dinner.dto.response.DinnerMenuItemResponseDto;
import com.saeal.MrDaebackService.dinner.repository.DinnerRepository;
import com.saeal.MrDaebackService.dinner.repository.DinnerMenuItemRepository;
import com.saeal.MrDaebackService.menuItems.domain.MenuItems;
import com.saeal.MrDaebackService.menuItems.repository.MenuItemsRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DinnerService {

    private final DinnerRepository dinnerRepository;
    private final DinnerMenuItemRepository dinnerMenuItemRepository;
    private final MenuItemsRepository menuItemsRepository;

    @Transactional
    public DinnerResponseDto createDinner(CreateDinnerRequest request) {
        boolean active = request.getIsActive() == null || request.getIsActive();

        Dinner dinner = Dinner.builder()
                .dinnerName(request.getDinnerName())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .isActive(active)
                .build();

        Dinner savedDinner = dinnerRepository.save(dinner);
        return DinnerResponseDto.from(savedDinner);
    }

    @Transactional
    public List<DinnerMenuItemResponseDto> getDefaultMenuItemsByDinner(UUID dinnerId) {
        return dinnerMenuItemRepository.findByDinnerId(dinnerId).stream()
                .map(DinnerMenuItemResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<DinnerResponseDto> getAllDinners() {
        return dinnerRepository.findAll().stream()
                .map(DinnerResponseDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public DinnerMenuItemResponseDto createDinnerMenuItem(CreateDinnerMenuItemRequest request) {
        UUID dinnerId = UUID.fromString(request.getDinnerId());
        UUID menuItemId = UUID.fromString(request.getMenuItemId());

        Dinner dinner = dinnerRepository.findById(dinnerId)
                .orElseThrow(() -> new IllegalArgumentException("Dinner not found: " + dinnerId));
        MenuItems menuItem = menuItemsRepository.findById(menuItemId)
                .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + menuItemId));

        DinnerMenuItem entity = DinnerMenuItem.builder()
                .dinner(dinner)
                .menuItem(menuItem)
                .defaultQuantity(request.getDefaultQuantity())
                .build();

        DinnerMenuItem saved = dinnerMenuItemRepository.save(entity);
        return DinnerMenuItemResponseDto.from(saved);
    }
}
