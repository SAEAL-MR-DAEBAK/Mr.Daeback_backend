package com.saeal.MrDaebackService.dinner.controller;

import com.saeal.MrDaebackService.dinner.dto.request.CreateDinnerMenuItemRequest;
import com.saeal.MrDaebackService.dinner.dto.request.CreateDinnerRequest;
import com.saeal.MrDaebackService.dinner.dto.response.DinnerResponseDto;
import com.saeal.MrDaebackService.dinner.dto.response.DinnerMenuItemResponseDto;
import com.saeal.MrDaebackService.dinner.service.DinnerService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dinners")
@Tag(name = "Dinner API", description = "Dinner 관련 API 입니다.")
public class DinnerController {

    private final DinnerService dinnerService;

    @PostMapping("/createDinner")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DinnerResponseDto> createDinner(@Valid @RequestBody CreateDinnerRequest request) {
        DinnerResponseDto response = dinnerService.createDinner(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DinnerResponseDto>> getAllDinners() {
        List<DinnerResponseDto> response = dinnerService.getAllDinners();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/menu-items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DinnerMenuItemResponseDto> createDinnerMenuItem(
            @Valid @RequestBody CreateDinnerMenuItemRequest request
    ) {
        DinnerMenuItemResponseDto response = dinnerService.createDinnerMenuItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{dinnerId}/default-menu-items")
    public ResponseEntity<List<DinnerMenuItemResponseDto>> getDefaultMenuItems(
            @PathVariable UUID dinnerId
    ) {
        List<DinnerMenuItemResponseDto> response = dinnerService.getDefaultMenuItemsByDinner(dinnerId);
        return ResponseEntity.ok(response);
    }
}
