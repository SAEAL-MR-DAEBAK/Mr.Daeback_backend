package com.saeal.MrDaebackService.dinner.service;

import com.saeal.MrDaebackService.dinner.Dinner;
import com.saeal.MrDaebackService.dinner.dto.request.CreateDinnerRequest;
import com.saeal.MrDaebackService.dinner.dto.response.DinnerResponseDto;
import com.saeal.MrDaebackService.dinner.dto.response.DinnerMenuItemResponseDto;
import com.saeal.MrDaebackService.dinner.repository.DinnerRepository;
import com.saeal.MrDaebackService.dinner.repository.DinnerMenuItemRepository;
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
}
