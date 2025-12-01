package com.saeal.MrDaebackService.servingStyle.service;

import com.saeal.MrDaebackService.servingStyle.domain.ServingStyle;
import com.saeal.MrDaebackService.servingStyle.dto.request.CreateServingStyleRequest;
import com.saeal.MrDaebackService.servingStyle.dto.response.ServingStyleResponseDto;
import com.saeal.MrDaebackService.servingStyle.repository.ServingStyleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServingStyleService {

    private final ServingStyleRepository servingStyleRepository;

    @Transactional
    public ServingStyleResponseDto createServingStyle(CreateServingStyleRequest request) {
        boolean active = request.getIsActive() == null ? true : request.getIsActive();

        ServingStyle servingStyle = ServingStyle.builder()
                .styleName(request.getStyleName())
                .description(request.getDescription())
                .extraPrice(request.getExtraPrice())
                .isActive(active)
                .build();

        ServingStyle saved = servingStyleRepository.save(servingStyle);
        return ServingStyleResponseDto.from(saved);
    }

    @Transactional
    public List<ServingStyleResponseDto> getAllServingStyles() {
        return servingStyleRepository.findAll().stream()
                .map(ServingStyleResponseDto::from)
                .collect(Collectors.toList());
    }
}
