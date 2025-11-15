package com.saeal.MrDaebackService.dinner.service;

import com.saeal.MrDaebackService.dinner.Dinner;
import com.saeal.MrDaebackService.dinner.dto.request.CreateDinnerRequest;
import com.saeal.MrDaebackService.dinner.dto.response.DinnerResponseDto;
import com.saeal.MrDaebackService.dinner.repository.DinnerRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DinnerService {

    private final DinnerRepository dinnerRepository;

    @Transactional
    public DinnerResponseDto createDinner(CreateDinnerRequest request) {
        boolean active = request.getIsActive() == null ? true : request.getIsActive();

        Dinner dinner = Dinner.builder()
                .dinnerName(request.getDinnerName())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .isActive(active)
                .build();

        Dinner savedDinner = dinnerRepository.save(dinner);
        return DinnerResponseDto.from(savedDinner);
    }
}
