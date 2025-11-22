package com.saeal.MrDaebackService.servingStyle.controller;

import com.saeal.MrDaebackService.servingStyle.dto.request.CreateServingStyleRequest;
import com.saeal.MrDaebackService.servingStyle.dto.response.ServingStyleResponseDto;
import com.saeal.MrDaebackService.servingStyle.service.ServingStyleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/serving-styles")
@Tag(name = "Serving Style API", description = "Serving Style 관련 API 입니다.")
public class ServingStyleController {

    private final ServingStyleService servingStyleService;

    @PostMapping("/createServingStyle")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServingStyleResponseDto> createServingStyle(
            @Valid @RequestBody CreateServingStyleRequest request
    ) {
        ServingStyleResponseDto response = servingStyleService.createServingStyle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
