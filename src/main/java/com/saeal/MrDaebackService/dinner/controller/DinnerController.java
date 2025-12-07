package com.saeal.MrDaebackService.dinner.controller;

import com.saeal.MrDaebackService.dinner.dto.response.DinnerResponseDto;
import com.saeal.MrDaebackService.dinner.dto.response.DinnerMenuItemResponseDto;
import com.saeal.MrDaebackService.dinner.service.DinnerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/dinners")
@Tag(name = "Dinner API", description = "Dinner 관련 API 입니다.")
public class DinnerController {

    private final DinnerService dinnerService;

    @GetMapping("/getAllDinners")
    @Operation(summary = "Dinner 리스트 반환", description = "Dinner들의 종류를 반환")
    public ResponseEntity<List<DinnerResponseDto>> getAllDinners() {
        List<DinnerResponseDto> response = dinnerService.getAllDinners();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{dinnerId}/default-menu-items")
    @Operation(summary = "Dinner의 기본 MenuItem 리스트 반환", description = "Dinner에 매핑된 MenuItem 목록을 반환합니다. Dinner 선택 시 어떠한 menuItem들이 들어가있는지 반환하기 위한 목적입니다.")
    public ResponseEntity<List<DinnerMenuItemResponseDto>> getDefaultMenuItems(
            @PathVariable UUID dinnerId
    ) {
        List<DinnerMenuItemResponseDto> response = dinnerService.getDefaultMenuItemsByDinner(dinnerId);
        return ResponseEntity.ok(response);
    }
}
