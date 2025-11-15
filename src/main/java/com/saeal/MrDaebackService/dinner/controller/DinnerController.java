package com.saeal.MrDaebackService.dinner.controller;

import com.saeal.MrDaebackService.dinner.dto.request.CreateDinnerRequest;
import com.saeal.MrDaebackService.dinner.dto.response.DinnerResponseDto;
import com.saeal.MrDaebackService.dinner.service.DinnerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dinners")
@Tag(name = "Dinner API", description = "Dinner 관련 API 입니다.")
public class DinnerController {

    private final DinnerService dinnerService;

    @PostMapping("/createDinner")
    public ResponseEntity<DinnerResponseDto> createDinner(@Valid @RequestBody CreateDinnerRequest request) {
        DinnerResponseDto response = dinnerService.createDinner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
