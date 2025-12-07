package com.saeal.MrDaebackService.servingStyle.controller;

import com.saeal.MrDaebackService.servingStyle.dto.request.CreateServingStyleRequest;
import com.saeal.MrDaebackService.servingStyle.dto.response.ServingStyleResponseDto;
import com.saeal.MrDaebackService.servingStyle.service.ServingStyleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/serving-styles")
@Tag(name = "Serving Style API", description = "Serving Style 관련 API 입니다.")
public class ServingStyleController {

    private final ServingStyleService servingStyleService;

    @GetMapping("/getAllServingStyles")
    @Operation(summary = "Serving Style 리스트 반환", description = "Serving Style들의 종류를 반환합니다.")
    public ResponseEntity<List<ServingStyleResponseDto>> getAllServingStyles() {
        List<ServingStyleResponseDto> response = servingStyleService.getAllServingStyles();
        return ResponseEntity.ok(response);
    }
}
